/* 
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.internal.element.*;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.*;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.whiteboard.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activates the pax web extender.
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
	/**
	 * List of service trackers. All trackers get closed on Bundle.stop
	 */
	private List<ServiceTracker<?, ?>> trackers;

	@Override
	public void start(final BundleContext bundleContext) throws Exception {
		// prepare whiteboard-DTO service
		ExtendedHttpServiceRuntime httpServiceRuntime = new ExtendedHttpServiceRuntime(bundleContext);
		bundleContext.registerService(HttpServiceRuntime.class, httpServiceRuntime, null);

		extenderContext = new ExtenderContext(httpServiceRuntime);
		trackers = new ArrayList<>();

		trackHttpContexts(bundleContext);
		trackServlets(bundleContext);
		trackResources(bundleContext);
		if (WebContainerUtils.WEB_CONATAINER_AVAILABLE) {
			trackFilters(bundleContext);
			trackListeners(bundleContext);
			trackJspMappings(bundleContext);
			trackErrorPages(bundleContext);
			trackWelcomeFiles(bundleContext);
			trackServletContextHelper(bundleContext);
			if (WebContainerUtils.WEBSOCKETS_AVAILABLE) {
				trackWebSockets(bundleContext);
			} else {
				LOG.info("No javax.websocket.Endpoint class found, WebSocketTracker is disabled");
			}
		} else {
			LOG.warn("Filters tracking has been disabled as the WebContainer (Pax Web) is not available");
			LOG.warn("Event Listeners tracking has been disabled as the WebContainer (Pax Web) is not available");
			LOG.warn("JSP mappings tracking has been disabled as the WebContainer (Pax Web) is not available");
		}
		LOG.debug("Pax Web Whiteboard Extender started");
	}

	@Override
	public void stop(final BundleContext bundleContext) throws Exception {
		List<ServiceTracker<?, ?>> serviceTrackers = new ArrayList<>(this.trackers);
		Collections.reverse(serviceTrackers);
		for (ServiceTracker<?, ?> tracker : serviceTrackers) {
			tracker.close();
		}
		this.trackers = null;
		LOG.debug("Pax Web Extender stopped");
	}

	/**
	 * Track http contexts.
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackHttpContexts(final BundleContext bundleContext) {
		final ServiceTracker<HttpContext, HttpContextMapping> httpContextTracker = HttpContextTracker
				.createTracker(extenderContext, bundleContext);

		httpContextTracker.open();
		trackers.add(0, httpContextTracker);

		final ServiceTracker<HttpContextMapping, HttpContextMapping> httpContextMappingTracker = HttpContextMappingTracker
				.createTracker(extenderContext, bundleContext);

		httpContextMappingTracker.open();
		trackers.add(0, httpContextMappingTracker);
	}

	/**
	 * Track servlets.
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackServletContextHelper(final BundleContext bundleContext) {
		final ServiceTracker<ServletContextHelper, ServletContextHelper> servletContextHelperTracker = ServletContextHelperTracker.createTracker(extenderContext, bundleContext);

		servletContextHelperTracker.open();
		trackers.add(0, servletContextHelperTracker);

	}

	/**
	 * Track servlets.
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackServlets(final BundleContext bundleContext) {
		final ServiceTracker<Servlet, ServletWebElement> servletTracker = ServletTracker
				.createTracker(extenderContext, bundleContext);

		servletTracker.open();
		trackers.add(0, servletTracker);

		final ServiceTracker<ServletMapping, ServletMappingWebElement> servletMappingTracker = ServletMappingTracker
				.createTracker(extenderContext, bundleContext);

		servletMappingTracker.open();
		trackers.add(0, servletMappingTracker);
	}

	/**
	 * Track resources.
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackResources(final BundleContext bundleContext) {
		ServiceTracker<Object, ResourceWebElement> resourceTracker = ResourceTracker.createTracker(extenderContext, bundleContext);

		resourceTracker.open();
		trackers.add(0, resourceTracker);

		final ServiceTracker<ResourceMapping, ResourceMappingWebElement> resourceMappingTracker = ResourceMappingTracker
				.createTracker(extenderContext, bundleContext);

		resourceMappingTracker.open();
		trackers.add(0, resourceMappingTracker);
	}

	/**
	 * Track filters.
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackFilters(final BundleContext bundleContext) {
		final ServiceTracker<Filter, FilterWebElement> filterTracker = FilterTracker
				.createTracker(extenderContext, bundleContext);

		filterTracker.open();
		trackers.add(0, filterTracker);

		// FIXME needed?
		final ServiceTracker<FilterMapping, FilterMappingWebElement> filterMappingTracker = FilterMappingTracker
				.createTracker(extenderContext, bundleContext);

		filterMappingTracker.open();
		trackers.add(0, filterMappingTracker);
	}

	/**
	 * Track listeners.
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackListeners(final BundleContext bundleContext) {
		final ServiceTracker<EventListener, ListenerWebElement> listenerTracker = ListenerTracker
				.createTracker(extenderContext, bundleContext);

		listenerTracker.open();
		trackers.add(0, listenerTracker);

		// FIXME needed?
		final ServiceTracker<ListenerMapping, ListenerMappingWebElement> listenerMappingTracker = ListenerMappingTracker
				.createTracker(extenderContext, bundleContext);

		listenerMappingTracker.open();
		trackers.add(0, listenerMappingTracker);
	}

	/**
	 * Track JSPs.
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackJspMappings(final BundleContext bundleContext) {
		final ServiceTracker<JspMapping, JspWebElement> jspMappingTracker = JspMappingTracker
				.createTracker(extenderContext, bundleContext);

		jspMappingTracker.open();
		trackers.add(0, jspMappingTracker);
	}

	/**
	 * Track welcome files
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackWelcomeFiles(final BundleContext bundleContext) {
		final ServiceTracker<WelcomeFileMapping, WelcomeFileWebElement> welcomeFileTracker = WelcomeFileMappingTracker
				.createTracker(extenderContext, bundleContext);

		welcomeFileTracker.open();
		trackers.add(0, welcomeFileTracker);
	}

	/**
	 * Track error pages
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackErrorPages(final BundleContext bundleContext) {
		final ServiceTracker<ErrorPageMapping, ErrorPageWebElement> errorPagesTracker = ErrorPageMappingTracker
				.createTracker(extenderContext, bundleContext);
		errorPagesTracker.open();
		trackers.add(0, errorPagesTracker);
	}

	/**
	 * Track WebSockets
	 *
	 * @param bundleContext the BundleContext associated with this bundle
	 */
	private void trackWebSockets(final BundleContext bundleContext) {
		final ServiceTracker<Object, WebSocketElement> webSocketTracker = WebSocketTracker.createTracker(extenderContext, bundleContext);
		webSocketTracker.open();
		trackers.add(0, webSocketTracker);
	}
}
