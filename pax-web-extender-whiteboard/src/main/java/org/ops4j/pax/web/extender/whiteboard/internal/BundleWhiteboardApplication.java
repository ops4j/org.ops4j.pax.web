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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 * <p>Actual trackers (tracker customizers) add elements/contexts to this application via {@link WhiteboardContext}
 * <strong>only</strong> - because all Whiteboard elements have to be tracked in
 * {@link org.osgi.service.http.runtime.HttpServiceRuntime}, which is kind of <em>Whiteboard registry</em>.</p>
 *
 * <p>Also, before Pax Web 8, this class was calling {@link WebContainer} registration methods, while now, interaction
 * with {@link WebContainer} happens at {@link WhiteboardContext} level.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public class BundleWhiteboardApplication {

	private static final Logger LOG = LoggerFactory.getLogger(BundleWhiteboardApplication.class);

	private final Bundle bundle;

	// contexts and elements should be accessed using WhiteboardContext.lock

	/** List of <em>web elements</em> that are registered by given {@link Bundle}. */
	private final Map<ElementModel<?, ?>, Boolean> webElements = new LinkedHashMap<>();

	/** List of <em>web contexts</em> that are registered by given {@link Bundle}. */
	private final Map<OsgiContextModel, Boolean> webContexts = new LinkedHashMap<>();

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
	}

	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * This method returns a snapshot of current {@link OsgiContextModel} contexts registered by given {@link Bundle}.
	 * @return
	 */
	public List<OsgiContextModel> getWebContexts() {
		return Collections.unmodifiableList(new LinkedList<>(webContexts.keySet()));
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
		return Collections.unmodifiableList(new LinkedList<>(webElements.keySet()));
	}

	/**
	 * When (tracked at {@link WhiteboardContext} level) new {@link WebContainer} {@link ServiceReference}
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
			webContexts.keySet().forEach(ctx -> {
				if (!webContexts.get(ctx)) {
					view.addWhiteboardOsgiContextModel(ctx);
					webContexts.put(ctx, true);
				}
			});
			webElements.keySet().forEach(element -> {
				if (!webElements.get(element)) {
					if (element.getContextModels().size() > 0) {
						element.register(view);
						webElements.put(element, true);
					}
				}
			});
		}
	}

	/**
	 * {@link WebContainer} reference was untracked at {@link WhiteboardContext} level.
	 * @param ref
	 */
	public void webContainerRemoved(ServiceReference<WebContainer> ref) {
		// uninstall all current contexts and elements
		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view != null) {
			webElements.keySet().forEach(element -> {
				if (element.getContextModels().size() > 0 && webElements.get(element)) {
					// no need to do this otherwise
					element.unregister(view);
					webElements.put(element, false);
				}
			});
			webContexts.keySet().forEach(ctx -> {
				if (webContexts.get(ctx)) {
					view.removeWhiteboardOsgiContextModel(ctx);
					webContexts.put(ctx, false);
				}
			});
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
		webContexts.put(webContext, false);
		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view != null) {
			view.addWhiteboardOsgiContextModel(webContext);
			webContexts.put(webContext, true);
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
		webElements.put(webElement, false);

		if (webElement.getContextModels().size() == 0) {
			// I found this situation to be possible with SCR despite the best attempts of user to register
			// a context BEFORE a servlet (using SCR's @References to declare some kind of dependencies) - because
			// Felix fires ServiceEvents using non-ordered HashMap<Bundle, List<ServiceListener>>
			//
			// that's why we have to remember such element anyway - simply because at some point there MAY
			// be a HttpContext/ServletContextHelper registered that matches given web element's selector
			LOG.info("No matching target context(s) for Whiteboard element {}. Filter: {}."
							+ " Element may be re-registered later, when matching context/s is/are registered.",
					webElement, webElement.getContextFilter());
		}

		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view == null) {
			LOG.debug("{} will be registered when WebContainer/HttpService is available", webElement);
			return;
		}

		// no need to register an element without associated contexts
		if (webElement.getContextModels().size() > 0) {
			webElement.register(view);
			webElements.put(webElement, true);
		}
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

		if (webElement.getContextModels().size() > 0) {
			// otherwise, this element may have already been unregistered
			webElement.unregister(view);
		}
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
			if (bundle.getBundleContext() != null) {
				bundle.getBundleContext().ungetService(webContainerServiceRef);
			}
		}
	}

	public WhiteboardWebContainerView getWhiteboardContainer() {
		return whiteboardContainer;
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
