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
import java.util.EventListener;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.extender.whiteboard.FilterMapping;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.JspMapping;
import org.ops4j.pax.web.extender.whiteboard.ListenerMapping;
import org.ops4j.pax.web.extender.whiteboard.ResourceMapping;
import org.ops4j.pax.web.extender.whiteboard.ServletMapping;
import org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ErrorPageWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.FilterWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.JspWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ListenerWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ResourceWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ServletWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WelcomeFileWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ErrorPageMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.FilterMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.FilterTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.HttpContextMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.HttpContextTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.JspMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ListenerMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ListenerTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ResourceMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.WelcomeFileMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
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

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	/**
	 * Extender context.
	 */
	private ExtenderContext extenderContext;
	/**
	 * List of service trackers.
	 */
	private List<ServiceTracker<?, ?>> trackers;

	/**
	 * @see BundleActivator#start(BundleContext)
	 */
	public void start(final BundleContext bundleContext) throws Exception {
		extenderContext = new ExtenderContext();
		extenderContext.open(bundleContext);
		trackers = new ArrayList<ServiceTracker<?, ?>>();

		trackHttpContexts(bundleContext);
		trackServlets(bundleContext);
		trackResources(bundleContext);
		if (WebContainerUtils.WEB_CONATAINER_AVAILABLE) {
			trackFilters(bundleContext);
			trackListeners(bundleContext);
			trackJspMappings(bundleContext);
			trackErrorPages(bundleContext);
			trackWelcomeFiles(bundleContext);
		} else {
			LOG.warn("Filters tracking has been disabled as the WebContainer (Pax Web) is not available");
			LOG.warn("Event Listeners tracking has been disabled as the WebContainer (Pax Web) is not available");
			LOG.warn("JSP mappings tracking has been disabled as the WebContainer (Pax Web) is not available");
		}
		LOG.debug("Pax Web Extender started");
	}

	/**
	 * @see BundleActivator#stop(BundleContext)
	 */
	public void stop(final BundleContext bundleContext) throws Exception {
		for (ServiceTracker<?, ?> tracker : trackers) {
			tracker.close();
		}
		trackers = null;
		extenderContext.close(bundleContext);
		LOG.debug("Pax Web Extender stopped");
	}

	/**
	 * Track http contexts.
	 * 
	 * @param bundleContext
	 *            a bundle context
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
	 * @param bundleContext
	 *            a bundle context
	 */
	private void trackServlets(final BundleContext bundleContext) {
		final ServiceTracker<Servlet, ServletWebElement> servletTracker = ServletTracker
				.createTracker(extenderContext, bundleContext, Servlet.class);

		servletTracker.open();
		trackers.add(0, servletTracker);

		final ServiceTracker<HttpServlet, ServletWebElement> httpServletTracker = ServletTracker
				.createTracker(extenderContext, bundleContext,
						HttpServlet.class);

		httpServletTracker.open();
		trackers.add(0, servletTracker);

		final ServiceTracker<ServletMapping, ServletWebElement> servletMappingTracker = ServletMappingTracker
				.createTracker(extenderContext, bundleContext);

		servletMappingTracker.open();
		trackers.add(0, servletMappingTracker);
	}

	/**
	 * Track resources.
	 * 
	 * @param bundleContext
	 *            a bundle context
	 */
	private void trackResources(final BundleContext bundleContext) {
		final ServiceTracker<ResourceMapping, ResourceWebElement> resourceMappingTracker = ResourceMappingTracker
				.createTracker(extenderContext, bundleContext);

		resourceMappingTracker.open();
		trackers.add(0, resourceMappingTracker);
	}

	/**
	 * Track filters.
	 * 
	 * @param bundleContext
	 *            a bundle context
	 */
	private void trackFilters(final BundleContext bundleContext) {
		final ServiceTracker<Filter, FilterWebElement> filterTracker = FilterTracker
				.createTracker(extenderContext, bundleContext);

		filterTracker.open();
		trackers.add(0, filterTracker);

		final ServiceTracker<FilterMapping, FilterWebElement> filterMappingTracker = FilterMappingTracker
				.createTracker(extenderContext, bundleContext);

		filterMappingTracker.open();
		trackers.add(0, filterMappingTracker);
	}

	/**
	 * Track listeners.
	 * 
	 * @param bundleContext
	 *            a bundle context
	 */
	private void trackListeners(final BundleContext bundleContext) {
		final ServiceTracker<EventListener, ListenerWebElement> listenerTracker = ListenerTracker
				.createTracker(extenderContext, bundleContext);

		listenerTracker.open();
		trackers.add(0, listenerTracker);

		final ServiceTracker<ListenerMapping, ListenerWebElement> listenerMappingTracker = ListenerMappingTracker
				.createTracker(extenderContext, bundleContext);

		listenerMappingTracker.open();
		trackers.add(0, listenerMappingTracker);
	}

	/**
	 * Track jsps.
	 * 
	 * @param bundleContext
	 *            a bundle context
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
	 * @param bundleContext
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
	 * @param bundleContext
	 */
	private void trackErrorPages(final BundleContext bundleContext) {
		final ServiceTracker<ErrorPageMapping, ErrorPageWebElement> errorPagesTracker = ErrorPageMappingTracker
				.createTracker(extenderContext, bundleContext);
		errorPagesTracker.open();
		trackers.add(0, errorPagesTracker);
	}

}
