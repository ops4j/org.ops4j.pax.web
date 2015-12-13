/**
 *
 */
package org.ops4j.pax.web.extender.whiteboard.internal.util.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 */
public class ReplaceableService<T> {

    /**
     * Bundle context. Constructor parameter. Cannot be null.
     */
    private final BundleContext context;
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

    private T replacableService;

    public ReplaceableService(BundleContext bundleContext, Class<T> replacableServiceClass, ReplaceableServiceListener<T> replacableServiceListener) {
        this.context = bundleContext;
        this.serviceClass = replacableServiceClass;
        this.serviceListener = replacableServiceListener;
        this.serviceTracker = new ServiceTracker<T, T>(bundleContext, replacableServiceClass, new Customizer());
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
                oldService = replacableService;
                replacableService = service;
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
            if (context == null) {
            	return; //nothing to do, since context is already down.
            }
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
            } catch (IllegalStateException e) {
                // Ignore if the bundle has been stopped already
            }
        }
    }

}
