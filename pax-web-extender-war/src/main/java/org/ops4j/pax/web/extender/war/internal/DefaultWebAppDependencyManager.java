/* Copyright 2012 Harald Wellmann
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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.ops4j.pax.swissbox.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks dependencies for web applications which do not require external
 * customization.
 * <p>
 * It publishes a {@link WebAppDependencyHolder} service for a web application
 * whenever the given web application and the required HTTP service are both
 * available.
 * 
 * @author Harald Wellmann
 * 
 */
public class DefaultWebAppDependencyManager {

	private Map<WebApp, ReplaceableService<HttpService>> trackers;

	public DefaultWebAppDependencyManager() {
		this.trackers = new HashMap<WebApp, ReplaceableService<HttpService>>();
	}

    public synchronized void addWebApp(final WebApp webApp) {
        final BundleContext webAppContext = webApp.getBundle().getBundleContext();
        ReplaceableService<HttpService> tracker = new ReplaceableService<HttpService>(
                webAppContext, HttpService.class, new ReplaceableServiceListener<HttpService>() {
            private ServiceRegistration<WebAppDependencyHolder> registration;
            @Override
            public void serviceChanged(HttpService oldService, HttpService newService) {
                if (registration != null) {
                    registration.unregister();
                    registration = null;
                }
                if (newService != null) {
                    WebAppDependencyHolder holder = new DefaultWebAppDependencyHolder(newService);
                    Dictionary<String, String> props = new Hashtable<String, String>();
                    props.put("bundle.id", Long.toString(webApp.getBundle().getBundleId()));
                    registration = webAppContext.registerService(WebAppDependencyHolder.class, holder, props);
                }
            }
        });
        trackers.put(webApp, tracker);
        tracker.start();
    }

    public synchronized void removeWebApp(WebApp webApp) {
        ReplaceableService<HttpService> tracker = trackers.remove(webApp);
        if (tracker != null) {
            tracker.stop();
        }
    }

}
