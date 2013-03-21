/*
 * Copyright 2007 Alin Dreghiciu, Achim Nierbeck, Guillaume Nodet.
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

import org.ops4j.pax.web.extender.war.internal.extender.AbstractExtender;
import org.ops4j.pax.web.extender.war.internal.extender.Extension;
import org.ops4j.pax.web.service.spi.WarManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Activator extends AbstractExtender {

    private WebEventDispatcher webEventDispatcher;
    private WebXmlObserver webXmlObserver;
    private ServiceTracker eventServiceTracker;
    private ServiceTracker logServiceTracker;
    private WebAppPublisher publisher;
    private ServiceRegistration registration;

    @Override
    protected void doStart() throws Exception {
        // Create the observer and register it
        publisher = new WebAppPublisher();
        webEventDispatcher = new WebEventDispatcher(getBundleContext());
        webXmlObserver = new WebXmlObserver(publisher, webEventDispatcher, getBundleContext());
        registration = getBundleContext().registerService(WarManager.class.getName(), webXmlObserver, new Hashtable());

        // Use a filter to avoid a direct dependency on eventadmin package
        Filter filterEvent = getBundleContext().createFilter("(objectClass=org.osgi.service.event.EventAdmin)");
        eventServiceTracker = new ServiceTracker(getBundleContext(), filterEvent, new EventServiceCustomizer());
        eventServiceTracker.open();

        // Use a filter to avoid a direct dependency on log package
        Filter filterLog = getBundleContext().createFilter("(objectClass=org.osgi.service.log.LogService)");
        logServiceTracker = new ServiceTracker(getBundleContext(), filterLog, new LogServiceCustomizer());
        logServiceTracker.open();

        // Start
        startTracking();
    }

    @Override
    protected void doStop() throws Exception {
        registration.unregister();
        stopTracking();
        eventServiceTracker.close();
        logServiceTracker.close();
        webEventDispatcher.destroy();
    }

    protected Extension doCreateExtension(Bundle bundle) throws Exception {
        return webXmlObserver.createExtension(bundle);
    }

    @Override
    protected ScheduledExecutorService createExecutor() {
        return Executors.newScheduledThreadPool(3, new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger();
            public Thread newThread(Runnable r) {
                final Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setName("WebPublisherExecutor" + ": " + count.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }

    private class LogServiceCustomizer implements ServiceTrackerCustomizer {

        public Object addingService(ServiceReference reference) {
            Object logService = getBundleContext().getService(reference);
            if (logService instanceof LogService) {
                webEventDispatcher.setLogService(logService);
            }
            return logService;
        }

        public void modifiedService(ServiceReference reference, Object service) {
        }

        public void removedService(ServiceReference reference, Object service) {
            webEventDispatcher.setLogService(null);
            getBundleContext().ungetService(reference);
        }

    }

    private class EventServiceCustomizer implements ServiceTrackerCustomizer {

        public Object addingService(ServiceReference reference) {
            Object eventService = getBundleContext().getService(reference);
            if (eventService instanceof EventAdmin) {
                webEventDispatcher.setEventAdminService(eventService);
            }
            return eventService;
        }

        public void modifiedService(ServiceReference reference, Object service) {
        }

        public void removedService(ServiceReference reference, Object service) {
            webEventDispatcher.setEventAdminService(null);
            getBundleContext().ungetService(reference);
        }

    }

}
