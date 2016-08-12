/*
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
package org.ops4j.pax.web.extender.war.internal.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ops4j.pax.swissbox.core.BundleUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ReplaceableService<T> {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReplaceableService.class);

    /**
     * Bundle context. Constructor parameter. Cannot be null.
     */
    private final BundleContext bundleContext;
    /**
     * Service class. Constructor parameter. Cannot be null.
     */
    @SuppressWarnings("unused")
	private final Class<T> serviceClass;
    /**
     * Listener for backing service related events. Constructor paramater. Can be null.
     */
    private final ReplaceableServiceListener<T> serviceListener;
    /**
     * Service tracker. Cannot be null.
     */
    private final ServiceTracker<T, T> serviceTracker;

    private final List<ServiceReference<T>> boundReferences;

    private T service;

    public ReplaceableService(BundleContext context, Class<T> serviceClass, ReplaceableServiceListener<T> serviceListener) {
        this.bundleContext = context;
        this.serviceClass = serviceClass;
        this.serviceListener = serviceListener;
		this.serviceTracker = new ServiceTracker<>(context, serviceClass, new Customizer());
		this.boundReferences = new ArrayList<>();
    }

    public void start() {
        this.serviceTracker.open();
    }

    public void stop() {
        this.serviceTracker.close();
    }

    protected void bind(T serviceToBind) {
        if (serviceListener != null) {
            T oldService;
            synchronized (this) {
                oldService = service;
                service = serviceToBind;
            }
            serviceListener.serviceChanged(oldService, serviceToBind);
        }
    }

    private class Customizer implements ServiceTrackerCustomizer<T, T> {
        @Override
        public T addingService(ServiceReference<T> reference) {
            T bundleService = bundleContext.getService(reference);
            ServiceReference<T> bind;
            synchronized (boundReferences) {
                boundReferences.add(reference);
                Collections.sort(boundReferences);
                bind = boundReferences.get(0);
            }
            if (bind == reference) {
                bind(bundleService);
            } else {
                bind(serviceTracker.getService(bind));
            }
            return bundleService;
        }

        @Override
        public void modifiedService(ServiceReference<T> reference, T modifiedService) {
        }

        @Override
        public void removedService(ServiceReference<T> reference, T removedService) {
            ServiceReference<T> bind;
            synchronized (boundReferences) {
                boundReferences.remove(reference);
                if (boundReferences.isEmpty()) {
                    bind = null;
                } else {
                    bind = boundReferences.get(0);
                }
            }
            if (bind == null) {
                bind(null);
            } else {
                bind(serviceTracker.getService(bind));
            }
            
            bundleContext.ungetService(reference);
        }
    }

}
