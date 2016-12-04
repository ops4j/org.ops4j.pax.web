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
package org.ops4j.pax.web.extender.whiteboard.internal.util.tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
    private boolean withServiceProperties;

	public ReplaceableService(BundleContext bundleContext, Class<T> replacableServiceClass, ReplaceableServiceListener<T> replacableServiceListener) {
		this(bundleContext, replacableServiceClass, replacableServiceListener, false);
	}
	
	public ReplaceableService(BundleContext bundleContext, Class<T> replacableServiceClass, ReplaceableServiceListener<T> replacableServiceListener, boolean withServiceProperties) {
        this.context = bundleContext;
        this.serviceClass = replacableServiceClass;
        this.serviceListener = replacableServiceListener;
        this.serviceTracker = new ServiceTracker<>(bundleContext, replacableServiceClass, new Customizer());
        this.boundReferences = new ArrayList<>();
        this.withServiceProperties = withServiceProperties;
    }

	public void start() {
		this.serviceTracker.open();
	}

	public void stop() {
		this.serviceTracker.close();
	}

	protected void bind(T service, Map<String, Object> serviceProperties) {
        if (serviceListener != null) {
            T oldService;
            synchronized (this) {
                oldService = replacableService;
                replacableService = service;
            }
            serviceListener.serviceChanged(oldService, service, serviceProperties);
        }
    }

	private class Customizer implements ServiceTrackerCustomizer<T, T> {
		@Override
		public T addingService(ServiceReference<T> reference) {
			T service = context.getService(reference);
			final Map<String, Object> serviceProperties = new HashMap<>();
			if (withServiceProperties) {
			    Arrays.stream(reference.getPropertyKeys()).forEach(key -> serviceProperties.put(key, reference.getProperty(key)));
			}
			ServiceReference<T> bind;
			synchronized (boundReferences) {
				boundReferences.add(reference);
				Collections.sort(boundReferences);
				bind = boundReferences.get(0);
			}
			if (bind == reference) {
		        bind(service, serviceProperties);
			} else {
		        bind(serviceTracker.getService(bind), serviceProperties);
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
				bind(null, null);
			} else {
				bind(serviceTracker.getService(bind), null);
			}
			try {
				context.ungetService(reference);
			} catch (IllegalStateException e) {
				// Ignore if the bundle has been stopped already
			}
		}
	}

}
