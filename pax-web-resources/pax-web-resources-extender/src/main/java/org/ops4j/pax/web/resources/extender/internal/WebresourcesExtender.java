/* Copyright 2016 Marc Schlegel
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
package org.ops4j.pax.web.resources.extender.internal;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.ops4j.pax.web.resources.api.OsgiResourceLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebresourcesExtender implements BundleActivator, BundleListener {

	/**
	 * Namespace of OSGi extender capability. In OSGi 5.0.0 or higher, this is defined by
	 * {@code org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE}.
	 * Since this class is defined in osgi.cmpn which is not intended for runtime, we place
	 * this here.
	 */
	private static final String CAPABILITY_EXTENDER = "osgi.extender";

	private final transient Logger logger;
	private BundleContext bundleContext;
	private final List<OsgiResourceLocator> osgiResourceLocatorServices = new CopyOnWriteArrayList<>();

	private ServiceTracker<OsgiResourceLocator, OsgiResourceLocator> trackerResourceLocator;

	public WebresourcesExtender() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void start(BundleContext context) {
		this.bundleContext = context;

		IndexedOsgiResourceLocator indexedRegistryService = new IndexedOsgiResourceLocator(context);

		trackerResourceLocator = new ServiceTracker<>(context, OsgiResourceLocator.class.getName(), new ServiceTrackerCustomizer<OsgiResourceLocator, OsgiResourceLocator>() {
			@Override
			public OsgiResourceLocator addingService(ServiceReference<OsgiResourceLocator> reference) {
				OsgiResourceLocator service = context.getService(reference);
				if (service != null) {
					osgiResourceLocatorServices.add(service);
					logger.info("OsgiResourceLocator-Service available from bundle '{}'.",
							reference.getBundle().getSymbolicName());
					fullBundleScan(service);
					return service;
				}
				return null;
			}

			@Override
			public void modifiedService(ServiceReference<OsgiResourceLocator> reference, OsgiResourceLocator service) {
				// not interesting
			}

			@Override
			public void removedService(ServiceReference<OsgiResourceLocator> reference, OsgiResourceLocator service) {
				logger.info("OsgiResourceLocator from bundle '{}' removed.",
						reference.getBundle().getSymbolicName());
				osgiResourceLocatorServices.remove(service);
				// TOUNGET:
				context.ungetService(reference);
			}
		});

		trackerResourceLocator.open();
		// register service
		Dictionary<String, Object> props = new Hashtable<>(1);
		props.put(Constants.SERVICE_RANKING, -1);
		context.registerService(OsgiResourceLocator.class, indexedRegistryService, props);
		context.addBundleListener(WebresourcesExtender.this);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		trackerResourceLocator.close();
		context.removeBundleListener(this);
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.STARTED
				&& isJsfBundleForExtenderStartingOrActive(event.getBundle(), this::checkBundleWiringForExtender)) {
			osgiResourceLocatorServices.forEach(service -> service.register(event.getBundle()));
		} else if (isJsfBundleForExtenderStopping(event, this::checkBundleWiringForExtender)) {
			osgiResourceLocatorServices.forEach(service -> service.unregister(event.getBundle()));
		}
	}

	private boolean isJsfBundleForExtenderStartingOrActive(Bundle bundle, Function<Bundle, Boolean> extensionWiring) {
		if (bundle.getState() == Bundle.STARTING || bundle.getState() == Bundle.ACTIVE) {
			return extensionWiring.apply(bundle);
		}
		return false;
	}

	private boolean isJsfBundleForExtenderStopping(BundleEvent event, Function<Bundle, Boolean> extensionWiring) {
		Bundle bundle = event.getBundle();
		if (event.getType() == BundleEvent.STOPPED) {
			return extensionWiring.apply(bundle);
		}
		return false;
	}

	private boolean checkBundleWiringForExtender(Bundle bundle) {
		boolean wired = false;
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		if (bundleWiring != null) {
			List<BundleWire> wires = bundleWiring.getRequiredWires(CAPABILITY_EXTENDER);
			if (wires != null) {
				for (BundleWire wire : wires) {
					if (wire.getProviderWiring().getBundle().equals(bundleContext.getBundle())) {
						wired = true;
						break;
					}
				}
			}
		}
		return wired;
	}

	private void fullBundleScan(OsgiResourceLocator service) {
		logger.info("Scanning all bundles for Webresources");
		Arrays.stream(bundleContext.getBundles())
				.filter(bundle -> isJsfBundleForExtenderStartingOrActive(bundle, this::checkBundleWiringForExtender))
				.forEach(service::register);
	}

}
