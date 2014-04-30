/*
 * Copyright 2007 Damian Golda.
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.DictionaryUtils;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.extender.whiteboard.internal.util.tracker.ReplaceableService;
import org.ops4j.pax.web.extender.whiteboard.internal.util.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add JavaDoc
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class WebApplication implements ReplaceableServiceListener<HttpService> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(WebApplication.class);

    private final Bundle bundle;
    private final String httpContextId;
    private final Boolean sharedHttpContext;
	/**
	 * List of web elements that makes up this context.
	 */
	private final List<WebElement> webElements;
	/**
	 * Registerers lock.
	 */
	private final ReadWriteLock webElementsLock;
	/**
	 * Http service lock.
	 */
	private final ReadWriteLock httpServiceLock;

	/**
	 * Current http context mapping.
	 */
	private HttpContextMapping httpContextMapping;
    /**
     * Http service tracker
     */
    private ReplaceableService<HttpService> httpServiceTracker;
	/**
	 * Active http service;
	 */
	private HttpService httpService;
	/**
	 * Provided or created http context.
	 */
	private HttpContext httpContext;

	/**
	 * Constructor.
	 */
	public WebApplication(Bundle bundle, String httpContextId, Boolean sharedHttpContext) {
        this.bundle = bundle;
        this.httpContextId = httpContextId;
        this.sharedHttpContext = sharedHttpContext;
		webElements = new ArrayList<WebElement>();
		httpServiceLock = new ReentrantReadWriteLock();
		webElementsLock = new ReentrantReadWriteLock();
        httpServiceTracker = new ReplaceableService<HttpService>(bundle.getBundleContext(), HttpService.class, this);
	}

    public Bundle getBundle() {
        return bundle;
    }

    public String getHttpContextId() {
        return httpContextId;
    }

    public Boolean getSharedHttpContext() {
        return sharedHttpContext;
    }

    public void start() {
        httpServiceTracker.start();
    }

    public void stop() {
        httpServiceTracker.stop();
    }

    public void addWebElement(final WebElement webElement) {
		NullArgumentException.validateNotNull(webElement, "Registerer");
		// FIX for PAXWEB-485 changing order of registration.
		httpServiceLock.readLock().lock();
		try {
			registerWebElement(webElement);
		} finally {
			httpServiceLock.readLock().unlock();
		}
		webElementsLock.writeLock().lock();
		try {
			webElements.add(webElement);
		} finally {
			webElementsLock.writeLock().unlock();
		}
	}

	public boolean removeWebElement(final WebElement webElement) {
        boolean empty;
		NullArgumentException.validateNotNull(webElement, "Registerer");
		webElementsLock.writeLock().lock();
		try {
			webElements.remove(webElement);
            empty = webElements.isEmpty();
		} finally {
			webElementsLock.writeLock().unlock();
		}
		httpServiceLock.readLock().lock();
		try {
			unregisterWebElement(webElement);
		} finally {
			httpServiceLock.readLock().unlock();
		}
        return empty;
	}

    @Override
    public void serviceChanged(HttpService oldService, HttpService newService) {
        httpServiceLock.writeLock().lock();
        try {
            unregisterWebElements();
            httpService = newService;
            httpContext = null;
            registerHttpContext();
        } finally {
            httpServiceLock.writeLock().unlock();
        }
    }

    public boolean hasHttpContextMapping() {
		return httpContextMapping != null;
	}

	public void setHttpContextMapping(
			final HttpContextMapping httpContextMapping) {
		httpServiceLock.writeLock().lock();
		try {
			if (hasHttpContextMapping()) {
				unregisterHttpContext();
			}
			this.httpContextMapping = httpContextMapping;
			registerHttpContext();
		} finally {
			httpServiceLock.writeLock().unlock();
		}
	}

	private void unregisterHttpContext() {
		if (httpContext != null) {
			unregisterWebElements();
			httpContext = null;
		}
	}

	private void registerHttpContext() {
		if (httpContextMapping != null && httpService != null) {
			httpContext = httpContextMapping.getHttpContext();
			if (httpContext == null) {
				String sharedContext = null;
				if (httpContextMapping != null && httpContextMapping.getParameters() != null) {
					sharedContext = httpContextMapping.getParameters().get(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED);
				}
				
				if (null != sharedContext && Boolean.parseBoolean(sharedContext) && WebContainerUtils.isWebContainer(httpService)) {
					httpContext = ((WebContainer) httpService).createDefaultSharedHttpContext(); //PAXWEB-660
				} else if (httpContextId != null && WebContainerUtils.isWebContainer(httpService)) {
					httpContext = ((WebContainer) httpService).createDefaultHttpContext(httpContextId);
				} else {
					//default
					httpContext = httpService.createDefaultHttpContext();
				}
			}
			if (WebContainerUtils.isWebContainer(httpService)) {
				final Map<String, String> contextparams = new HashMap<String, String>();
				if (httpContextMapping.getPath() != null) {
					contextparams.put(WebContainerConstants.CONTEXT_NAME,
							httpContextMapping.getPath());
				}
				if (httpContextMapping.getParameters() != null) {
					contextparams.putAll(httpContextMapping.getParameters());
					String virtualHosts = contextparams
							.remove(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS);
					List<String> virtualHostsList = null;
					if (virtualHosts != null) {
						virtualHostsList = convertToList(virtualHosts);
					} else {
						virtualHostsList = new LinkedList<String>();
					}
					String connectors = contextparams
							.remove(ExtenderConstants.PROPERTY_HTTP_CONNECTORS);
					List<String> connectorsList = null;
					if (connectors != null) {
						connectorsList = convertToList(connectors);
					} else {
						connectorsList = new LinkedList<String>();
					}
					((WebContainer) httpService).setConnectorsAndVirtualHosts(connectorsList, virtualHostsList, httpContext);
				}
				((WebContainer) httpService).setContextParam(
						DictionaryUtils.adapt(contextparams), httpContext);
			}
			registerWebElements();
		}
	}

	private void registerWebElements() {
		webElementsLock.readLock().lock();
		try {
			if (httpService != null && httpContext != null) {
				for (WebElement registerer : webElements) {
					registerWebElement(registerer);
				}
			}
		} finally {
			webElementsLock.readLock().unlock();
		}
	}

	private List<String> convertToList(String elementListAsString) {
		List<String> elementList = new LinkedList<String>();
		if ((elementListAsString != null) && (elementListAsString.length() > 0)) {
			String[] elementArray = elementListAsString.split(",");
			for (String element : elementArray) {
				elementList.add(element.trim());
			}
		}
		return elementList;
	}

	private void registerWebElement(final WebElement registerer) {
		//CHECKSTYLE:OFF
		try {
			if (httpService != null && httpContext != null) {
				registerer.register(httpService, httpContext);
			}
		} catch (Exception ignore) {
			LOG.error("Registration skipped for [" + registerer
					+ "] due to error during registration", ignore);
		}
		//CHECKSTYLE:ON
	}

	private void unregisterWebElements() {
		webElementsLock.readLock().lock();
		try {
			if (httpService != null && httpContext != null) {
				for (WebElement registerer : webElements) {
					unregisterWebElement(registerer);
				}
			}
		} finally {
			webElementsLock.readLock().unlock();
		}
	}

	private void unregisterWebElement(final WebElement registerer) {
		if (httpService != null && httpContext != null) {
			registerer.unregister(httpService, httpContext);
		}
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("mapping=").append(httpContextMapping)
				.append("}").toString();
	}

}
