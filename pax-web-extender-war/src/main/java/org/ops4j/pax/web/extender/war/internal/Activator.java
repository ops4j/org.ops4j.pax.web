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

import java.util.Hashtable;

import org.ops4j.pax.web.extender.war.internal.extender.AbstractExtender;
import org.ops4j.pax.web.extender.war.internal.extender.Extension;
import org.ops4j.pax.web.extender.war.internal.parser.WebAppParser;
import org.ops4j.pax.web.service.spi.WarManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator extends AbstractExtender implements ServiceTrackerCustomizer<HttpService, ServiceReference<HttpService>> {

    private ServiceTracker<EventAdmin, EventAdmin> eventServiceTracker;
    private ServiceTracker<LogService, LogService> logServiceTracker;
    private ServiceTracker<PackageAdmin, PackageAdmin> packageAdminTracker;
    private ServiceTracker<HttpService, ServiceReference<HttpService>> httpServiceTracker;
    private WebObserver webObserver;
    private WebEventDispatcher webEventDispatcher;
    private ServiceRegistration<WarManager> registration;

    @Override
    protected void doStart() throws Exception {
        logger.debug("Pax Web WAR Extender - Starting");

        BundleContext bundleContext = getBundleContext();

        webEventDispatcher = new WebEventDispatcher(bundleContext);

        // Do use the filters this way the eventadmin packages can be resolved
        // optional!
        Filter filterEvent = bundleContext.createFilter("(objectClass=org.osgi.service.event.EventAdmin)");
        eventServiceTracker = new ServiceTracker<EventAdmin, EventAdmin>(
                bundleContext, filterEvent, new EventServiceCustomizer());
        eventServiceTracker.open();

        Filter filterLog = bundleContext.createFilter("(objectClass=org.osgi.service.log.LogService)");
        logServiceTracker = new ServiceTracker<LogService, LogService>(
                bundleContext, filterLog, new LogServiceCustomizer());
        logServiceTracker.open();

        Filter filterPackage = bundleContext.createFilter("(objectClass=org.osgi.service.packageadmin.PackageAdmin)");
        packageAdminTracker = new ServiceTracker<PackageAdmin, PackageAdmin>(
                bundleContext, filterPackage, null);
        packageAdminTracker.open();

        DefaultWebAppDependencyManager dependencyManager = new DefaultWebAppDependencyManager();

        webObserver = new WebObserver(new WebAppParser(packageAdminTracker),
                new WebAppPublisher(webEventDispatcher, bundleContext), webEventDispatcher, dependencyManager,
                bundleContext);

        httpServiceTracker = new ServiceTracker<HttpService, ServiceReference<HttpService>>(bundleContext, HttpService.class, this);
        httpServiceTracker.open();

        logger.debug("Pax Web WAR Extender - Started");
    }

    @Override
    protected void doStop() throws Exception {
        logger.debug("Pax Web WAR Extender - Stopping");
        httpServiceTracker.close();
        eventServiceTracker.close();
        logServiceTracker.close();
        packageAdminTracker.close();
        webEventDispatcher.destroy();
        logger.debug("Pax Web WAR Extender - Stopped");
    }

    protected void startExtender() {
        startTracking();
        registration = getBundleContext().registerService(
                WarManager.class, webObserver,
                new Hashtable<String, Object>());
    }

    protected void stopExtender() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
        stopTracking();
    }

    @Override
    public ServiceReference<HttpService> addingService(ServiceReference<HttpService> reference) {
        if (httpServiceTracker.getServiceReference() == null) {
            startExtender();
        }
        return reference;
    }

    @Override
    public void modifiedService(ServiceReference<HttpService> reference, ServiceReference<HttpService> service) {
    }

    @Override
    public void removedService(ServiceReference<HttpService> reference, ServiceReference<HttpService> service) {
        stopExtender();
        if (httpServiceTracker.getServiceReference() != null) {
            startExtender();
        }
    }

    @Override
    protected Extension doCreateExtension(Bundle bundle) throws Exception {
        return webObserver.createExtension(bundle);
    }


	private class LogServiceCustomizer implements
			ServiceTrackerCustomizer<LogService, LogService> {

		@Override
		public LogService addingService(ServiceReference<LogService> reference) {
			LogService logService = getBundleContext().getService(reference);
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
			getBundleContext().ungetService(reference);
		}

	}

	private class EventServiceCustomizer implements
			ServiceTrackerCustomizer<EventAdmin, EventAdmin> {

		@Override
		public EventAdmin addingService(ServiceReference<EventAdmin> reference) {
			EventAdmin eventService = getBundleContext().getService(reference);
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
			getBundleContext().ungetService(reference);
		}

	}
}
