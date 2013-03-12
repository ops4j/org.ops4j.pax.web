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
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;
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
public class WebApplication implements HttpServiceListener {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(WebApplication.class);

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
	public WebApplication() {
		webElements = new ArrayList<WebElement>();
		httpServiceLock = new ReentrantReadWriteLock();
		webElementsLock = new ReentrantReadWriteLock();
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

	public void removeWebElement(final WebElement webElement) {
		NullArgumentException.validateNotNull(webElement, "Registerer");
		webElementsLock.writeLock().lock();
		try {
			webElements.remove(webElement);
		} finally {
			webElementsLock.writeLock().unlock();
		}
		httpServiceLock.readLock().lock();
		try {
			unregisterWebElement(webElement);
		} finally {
			httpServiceLock.readLock().unlock();
		}
	}

	public void available(final HttpService httpServiceIn) throws Exception {
		NullArgumentException.validateNotNull(httpServiceIn, "Http service");
		httpServiceLock.writeLock().lock();
		try {
			if (httpService != null) {
				unavailable(httpService);
			}
			httpService = httpServiceIn;
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
				httpContext = httpService.createDefaultHttpContext();
			}
			if (WebContainerUtils.isWebContainer(httpService)) {
				final Map<String, String> contextparams = new HashMap<String, String>();
				if (httpContextMapping.getPath() != null) {
					contextparams.put(WebContainerConstants.CONTEXT_NAME,
							httpContextMapping.getPath());
				}
				if (httpContextMapping.getParameters() != null) {
					Map<String, String> contextParams = httpContextMapping
							.getParameters();
					String virtualHosts = contextParams
							.get(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS);
					if (virtualHosts != null) {
						((WebContainer) httpService).setVirtualHosts(
								convertToList(virtualHosts), httpContext);
					}
					String connectors = contextParams
							.get(ExtenderConstants.PROPERTY_HTTP_CONNECTORS);
					if (connectors != null) {
						((WebContainer) httpService).setConnectors(
								convertToList(connectors), httpContext);
					}
					contextparams.putAll(httpContextMapping.getParameters());
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
		try {
			if (httpService != null && httpContext != null) {
				registerer.register(httpService, httpContext);
			}
		} catch (Exception ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration skipped for [" + registerer
					+ "] due to error during registration", ignore);
		}
	}

	public void unavailable(final HttpService httpServiceIn) {
		NullArgumentException.validateNotNull(httpServiceIn, "Http service");
		httpServiceLock.writeLock().lock();
		try {
			if (httpServiceIn != httpService) {
				throw new IllegalStateException("Unavailable http service ["
						+ httpServiceIn
						+ "] is not equal with prior available http service ["
						+ httpService + "]");
			}
			unregisterWebElements();
			httpService = null;
			httpContext = null;
		} finally {
			httpServiceLock.writeLock().unlock();
		}
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
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("mapping=").append(httpContextMapping)
				.append("}").toString();
	}

}
