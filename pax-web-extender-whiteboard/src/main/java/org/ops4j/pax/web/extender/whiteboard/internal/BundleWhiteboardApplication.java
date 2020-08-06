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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Before Pax Web 8, this class was representing a bundle-scoped <em>web application</em> and was called
 * {@code WebApplication}. However in Pax Web 8, it's just a holder of Whiteboard elements registered from single
 * {@link Bundle}, while the <em>target web application</em> may actually contain <em>web elements</em> registered
 * by many different bundles. Also, single bundle may register Whiteboard elements for many <em>web
 * applications.</em></p>
 *
 * <p>Actual trackers (tracker customizers) add elements/contexts to this application via {@link ExtenderContext}
 * <strong>only</strong> - because all Whiteboard elements have to be tracked in
 * {@link org.osgi.service.http.runtime.HttpServiceRuntime}, which is kind of <em>Whiteboard registry</em>.</p>
 *
 * <p>Also, before Pax Web 8, this class was calling {@link WebContainer} registration methods, while now, interaction
 * with {@link WebContainer} happens at {@link ExtenderContext} level.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public class BundleWhiteboardApplication {

	private static final Logger LOG = LoggerFactory.getLogger(BundleWhiteboardApplication.class);

	private final Bundle bundle;

	/** List of <em>web elements</em> that are registered by given {@link Bundle}. */
	private final List<ElementModel<?, ?>> webElements;

	/** List of <em>web contexts</em> that are registered by given {@link Bundle}. */
	private final List<OsgiContextModel> webContexts;

	/**
	 * <p>pax-web-extender-whiteboard operates on target {@link WebContainer} and special
	 * {@link org.ops4j.pax.web.service.views.PaxWebContainerView} it provides for Whiteboard purposes.</p>
	 *
	 * <p>At any given time, we operate on single target {@link WebContainer} and when
	 * new one is registered, we simply <em>move</em> current Whiteboard applications to new target runtime.
	 * This view is kept at {@link BundleWhiteboardApplication} level, because it's bundle scoped.</p>
	 */
	private volatile WhiteboardWebContainerView whiteboardContainer;

	/**
	 * Current {@link ServiceReference} to unget it if there's new {@link WebContainer} added without removing
	 * current one.
	 */
	private volatile ServiceReference<WebContainer> webContainerServiceRef;

	/**
	 * Constructor.
	 */
	public BundleWhiteboardApplication(Bundle bundle, ExtendedHttpServiceRuntime httpServiceRuntime) {
		this.bundle = bundle;
		this.webElements = new CopyOnWriteArrayList<>();
		this.webContexts = new CopyOnWriteArrayList<>();
	}

	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * This method returns a snapshot of current {@link OsgiContextModel} contexts registered by given {@link Bundle}.
	 * @return
	 */
	public List<OsgiContextModel> getWebContexts() {
		return Collections.unmodifiableList(webContexts);
	}

	public List<OsgiContextModel> getWebContainerOsgiContextModels() {
		List<OsgiContextModel> models = new LinkedList<>();
		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view != null) {
			models.addAll(view.getOsgiContextModels(bundle));
		}
		return models;
	}

	/**
	 * This method returns a snapshot of current {@link ElementModel} elements registered by given {@link Bundle}.
	 * @return
	 */
	public List<ElementModel<?, ?>> getWebElements() {
		return Collections.unmodifiableList(webElements);
	}

	/**
	 * When (tracked at {@link ExtenderContext} level) new {@link WebContainer} {@link ServiceReference}
	 * is added, it is passed to each {@link BundleWhiteboardApplication}, so it can (now and later)
	 * managed its own bundle-scoped {@link WhiteboardWebContainerView} to install/uninstall web
	 * elements and contexts
	 * @param ref
	 */
	public void webContainerAdded(ServiceReference<WebContainer> ref) {
		webContainerServiceRef = ref;
		WebContainer container = bundle.getBundleContext().getService(webContainerServiceRef);
		if (container != null) {
			whiteboardContainer = container.adapt(WhiteboardWebContainerView.class);
		}

		// install all current contexts and elements. Lifecycle is managed at ExtenderContext level,
		// so we don't have to care about uninstalling the contexts/elements from previous WebContainer
		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view != null) {
			webContexts.forEach(view::addWhiteboardOsgiContextModel);
			webElements.forEach(element -> element.register(view));
		}
	}

	/**
	 * {@link WebContainer} reference was untracked at {@link ExtenderContext} level.
	 * @param ref
	 */
	public void webContainerRemoved(ServiceReference<WebContainer> ref) {
		// uninstall all current contexts and elements
		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view != null) {
			webElements.forEach(element -> element.unregister(view));
			webContexts.forEach(view::removeWhiteboardOsgiContextModel);
		}

		bundle.getBundleContext().ungetService(webContainerServiceRef);
		webContainerServiceRef = null;
		whiteboardContainer = null;
	}

	/**
	 * Associates given customized <em>context</em> in the form of {@link OsgiContextModel} with current
	 * <em>bundle web application</em> and if the {@link WebContainer} is already available, register it there.
	 * @param webContext
	 */
	public void addWebContext(final OsgiContextModel webContext) {
		webContexts.add(webContext);
		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view != null) {
			view.addWhiteboardOsgiContextModel(webContext);
		}
	}

	/**
	 * Deassociates given customized <em>context</em> from current <em>bundle web application</em> and unregisters
	 * it from {@link WebContainer}.
	 * @param webContext
	 */
	public void removeWebContext(final OsgiContextModel webContext) {
		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view != null) {
			view.removeWhiteboardOsgiContextModel(webContext);
		}
		webContexts.remove(webContext);
	}

	/**
	 * Adds an {@link ElementModel} as the one associated with given {@link Bundle}
	 * @param webElement
	 */
	public void addWebElement(final ElementModel<?, ?> webElement) {
		webElements.add(webElement);

		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view == null) {
			LOG.debug("{} will be registered when WebContainer/HttpService is available", webElement);
			return;
		}

		webElement.register(view);

		// TOCHECK: Is it a good place to think about "transactions"?

				//		boolean empty;
				//		NullArgumentException.validateNotNull(webElement, "Registerer");
				//		httpServiceLock.readLock().lock();
				//		try {
				//			webElements.remove(webElement);
				//			empty = webElements.isEmpty();
				//			unregisterWebElement(webElement);
				//		} finally {
				//			httpServiceRuntime.removeWhiteboardElement(webElement);
				//			httpServiceLock.readLock().unlock();
				//		}
				//		return empty;

				//		NullArgumentException.validateNotNull(webElement, "Registerer");
				//		// FIX for PAXWEB-485 changing order of registration.
				//		httpServiceLock.writeLock().lock();
				//		try {
				//			//check if servlets and such are already registered while this is a ServletContextListener
				//			if (webElement instanceof ListenerWebElement) {
				//				LOG.debug("registering a ListenerWebElement");
				//				List<WebElement> stoppableElements = webElements.stream()
				//						.filter(element -> !(element instanceof ListenerWebElement))
				//						.filter(element -> !(element instanceof ResourceWebElement))
				//						.collect(Collectors.toList());
				//				stoppableElements.forEach(element -> {
				//					LOG.debug("unregistering element {}", element);
				//					unregisterWebElement(element);
				//				});
				//				LOG.debug("registering weblement:{}", webElement);
				//				registerWebElement(webElement);
				//				//first register all ServletWebElements
				//				LOG.debug("registering servlet elements again");
				//				stoppableElements.stream().filter(elem -> (elem instanceof ServletWebElement)).forEach(this::registerWebElement);
				//				//second register all filters
				//				LOG.debug("registering filters again");
				//				stoppableElements.stream().filter(elem -> (elem instanceof FilterWebElement)).forEach(this::registerWebElement);
				//				//the leftovers ...
				//				LOG.debug("registering the others");
				//				stoppableElements.stream().filter(elem -> !(elem instanceof ServletWebElement || elem instanceof FilterWebElement)).forEach(this::registerWebElement);
				//			} else if (webElement instanceof ServletWebElement) {
				//				//find all previous registered filters deregister those and go again
				//				List<WebElement> filterWebElements = webElements.stream().filter(elem -> (elem instanceof FilterWebElement)).collect(Collectors.toList());
				//				LOG.debug("de-registering {} servlet filters", filterWebElements.size());
				//				filterWebElements.forEach(this::unregisterWebElement);
				//
				//				List<WebElement> welcomeFileMappings = webElements.stream().filter(elem -> (elem instanceof WelcomeFileWebElement)).collect(Collectors.toList());
				//				LOG.debug("de-registering {} welcomefilemappings", welcomeFileMappings.size());
				//				welcomeFileMappings.forEach(this::unregisterWebElement);
				//
				//				LOG.debug("registering weblement:{}", webElement);
				//				registerWebElement(webElement);
				//
				//				LOG.debug("registering filters again");
				//				filterWebElements.forEach(this::registerWebElement);
				//				LOG.debug("filters registerd again");
				//
				//				LOG.debug("registering welcomefiles again");
				//				welcomeFileMappings.forEach(this::registerWebElement);
				//				LOG.debug("registered welcomeFiles again");
				//			} else {
				//				LOG.debug("registering weblement:{}", webElement);
				//				registerWebElement(webElement);
				//			}
				//		} finally {
				//			webElements.add(webElement);
				//			httpServiceLock.writeLock().unlock();
				//		}
	}

	/**
	 * Removes an {@link ElementModel} from the list of elements registered by given bundle.
	 * @param webElement
	 */
	public void removeWebElement(final ElementModel<?, ?> webElement) {
		webElements.remove(webElement);

		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view == null) {
			LOG.debug("{} will be unregistered when WebContainer/HttpService is available", webElement);
			return;
		}

		webElement.unregister(view);

		//		Boolean sharedHttpContext = ServicePropertiesUtils.extractSharedHttpContext(serviceReference);
		//
		//		final BundleWhiteboardApplication webApplication = extenderContext.getExistingWebApplication(serviceReference.getBundle(),
		//				webElement.getHttpContextId(), sharedHttpContext);
		//		boolean remove = true;
		//
		//		if (sharedHttpContext) {
		//			LOG.debug("Shared Context ... ");
		//			Integer sharedWebApplicationCounter = extenderContext.getSharedWebApplicationCounter(webApplication);
		//			LOG.debug("... counter:" + sharedWebApplicationCounter);
		//			if (sharedWebApplicationCounter != null && sharedWebApplicationCounter > 0) {
		//				remove = false;
		//				Integer reduceSharedWebApplicationCount = extenderContext
		//						.reduceSharedWebApplicationCount(webApplication);
		//				LOG.debug("reduced counter:" + reduceSharedWebApplicationCount);
		//				if (reduceSharedWebApplicationCount == 0) {
		//					remove = true;
		//				}
		//			}
		//
		//			S registered = bundleContext.getService(serviceReference);
		//			if (!remove && Servlet.class.isAssignableFrom(registered.getClass())) {
		//				// special case where the removed service is a servlet, all
		//				// other filters etc. should be stopped now too.
		//				remove = true;
		//			}
		//			LOG.debug("service can be removed: " + remove);
		//			bundleContext.ungetService(serviceReference);
		//		}
		//
		//		if (webApplication != null && remove) {
		//			if (webApplication.removeWebElement(webElement)) {
		//				extenderContext.removeWebApplication(webApplication);
		//			}
		//		}
	}

	/**
	 * <p>Method called after bundle has stopped. The only task to do here is to unget {@link WebContainer}, which
	 * (being a {@link org.osgi.framework.ServiceFactory}) should stop the underlying bundle-scoped service and
	 * unregister all the web contexts/elements.</p>
	 *
	 * <p>We don't have to iterate over our elements/contexts, because they're kept in
	 * {@link org.ops4j.pax.web.service.spi.model.ServiceModel} anyway. Also it's not our task to unregister
	 * Whiteboard {@link ServiceReference references}.</p>
	 */
	public void cleanup() {
		this.webContexts.clear();
		this.webElements.clear();
		if (webContainerServiceRef != null) {
			bundle.getBundleContext().ungetService(webContainerServiceRef);
		}
	}

	//	@Override
			//	public void serviceChanged(HttpService oldService, HttpService newService, Map<String, Object> serviceProperties) {
			//		if (newService != null && !WebContainerUtils.isWebContainer(newService)) {
			//			throw new IllegalStateException("HttpService must be implementing Pax-Web WebContainer!");
			//		}
			//		httpServiceLock.writeLock().lock();
			//		try {
			//			unregisterWebElements();
			//			webContainer = (WebContainer)newService;
			//			httpContext = null;
			//			registerHttpContext();
			//		} finally {
			//			httpServiceLock.writeLock().unlock();
			//		}
			//	}
			//
			//	public boolean hasHttpContextMapping() {
			//		return httpContextMapping != null;
			//	}
			//
			//	public void setHttpContextMapping(
			//			final HttpContextMapping httpContextMapping) {
			//		httpServiceLock.writeLock().lock();
			//		try {
			//			if (hasHttpContextMapping()) {
			//				unregisterHttpContext();
			//			}
			//			this.httpContextMapping = httpContextMapping;
			//			registerHttpContext();
			//		} finally {
			//			httpServiceLock.writeLock().unlock();
			//		}
			//	}
			//
			//	private void unregisterHttpContext() {
			//		if (httpContext != null) {
			//			unregisterWebElements();
			//			httpServiceRuntime.stop();
			//			httpContext = null;
			//		}
			//	}
			//
			//	private void registerHttpContext() {
			//		if (httpContextMapping != null && webContainer != null) {
			//			getHttpContext();
			//			if (WebContainerUtils.isWebContainer(webContainer)) {
			//				final Map<String, String> contextparams = new HashMap<>();
			//				if (httpContextMapping.getContextPath() != null) {
			//					contextparams.put(PaxWebConstants.CONTEXT_NAME,
			//							httpContextMapping.getContextPath());
			//				}
			//				if (httpContextMapping.getInitParameters() != null) {
			//					contextparams.putAll(httpContextMapping.getInitParameters());
			//					String virtualHosts = contextparams.remove(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS);
			//					List<String> virtualHostsList = convertToList(virtualHosts);
			//					String connectors = contextparams.remove(ExtenderConstants.PROPERTY_HTTP_CONNECTORS);
			//					List<String> connectorsList = convertToList(connectors);
			////					webContainer.setConnectorsAndVirtualHosts(connectorsList, virtualHostsList, httpContext);
			//				}
			////				webContainer.setContextParam(
			////						DictionaryUtils.adapt(contextparams), httpContext);
			//			}
			//			registerWebElements();
			//		}
			//	}
			//
			//	private void getHttpContext() {
			//		httpContext = httpContextMapping.getHttpContext();
			//		if (httpContext == null) {
			//			if (servletContextHelper != null) {
			//				httpContext = new WebContainerContext() {
			//					@Override
			//					public Set<String> getResourcePaths(String name) {
			//						return null;
			//					}
			//
			//					@Override
			//					public String getContextId() {
			//						return httpContextId;
			//					}
			//
			//					@Override
			//					public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
			//						return servletContextHelper.handleSecurity(request, response);
			//					}
			//
			//					@Override
			//					public URL getResource(String name) {
			//						return servletContextHelper.getResource(name);
			//					}
			//
			//					@Override
			//					public String getMimeType(String name) {
			//						return servletContextHelper.getMimeType(name);
			//					}
			//				};
			//			} else {
			//				String sharedContext = null;
			//				if (httpContextMapping != null && httpContextMapping.getInitParameters() != null) {
			//					sharedContext = httpContextMapping.getInitParameters().get(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED);
			//				}
			//
			//				if (Boolean.parseBoolean(sharedContext) && WebContainerUtils.isWebContainer(webContainer)) {
			//					//PAXWEB-660
			//					httpContext = webContainer.createDefaultSharedHttpContext();
			//				} else if (httpContextId != null && WebContainerUtils.isWebContainer(webContainer)) {
			//					httpContext = webContainer.createDefaultHttpContext(httpContextId);
			//				} else {
			//					//default
			//					httpContext = webContainer.createDefaultHttpContext();
			//				}
			//			}
			//		} else if (!(httpContext instanceof WebContainerContext)) {
			//			// wrap registered HttpContext in pax-web specific context
			//			final HttpContext localHttpContext = httpContext;
			//			httpContext = new WebContainerContext() {
			//				@Override
			//				public Set<String> getResourcePaths(String name) {
			//					// FIXME check if this is valid for plain HttpContext-registrations
			//					return null;
			//				}
			//
			//				@Override
			//				public String getContextId() {
			//					return httpContextId;
			//				}
			//
			//				@Override
			//				public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
			//					return localHttpContext.handleSecurity(request, response);
			//				}
			//
			//				@Override
			//				public URL getResource(String name) {
			//					return localHttpContext.getResource(name);
			//				}
			//
			//				@Override
			//				public String getMimeType(String name) {
			//					return localHttpContext.getMimeType(name);
			//				}
			//			};
			//		}
			//	}
			//
			//	private void registerWebElements() {
			//		httpServiceLock.readLock().lock();
			//		try {
			//			if (webContainer != null && httpContext != null) {
			//				for (WebElement registerer : webElements) {
			//					registerWebElement(registerer);
			//				}
			//			}
			//		} finally {
			//			httpServiceLock.readLock().unlock();
			//		}
			//	}
			//
			//	private List<String> convertToList(String elementListAsString) {
			//		List<String> elementList = new LinkedList<>();
			//		if (elementListAsString != null) {
			//			String[] elementArray = elementListAsString.split(",");
			//			elementList = Arrays.stream(elementArray).map(String::trim).collect(Collectors.toList());
			//		}
			//		return elementList;
			//	}
			//
			//	private void registerWebElement(final WebElement registerer) {
			//		//CHECKSTYLE:OFF
			//		try {
			//			if (webContainer != null && httpContext != null && registerer.isValid()) {
			//				registerer.register(webContainer, httpContext);
			//			}
			//		} catch (Exception ignore) {
			//			LOG.error("Registration skipped for [" + registerer
			//					+ "] due to error during registration", ignore);
			//		} finally {
			//			httpServiceRuntime.addWhiteboardElement(registerer);
			//		}
			//		//CHECKSTYLE:ON
			//	}
			//
			//	private void unregisterWebElements() {
			//		httpServiceLock.readLock().lock();
			//		try {
			//			if (webContainer != null && httpContext != null) {
			//				webElements.forEach(this::unregisterWebElement);
			//			}
			//		} finally {
			//			webElements.forEach(httpServiceRuntime::removeWhiteboardElement);
			//			httpServiceLock.readLock().unlock();
			//		}
			//	}
			//
			//	private void unregisterWebElement(final WebElement registerer) {
			//		if (webContainer != null && httpContext != null && registerer.isValid()) {
			//			registerer.unregister(webContainer, httpContext);
			//		}
			//	}
			//
			//	public void setServletContextHelper(final ServletContextHelper servletContextHelper, final HttpContextMapping httpContextMapping) {
			//		httpServiceLock.writeLock().lock();
			//		try {
			//			if (hasHttpContextMapping()) {
			//				unregisterHttpContext();
			//			}
			//			this.servletContextHelper = servletContextHelper;
			//			this.httpContextMapping = httpContextMapping;
			//			registerHttpContext();
			//		} finally {
			//			httpServiceLock.writeLock().unlock();
			//		}
			//	}
			//
			//	@Override
			//	public String toString() {
			//		return this.getClass().getSimpleName() + "{mapping=" + httpContextMapping + "}";
			//	}

}
