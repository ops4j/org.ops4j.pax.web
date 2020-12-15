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
package org.ops4j.pax.web.extender.war.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.swissbox.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publish/Unpublish a web application.
 *
 * @author Alin Dreghiciu
 * @author Marc Klinger - mklinger[at]nightlabs[dot]de
 * @since 0.3.0, December 27, 2007
 */
class WebAppPublisher {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(WebAppPublisher.class);
	/**
	 * In use web apps.
	 */
	private final Map<WebApp, ServiceTracker<WebAppDependencyHolder, WebAppDependencyHolder>> webApps;

	private final WebApplicationEventDispatcher eventDispatcher;

	private final BundleContext bundleContext;

	/**
	 * Creates a new web app publisher.
	 */
	WebAppPublisher(WebApplicationEventDispatcher eventDispatcher,
					BundleContext bundleContext) {
		webApps = Collections
				.synchronizedMap(new HashMap<WebApp, ServiceTracker<WebAppDependencyHolder, WebAppDependencyHolder>>());
		this.eventDispatcher = eventDispatcher;
		this.bundleContext = bundleContext;
	}

	/**
	 * Publish a web application.
	 *
	 * @param webApp web application to be published.
	 * @throws NullArgumentException if web app is null
	 */
	public void publish(final WebApp webApp) {
		NullArgumentException.validateNotNull(webApp, "Web app");
		LOG.debug("Publishing web application [{}]", webApp);
		final BundleContext webAppBundleContext = BundleUtils
				.getBundleContext(webApp.getBundle());
		if (webAppBundleContext != null) {
			try {
				Filter filter = webAppBundleContext.createFilter(String.format(
						"(&(objectClass=%s)(bundle.id=%d))",
						WebAppDependencyHolder.class.getName(), webApp
								.getBundle().getBundleId()));
				ServiceTracker<WebAppDependencyHolder, WebAppDependencyHolder> dependencyTracker = new ServiceTracker<WebAppDependencyHolder, WebAppDependencyHolder>(
						webAppBundleContext, filter,
						new WebAppDependencyListener(webApp, eventDispatcher,
								bundleContext));
				webApps.put(webApp, dependencyTracker);
				dependencyTracker.open();
			} catch (InvalidSyntaxException exc) {
				throw new IllegalArgumentException(exc);
			}
		} else {
			LOG.warn("Bundle context could not be discovered for bundle ["
					+ webApp.getBundle() + "]"
					+ "Skipping publishing of web application [" + webApp + "]");
		}
	}

	/**
	 * Unpublish a web application.
	 *
	 * @param webApp web aplication to be unpublished
	 * @throws NullArgumentException if web app is null
	 */
	public void unpublish(final WebApp webApp) {
		NullArgumentException.validateNotNull(webApp, "Web app");
		LOG.debug("Unpublishing web application [{}]", webApp);
		final ServiceTracker<WebAppDependencyHolder, WebAppDependencyHolder> tracker = webApps
				.remove(webApp);
		if (tracker != null) {
			tracker.close();
		}
	}

	/**
	 * Http Service listener that will register/unregister the web app as soon
	 * as an http service becomes available/unavailable.
	 */
	public static class WebAppDependencyListener
			implements
			ServiceTrackerCustomizer<WebAppDependencyHolder, WebAppDependencyHolder> {

		/**
		 * Web app to be registered.
		 */
		private final WebApp webApp;

		private final WebApplicationEventDispatcher eventDispatcher;

		private BundleContext bundleContext;

		/**
		 * Http service in use.
		 */
		private HttpService httpService;

		private WebAppDependencyHolder dependencyHolder;

		/**
		 * Creates a new http service listener.
		 *
		 * @param webApp web app to be registered
		 * @throws NullArgumentException if web app is null
		 */
		WebAppDependencyListener(final WebApp webApp,
								 WebApplicationEventDispatcher eventDispatcher, BundleContext bundleContext) {
			NullArgumentException.validateNotNull(webApp, "Web app");
			this.webApp = webApp;
			this.eventDispatcher = eventDispatcher;
			this.bundleContext = bundleContext;
		}

		@Override
		public WebAppDependencyHolder addingService(
				ServiceReference<WebAppDependencyHolder> reference) {
			LOG.debug("Adding service for service reference {}", reference);
			WebAppDependencyHolder webAppDependencyHolder = bundleContext.getService(reference);
			HttpService webAppHttpService = webAppDependencyHolder.getHttpService();
			synchronized (this) {
				this.dependencyHolder = webAppDependencyHolder;
				this.httpService = webAppHttpService;
			}
			register(webAppDependencyHolder, webAppHttpService);
			return webAppDependencyHolder;
		}

		/**
		 * In case that the http service changes, first unregister the web app
		 * from the old one (if not null) and then register the web app with the
		 * new service.
		 *
		 * @see ReplaceableServiceListener#serviceChanged(Object, Object)
		 */
		@Override
		public void modifiedService(
				ServiceReference<WebAppDependencyHolder> reference,
				WebAppDependencyHolder service) {
			LOG.debug("modified Service for service reference {}", reference);
			WebAppDependencyHolder oldDependencyHolder;
			HttpService oldHttpService;
			WebAppDependencyHolder newDependencyHolder = bundleContext.getService(reference);
			HttpService newHttpService = newDependencyHolder.getHttpService();
			synchronized (this) {
				oldDependencyHolder = this.dependencyHolder;
				oldHttpService = this.httpService;
				this.dependencyHolder = newDependencyHolder;
				this.httpService = newHttpService;
			}
			unregister(oldDependencyHolder, oldHttpService);
			register(newDependencyHolder, newHttpService);
		}

		@Override
		public void removedService(
				ServiceReference<WebAppDependencyHolder> reference,
				WebAppDependencyHolder service) {
			WebAppDependencyHolder webAppDependencyHolder;
			HttpService webAppHttpService;
			synchronized (this) {
				webAppDependencyHolder = this.dependencyHolder;
				webAppHttpService = this.httpService;
				this.dependencyHolder = null;
				this.httpService = null;
			}
			unregister(webAppDependencyHolder, webAppHttpService);
		}

		/**
		 * Registers a web app with current http service, if any.
		 */
		private void register(WebAppDependencyHolder webAppDependencyHolder, HttpService webAppHttpService) {
			if (webAppHttpService != null) {
				LOG.debug(
						"Registering web application [{}] from http service [{}]",
						webApp, webAppHttpService);
				try {
					if (WebContainerUtils.webContainerAvailable(webAppHttpService)) {
						webApp.accept(new RegisterWebAppVisitorWC(
								webAppDependencyHolder));
					} else {
						webApp.accept(new RegisterWebAppVisitorHS(webAppHttpService));
					}

					webApp.setDeploymentState(WebEvent.DEPLOYED);
					eventDispatcher.webEvent(webApp, WebEvent.DEPLOYED,
							webAppHttpService);
					//CHECKSTYLE:OFF
				} catch (Throwable e) {
					LOG.error("Error deploying web application", e);
					eventDispatcher.webEvent(webApp, WebEvent.FAILED, e);
				}
				//CHECKSTYLE:ON
			}
		}

		/**
		 * Unregisters a web app from current http service, if any.
		 */
		private void unregister(WebAppDependencyHolder webAppDependencyHolder, HttpService webAppHttpService) {
			if (webAppHttpService != null) {
				try {
					LOG.debug(
							"Unregistering web application [{}] from http service [{}]",
							webApp, webAppHttpService);

					if (WebContainerUtils.webContainerAvailable(webAppHttpService)) {
						webApp.accept(new UnregisterWebAppVisitorWC(
								(WebContainer) webAppHttpService));
					} else {
						webApp.accept(new UnregisterWebAppVisitorHS(webAppHttpService));
					}
					//CHECKSTYLE:OFF
				} catch (Exception e) {
					LOG.warn("Error undeploying web application", e);
				}
				//CHECKSTYLE:ON
			}
		}

	}

}
