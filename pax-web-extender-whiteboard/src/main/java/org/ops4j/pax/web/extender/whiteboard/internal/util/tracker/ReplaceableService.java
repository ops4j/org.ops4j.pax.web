/**
 *
 */
package org.ops4j.pax.web.extender.whiteboard.internal.util.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ops4j.pax.swissbox.core.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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
    private final BundleContext context;
    /**
     * Service class. Constructor parameter. Cannot be null.
     */
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
        this.context = context;
        this.serviceClass = serviceClass;
        this.serviceListener = serviceListener;
        this.serviceTracker = new ServiceTracker<T, T>(context, serviceClass, new Customizer());
        this.boundReferences = new ArrayList<ServiceReference<T>>();
    }

    public void start() {
        this.serviceTracker.open();
    }

    public void stop() {
        this.serviceTracker.close();
    }

    protected void bind(T service) {
        if (serviceListener != null) {
            T oldService;
            synchronized (this) {
                oldService = service;
                this.service = service;
            }
            serviceListener.serviceChanged(oldService, service);
        }
    }

    private class Customizer implements ServiceTrackerCustomizer<T, T> {
        @Override
        public T addingService(ServiceReference<T> reference) {
            T service = context.getService(reference);
            ServiceReference<T> bind;
            synchronized (boundReferences) {
                boundReferences.add(reference);
                Collections.sort(boundReferences);
                bind = boundReferences.get(0);
            }
            if (bind == reference) {
                bind(service);
            } else {
                bind(serviceTracker.getService(bind));
            }
            return service;
        }

        @Override
        public void modifiedService(ServiceReference<T> reference, T service) {
        }

        @Override
        public void removedService(ServiceReference<T> reference, T service) {
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
            try {
            	context.ungetService(reference);
            } catch (IllegalStateException ise) {
            	LOG.debug("trying to unget service from stoped bundle!");
            }
        }
    }

}
