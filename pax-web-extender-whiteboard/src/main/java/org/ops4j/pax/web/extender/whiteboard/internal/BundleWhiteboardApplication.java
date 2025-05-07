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
import org.ops4j.pax.web.service.spi.util.WebContainerManager;
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
 * <p>Actual trackers (tracker customizers) add elements/contexts to this application via {@link WhiteboardExtenderContext}
 * <strong>only</strong> - because all Whiteboard elements have to be tracked in
 * {@link org.osgi.service.http.runtime.HttpServiceRuntime}, which is kind of <em>Whiteboard registry</em>.</p>
 *
 * <p>Also, before Pax Web 8, this class was calling {@link WebContainer} registration methods, while now, interaction
 * with {@link WebContainer} happens at {@link WhiteboardExtenderContext} level.</p>
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

	private final WebContainerManager webContainerManager;

	/**
	 * Current {@link ServiceReference} to use when obtaining a {@link WhiteboardWebContainerView} from
	 * {@link WebContainerManager}. {@link WebContainerManager} ensures that this reference is consistent - never
	 * set when there's already a reference set without unsetting it first.
	 */
	private volatile ServiceReference<WebContainer> webContainerServiceRef;

	public BundleWhiteboardApplication(Bundle bundle, WebContainerManager webContainerManager) {
		this.bundle = bundle;
		this.webContainerManager = webContainerManager;
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

	public boolean isRegistered(OsgiContextModel contextModel) {
		return webContexts.containsKey(contextModel) && webContexts.get(contextModel);
	}

	public List<OsgiContextModel> getWebContainerOsgiContextModels() {
		List<OsgiContextModel> models = new LinkedList<>();
		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, webContainerServiceRef);
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
	 * When (tracked at {@link WebContainerManager} level) new {@link WebContainer} {@link ServiceReference}
	 * is added, it is passed to each {@link BundleWhiteboardApplication}, so it can (now and later)
	 * managed its own bundle-scoped {@link WhiteboardWebContainerView} to install/uninstall web
	 * elements and contexts
	 * @param ref
	 */
	public void webContainerAdded(ServiceReference<WebContainer> ref) {
		webContainerServiceRef = ref;

		// install all current contexts and elements. Lifecycle is managed at WhiteboardExtenderContext level,
		// so we don't have to care about uninstalling the contexts/elements from previous WebContainer

		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, ref);
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
	 * {@link WebContainer} reference was untracked at {@link WebContainerManager} level.
	 * @param ref
	 */
	public void webContainerRemoved(ServiceReference<WebContainer> ref) {
		if (ref != webContainerServiceRef) {
			throw new IllegalStateException("Removing unknown WebContainer reference " + ref + ", expecting " + webContainerServiceRef);
		}

		// no need to uninstall current contexts and elements - they'll get unregistered when bundle-scoped
		// HttpService/WebContainer is stopped. Here we only mark them as unregistered
		webElements.keySet().forEach(element -> {
			if (element.getContextModels().size() > 0 && webElements.get(element)) {
				webElements.put(element, false);
			}
			if (element instanceof ServletModel && ((ServletModel) element).isResourceServlet()) {
				// we have to clear up the supplier which is related to old HttpService instance
				element.setElementSupplier(null);
			}
		});
		webContexts.keySet().forEach(ctx -> {
			if (webContexts.get(ctx)) {
				webContexts.put(ctx, false);
			}
		});

		webContainerManager.releaseContainer(bundle, ref);
		webContainerServiceRef = null;
	}

	/**
	 * Associates given customized <em>context</em> in the form of {@link OsgiContextModel} with current
	 * <em>bundle web application</em> and if the {@link WebContainer} is already available, register it there.
	 * @param webContext
	 */
	public void addWebContext(final OsgiContextModel webContext) {
		webContexts.put(webContext, false);
		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, webContainerServiceRef);
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
		webContexts.remove(webContext);

		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, webContainerServiceRef);
		if (view == null) {
			LOG.debug("{} will be unregistered when WebContainer/HttpService is available", webContext);
			return;
		}

		view.removeWhiteboardOsgiContextModel(webContext);
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

		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, webContainerServiceRef);
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

		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, webContainerServiceRef);
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
		webContainerManager.releaseContainer(bundle);
	}

	public WhiteboardWebContainerView getWhiteboardContainer() {
		return webContainerManager.whiteboardView(bundle, webContainerServiceRef);
	}

}
