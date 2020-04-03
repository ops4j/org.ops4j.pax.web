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

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.osgi.framework.Bundle;

public class JettyServerControllerFactory implements ServerControllerFactory {

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
	private final Set<PriorityValue<Connector>> connectors;
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

		Comparator<PriorityValue<?>> c = Comparator.comparingInt(o -> o.priority);

		handlers = new TreeSet<PriorityValue<Handler>>(c);
		connectors = new TreeSet<PriorityValue<Connector>>(c);
		customizers = new TreeSet<PriorityValue<Customizer>>(c);

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

		return controller;

//		return new JettyServerController(new JettyFactory(bundle), priorityComparator) {
//
//			@Override
//			public synchronized void start() {
//				synchronized (serverControllers) {
//					for (PriorityValue<Connector> pv : connectors) {
//						jettyServer.addConnector(pv.value);
//					}
//					super.start();
//					if (!customizers.isEmpty()) {
//						addCustomizers(JettyServerControllerFactory.this.customizers.stream()
//								.map(pv -> pv.value).collect(Collectors.toList()));
//					}
//					for (PriorityValue<Handler> pv : handlers) {
//						jettyServer.addHandler(pv.value);
//					}
//					serverControllers.put(configuration.id(), this);
//				}
//			}
//
//			@Override
//			public synchronized void stop() {
//				synchronized (serverControllers) {
//					serverControllers.remove(this);
//					for (Handler handler : JettyServerControllerFactory.this.handlers.keySet()) {
//						jettyServer.removeHandler(handler);
//					}
//					if (!customizers.isEmpty()) {
//						removeCustomizers(customizers.keySet());
//					}
//					super.stop();
//					for (Connector connector : connectors.keySet()) {
//						jettyServer.addConnector(connector);
//					}
//				}
//			}
//		};
	}

//	public void addHandler(Handler handler, int ranking) {
//		synchronized (JettyServerControllerFactory.this.serverControllers) {
//			for (JettyServerController serverController : this.serverControllers.values()) {
//				serverController.jettyServer.addHandler(handler);
//			}
//			this.handlers.add(new PriorityValue<>(handler, ranking));
//		}
//	}
//
//	public void removeHandler(Handler handler) {
//		synchronized (JettyServerControllerFactory.this.serverControllers) {
//			this.handlers.removeIf(pv -> pv.value == handler);
//			for (JettyServerController serverController : this.serverControllers.values()) {
//				serverController.jettyServer.removeHandler(handler);
//			}
//		}
//	}
//
//	public void addConnector(Connector connector, int ranking) {
//		synchronized (JettyServerControllerFactory.this.serverControllers) {
//			for (JettyServerController serverController : this.serverControllers.values()) {
//				serverController.jettyServer.addConnector(connector);
//			}
//			this.connectors.add(new PriorityValue<>(connector, ranking));
//		}
//	}
//
//	public void removeConnector(Connector connector) {
//		synchronized (JettyServerControllerFactory.this.serverControllers) {
//			this.connectors.removeIf(pv -> pv.value == connector);
//			for (JettyServerController serverController : this.serverControllers.values()) {
//				serverController.jettyServer.removeConnector(connector);
//			}
//		}
//	}
//
//	public void addCustomizer(Customizer customizer, int ranking) {
//		synchronized (JettyServerControllerFactory.this.serverControllers) {
//			List<Customizer> customizers = Collections.singletonList(customizer);
////			for (JettyServerController serverController : this.serverControllers.values()) {
////				serverController.addCustomizers(customizers);
////			}
//			this.customizers.add(new PriorityValue<>(customizer, ranking));
//		}
//	}
//
//	public void removeCustomizer(Customizer customizer) {
//		synchronized (JettyServerControllerFactory.this.serverControllers) {
//			this.customizers.removeIf(pv -> pv.value == customizer);
//			List<Customizer> customizers = Collections.singletonList(customizer);
////			for (JettyServerController serverController : this.serverControllers.values()) {
////				serverController.removeCustomizers(customizers);
////			}
//		}
//	}

	private static class PriorityValue<T> {
		private final T value;
		private final Integer priority;

		public PriorityValue(T value, Integer priority) {
			this.value = value;
			this.priority = priority;
		}

		public T getValue() {
			return value;
		}

		public Integer getPriority() {
			return priority;
		}
	}

}
