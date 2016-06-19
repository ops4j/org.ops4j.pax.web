/*
 * Copyright 2007 Damian Golda.
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2010 Achim Nierbeck
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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.element.FilterWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ListenerWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ResourceWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ServletWebElement;
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
import org.osgi.service.http.context.ServletContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a whiteboard instance of a webapplication, implements a service listener for HttpService.
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
	 * ServletContextHelper
	 */
    private ServletContextHelper servletContextHelper;

	/**
	 * Constructor.
	 */
	public WebApplication(Bundle bundle, String httpContextId, Boolean sharedHttpContext) {
        this.bundle = bundle;
        this.httpContextId = httpContextId;
        this.sharedHttpContext = sharedHttpContext;
		webElements = new CopyOnWriteArrayList<WebElement>();
		httpServiceLock = new ReentrantReadWriteLock();
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
		    //check if servlets and such are already registered while this is a ServletContextListener
		    if (webElement instanceof ListenerWebElement) {
                LOG.debug("registering a ListenerWebElement");
		        List<WebElement> stoppableElements = webElements.stream()
		            .filter(element -> !(element instanceof ListenerWebElement))
		            .filter(element -> !(element instanceof ResourceWebElement))
		            .collect(Collectors.toList());
		        stoppableElements.forEach(element -> {
		                LOG.info("unregistering element {}", element);
		                unregisterWebElement(element);
		            });
		        LOG.debug("registering weblement:{}", webElement );
	            registerWebElement(webElement);
	            //first register all ServletWebElements
	            LOG.debug("registering servlet elements again");
	            stoppableElements.stream().filter(elem -> (elem instanceof ServletWebElement)).forEach(elem -> registerWebElement(elem));
	            //second register all filters
	            LOG.debug("registering filters again");
	            stoppableElements.stream().filter(elem -> (elem instanceof FilterWebElement)).forEach(elem -> registerWebElement(elem));
	            //the leftovers ... 
	            LOG.debug("registering the others");
	            stoppableElements.stream().filter(elem-> !(elem instanceof ServletWebElement || elem instanceof FilterWebElement)).forEach(element -> registerWebElement(element));
		    } else if (webElement instanceof ServletWebElement) {
		        //find all previous registered filters deregister those and go again
		        List<WebElement> filterWebElements = webElements.stream().filter(elem -> (elem instanceof FilterWebElement)).collect(Collectors.toList());
		        LOG.debug("de-registering {} servlet filters",filterWebElements.size());
		        filterWebElements.stream().forEach(elem -> unregisterWebElement(elem));
                LOG.debug("registering weblement:{}", webElement );
		        registerWebElement(webElement);
		        LOG.debug("registering filters again");
		        filterWebElements.stream().forEach(elem -> registerWebElement(elem));
		        LOG.debug("filters registerd again");
		    } else {
                LOG.debug("registering weblement:{}", webElement );
		        registerWebElement(webElement);
		    }
		} finally {
			webElements.add(webElement);
			httpServiceLock.readLock().unlock();
		}
	}

	public boolean removeWebElement(final WebElement webElement) {
        boolean empty;
		NullArgumentException.validateNotNull(webElement, "Registerer");
		httpServiceLock.readLock().lock();
		try {
			webElements.remove(webElement);
            empty = webElements.isEmpty();
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
			getHttpContext();
			if (WebContainerUtils.isWebContainer(httpService)) {
				final Map<String, String> contextparams = new HashMap<String, String>();
				if (httpContextMapping.getPath() != null) {
					contextparams.put(WebContainerConstants.CONTEXT_NAME,
							httpContextMapping.getPath());
				}
				if (httpContextMapping.getParameters() != null) {
					contextparams.putAll(httpContextMapping.getParameters());
					Optional<String> virtualHosts = Optional.ofNullable(contextparams.remove(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS));
					List<String> virtualHostsList = convertToList(virtualHosts);
					Optional<String> connectors = Optional.ofNullable(contextparams.remove(ExtenderConstants.PROPERTY_HTTP_CONNECTORS));
					List<String> connectorsList = convertToList(connectors);
					((WebContainer) httpService).setConnectorsAndVirtualHosts(connectorsList, virtualHostsList, httpContext);
				}
				((WebContainer) httpService).setContextParam(
						DictionaryUtils.adapt(contextparams), httpContext);
			}
			registerWebElements();
		}
	}

	private void getHttpContext() {
		httpContext = httpContextMapping.getHttpContext();
		if (httpContext == null) {
			if (servletContextHelper != null) {
				httpContext = new HttpContext() {
					@Override
					public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
						return servletContextHelper.handleSecurity(request, response);
					}
					@Override
					public URL getResource(String name) {
						return servletContextHelper.getResource(name);
					}
					@Override
					public String getMimeType(String name) {
						return servletContextHelper.getMimeType(name);
					}
				};
			} else {
				String sharedContext = null;
				if (httpContextMapping != null && httpContextMapping.getParameters() != null) {
					sharedContext = httpContextMapping.getParameters().get(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED);
				}

				if (null != sharedContext && Boolean.parseBoolean(sharedContext) && WebContainerUtils.isWebContainer(httpService)) {
					//PAXWEB-660
					httpContext = ((WebContainer) httpService).createDefaultSharedHttpContext();
				} else if (httpContextId != null && WebContainerUtils.isWebContainer(httpService)) {
					httpContext = ((WebContainer) httpService).createDefaultHttpContext(httpContextId);
				} else {
					//default
					httpContext = httpService.createDefaultHttpContext();
				}
			}
		}
	}

	private void registerWebElements() {
		httpServiceLock.readLock().lock();
		try {
			if (httpService != null && httpContext != null) {
				for (WebElement registerer : webElements) {
					registerWebElement(registerer);
				}
			}
		} finally {
			httpServiceLock.readLock().unlock();
		}
	}

	private List<String> convertToList(Optional<String> elementListAsString) {
		List<String> elementList = new LinkedList<String>();
		if (elementListAsString.isPresent()) {
		    String[] elementArray = elementListAsString.get().split(",");
		    elementList = Arrays.asList(elementArray).stream().map(elem -> elem.trim()).collect(Collectors.toList());
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
		httpServiceLock.readLock().lock();
		try {
			if (httpService != null && httpContext != null) {
			    webElements.stream().forEach(webElement -> unregisterWebElement(webElement));
			}
		} finally {
			httpServiceLock.readLock().unlock();
		}
	}

	private void unregisterWebElement(final WebElement registerer) {
		if (httpService != null && httpContext != null) {
			registerer.unregister(httpService, httpContext);
		}
	}

    public void setServletContextHelper(final ServletContextHelper servletContextHelper) {
        httpServiceLock.writeLock().lock();
        try {
            if (hasHttpContextMapping()) {
                unregisterHttpContext();
            }
            this.servletContextHelper = servletContextHelper;
            registerHttpContext();
        } finally {
            httpServiceLock.writeLock().unlock();
        }
    }

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("mapping=").append(httpContextMapping)
				.append("}").toString();
	}

}
