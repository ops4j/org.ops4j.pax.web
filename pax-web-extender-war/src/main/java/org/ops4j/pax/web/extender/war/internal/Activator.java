/*
 * Copyright 2007 Alin Dreghiciu, Achim Nierbeck.
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
package org.ops4j.pax.web.extender.war.internal;

import java.net.URL;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.ops4j.pax.swissbox.extender.BundleURLScanner;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.ops4j.pax.web.extender.war.internal.parser.dom.DOMWebXmlParser;
import org.ops4j.pax.web.service.spi.WarManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WAR Extender activator.<br/>
 * Starts an web.xml watcher on installed bundles. When a bundle containing
 * "WEB-INF/web.xml" is started the web.xml will get parsed and an web app will
 * be created. On stop of bundle containing web.xml or stop of this bundle , the
 * created web app will be unregistered.
 * 
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class Activator implements BundleActivator {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	/**
	 * Bundle watcher of web.xml.
	 */
	private BundleWatcher<URL> webXmlWatcher;

	private ServiceTracker<EventAdmin, EventAdmin> eventServiceTracker;

	private ServiceTracker<LogService, LogService> logServiceTracker;

	private ReplaceableService<HttpService> httpServiceTracker;

	private WebXmlObserver webXmlObserver;

	private BundleContext bundleContext;

	private ScheduledExecutorService executors;

	private WebEventDispatcher webEventDispatcher;

	private BundleWatcher<String> servletWatcher;

	private ServletObserver servletObserver;

	/**
	 * Starts an web.xml watcher on installed bundles.
	 * 
	 * @see BundleActivator#start(BundleContext)
	 */
	@SuppressWarnings("unchecked")
	public void start(final BundleContext bundleContext) throws Exception {
		LOG.debug("Pax Web WAR Extender - Starting");
		this.bundleContext = bundleContext;

		executors = Executors.newScheduledThreadPool(3, new ThreadFactory() {

			private final AtomicInteger count = new AtomicInteger();

			public Thread newThread(Runnable r) {
				final Thread t = Executors.defaultThreadFactory().newThread(r);
				t.setName("WebListenerExecutor" + ": "
						+ count.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		});

		webEventDispatcher = new WebEventDispatcher(bundleContext, executors);

		// Do use the filters this way the eventadmin packages can be resolved
		// optional!
		Filter filterEvent = bundleContext
				.createFilter("(objectClass=org.osgi.service.event.EventAdmin)");
		eventServiceTracker = new ServiceTracker<EventAdmin, EventAdmin>(
				bundleContext, filterEvent, new EventServiceCustomizer());
		eventServiceTracker.open();

		Filter filterLog = bundleContext
				.createFilter("(objectClass=org.osgi.service.log.LogService)");
		logServiceTracker = new ServiceTracker<LogService, LogService>(
				bundleContext, filterLog, new LogServiceCustomizer());
		logServiceTracker.open();

		DefaultWebAppDependencyManager dependencyManager = new DefaultWebAppDependencyManager(
				bundleContext);

		webXmlObserver = new WebXmlObserver(new DOMWebXmlParser(),
				new WebAppPublisher(), webEventDispatcher, dependencyManager,
				bundleContext);
		webXmlWatcher = new BundleWatcher<URL>(bundleContext,
				new BundleURLScanner("Webapp-Root", null, null, "WEB-INF/",
						"*web*.xml", true // do recurse
				), webXmlObserver);
		webXmlWatcher.start();

		// PAXWEB-410 -- begin
		servletObserver = new ServletObserver(new DOMWebXmlParser(),
				new WebAppPublisher(), webEventDispatcher, dependencyManager,
				bundleContext);
		servletWatcher = new BundleWatcher<String>(bundleContext,
				new BundleServletScanner(bundleContext), servletObserver);
		servletWatcher.start();
		// PAXWEB-410 -- end

		httpServiceTracker = new ReplaceableService<HttpService>(bundleContext,
				HttpService.class, dependencyManager);
		httpServiceTracker.start();

		bundleContext.registerService(
				new String[] { WarManager.class.getName() }, webXmlObserver,
				new Hashtable<String, Object>());
		LOG.debug("Pax Web WAR Extender - Started");
	}

	/**
	 * Stops the watcher, fact that will trigger that all registered web appas
	 * to be unregistered.
	 * 
	 * @see BundleActivator#stop(BundleContext)
	 */
	public void stop(final BundleContext bundleContext) throws Exception {
		LOG.debug("Pax Web WAR Extender - Stopping");
		// Stop the bundle watcher.
		// This will result in unpublish of each web application that was
		// registered during the lifetime of
		// bundle watcher.
		if (webXmlWatcher != null) {
			webXmlWatcher.stop();
			webXmlWatcher = null;
		}
		eventServiceTracker.close();
		logServiceTracker.close();
		httpServiceTracker.stop();
		webEventDispatcher.destroy();
		executors.shutdown();
		LOG.debug("Pax Web WAR Extender - Stopped");
	}

	private class LogServiceCustomizer implements
			ServiceTrackerCustomizer<LogService, LogService> {

		@Override
		public LogService addingService(ServiceReference<LogService> reference) {
			LogService logService = bundleContext.getService(reference);
			webEventDispatcher.setLogService(logService);
			return logService;
		}

		@Override
		public void modifiedService(ServiceReference<LogService> reference,
				LogService service) {
		}

		@Override
		public void removedService(ServiceReference<LogService> reference,
				LogService service) {
			webEventDispatcher.setLogService(null);
			bundleContext.ungetService(reference);
		}

	}

	private class EventServiceCustomizer implements
			ServiceTrackerCustomizer<EventAdmin, EventAdmin> {

		@Override
		public EventAdmin addingService(ServiceReference<EventAdmin> reference) {
			EventAdmin eventService = bundleContext.getService(reference);
			webEventDispatcher.setEventAdminService(eventService);
			return eventService;
		}

		@Override
		public void modifiedService(ServiceReference<EventAdmin> reference,
				EventAdmin service) {
		}

		@Override
		public void removedService(ServiceReference<EventAdmin> reference,
				EventAdmin service) {
			webEventDispatcher.setEventAdminService(null);
			bundleContext.ungetService(reference);
		}

	}
}
