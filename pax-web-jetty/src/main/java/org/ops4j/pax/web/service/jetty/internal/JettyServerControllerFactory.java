/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.jetty.internal;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.osgi.framework.Bundle;

public class JettyServerControllerFactory implements ServerControllerFactory {

	static Comparator<PriorityValue<?>> priorityComparator = Comparator.comparingInt(PriorityValue::getPriority);

	/**
	 * Bundle to raise environment awareness.
	 * When configuring Jetty inside OSGi runtime, we need to do some TCCL tricks and we need different bundles'
	 * class loaders to do that.
	 */
	private final Bundle paxWebJettyBundle;

	/**
	 * When not running in OSGi environment, we still can use a Class Loader to detect some capabilities
	 * (jars available on CLASSPATH)
	 */
	private final ClassLoader classLoader;

	/** One {@link org.ops4j.pax.web.service.spi.ServerController} per unique configuration ID. */
	private final Map<String, JettyServerController> serverControllers = new HashMap<>();

	/** Utility class to construct different Jetty supporting objects */
	private final JettyFactory jettyFactory;

	// --- low level Jetty components that impact server behavior. All with corresponding priority

	private final Set<PriorityValue<Handler>> handlers;
	private final Set<PriorityValue<Customizer>> customizers;

	/**
	 * Construct global instance (no more needed) of {@link ServerControllerFactory} for Jetty.
	 *
	 * @param paxWebJettyBundle bundle that instantiated this factory. Not required (e.g., in tests)
	 * @param classLoader used to detect server capabilities which can be enabled if some classes are present
	 *        on the classpath - at runtime (not in tests), this should be the classloader of the bundle
	 */
	public JettyServerControllerFactory(Bundle paxWebJettyBundle, ClassLoader classLoader) {
		this.paxWebJettyBundle = paxWebJettyBundle;
		this.classLoader = classLoader;

		handlers = new TreeSet<>(priorityComparator);
		customizers = new TreeSet<>(priorityComparator);

		jettyFactory = new JettyFactory(paxWebJettyBundle, classLoader);
	}

	@Override
	public synchronized ServerController createServerController(final Configuration configuration) {
		if (serverControllers.containsKey(configuration.id())) {
			return serverControllers.get(configuration.id());
		}

		JettyServerController controller = new JettyServerController(paxWebJettyBundle, classLoader,
				jettyFactory, configuration);
		serverControllers.put(configuration.id(), controller);

		// this is where registered handlers and connectors are added to a JettyServerController.
		// customizers are added anytime when needed
		controller.setHandlers(handlers);
		controller.setCustomizers(customizers);

		return controller;
	}

	@Override
	public void releaseServerController(ServerController controller, Configuration configuration) {
		if (controller instanceof JettyServerController) {
			serverControllers.remove(configuration.id(), controller);
		}
	}

	public void addHandler(Handler handler, int ranking) {
		// there should be no controllers at all, because when a handler is added, ServerController is unregistered
		this.handlers.add(new PriorityValue<>(handler, ranking));
	}

	public void removeHandler(Handler handler) {
		this.handlers.removeIf(pv -> pv.getValue() == handler);
		// we don't have to remove the handler from controllers, because the controllers will be stopped and
		// removed anyway
	}

	public void addCustomizer(Customizer customizer, int ranking) {
		synchronized (JettyServerControllerFactory.this.serverControllers) {
			PriorityValue<Customizer> pv = new PriorityValue<>(customizer, ranking);
			this.customizers.add(pv);
			// for customizers there are existing controllers
			for (JettyServerController serverController : this.serverControllers.values()) {
				serverController.setCustomizers(customizers);
			}
		}
	}

	public void removeCustomizer(Customizer customizer) {
		synchronized (JettyServerControllerFactory.this.serverControllers) {
			this.customizers.removeIf(pv -> pv.getValue() == customizer);
			// for customizers there are existing controllers
			for (JettyServerController serverController : this.serverControllers.values()) {
				serverController.removeCustomizer(customizer);
			}
		}
	}

}
