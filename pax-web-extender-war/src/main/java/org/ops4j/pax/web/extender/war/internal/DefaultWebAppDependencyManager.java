/* Copyright 2012 Harald Wellmann
 * Copyright 2016 Achim Nierbeck
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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.tracker.ReplaceableService;
import org.ops4j.pax.web.extender.war.internal.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks dependencies for web applications which do not require external
 * customization.
 * <p>
 * It publishes a {@link WebAppDependencyHolder} service for a web application
 * whenever the given web application and the required HTTP service are both
 * available.
 *
 * @author Harald Wellmann
 */
public class DefaultWebAppDependencyManager {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultWebAppDependencyHolder.class);

	private ConcurrentMap<WebApp, ReplaceableService<HttpService>> trackers;
	private ConcurrentHashMap<WebApp, ServiceRegistration<WebAppDependencyHolder>> services;


	public DefaultWebAppDependencyManager() {
		this.trackers = new ConcurrentHashMap<>();
		this.services = new ConcurrentHashMap<>();
	}

	public void addWebApp(final WebApp webApp) {
		final BundleContext webAppContext = webApp.getBundle().getBundleContext();
		ReplaceableService<HttpService> tracker = new ReplaceableService<>(
				webAppContext, HttpService.class, new ReplaceableServiceListener<HttpService>() {
			@Override
			public void serviceChanged(HttpService oldService, HttpService newService) {
				ServiceRegistration<WebAppDependencyHolder> oldReg;
				ServiceRegistration<WebAppDependencyHolder> newReg;
				if (newService != null) {
					WebAppDependencyHolder holder = new DefaultWebAppDependencyHolder(newService);
					Dictionary<String, String> props = new Hashtable<>();
					props.put("bundle.id", Long.toString(webApp.getBundle().getBundleId()));
					newReg = webAppContext.registerService(WebAppDependencyHolder.class, holder, props);
				} else {
					newReg = null;
				}
				synchronized (this) {
					oldReg = services.containsKey(webApp) ? services.get(webApp) : null;
					if (newReg != null) {
						services.put(webApp, newReg);
					} else {
						services.remove(webApp);
					}
				}
				if (oldReg != null) {
					try {
						oldReg.unregister();
					} catch (IllegalStateException e) {
						//ignore, service is already gone.
						LOG.info("Unregistering an already unregistered Service: {} ", oldReg.getClass());
					}
				}
			}
		});
		if (trackers.putIfAbsent(webApp, tracker) == null) {
			tracker.start();
		}
	}

	public void removeWebApp(WebApp webApp) {
		ServiceRegistration<WebAppDependencyHolder> serviceRegistration = services.get(webApp);
		if (serviceRegistration != null) {
			try {
				serviceRegistration.unregister();
			} catch (IllegalStateException e) {
				// ignore, service is already gone.
				LOG.info("Unregistering an already unregistered Service: {} ", serviceRegistration.getClass());
			}
		}
		ReplaceableService<HttpService> tracker = trackers.remove(webApp);
		if (tracker != null) {
			tracker.stop();
		}
	}

}
