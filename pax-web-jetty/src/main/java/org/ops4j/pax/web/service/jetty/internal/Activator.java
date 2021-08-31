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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
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

	private static class PriorityComparator implements Comparator<Object> {

		@Override
		public int compare(Object element1, Object element2) {
			Integer comparison = 0;
			if (element1 != element2) {
				comparison = null;
				for (Object element : Arrays.asList(element1, element2)) {
//					javax.annotation.Priority annotation = element.getClass().getAnnotation(javax.annotation.Priority.class);
//					int priority = annotation == null ? 0 : annotation.value();
//					comparison = comparison == null ? priority : comparison - priority;
				}
			}
			return comparison;
		}
	}

	private BundleContext bundleContext;

	private ServiceRegistration<ServerControllerFactory> registration;

	private ServiceTracker<Handler, Handler> handlerTracker;
	private ServiceTracker<Connector, Connector> connectorTracker;
	private ServiceTracker<Customizer, Customizer> customizerTracker;

	private JettyServerControllerFactory serverControllerFactory;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;

		for (Bundle b : bundleContext.getBundles()) {
			if ("org.eclipse.jetty.util".equals(b.getSymbolicName())) {
				// explicitly set org.eclipse.jetty.util.thread.ShutdownThread's TCCL to its own CL
				ClassLoader tcl = Thread.currentThread().getContextClassLoader();
				try {
					ClassLoader cl = b.adapt(BundleWiring.class).getClassLoader();
					Thread.currentThread().setContextClassLoader(cl);
					ShutdownThread.getInstance().setContextClassLoader(b.adapt(BundleWiring.class).getClassLoader());
				} finally {
					Thread.currentThread().setContextClassLoader(tcl);
				}
			}
		}

		handlerTracker = new ServiceTracker<>(bundleContext, Handler.class, new HandlerCustomizer());
		handlerTracker.open();

		connectorTracker = new ServiceTracker<>(bundleContext, Connector.class, new ConnectorCustomizer());
		connectorTracker.open();
		
		customizerTracker = new ServiceTracker<>(bundleContext, Customizer.class, new CustomizerCustomizer());
		customizerTracker.open();

		Bundle paxWebJettyBundle = bundleContext.getBundle();
		ClassLoader loader = paxWebJettyBundle.adapt(BundleWiring.class).getClassLoader();

		serverControllerFactory = new JettyServerControllerFactory(paxWebJettyBundle, loader);
		registration = bundleContext.registerService(ServerControllerFactory.class,
				serverControllerFactory, new Hashtable<>());
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		try {
			registration.unregister();
		} catch (IllegalStateException e) {
			// bundle context has already been invalidated ?
		}
		connectorTracker.close();
		handlerTracker.close();
		customizerTracker.close();
	}

	private class HandlerCustomizer implements ServiceTrackerCustomizer<Handler, Handler> {

		@Override
		public Handler addingService(ServiceReference<Handler> reference) {
			Handler handler = bundleContext.getService(reference);
			Integer ranking = (Integer) reference.getProperty(Constants.SERVICE_RANKING);

            //add handler to factory and restart. 
            if (registration != null) {
                registration.unregister();
            }
			
//			serverControllerFactory.addHandler(handler, ranking == null ? 0 : ranking);

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
			bundleContext.ungetService(reference);
//			try {
//				serverControllerFactory.removeHandler(handler);
//			} catch (NoClassDefFoundError e) {
//				// we should never go here, but if this happens silently ignore it
//			}
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
			Integer ranking = (Integer) reference.getProperty(Constants.SERVICE_RANKING);
//			serverControllerFactory.addConnector(connector, ranking == null ? 0 : ranking);


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
			bundleContext.ungetService(reference);
			try {
				// remove handler from factory and restart it. 
				if (registration != null) {
					registration.unregister();
				}

//				serverControllerFactory.removeConnector(connector);


				registration = bundleContext.registerService(
						ServerControllerFactory.class,
						serverControllerFactory,
						new Hashtable<>());
			} catch (NoClassDefFoundError e) {
				// we should never go here, but if this happens silently ignore it
			}
		}

	}
	
	private class CustomizerCustomizer implements ServiceTrackerCustomizer<Customizer, Customizer> {

		@Override
		public Customizer addingService(ServiceReference<Customizer> reference) {
			Customizer customizer = bundleContext.getService(reference);
			Integer ranking = (Integer) reference.getProperty(Constants.SERVICE_RANKING);
//			serverControllerFactory.addCustomizer(customizer, ranking == null ? 0 : ranking);

			return customizer;
		}

		@Override
		public void modifiedService(ServiceReference<Customizer> reference, Customizer service) {
			// ignore
		}

		@Override
		public void removedService(ServiceReference<Customizer> reference, Customizer customizer) {
			bundleContext.ungetService(reference);
//			try {
//
//				serverControllerFactory.removeCustomizer(customizer);
//
//			} catch (NoClassDefFoundError e) {
//				// we should never go here, but if this happens silently ignore it
//			}
		}

	}

}
