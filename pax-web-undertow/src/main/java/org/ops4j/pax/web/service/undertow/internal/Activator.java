/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.undertow.internal;

import java.util.Hashtable;

import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.xnio.XnioProvider;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<XnioProvider, XnioProvider> {

	private BundleContext bundleContext;
	private ServiceTracker<XnioProvider, XnioProvider>  xnioTracker;

	private ServiceRegistration<ServerControllerFactory> registration;

	private UndertowServerControllerFactory serverControllerFactory;

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		xnioTracker = new ServiceTracker<>(context, XnioProvider.class, this);
		xnioTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			if (registration != null) {
				registration.unregister();
			}
		} catch (IllegalStateException ignored) {
		}
		if (xnioTracker != null) {
			xnioTracker.close();
		}
	}

	@Override
	public XnioProvider addingService(ServiceReference<XnioProvider> reference) {
		XnioProvider provider = bundleContext.getService(reference);
		Bundle bundle = bundleContext.getBundle();
		UndertowServerControllerFactory factory = new UndertowServerControllerFactory(bundle,
				bundle.adapt(BundleWiring.class).getClassLoader(), provider);
		registration = bundleContext.registerService(ServerControllerFactory.class, factory, new Hashtable<>());
		return provider;
	}

	@Override
	public void modifiedService(ServiceReference<XnioProvider> reference, XnioProvider service) {
	}

	@Override
	public void removedService(ServiceReference<XnioProvider> reference, XnioProvider service) {
		if (registration != null) {
			registration.unregister();
		}
	}

}
