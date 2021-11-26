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

import java.util.HashMap;
import java.util.Map;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.osgi.framework.Bundle;
import org.xnio.XnioProvider;

/**
 * @author Achim Nierbeck
 */
public class UndertowServerControllerFactory implements ServerControllerFactory {

	// the layout of this class is similar to JettyServerControllerFactory (which was "first")

	private final Bundle paxWebUndertowBundle;

	/**
	 * When not running in OSGi environment, we still can use a Class Loader to detect some capabilities
	 * (jars available on CLASSPATH)
	 */
	private final ClassLoader classLoader;

	/** One {@link org.ops4j.pax.web.service.spi.ServerController} per unique configuration ID. */
	private final Map<String, ServerController> serverControllers = new HashMap<>();

	/** Utility class to construct different Undertow supporting objects */
	private final UndertowFactory undertowFactory;

	/**
	 * Constructs global instance (no more needed) of {@link ServerControllerFactory} for Undertow.
	 *
	 * @param paxWebUndertowBundle bundle that instantiated this factory. Not required (e.g., in tests)
	 * @param classLoader used to detect server capabilities which can be enabled if some classes are present
	 *        on the classpath - at runtime (not in tests), this should be the classloader of the bundle
	 */
	public UndertowServerControllerFactory(Bundle paxWebUndertowBundle, ClassLoader classLoader,
			XnioProvider provider) {
		this.paxWebUndertowBundle = paxWebUndertowBundle;
		this.classLoader = classLoader;

		undertowFactory = new UndertowFactory(classLoader, provider);
	}

	@Override
	public synchronized ServerController createServerController(final Configuration configuration) {
		if (serverControllers.containsKey(configuration.id())) {
			return serverControllers.get(configuration.id());
		}

		UndertowServerController controller = new UndertowServerController(paxWebUndertowBundle, classLoader,
				undertowFactory, configuration);
		serverControllers.put(configuration.id(), controller);

		return controller;
	}

	@Override
	public void releaseServerController(ServerController controller, Configuration configuration) {
		serverControllers.remove(configuration.id(), controller);
	}

}
