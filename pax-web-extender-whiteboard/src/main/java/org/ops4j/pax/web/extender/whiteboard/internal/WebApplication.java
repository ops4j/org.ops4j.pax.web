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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.internal.element.FilterWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ListenerWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ResourceWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ServletWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WelcomeFileWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.extender.whiteboard.internal.util.tracker.ReplaceableService;
import org.ops4j.pax.web.extender.whiteboard.internal.util.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardElement;
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
	 * An extended version of the HttpServiceRuntime which caches registered
	 * {@link WhiteboardElement}s
	 */
	private final ExtendedHttpServiceRuntime httpServiceRuntime;

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
	private WebContainer webContainer;
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
	public WebApplication(Bundle bundle, String httpContextId, Boolean sharedHttpContext,
						  ExtendedHttpServiceRuntime httpServiceRuntime) {
		this.bundle = bundle;
		this.httpContextId = httpContextId;
		this.sharedHttpContext = sharedHttpContext;
		this.webElements = new CopyOnWriteArrayList<>();
		this.httpServiceLock = new ReentrantReadWriteLock();
		this.httpServiceTracker = new ReplaceableService<>(bundle.getBundleContext(), HttpService.class, this);
		this.httpServiceRuntime = httpServiceRuntime;
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
		httpServiceLock.writeLock().lock();
		try {
			//check if servlets and such are already registered while this is a ServletContextListener
			if (webElement instanceof ListenerWebElement) {
				LOG.debug("registering a ListenerWebElement");
				List<WebElement> stoppableElements = webElements.stream()
						.filter(element -> !(element instanceof ListenerWebElement))
						.filter(element -> !(element instanceof ResourceWebElement))
						.collect(Collectors.toList());
				stoppableElements.forEach(element -> {
					LOG.debug("unregistering element {}", element);
					unregisterWebElement(element);
				});
				LOG.debug("registering weblement:{}", webElement);
				registerWebElement(webElement);
				//first register all ServletWebElements
				LOG.debug("registering servlet elements again");
				stoppableElements.stream().filter(elem -> (elem instanceof ServletWebElement)).forEach(this::registerWebElement);
				//second register all filters
				LOG.debug("registering filters again");
				stoppableElements.stream().filter(elem -> (elem instanceof FilterWebElement)).forEach(this::registerWebElement);
				//the leftovers ...
				LOG.debug("registering the others");
				stoppableElements.stream().filter(elem -> !(elem instanceof ServletWebElement || elem instanceof FilterWebElement)).forEach(this::registerWebElement);
			} else if (webElement instanceof ServletWebElement) {
				//find all previous registered filters deregister those and go again
				List<WebElement> filterWebElements = webElements.stream().filter(elem -> (elem instanceof FilterWebElement)).collect(Collectors.toList());
				LOG.debug("de-registering {} servlet filters", filterWebElements.size());
				filterWebElements.forEach(this::unregisterWebElement);

				List<WebElement> welcomeFileMappings = webElements.stream().filter(elem -> (elem instanceof WelcomeFileWebElement)).collect(Collectors.toList());
				LOG.debug("de-registering {} welcomefilemappings", welcomeFileMappings.size());
				welcomeFileMappings.forEach(this::unregisterWebElement);
				
				LOG.debug("registering weblement:{}", webElement);
				registerWebElement(webElement);
				
				LOG.debug("registering filters again");
				filterWebElements.forEach(this::registerWebElement);
				LOG.debug("filters registerd again");
				
				LOG.debug("registering welcomefiles again");
				welcomeFileMappings.forEach(this::registerWebElement);
				LOG.debug("registered welcomeFiles again");
			} else {
				LOG.debug("registering weblement:{}", webElement);
				registerWebElement(webElement);
			}
		} finally {
			webElements.add(webElement);
			httpServiceLock.writeLock().unlock();
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
			httpServiceRuntime.removeWhiteboardElement(webElement);
			httpServiceLock.readLock().unlock();
		}
		return empty;
	}

	@Override
	public void serviceChanged(HttpService oldService, HttpService newService, Map<String, Object> serviceProperties) {
		if (newService != null && !WebContainerUtils.isWebContainer(newService)) {
			throw new IllegalStateException("HttpService must be implementing Pax-Web WebContainer!");
		}
		httpServiceLock.writeLock().lock();
		try {
			unregisterWebElements();
			webContainer = (WebContainer)newService;
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
			httpServiceRuntime.stop();
			httpContext = null;
		}
	}

	private void registerHttpContext() {
		if (httpContextMapping != null && webContainer != null) {
			getHttpContext();
			if (WebContainerUtils.isWebContainer(webContainer)) {
				final Map<String, String> contextparams = new HashMap<>();
				if (httpContextMapping.getContextPath() != null) {
					contextparams.put(PaxWebConstants.CONTEXT_NAME,
							httpContextMapping.getContextPath());
				}
				if (httpContextMapping.getInitParameters() != null) {
					contextparams.putAll(httpContextMapping.getInitParameters());
					String virtualHosts = contextparams.remove(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS);
					List<String> virtualHostsList = convertToList(virtualHosts);
					String connectors = contextparams.remove(ExtenderConstants.PROPERTY_HTTP_CONNECTORS);
					List<String> connectorsList = convertToList(connectors);
//					webContainer.setConnectorsAndVirtualHosts(connectorsList, virtualHostsList, httpContext);
				}
//				webContainer.setContextParam(
//						DictionaryUtils.adapt(contextparams), httpContext);
			}
			registerWebElements();
		}
	}

	private void getHttpContext() {
		httpContext = httpContextMapping.getHttpContext();
		if (httpContext == null) {
			if (servletContextHelper != null) {
				httpContext = new WebContainerContext() {
					@Override
					public Set<String> getResourcePaths(String name) {
						return null;
					}

					@Override
					public String getContextId() {
						return httpContextId;
					}

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
				if (httpContextMapping != null && httpContextMapping.getInitParameters() != null) {
					sharedContext = httpContextMapping.getInitParameters().get(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED);
				}

				if (Boolean.parseBoolean(sharedContext) && WebContainerUtils.isWebContainer(webContainer)) {
					//PAXWEB-660
					httpContext = webContainer.createDefaultSharedHttpContext();
				} else if (httpContextId != null && WebContainerUtils.isWebContainer(webContainer)) {
					httpContext = webContainer.createDefaultHttpContext(httpContextId);
				} else {
					//default
					httpContext = webContainer.createDefaultHttpContext();
				}
			}
		} else if (!(httpContext instanceof WebContainerContext)) {
			// wrap registered HttpContext in pax-web specific context
			final HttpContext localHttpContext = httpContext;
			httpContext = new WebContainerContext() {
				@Override
				public Set<String> getResourcePaths(String name) {
					// FIXME check if this is valid for plain HttpContext-registrations
					return null;
				}

				@Override
				public String getContextId() {
					return httpContextId;
				}

				@Override
				public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
					return localHttpContext.handleSecurity(request, response);
				}

				@Override
				public URL getResource(String name) {
					return localHttpContext.getResource(name);
				}

				@Override
				public String getMimeType(String name) {
					return localHttpContext.getMimeType(name);
				}
			};
		}
	}

	private void registerWebElements() {
		httpServiceLock.readLock().lock();
		try {
			if (webContainer != null && httpContext != null) {
				for (WebElement registerer : webElements) {
					registerWebElement(registerer);
				}
			}
		} finally {
			httpServiceLock.readLock().unlock();
		}
	}

	private List<String> convertToList(String elementListAsString) {
		List<String> elementList = new LinkedList<>();
		if (elementListAsString != null) {
			String[] elementArray = elementListAsString.split(",");
			elementList = Arrays.stream(elementArray).map(String::trim).collect(Collectors.toList());
		}
		return elementList;
	}

	private void registerWebElement(final WebElement registerer) {
		//CHECKSTYLE:OFF
		try {
			if (webContainer != null && httpContext != null && registerer.isValid()) {
				registerer.register(webContainer, httpContext);
			}
		} catch (Exception ignore) {
			LOG.error("Registration skipped for [" + registerer
					+ "] due to error during registration", ignore);
		} finally {
			httpServiceRuntime.addWhiteboardElement(registerer);
		}
		//CHECKSTYLE:ON
	}

	private void unregisterWebElements() {
		httpServiceLock.readLock().lock();
		try {
			if (webContainer != null && httpContext != null) {
				webElements.forEach(this::unregisterWebElement);
			}
		} finally {
			webElements.forEach(httpServiceRuntime::removeWhiteboardElement);
			httpServiceLock.readLock().unlock();
		}
	}

	private void unregisterWebElement(final WebElement registerer) {
		if (webContainer != null && httpContext != null && registerer.isValid()) {
			registerer.unregister(webContainer, httpContext);
		}
	}

	public void setServletContextHelper(final ServletContextHelper servletContextHelper, final HttpContextMapping httpContextMapping) {
		httpServiceLock.writeLock().lock();
		try {
			if (hasHttpContextMapping()) {
				unregisterHttpContext();
			}
			this.servletContextHelper = servletContextHelper;
			this.httpContextMapping = httpContextMapping;
			registerHttpContext();
		} finally {
			httpServiceLock.writeLock().unlock();
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{mapping=" + httpContextMapping + "}";
	}

}
