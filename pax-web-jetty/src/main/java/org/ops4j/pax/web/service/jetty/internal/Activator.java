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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the ServletControllerFactory on startup
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.7.0, July 31, 2009
 */
public class Activator implements BundleActivator {

	public static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private BundleContext bundleContext;

	private ServiceRegistration<ServerControllerFactory> registration;

	private ServiceTracker<Handler, Handler> handlerTracker;
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
		handlerTracker.close();
		customizerTracker.close();
	}

	/**
	 * {@link ServiceTrackerCustomizer} that handles {@link Handler} services.
	 */
	private class HandlerCustomizer implements ServiceTrackerCustomizer<Handler, Handler> {
		@Override
		public Handler addingService(ServiceReference<Handler> reference) {
			Handler handler = bundleContext.getService(reference);
			Integer ranking = (Integer) reference.getProperty(Constants.SERVICE_RANKING);

			LOG.debug("Registered Jetty Handler: {}", handler);

			// add handler to factory and restart.
			if (registration != null) {
				try {
					registration.unregister();
				} catch (java.lang.IllegalStateException ignored) {
				}
			}

			LOG.debug("Adding {} Handler to Jetty server", handler);
			serverControllerFactory.addHandler(handler, ranking == null ? 0 : ranking);

			registration = bundleContext.registerService(ServerControllerFactory.class,
					serverControllerFactory, new Hashtable<>());

			return handler;
		}

		@Override
		public void modifiedService(ServiceReference<Handler> reference, Handler service) {
			// we could handle a change of "service.ranking" property, but we're not doing it
		}

		@Override
		public void removedService(ServiceReference<Handler> reference, Handler handler) {
			bundleContext.ungetService(reference);

			LOG.debug("Unegistered Jetty Handler: {}", handler);

			// remove handler from factory and restart.
			if (registration != null) {
				try {
					registration.unregister();
				} catch (java.lang.IllegalStateException ignored) {
				}
			}

			LOG.debug("Removing {} Handler from Jetty server", handler);
			serverControllerFactory.removeHandler(handler);

			registration = bundleContext.registerService(ServerControllerFactory.class,
					serverControllerFactory, new Hashtable<>());
		}
	}

	/**
	 * {@link ServiceTrackerCustomizer} that handles {@link Customizer} services. Registration of such
	 * services doesn't require re-registration of {@link ServerControllerFactory}, because {@link Customizer}
	 * works for each request individually.
	 */
	private class CustomizerCustomizer implements ServiceTrackerCustomizer<Customizer, Customizer> {
		@Override
		public Customizer addingService(ServiceReference<Customizer> reference) {
			Customizer customizer = bundleContext.getService(reference);
			Integer ranking = (Integer) reference.getProperty(Constants.SERVICE_RANKING);

			LOG.debug("Registered Jetty Customizer: {}", customizer);

			LOG.debug("Adding {} Customizer to Jetty server", customizer);
			serverControllerFactory.addCustomizer(customizer, ranking == null ? 0 : ranking);

			return customizer;
		}

		@Override
		public void modifiedService(ServiceReference<Customizer> reference, Customizer service) {
			// we could handle a change of "service.ranking" property, but we're not doing it
		}

		@Override
		public void removedService(ServiceReference<Customizer> reference, Customizer customizer) {
			bundleContext.ungetService(reference);

			LOG.debug("Unegistered Jetty Customizer: {}", customizer);

			LOG.debug("Removing {} Customizer from Jetty server", customizer);
			serverControllerFactory.removeCustomizer(customizer);
		}
	}

}
