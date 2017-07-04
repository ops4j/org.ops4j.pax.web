/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.jetty.internal;

import java.util.Hashtable;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Registers the ServletControllerFactory on startup
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.7.0, July 31, 2009
 */
public class Activator implements BundleActivator {

	@SuppressWarnings("rawtypes")
	private ServiceRegistration registration;
	private ServiceTracker<Handler, Handler> handlerTracker;
	private BundleContext bundleContext;
	private ServiceTracker<Connector, Connector> connectorTracker;
	private ServerControllerFactoryImpl serverControllerFactory;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;

		for (Bundle b : bundleContext.getBundles()) {
			if ("org.eclipse.jetty.util".equals(b.getSymbolicName())) {
				// explicitly set org.eclipse.jetty.util.thread.ShutdownThread's TCCL to its own CL
				ShutdownThread.getInstance().setContextClassLoader(b.adapt(BundleWiring.class).getClassLoader());
			}
		}

		serverControllerFactory = new ServerControllerFactoryImpl(bundleContext.getBundle());

		handlerTracker = new ServiceTracker<>(bundleContext, Handler.class, new HandlerCustomizer());
		handlerTracker.open();

		connectorTracker = new ServiceTracker<>(bundleContext, Connector.class, new ConnectorCustomizer());
		connectorTracker.open();

		registration = bundleContext.registerService(
				ServerControllerFactory.class,
				serverControllerFactory,
				new Hashtable<>());
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		try {
			registration.unregister();
		} catch (IllegalStateException e) {
			// bundle context has already been invalidated ?
		}
	}

	private class HandlerCustomizer implements ServiceTrackerCustomizer<Handler, Handler> {

		@Override
		public Handler addingService(ServiceReference<Handler> reference) {
			Handler handler = bundleContext.getService(reference);

			//add handler to factory and restart. 
			if (registration != null) {
				registration.unregister();
			}

			serverControllerFactory.addHandler(handler);


			registration = bundleContext.registerService(
					ServerControllerFactory.class,
					serverControllerFactory,
					new Hashtable<>());

			return handler;
		}

		@Override
		public void modifiedService(ServiceReference<Handler> reference, Handler service) {
			// do nothing
		}

		@Override
		public void removedService(ServiceReference<Handler> reference, Handler handler) {
			// What ever happens: We un-get the service first
			bundleContext.ungetService(reference);
			try {
				// remove handler from factory and restart it. 
				if (registration != null) {
					registration.unregister();
				}

				serverControllerFactory.removeHandler(handler);


				registration = bundleContext.registerService(
						ServerControllerFactory.class,
						serverControllerFactory,
						new Hashtable<>());
			} catch (NoClassDefFoundError e) {
				// we should never go here, but if this happens silently ignore it
			}
		}

	}

	private class ConnectorCustomizer implements ServiceTrackerCustomizer<Connector, Connector> {

		@Override
		public Connector addingService(ServiceReference<Connector> reference) {
			Connector connector = bundleContext.getService(reference);

			//add handler to factory and restart. 
			if (registration != null) {
				registration.unregister();
			}

			serverControllerFactory.addConnector(connector);


			registration = bundleContext.registerService(
					ServerControllerFactory.class,
					serverControllerFactory,
					new Hashtable<>());

			return connector;
		}

		@Override
		public void modifiedService(ServiceReference<Connector> reference, Connector service) {
			// ignore
		}

		@Override
		public void removedService(ServiceReference<Connector> reference, Connector connector) {
			// What ever happens: We un-get the service first
			bundleContext.ungetService(reference);
			try {
				// remove handler from factory and restart it. 
				if (registration != null) {
					registration.unregister();
				}

				serverControllerFactory.removeConnector(connector);


				registration = bundleContext.registerService(
						ServerControllerFactory.class,
						serverControllerFactory,
						new Hashtable<>());
			} catch (NoClassDefFoundError e) {
				// we should never go here, but if this happens silently ignore it
			}
		}

	}

}
