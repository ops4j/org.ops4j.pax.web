/*
 * Copyright 2007 Alin Dreghiciu.
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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.internal.tracker.FilterTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.HttpContextTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletContextHelperTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.FilterMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.HttpContextMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ServletContextHelperMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ServletMappingTracker;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.context.DefaultServletContextHelper;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.whiteboard.FilterMapping;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activates the pax web extender. The goal is to setup all required {@link ServiceTracker trackers} for all
 * standard (OSGi R7 Whiteboard Service) and non-standard (Pax Web specific) interfaces that can be registered.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, August 21, 2007
 */
public class Activator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	/**
	 * Extender context.
	 */
	private ExtenderContext extenderContext;

	/** {@link BundleListener} to cleanup bundle-registered Whiteboard elements when the bundle is gone */
	private BundleListener bundleListener;

	/** {@link ServiceListener} to track {@link WebContainer} instances to register Whiteboard elements there */
	private ServiceListener webContainerListener;

	/**
	 * List of service trackers. All trackers get closed on {@link BundleActivator#stop(BundleContext)}.
	 */
	private final List<ServiceTracker<?, ?>> trackers = new ArrayList<>();

    private ExtendedHttpServiceRuntime httpServiceRuntime;

	private BundleContext bundleContext;

	@Override
	@SuppressWarnings("unchecked")
	public void start(final BundleContext bundleContext) throws Exception {
		LOG.info("Starting Pax Web Whiteboard Extender");

		this.bundleContext = bundleContext;

		// this is implementation of Whiteboard Service's org.osgi.service.http.runtime.HttpServiceRuntime
		httpServiceRuntime = new ExtendedHttpServiceRuntime(bundleContext);
		httpServiceRuntime.start();

		// this is where "trackers" register/unregister their customized objects.
		// a short summary:
		//  - we have lot of customizers in org.ops4j.pax.web.extender.whiteboard.internal.tracker package ("tracker"
		//    name was always used...)
		//  - each such customizer has static method to create actual ServiceTracker with "this" as the customizer
		//  - each customizer translates "incoming" e.g., javax.servlet.Servlet into internal "model" (a.k.a.
		//    "customized object")
		//  - each tracker takes "bundle whiteboard application" (collection of web elements/contexts regitered by
		//    given bundle) from the extender context and registers/unregister such "customized object"
		//  - extender contains refrence to current WebContainer, so the whiteboard elements/contexts may be
		//    registered there
		//
		// in other words - "extender context" is the component that bridges between tracked web elements + customized
		// model elements on one side and "current" WebContainer on the other side
		extenderContext = new ExtenderContext(httpServiceRuntime, bundleContext);

		// we immediately register "default" ServletContextHelper as OSGi service
		// felix.http registers something like this:
		//
		//     objectClass = [org.osgi.service.http.context.ServletContextHelper]
		//     osgi.http.whiteboard.context.name = default
		//     osgi.http.whiteboard.context.path = /
		//     service.bundleid = 44
		//     service.id = 97
		//     service.ranking = -2147483648
		//     service.scope = bundle
		//
		// this ServletContextHelper won't be processed by Whiteboard trackers and won't be passed to WebContainer
		// as well - WebContainer will use the same OsgiContextModel.DEFAULT_CONTEXT_MODEL instance to prepopulate
		// its own (Http Service oriented) ServerModel
		Dictionary<String, Object> properties = new Hashtable<>();
		final OsgiContextModel defaultContextModel = OsgiContextModel.DEFAULT_CONTEXT_MODEL;
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, defaultContextModel.getName());
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, defaultContextModel.getContextPath());
		// we'll add this special property to indicate that this _context_ is common for HttpService and
		// Whiteboard service
		properties.put(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY, defaultContextModel.getName());
		properties.put(Constants.SERVICE_RANKING, 0);
		properties.put(PaxWebConstants.SERVICE_PROPERTY_INTERNAL, true);
		//		props.put(PaxWebConstants.SERVICE_PROPERTY_VIRTUAL_HOSTS, new String[] { "*" });
		bundleContext.registerService(ServletContextHelper.class,
				new DefaultServletContextHelperServiceFactory(), properties);

		// bundle listener to manager per-bundle cache of Whiteboard elements and contexts
		bundleListener = event -> {
			if (event.getType() == BundleEvent.STOPPED) {
				extenderContext.bundleStopped(event.getBundle());
			}
		};
		bundleContext.addBundleListener(bundleListener);

		// track WebContainer service, because we pass Whiteboard services (customized into "element models") to
		// a _view_ of PaxWeb-specific extension of HttpService
		webContainerListener = event -> {
			switch (event.getType()) {
				case ServiceEvent.REGISTERED: {
					// new WebContainer was registered
					extenderContext.webContainerAdded((ServiceReference<WebContainer>) event.getServiceReference());
				}
				case ServiceEvent.MODIFIED: {
					// properties have changed - but there's nothing we care about - even if a HttpService/WebContainer
					// has any ID associated, Whiteboard elements may only target selected Whiteboard implementation,
					// not the HttpService implementation
					// "Whiteboard implementation" is represented by registered instance of
					// org.osgi.service.http.runtime.HttpServiceRuntime, not by HttpService/WebContainer
				}
				case ServiceEvent.MODIFIED_ENDMATCH: {
					// no chance for this - we filter by objectClass and it can't change
				}
				case ServiceEvent.UNREGISTERING: {
					extenderContext.webContainerRemoved((ServiceReference<WebContainer>) event.getServiceReference());
				}
			}
		};
		String filter = String.format("(%s=%s)", Constants.OBJECTCLASS, WebContainer.class.getName());
		bundleContext.addServiceListener(webContainerListener, filter);

		// web contexts
		trackHttpContexts();
		trackServletContextHelpers();

		// web elements
		trackServlets();
		trackFilters();
//		trackResources(bundleContext);

//		if (WebContainerUtils.WEB_CONATAINER_AVAILABLE) {
//			trackListeners(bundleContext);
//			trackJspMappings(bundleContext);
//			trackErrorPages(bundleContext);
//			trackWelcomeFiles(bundleContext);
//			if (WebContainerUtils.WEBSOCKETS_AVAILABLE) {
//				trackWebSockets(bundleContext);
//			} else {
//				LOG.info("No javax.websocket.Endpoint class found, WebSocketTracker is disabled");
//			}
//		} else {
//			LOG.warn("Filters tracking has been disabled as the WebContainer (Pax Web) is not available");
//			LOG.warn("Event Listeners tracking has been disabled as the WebContainer (Pax Web) is not available");
//			LOG.warn("JSP mappings tracking has been disabled as the WebContainer (Pax Web) is not available");
//		}

		LOG.debug("Pax Web Whiteboard Extender started");
	}

	@Override
	public void stop(final BundleContext bundleContext) throws Exception {
		List<ServiceTracker<?, ?>> serviceTrackers = new ArrayList<>(this.trackers);
		Collections.reverse(serviceTrackers);
		for (ServiceTracker<?, ?> tracker : serviceTrackers) {
			tracker.close();
		}
		this.trackers.clear();
		httpServiceRuntime.stop();

		if (webContainerListener != null) {
			bundleContext.removeServiceListener(webContainerListener);
			webContainerListener = null;
		}

		if (bundleListener != null) {
			bundleContext.removeBundleListener(bundleListener);
			bundleListener = null;
		}

		LOG.debug("Pax Web Whiteboard Extender stopped");
	}

	/**
	 * Track {@link HttpContext} and {@link HttpContextMapping} services.
	 */
	private void trackHttpContexts() {
		ServiceTracker<HttpContext, OsgiContextModel> httpContextTracker
				= HttpContextTracker.createTracker(extenderContext, bundleContext);
		httpContextTracker.open();
		trackers.add(0, httpContextTracker);

		ServiceTracker<HttpContextMapping, OsgiContextModel> httpContextMappingTracker
				= HttpContextMappingTracker.createTracker(extenderContext, bundleContext);
		httpContextMappingTracker.open();
		trackers.add(0, httpContextMappingTracker);
	}

	/**
	 * Track {@link ServletContextHelper} and {@link ServletContextHelperMapping} services.
	 */
	private void trackServletContextHelpers() {
		ServiceTracker<ServletContextHelper, OsgiContextModel> servletContextHelperTracker
				= ServletContextHelperTracker.createTracker(extenderContext, bundleContext);
		servletContextHelperTracker.open();
		trackers.add(0, servletContextHelperTracker);

		ServiceTracker<ServletContextHelperMapping, OsgiContextModel> servletContextHelperMappingTracker
				= ServletContextHelperMappingTracker.createTracker(extenderContext, bundleContext);
		servletContextHelperMappingTracker.open();
		trackers.add(0, servletContextHelperMappingTracker);
	}

	/**
	 * <p>Track servlets:<ul>
	 *     <li>{@link Servlet} from OSGi CPMN R7 Whiteboard Service specification</li>
	 *     <li>{@link ServletMapping} from Pax Web</li>
	 * </ul></p>
	 */
	private void trackServlets() {
		ServiceTracker<Servlet, ServletModel> servletTracker
				= ServletTracker.createTracker(extenderContext, bundleContext);
		servletTracker.open();
		trackers.add(servletTracker);

		ServiceTracker<ServletMapping, ServletModel> servletMappingTracker
				= ServletMappingTracker.createTracker(extenderContext, bundleContext);
		servletMappingTracker.open();
		trackers.add(servletMappingTracker);
	}

//
//	/**
//	 * Track resources.
//	 *
//	 * @param bundleContext the BundleContext associated with this bundle
//	 */
//	private void trackResources(final BundleContext bundleContext) {
//		ServiceTracker<Object, ResourceWebElement> resourceTracker = ResourceTracker.createTracker(extenderContext, bundleContext);
//
//		resourceTracker.open();
//		trackers.add(0, resourceTracker);
//
//		final ServiceTracker<ResourceMapping, ResourceMappingWebElement> resourceMappingTracker = ResourceMappingTracker
//				.createTracker(extenderContext, bundleContext);
//
//		resourceMappingTracker.open();
//		trackers.add(0, resourceMappingTracker);
//	}

	/**
	 * <p>Track filters:<ul>
	 *     <li>{@link Filter} from from OSGi CPMN R7 Whiteboard Service specification</li>
	 *     <li>{@link FilterMapping} from Pax Web</li>
	 * </ul></p>
	 */
	private void trackFilters() {
		final ServiceTracker<Filter, FilterModel> filterTracker
				= FilterTracker.createTracker(extenderContext, bundleContext);
		filterTracker.open();
		trackers.add(filterTracker);

		final ServiceTracker<FilterMapping, FilterModel> filterMappingTracker
				= FilterMappingTracker.createTracker(extenderContext, bundleContext);
		filterMappingTracker.open();
		trackers.add(filterMappingTracker);
	}

	/**
	 * <p>{@link ServiceFactory} returning default {@link ServletContextHelper} as specified by
	 * "140.2 The Servlet Context":<blockquote>
	 *     Some implementations of the ServletContextHelper may be implemented using a Service Factory,
	 *     for example to provide resources from the associated bundle, as the default implementation does.
	 * </blockquote></p>
	 */
	private static class DefaultServletContextHelperServiceFactory implements ServiceFactory<ServletContextHelper> {

		@Override
		public ServletContextHelper getService(Bundle bundle, ServiceRegistration<ServletContextHelper> registration) {
			return new DefaultServletContextHelper(bundle);
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration<ServletContextHelper> registration, ServletContextHelper service) {
		}
	}

	//
//	/**
//	 * Track listeners.
//	 *
//	 * @param bundleContext the BundleContext associated with this bundle
//	 */
//	private void trackListeners(final BundleContext bundleContext) {
//		final ServiceTracker<EventListener, ListenerWebElement> listenerTracker = ListenerTracker
//				.createTracker(extenderContext, bundleContext);
//
//		listenerTracker.open();
//		trackers.add(0, listenerTracker);
//
//		// FIXME needed?
//		final ServiceTracker<ListenerMapping, ListenerMappingWebElement> listenerMappingTracker = ListenerMappingTracker
//				.createTracker(extenderContext, bundleContext);
//
//		listenerMappingTracker.open();
//		trackers.add(0, listenerMappingTracker);
//	}
//
//	/**
//	 * Track JSPs.
//	 *
//	 * @param bundleContext the BundleContext associated with this bundle
//	 */
//	private void trackJspMappings(final BundleContext bundleContext) {
//		final ServiceTracker<JspMapping, JspWebElement> jspMappingTracker = JspMappingTracker
//				.createTracker(extenderContext, bundleContext);
//
//		jspMappingTracker.open();
//		trackers.add(0, jspMappingTracker);
//	}
//
//	/**
//	 * Track welcome files
//	 *
//	 * @param bundleContext the BundleContext associated with this bundle
//	 */
//	private void trackWelcomeFiles(final BundleContext bundleContext) {
//		final ServiceTracker<WelcomeFileMapping, WelcomeFileWebElement> welcomeFileTracker = WelcomeFileMappingTracker
//				.createTracker(extenderContext, bundleContext);
//
//		welcomeFileTracker.open();
//		trackers.add(0, welcomeFileTracker);
//	}
//
//	/**
//	 * Track error pages
//	 *
//	 * @param bundleContext the BundleContext associated with this bundle
//	 */
//	private void trackErrorPages(final BundleContext bundleContext) {
//		final ServiceTracker<ErrorPageMapping, ErrorPageWebElement> errorPagesTracker = ErrorPageMappingTracker
//				.createTracker(extenderContext, bundleContext);
//		errorPagesTracker.open();
//		trackers.add(0, errorPagesTracker);
//	}
//
//	/**
//	 * Track WebSockets
//	 *
//	 * @param bundleContext the BundleContext associated with this bundle
//	 */
//	private void trackWebSockets(final BundleContext bundleContext) {
//		final ServiceTracker<Object, WebSocketElement> webSocketTracker = WebSocketTracker.createTracker(extenderContext, bundleContext);
//		webSocketTracker.open();
//		trackers.add(0, webSocketTracker);
//	}

}
