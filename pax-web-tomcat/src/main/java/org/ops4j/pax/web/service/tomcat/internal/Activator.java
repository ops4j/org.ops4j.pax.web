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
package org.ops4j.pax.web.service.tomcat.internal;

import java.util.Hashtable;

import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Registers the ServletControllerFactory on startup
 *
 * @author Romaim Gilles
 */
public class Activator implements BundleActivator {

	private BundleContext bundleContext;

	private ServiceRegistration<ServerControllerFactory> registration;

	private TomcatServerControllerFactory serverControllerFactory;

	@Override
	public void start(BundleContext context) throws Exception {
		this.bundleContext = context;

		// TODO: we should think more about it. "true"/empty means there'll be org.apache.catalina.core.NamingContextListener
		//       added to StandardService
		System.setProperty("catalina.useNaming", "false");

		Bundle paxWebTomcatBundle = bundleContext.getBundle();
		ClassLoader loader = paxWebTomcatBundle.adapt(BundleWiring.class).getClassLoader();

		serverControllerFactory = new TomcatServerControllerFactory(paxWebTomcatBundle, loader);
		registration = bundleContext.registerService(ServerControllerFactory.class,
				serverControllerFactory, new Hashtable<>());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			registration.unregister();
		} catch (IllegalStateException e) {
			// bundle context has already been invalidated ?
		}
	}

}
