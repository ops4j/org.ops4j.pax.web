/**
 *
 */
package org.ops4j.pax.web.extender.war.internal.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private final BundleContext m_context;
    /**
     * Service class. Constructor parameter. Cannot be null.
     */
    private final Class<T> m_serviceClass;
    /**
     * Listener for backing service related events. Constructor paramater. Can be null.
     */
    private final ReplaceableServiceListener<T> m_serviceListener;
    /**
     * Service tracker. Cannot be null.
     */
    private final ServiceTracker<T, T> m_serviceTracker;

    private final List<ServiceReference<T>> m_boundReferences;

    private T m_service;

    public ReplaceableService(BundleContext context, Class<T> serviceClass, ReplaceableServiceListener<T> serviceListener) {
        this.m_context = context;
        this.m_serviceClass = serviceClass;
        this.m_serviceListener = serviceListener;
        this.m_serviceTracker = new ServiceTracker<T, T>(context, serviceClass, new Customizer());
        this.m_boundReferences = new ArrayList<ServiceReference<T>>();
    }

    public void start() {
        this.m_serviceTracker.open();
    }

    public void stop() {
        this.m_serviceTracker.close();
    }

    protected void bind(T service) {
        if (m_serviceListener != null) {
            T oldService;
            synchronized (this) {
                oldService = m_service;
                m_service = service;
            }
            m_serviceListener.serviceChanged(oldService, service);
        }
    }

    private class Customizer implements ServiceTrackerCustomizer<T, T> {
        @Override
        public T addingService(ServiceReference<T> reference) {
            T service = m_context.getService(reference);
            ServiceReference<T> bind;
            synchronized (m_boundReferences) {
                m_boundReferences.add(reference);
                Collections.sort(m_boundReferences);
                bind = m_boundReferences.get(0);
            }
            if (bind == reference) {
                bind(service);
            } else {
                bind(m_serviceTracker.getService(bind));
            }
            return service;
        }

        @Override
        public void modifiedService(ServiceReference<T> reference, T service) {
        }

        @Override
        public void removedService(ServiceReference<T> reference, T service) {
            ServiceReference<T> bind;
            synchronized (m_boundReferences) {
                m_boundReferences.remove(reference);
                if (m_boundReferences.isEmpty()) {
                    bind = null;
                } else {
                    bind = m_boundReferences.get(0);
                }
            }
            if (bind == null) {
                bind(null);
            } else {
                bind(m_serviceTracker.getService(bind));
            }
            m_context.ungetService(reference);
        }
    }

}
