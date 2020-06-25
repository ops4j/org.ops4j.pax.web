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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.ServerState;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.tomcat.internal.web.TomcatResourceServlet;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage a single instance of embedded Tomcat server. As with Jetty, "controller" holds a refrence to a "wrapper"
 * which in turn holds an instance of actual "Tomcat"
 *
 * @author Romain Gilles
 */
class TomcatServerController implements ServerController {

	private static final Logger LOG = LoggerFactory.getLogger(TomcatServerController.class);

	private final Bundle paxWebTomcatBundle;
	private final ClassLoader classLoader;

	private final Configuration configuration;
	private org.ops4j.pax.web.service.spi.ServerState state;

	private final List<ServerListener> listeners;

	private final TomcatFactory tomcatFactory;

	/**
	 * An instance of Tomcat Wrapper (and wrapped {@link org.apache.catalina.core.StandardServer} inside) that's
	 * managed by this {@link ServerController}.
	 */
	private TomcatServerWrapper tomcatServerWrapper;

	TomcatServerController(Bundle paxWebTomcatBundle, ClassLoader classLoader,
			TomcatFactory tomcatFactory, Configuration configuration) {
		this.paxWebTomcatBundle = paxWebTomcatBundle;
		this.classLoader = classLoader;
		this.tomcatFactory = tomcatFactory;
		this.configuration = configuration;
		this.state = ServerState.UNCONFIGURED;

		this.listeners = new CopyOnWriteArrayList<>();
	}

	// --- lifecycle methods

	@Override
	public ServerState getState() {
		return state;
	}

	@Override
	public void configure() throws Exception {
		LOG.info("Configuring {}", this);

		// controller can be configured only once
		if (state != ServerState.UNCONFIGURED) {
			throw new IllegalStateException("Can't configure Tomcat server controller in state " + state);
		}

		tomcatServerWrapper = new TomcatServerWrapper(configuration, tomcatFactory, paxWebTomcatBundle, classLoader);
		tomcatServerWrapper.configure();

		state = ServerState.STOPPED;
		notifyListeners(ServerEvent.CONFIGURED);
	}

	@Override
	public void start() throws Exception {
		LOG.info("Starting {}", this);

		if (state != ServerState.STOPPED) {
			throw new IllegalStateException("Can't start Tomcat server controller in state " + state);
		}

		tomcatServerWrapper.start();

		state = ServerState.STARTED;
		notifyListeners(ServerEvent.STARTED);
	}

	@Override
	public void stop() throws Exception {
		LOG.info("Stopping {}", this);

		if (state != ServerState.STARTED) {
			throw new IllegalStateException("Can't stop Tomcat server controller in state " + state);
		}

		tomcatServerWrapper.stop();

		state = ServerState.STOPPED;
		notifyListeners(ServerEvent.STOPPED);
	}

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	// --- listener related methods

	@Override
	public void addListener(ServerListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("ServerListener is null");
		}
		listeners.add(listener);
	}

	@Override
	public void removeListener(ServerListener listener) {
		listeners.remove(listener);
	}

	void notifyListeners(ServerEvent event) {
		for (ServerListener listener : listeners) {
			listener.stateChanged(event);
		}
	}

	@Override
	public Servlet createResourceServlet(URL urlBase, String base) {
		File baseDirectory;
		try {
			baseDirectory = urlBase == null ? null : new File(urlBase.toURI());
		} catch (URISyntaxException notPossbleButStill) {
			throw new IllegalArgumentException(notPossbleButStill.getMessage(), notPossbleButStill);
		}
		String chroot = baseDirectory == null ? base : null;

		return new TomcatResourceServlet(baseDirectory, chroot, configuration.resources());
	}

	@Override
	public void sendBatch(Batch batch) {
		LOG.info("Receiving {}", batch);

		if (state == ServerState.UNCONFIGURED) {
			throw new IllegalStateException("Can't process batch in Tomcat server controller in state " + state);
		}

		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			batch.accept(tomcatServerWrapper);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public String toString() {
		return "TomcatServerController{configuration=" + configuration.id() + ",state=" + state + "}";
	}

}
