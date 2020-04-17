/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Whiteboard extender context.</p>
 *
 * <p>This class collects all the customized objects related to <em>whiteboard web elements</em> (like servlets,
 * filters, ...) and <em>whiteboard contexts</em> (like {@link org.osgi.service.http.HttpContext} or
 * {@link org.osgi.service.http.context.ServletContextHelper} objects). All such customized objects (internal
 * representation of user-registered OSGi services) are then somehow passed to current
 * {@link org.osgi.service.http.HttpService} which should rather be Pax Web's
 * {@link WebContainer} extension of {@link org.osgi.service.http.HttpService}. It's not
 * a requirement, but initially the tests focus on integrating pax-web-extender-whiteboard with pax-web-runtime.</p>
 *
 * <p>The biggest change is that incoming <em>web elements</em> are not registered in <em>web applications</em>, but
 * are directly passed to {@link WebContainer}, because in Pax Web 8 the model has changed,
 * allowing to associated single servlet (or filter, or ...) with more <em>contexts</em>.</p>
 *
 * <p>Most of the methods are synchronized, because they're called from service tracking methods that register and
 * unregister Whiteboard elements and contexts and on the other hand - from service listener methods that may
 * register/unregister target {@link WebContainer}. Synchronization isn't a problem, because it is assumed that
 * registration/unregistration happens rarely (opposite to actual HTTP request processing) and is expected to
 * take some time.</p>
 *
 * TODO: think about synchronization requirements at this level
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek (since Pax Web 8)
 * @since 0.4.0, April 01, 2008
 */
public class ExtenderContext {

	private static final Logger LOG = LoggerFactory.getLogger(ExtenderContext.class);

	private final BundleContext bundleContext;

	/** Per-{@link Bundle} cache of lists of Whiteboard services - to clean them up when bundle is gone. */
	private final Map<Bundle, BundleWhiteboardApplication> bundleApplications = new ConcurrentHashMap<>();

	//	private final ConcurrentHashMap<WebApplication, Integer> sharedWebApplicationCounter = new ConcurrentHashMap<>();

	/**
	 * <p>Context <em>name</em> to ordered (by ranking / service id) set of {@link OsgiContextModel} mapping.</p>
	 *
	 * <p>With Whiteboard web element registration, target {@link OsgiContextModel} is <strong>always</strong>
	 * referenced by name and there may be many <em>contexts</em> with the same name.
	 * {@link javax.servlet.Servlet} registered to e.g., {@code default} <em>context</em> may be re-registered if
	 * there's <em>better</em> context registered with the same {@code default} name and different ranking.</p>
	 *
	 * <p>The target <em>physical</em> servlet context (which in {@link org.ops4j.pax.web.service.spi.model.ServerModel}
	 * is kept as {@link org.ops4j.pax.web.service.spi.model.ServletContextModel}) is <strong>not</strong> stored
	 * here. Each {@link OsgiContextModel} refer to <em>physical</em> servlet context only by single service
	 * registration property - {@link HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_PATH} - regardless of the
	 * type of the service registered by user (whether it was <em>official</em> {@link ServletContextHelper} or
	 * "legacy" {@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping}).</p>
	 */
	private final Map<String, TreeSet<OsgiContextModel>> osgiContexts = new ConcurrentHashMap<>();

	/**
	 * We also have to store {@link OsgiContextModel} contexts in flat list to perform {@link Filter} based
	 * lookup.
	 */
	private final List<OsgiContextModel> osgiContextsList = new ArrayList<>();

	/**
	 * <p>pax-web-extender-whiteboard operates on target {@link WebContainer} and special
	 * {@link org.ops4j.pax.web.service.views.PaxWebContainerView} it provides for Whiteboard purposes.</p>
	 *
	 * <p>At any given time, we operate on single target {@link WebContainer} and when
	 * new one is registered, we simply <em>move</em> current Whiteboard applications to new target runtime.</p>
	 */
	private volatile WhiteboardWebContainerView whiteboardContainer;

	/** Guard to check if we're operating on correct service for the {@link WebContainer} */
	private volatile long webContainerServiceId = -1;

	/**
	 * Current {@link ServiceReference} to unget it if there's new {@link WebContainer} added without removing
	 * current one.
	 */
	private volatile ServiceReference<WebContainer> webContainerServiceRef;

	/** Implementation of {@link org.osgi.service.http.runtime.HttpServiceRuntime} from Whiteboard Service spec. */
	private final ExtendedHttpServiceRuntime httpServiceRuntime;

	public ExtenderContext(ExtendedHttpServiceRuntime httpServiceRuntime, BundleContext bundleContext) {
		this.httpServiceRuntime = httpServiceRuntime;
		this.bundleContext = bundleContext;

		// remember ONLY "default" (mapped to "/") context model
		OsgiContextModel model = OsgiContextModel.DEFAULT_CONTEXT_MODEL;
		osgiContexts.computeIfAbsent(model.getName(), n -> new TreeSet<>()).add(model);
		osgiContextsList.add(model);
	}

	public ExtendedHttpServiceRuntime getHttpServiceRuntime() {
		return httpServiceRuntime;
	}

	/**
	 * <p>Intermediary method used during Whiteboard element registration, that returns list of target
	 * {@link OsgiContextModel OSGi contexts} to register tracked Whiteboard element with.</p>
	 *
	 * <p>Remember - target {@link WebContainer} may <strong>not</strong> be available yet, but web elements
	 * tracked by pax-web-extender-whiteboard should know up front with which OSGi context models they'll
	 * be associated with. It's cleary stated in specification (140.3 Common Whiteboard Properties):<blockquote>
	 *    An LDAP-style filter to select the associated ServletContextHelper service to use. Any service property of
	 *    the Servlet Context Helper can be filtered on. If this property is missing the default Servlet Context Helper
	 *    is used.
	 *    [...]
	 *    Any service property of the Servlet Context Helper can be filtered on. If this property is missing the
	 *    default Servlet Context Helper is used.
	 * </blockquote></p>
	 *
	 *
	 * @param bundle {@link Bundle} of the Whiteboard element for which we're looking for the associated contexts
	 * @param selector
	 * @return list of {@link OsgiContextModel} to associate the service (e.g., {@link javax.servlet.Servlet}) with.
	 */
	public synchronized List<OsgiContextModel> resolveContexts(Bundle bundle, Filter selector) {
		if (selector == null) {
			// easy - highest ranked "default" context model
			OsgiContextModel defaultModel
					= osgiContexts.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME).iterator().next();
			return Collections.singletonList(defaultModel);
		}

		// more difficult, as according to "140.3 Common Whiteboard Properties", "Table 140.3 Common properties":
		//
		//     osgi.http.whiteboard.context.select [...] An LDAP-style filter to select the associated
		//     ServletContextHelper service to use. Any service property of the Servlet Context Helper can be filtered
		//     on. If this property is missing the default Servlet Context Helper is used.
		//
		// so we can even target two different "default" ServletContextHelpers and actual physical ServletContextModel
		// will be determined at registration time
		List<OsgiContextModel> targetContexts = new ArrayList<>();

		// check _contexts_ managed at pax-web-extender-whiteboard level
		for (OsgiContextModel model : osgiContextsList) {
			// one line "140.3 Common Whiteboard Properties" implementation of LDAP filter matching
			if (selector.matchCase(model.getContextRegistrationProperties())) {
				targetContexts.add(model);
			}
		}

		WhiteboardWebContainerView view = this.whiteboardContainer;
		if (view != null) {
			// get all the contexts (shared and scoped for web element's bundle) from HttpService. These will never
			// have "osgi.http.whiteboard.context.name" property specified, only
			// "osgi.http.whiteboard.context.httpservice" property, so no special check should be performed - just
			// matching the selector
			for (OsgiContextModel model : view.getOsgiContextModels(bundle)) {
				if (selector.matchCase(model.getContextRegistrationProperties())) {
					targetContexts.add(model);
				}
			}
		}

		// "140.2 The Servlet Context"
		//
		//     If multiple Servlet Context Helper services are registered with the same name, the one with the highest
		//     Service Ranking is used
		//
		// so we can end with several OsgiContextModels with the same name - we need only one of each - with
		// highest rank. Whiteboard and HttpService models are treated equally here.
		Map<String, OsgiContextModel> uniqueContexts = new HashMap<>();
		for (OsgiContextModel c : targetContexts) {
			if (!uniqueContexts.containsKey(c.getName())) {
				uniqueContexts.put(c.getName(), c);
			} else if (uniqueContexts.get(c.getName()).compareTo(c) > 0) {
				uniqueContexts.put(c.getName(), c);
			}
		}

		targetContexts.clear();
		targetContexts.addAll(uniqueContexts.values());

		return targetContexts;
	}

	/**
	 * Method called from some {@link BundleListener} to clean up the cache of bundle-related Whiteboard services
	 * @param bundle
	 */
	public void bundleStopped(Bundle bundle) {
		LOG.debug("Clearing Whiteboard cache for {}", bundle);

		BundleWhiteboardApplication application = bundleApplications.remove(bundle);
		if (application != null) {
			// TOCHECK: when bundle was stopping, it should automatically unregister all its Whiteboard services
//			application.cleanup();
		}
	}

	// --- Handling registration/unregistration of target WebContainer, where we want to register Whiteboard services

	public synchronized void webContainerAdded(ServiceReference<WebContainer> ref) {
		long serviceId = Utils.getServiceId(ref);

		if (webContainerServiceId != -1) {
			LOG.warn("New WebContainer was registered (service.id={}) and current one (service.id={}) was not"
					+ " unregistered. Unregistering Whiteboard applications from current WebContainer.",
					serviceId, webContainerServiceId);

			uninstallWhiteboardApplications();
		}

		WebContainer webContainer = bundleContext.getService(ref);
		if (webContainer == null) {
			LOG.warn("Can't get a WebContainer service from {}", ref);
			return;
		}

		webContainerServiceId = serviceId;
		webContainerServiceRef = ref;

		// this "whiteboard container view" is obtained from bundle-scoped org.ops4j.pax.web.service.WebContainer
		// but actually the view can access global data of the web container (i.e., the "server model") and
		// the bundleContext used to getService for the WebContainer is not relevant
		whiteboardContainer = webContainer.adapt(WhiteboardWebContainerView.class);

		installWhiteboardApplications();
	}

	public synchronized void webContainerRemoved(ServiceReference<WebContainer> ref) {
		long serviceId = Utils.getServiceId(ref);

		if (webContainerServiceId != serviceId) {
			if (webContainerServiceId != -1) {
				LOG.warn("Unregistration of unknown WebContainer service with service.id={}, expected {}",
						serviceId, webContainerServiceId);
			} else {
				LOG.warn("Unregistration of unknown WebContainer service with service.id={}", serviceId);
			}
			return;
		}

		uninstallWhiteboardApplications();

		bundleContext.ungetService(webContainerServiceRef);
		whiteboardContainer = null;
		webContainerServiceRef = null;
		webContainerServiceId = -1;
	}

	// --- Handling registration/unregistration of Whiteboard web elements and contexts
	//     Those methods perform 3 actions:
	//     1) mark incoming customized web element or context as associated with given "bundle whiteboard application"
	//     2) handle Whiteboard DTO registry
	//     3) register given customized object in actual WebContainer from pax-web-runtime (if available)

	public void addWebContext(Bundle bundle, OsgiContextModel model) {
		if (model.getHttpContext() != null) {
			// special handling. Whiteboard-registered service that's customized to OsgiContextModel, where
			// HttpContext (WebContainerContext) is specified directly is an implementation of
			// "140.10 Integration with Http Service Contexts" chapter to allow Whiteboard web elements (surprisingly,
			// not servlets) to be registered to contexts managed inside HttpService area of the implementation
			//
			// Whiteboard specification doesn't mention registration of HttpContext at all - it's legacy Pax Web
			// mechanism. But when such service is registered and it's neither a singleton nor any kind of factory
			// (i.e., we can obtain only one HttpContext), then we pass such OsgiContextModel directly to
			// WebContainer (HttpService) implementation and not store here.
			LOG.info("{} will be passed directly to WebContainer service (when available)", model);
		} else {
			// normal Whiteboard scenario - context is remembered here, so when web elements are registered with
			// context selectors, we search them using these lists/sets
			osgiContexts.computeIfAbsent(model.getName(), cp -> new TreeSet<>()).add(model);
			osgiContextsList.add(model);
		}

		getBundleApplication(bundle).addWebContext(model);
	}

	public void removeWebContext(Bundle bundle, OsgiContextModel model) {
		getBundleApplication(bundle).removeWebContext(model);

		if (model.getHttpContext() == null) {
			osgiContexts.get(model.getName()).remove(model);
			osgiContextsList.remove(model);
		}
	}

	public <R, T extends ElementModel<R>> void addWebElement(Bundle bundle, T webElement) {
		getBundleApplication(bundle).addWebElement(webElement);
	}

	public <R, T extends ElementModel<R>> void removeWebElement(Bundle bundle, T webElement) {
		getBundleApplication(bundle).removeWebElement(webElement);
	}

	/**
	 * This method iterates over all the {@link BundleWhiteboardApplication} and registers all the Whiteboard contexts
	 * and elements (tracked so far) in newly registered {@link WebContainer}.
	 */
	private synchronized void installWhiteboardApplications() {
		// TODO: do actual installation of "Whiteboard applications" after WebContainer becomes available
//		whiteboardContainer.xxx()
	}

	/**
	 * This method iterates over all the {@link BundleWhiteboardApplication} and unregisters all the contexts
	 * and elements from current {@link WebContainer}, which is about to removed.
	 */
	private void uninstallWhiteboardApplications() {
		// TODO: do actual uninstallation of "Whiteboard applications" after WebContainer stops being available
//		whiteboardContainer.xxx()
	}

	/**
	 * Return (create if needed) a new bundle-scoped {@link BundleWhiteboardApplication}.
	 *
	 * @param bundle
	 * @return
	 */
	private BundleWhiteboardApplication getBundleApplication(final Bundle bundle) {
		if (bundle == null) {
			// PAXWEB-500 - it might happen that the bundle is already gone!
			return null;
		}

		BundleWhiteboardApplication bundleApplication = bundleApplications.get(bundle);
		if (bundleApplication == null) {
			bundleApplication = new BundleWhiteboardApplication(bundle, httpServiceRuntime);

				//			// PAXWEB-681 - bundleApplication and existing bundleApplication might not be the same.
				//			BundleWhiteboardApplication existingWebApplication = webApplications.putIfAbsent(contextKey, bundleApplication);
				//			if (existingWebApplication == null) {
				//				bundleApplication.start();
				//			} else {
				//				bundleApplication = existingWebApplication;
				//			}
			bundleApplications.put(bundle, bundleApplication);
		}

				//		if (sharedHttpContext) {
				//			Integer counter = sharedWebApplicationCounter.get(bundleApplication);
				//			if (counter == null) {
				//				counter = 0;
				//			}
				//			sharedWebApplicationCounter.put(bundleApplication, ++counter);
				//			LOG.debug("Shared Webapplication Counter for {}, increased to {} for ContxtKey: {}", bundleApplication, counter, contextKey);
				//		}

		return bundleApplication;
	}

	// TODO: handling successful/failed DTO for elements/contexts

	/**
	 * This method is invoked after checking that element model {@link ElementModel#isValid() is valid}.
	 * @param httpServiceRuntime
	 */
	public void configureDTOs(ElementModel<?> webElement) {
	}

	/**
	 * This method is invoked after checking that element model {@link ElementModel#isValid() is not valid}.
	 * @param httpServiceRuntime
	 */
	public void configureFailedDTOs(ElementModel<?> webElement) {
	}

	/**
	 * This method is invoked after checking that context model {@link OsgiContextModel#isValid() is valid}.
	 * @param httpServiceRuntime
	 */
	public void configureDTOs(OsgiContextModel webContext) {
	}

	/**
	 * This method is invoked after checking that context model {@link OsgiContextModel#isValid() is not valid}.
	 * @param httpServiceRuntime
	 */
	public void configureFailedDTOs(OsgiContextModel webContext) {
	}

}
