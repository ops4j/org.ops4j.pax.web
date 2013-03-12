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
import org.ops4j.pax.web.service.spi.ServletContextManager;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
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

	/**
	 * Creates a new web app publisher.
	 */
	WebAppPublisher() {
		webApps = Collections
				.synchronizedMap(new HashMap<WebApp, ServiceTracker<WebAppDependencyHolder, WebAppDependencyHolder>>());
	}

	/**
	 * Publish a web application.
	 * 
	 * @param webApp
	 *            web application to be published.
	 * 
	 * @throws NullArgumentException
	 *             if web app is null
	 */
	public void publish(final WebApp webApp,
			final WebEventDispatcher eventDispatcher,
			BundleContext bundleContext) {
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
				dependencyTracker.open();
				webApps.put(webApp, dependencyTracker);
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
	 * @param webApp
	 *            web aplication to be unpublished
	 * 
	 * @throws NullArgumentException
	 *             if web app is null
	 */
	public void unpublish(final WebApp webApp) {
		NullArgumentException.validateNotNull(webApp, "Web app");
		LOG.debug("Unpublishing web application [{}]", webApp);
		final ServiceTracker<WebAppDependencyHolder, WebAppDependencyHolder> httpServiceTracker = webApps
				.get(webApp);
		if (httpServiceTracker != null) {
			webApps.remove(webApp);
			// if the bundle is not active then do nothing as http service
			// already released all the web app
			if (Bundle.ACTIVE == webApp.getBundle().getState()) {
				httpServiceTracker.close();
			}
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

		private final WebEventDispatcher eventDispatcher;

		private BundleContext bundleContext;

		/**
		 * Http service in use.
		 */
		private HttpService httpService;

		private WebAppDependencyHolder dependencyHolder;

		/**
		 * Creates a new http service listener.
		 * 
		 * @param webApp
		 *            web app to be registered
		 * 
		 * @throws NullArgumentException
		 *             if web app is null
		 */
		WebAppDependencyListener(final WebApp webApp,
				WebEventDispatcher eventDispatcher, BundleContext bundleContext) {
			NullArgumentException.validateNotNull(webApp, "Web app");
			this.webApp = webApp;
			this.eventDispatcher = eventDispatcher;
			this.bundleContext = bundleContext;
		}

		/**
		 * In case that the http service changes, first unregister the web app
		 * from the old one (if not null) and then register the web app with the
		 * new service.
		 * 
		 * @see ReplaceableServiceListener#serviceChanged(Object, Object)
		 */
		@Override
		public synchronized void modifiedService(
				ServiceReference<WebAppDependencyHolder> reference,
				WebAppDependencyHolder service) {
			LOG.debug("modified Service for service reference {}", reference);
			unregister();
			dependencyHolder = service;
			httpService = dependencyHolder.getHttpService();
			register();
		}

		/**
		 * Registers a web app with current http service, if any.
		 */
		private void register() {
			if (httpService != null) {
				LOG.debug("Registering web application [{}] from http service [{}]", webApp, httpService);
				if (WebContainerUtils.webContainerAvailable(httpService)) {
					webApp.accept(new RegisterWebAppVisitorWC(dependencyHolder));
				} else {
					webApp.accept(new RegisterWebAppVisitorHS(httpService));
				}

				/*
				 * In Pax Web 2, the servlet context was started on creation,
				 * implicitly on registering the first servlet.
				 * 
				 * In Pax Web 3, we support extensions registering a servlet
				 * container initializer to customize the servlet context, e.g.
				 * by decorating servlets. For decorators to have any effect,
				 * the servlet context must not be started when the decorators
				 * are registered.
				 * 
				 * At this point, the servlet context is fully configured, so
				 * this is the right time to start it.
				 */
				ServletContextManager.startContext("/"
						+ webApp.getContextName());

				webApp.setDeploymentState(WebApp.DEPLOYED_STATE);
				eventDispatcher.webEvent(new WebEvent(WebEvent.DEPLOYED, "/"
						+ webApp.getContextName(), webApp.getBundle(),
						bundleContext.getBundle(), httpService, webApp
								.getHttpContext()));
			}
		}

		/**
		 * Unregisters a web app from current http service, if any.
		 */
		private void unregister() {
			if (httpService != null) {
				LOG.debug("Unregistering web application [{}] from http service [{}]", webApp, httpService );
				if (WebContainerUtils.webContainerAvailable(httpService)) {
					webApp.accept(new UnregisterWebAppVisitorWC(
							(WebContainer) httpService));
				} else {
					webApp.accept(new UnregisterWebAppVisitorHS(httpService));
				}
			}
		}

		@Override
		public WebAppDependencyHolder addingService(
				ServiceReference<WebAppDependencyHolder> reference) {
			LOG.debug("Adding service for service reference {}", reference);
			WebAppDependencyHolder service = bundleContext
					.getService(reference);
			dependencyHolder = service;
			httpService = service.getHttpService();
			register();
			return service;
		}

		@Override
		public void removedService(
				ServiceReference<WebAppDependencyHolder> reference,
				WebAppDependencyHolder service) {
			unregister();
		}

	}

}
