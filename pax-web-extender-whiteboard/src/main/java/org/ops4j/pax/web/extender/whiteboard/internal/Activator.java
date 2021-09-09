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
import java.util.EventListener;
import java.util.Hashtable;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.internal.tracker.FilterTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.HttpContextTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ListenerTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ResourceTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletContextHelperTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.WebSocketTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ErrorPageMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.FilterMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.HttpContextMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.JspMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ListenerMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ResourceMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ServletContextHelperMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ServletMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.WelcomeFileMappingTracker;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.context.DefaultServletContextHelper;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.JspModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WebSocketModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.service.whiteboard.FilterMapping;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.whiteboard.JspMapping;
import org.ops4j.pax.web.service.whiteboard.ListenerMapping;
import org.ops4j.pax.web.service.whiteboard.ResourceMapping;
import org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
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

	/** Nothing can be done without it */
	private BundleContext context;

	/** Whiteboard Extender context - this is where the Whiteboard magic is managed. */
	private WhiteboardExtenderContext whiteboardExtenderContext;

	/** The default {@link ServletContextHelper} registration as in 140.2 The Servlet Context*/
	private ServiceRegistration<ServletContextHelper> registration;

	/**
	 * List of service trackers. All trackers get closed on {@link BundleActivator#stop(BundleContext)}. This is
	 * how the "whiteboard extender" learns about <em>incoming</em> services which may be added to the <em>web
	 * applications</em> managed by the extender.
	 */
	private final List<ServiceTracker<?, ?>> trackers = new ArrayList<>();

	@Override
	public void start(final BundleContext bundleContext) throws Exception {
		LOG.info("Starting Pax Web Whiteboard Extender");

		this.context = bundleContext;

		// this is where "trackers" register/unregister their customized objects.
		// a short summary:
		//  - we have lot of customizers in org.ops4j.pax.web.extender.whiteboard.internal.tracker package ("tracker"
		//    name was always used...)
		//  - each such customizer has static method to create actual ServiceTracker with "this" as the customizer
		//  - each customizer translates "incoming service" like javax.servlet.Servlet into "internal model" (a.k.a.
		//    "customized object")
		//  - each tracker takes "bundle whiteboard application" (collection of web elements/contexts registered by
		//    given bundle) from the extender context and registers/unregister such "customized object"
		//  - extender contains refrence to current WebContainer, so the whiteboard elements/contexts may be
		//    registered there
		//  - the reference to WebContainer is (known to be) a service factory, so it has to be dereferenced using
		//    proper bundle(context) - the one associated with the web element/context being registered
		//
		// in other words - "extender context" is the component that bridges between tracked web elements + customized
		// model elements on one side and "current" WebContainer obtained using proper bundle(context) on the other side
		whiteboardExtenderContext = new WhiteboardExtenderContext(bundleContext);

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
		properties.put(Constants.SERVICE_RANKING, defaultContextModel.getServiceRank());
		properties.put(PaxWebConstants.SERVICE_PROPERTY_INTERNAL, true);
		// TODO: props.put(PaxWebConstants.SERVICE_PROPERTY_VIRTUAL_HOSTS, new String[] { "*" });
		registration = bundleContext.registerService(ServletContextHelper.class,
				new DefaultServletContextHelperServiceFactory(), properties);

		// web contexts
		trackHttpContexts();
		trackServletContextHelpers();

		// web elements
		trackServlets();
		trackFilters();
		trackResources();
		trackWelcomeFiles();
		trackErrorPages();
		if (Utils.getPaxWebJspBundle(bundleContext) != null) {
			trackJspMappings();
		}

		// other
		trackListeners();
		trackWebSockets();

		LOG.debug("Pax Web Whiteboard Extender started");
	}

	@Override
	public void stop(final BundleContext bundleContext) throws Exception {
		LOG.debug("Stopping Pax Web Whiteboard Extender");

		whiteboardExtenderContext.acceptWabContexts.set(false);

		List<ServiceTracker<?, ?>> serviceTrackers = new ArrayList<>(this.trackers);
		Collections.reverse(serviceTrackers);
		for (ServiceTracker<?, ?> tracker : serviceTrackers) {
			tracker.close();
		}
		this.trackers.clear();

		if (registration != null) {
			registration.unregister();
			registration = null;
		}

		whiteboardExtenderContext.shutdown();

		LOG.debug("Pax Web Whiteboard Extender stopped");
	}

	/**
	 * Track {@link HttpContext} and {@link HttpContextMapping} services.
	 */
	private void trackHttpContexts() {
		ServiceTracker<HttpContext, OsgiContextModel> httpContextTracker
				= HttpContextTracker.createTracker(whiteboardExtenderContext, context);
		httpContextTracker.open();
		trackers.add(0, httpContextTracker);

		ServiceTracker<HttpContextMapping, OsgiContextModel> httpContextMappingTracker
				= HttpContextMappingTracker.createTracker(whiteboardExtenderContext, context);
		httpContextMappingTracker.open();
		trackers.add(0, httpContextMappingTracker);
	}

	/**
	 * Track {@link ServletContextHelper} and {@link ServletContextHelperMapping} services.
	 */
	private void trackServletContextHelpers() {
		ServiceTracker<ServletContextHelper, OsgiContextModel> servletContextHelperTracker
				= ServletContextHelperTracker.createTracker(whiteboardExtenderContext, context);
		servletContextHelperTracker.open();
		trackers.add(0, servletContextHelperTracker);

		ServiceTracker<ServletContextHelperMapping, OsgiContextModel> servletContextHelperMappingTracker
				= ServletContextHelperMappingTracker.createTracker(whiteboardExtenderContext, context);
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
				= ServletTracker.createTracker(whiteboardExtenderContext, context);
		servletTracker.open();
		trackers.add(servletTracker);

		ServiceTracker<ServletMapping, ServletModel> servletMappingTracker
				= ServletMappingTracker.createTracker(whiteboardExtenderContext, context);
		servletMappingTracker.open();
		trackers.add(servletMappingTracker);
	}

	/**
	 * <p>Track resources:<ul>
	 *     <li>Any object from OSGi CPMN R7 Whiteboard Service specification with
	 *     {@link HttpWhiteboardConstants#HTTP_WHITEBOARD_RESOURCE_PATTERN} registration property</li>
	 *     <li>{@link ResourceMapping} from Pax Web</li>
	 * </ul></p>
	 */
	private void trackResources() {
		ServiceTracker<Object, ServletModel> resourceTracker
				= ResourceTracker.createTracker(whiteboardExtenderContext, context);
		resourceTracker.open();
		trackers.add(resourceTracker);

		ServiceTracker<ResourceMapping, ServletModel> resourceMappingTracker
				= ResourceMappingTracker.createTracker(whiteboardExtenderContext, context);
		resourceMappingTracker.open();
		trackers.add(resourceMappingTracker);
	}

	/**
	 * <p>Track filters:<ul>
	 *     <li>{@link Filter} from from OSGi CPMN R7 Whiteboard Service specification</li>
	 *     <li>{@link FilterMapping} from Pax Web</li>
	 * </ul></p>
	 */
	private void trackFilters() {
		final ServiceTracker<Filter, FilterModel> filterTracker
				= FilterTracker.createTracker(whiteboardExtenderContext, context);
		filterTracker.open();
		trackers.add(filterTracker);

		final ServiceTracker<FilterMapping, FilterModel> filterMappingTracker
				= FilterMappingTracker.createTracker(whiteboardExtenderContext, context);
		filterMappingTracker.open();
		trackers.add(filterMappingTracker);
	}

	/**
	 * Track welcome files
	 */
	private void trackWelcomeFiles() {
		final ServiceTracker<WelcomeFileMapping, WelcomeFileModel> welcomeFileTracker
				= WelcomeFileMappingTracker.createTracker(whiteboardExtenderContext, context);
		welcomeFileTracker.open();
		trackers.add(welcomeFileTracker);
	}

	/**
	 * Track error pages
	 */
	private void trackErrorPages() {
		final ServiceTracker<ErrorPageMapping, ErrorPageModel> errorPagesTracker
				= ErrorPageMappingTracker.createTracker(whiteboardExtenderContext, context);
		errorPagesTracker.open();
		trackers.add(errorPagesTracker);
	}

	/**
	 * Track listeners.
	 */
	private void trackListeners() {
		final ServiceTracker<EventListener, EventListenerModel> listenerTracker
				= ListenerTracker.createTracker(whiteboardExtenderContext, context);
		listenerTracker.open();
		trackers.add(listenerTracker);

		final ServiceTracker<ListenerMapping, EventListenerModel> listenerMappingTracker
				= ListenerMappingTracker.createTracker(whiteboardExtenderContext, context);
		listenerMappingTracker.open();
		trackers.add(listenerMappingTracker);
	}

	/**
	 * Track JSPs.
	 */
	private void trackJspMappings() {
		final ServiceTracker<JspMapping, JspModel> jspMappingTracker
				= JspMappingTracker.createTracker(whiteboardExtenderContext, context);

		jspMappingTracker.open();
		trackers.add(jspMappingTracker);
	}

	/**
	 * Track WebSockets
	 */
	private void trackWebSockets() {
		final ServiceTracker<Object, WebSocketModel> webSocketTracker
				= WebSocketTracker.createTracker(whiteboardExtenderContext, context);
		webSocketTracker.open();
		trackers.add(webSocketTracker);
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

}
