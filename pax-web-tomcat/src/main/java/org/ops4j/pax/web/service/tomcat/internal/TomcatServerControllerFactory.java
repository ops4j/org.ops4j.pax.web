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

import java.util.HashMap;
import java.util.Map;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.osgi.framework.Bundle;

/**
 * @author Romaim Gilles
 */
public class TomcatServerControllerFactory implements ServerControllerFactory {

	// the layout of this class is similar to JettyServerControllerFactory (which was "first")

	private final Bundle paxWebTomcatBundle;

	/**
	 * When not running in OSGi environment, we still can use a Class Loader to detect some capabilities
	 * (jars available on CLASSPATH)
	 */
	private final ClassLoader classLoader;

	/** One {@link org.ops4j.pax.web.service.spi.ServerController} per unique configuration ID. */
	private final Map<String, TomcatServerController> serverControllers = new HashMap<>();

	/** Utility class to construct different Tomcat supporting objects */
	private final TomcatFactory tomcatFactory;

	/**
	 * Construct global instance (no more needed) of {@link ServerControllerFactory} for Tomcat.
	 *
	 * @param paxWebTomcatBundle bundle that instantiated this factory. Not required (e.g., in tests)
	 * @param classLoader used to detect server capabilities which can be enabled if some classes are present
	 *        on the classpath - at runtime (not in tests), this should be the classloader of the bundle
	 */
	public TomcatServerControllerFactory(Bundle paxWebTomcatBundle, ClassLoader classLoader) {
		this.paxWebTomcatBundle = paxWebTomcatBundle;
		this.classLoader = classLoader;

		tomcatFactory = new TomcatFactory(classLoader);
	}

	@Override
	public synchronized ServerController createServerController(final Configuration configuration) {
		if (serverControllers.containsKey(configuration.id())) {
			return serverControllers.get(configuration.id());
		}

		TomcatServerController controller = new TomcatServerController(paxWebTomcatBundle, classLoader,
				tomcatFactory, configuration);
		serverControllers.put(configuration.id(), controller);

		return controller;
	}

}
