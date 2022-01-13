/*
 * Copyright 2007 Alin Dreghiciu.
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.Servlet;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.util.resource.PathResource;
import org.ops4j.pax.web.service.jetty.internal.web.JettyResourceServlet;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerState;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.events.ServerEvent;
import org.ops4j.pax.web.service.spi.model.events.ServerListener;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main task of this {@link ServerController} is to manage a single instance of embedded Jetty server. The management
 * is done through a "wrapping" {@link JettyServerWrapper} to have special translation layer between the
 * <em>model</em> managed and maintained at pax-web-runtime side and actual set of objects that make real
 * Jetty server.
 */
class JettyServerController implements ServerController {

	private static final Logger LOG = LoggerFactory.getLogger(JettyServerController.class);

	private final Bundle paxWebJettyBundle;
	private final ClassLoader classLoader;

	private final Configuration configuration;
	private ServerState state;

	private final Set<ServerListener> listeners;

	private final JettyFactory jettyFactory;

	/**
	 * An instance of Jetty Wrapper (and wrapped {@link org.eclipse.jetty.server.Server} inside) that's
	 * managed by this {@link ServerController}.
	 */
	private JettyServerWrapper jettyServerWrapper;

	JettyServerController(Bundle paxWebJettyBundle, ClassLoader classLoader,
			JettyFactory jettyFactory, Configuration configuration) {
		this.paxWebJettyBundle = paxWebJettyBundle;
		this.classLoader = classLoader;
		this.jettyFactory = jettyFactory;
		this.configuration = configuration;
		this.state = ServerState.UNCONFIGURED;

		this.listeners = Collections.synchronizedSet(new LinkedHashSet<>());

		jettyServerWrapper = new JettyServerWrapper(configuration, jettyFactory, paxWebJettyBundle, classLoader);
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
			throw new IllegalStateException("Can't configure Jetty server controller in state " + state);
		}

		jettyServerWrapper.configure();

		state = ServerState.STOPPED;
		notifyListeners(new ServerEvent(ServerEvent.State.CONFIGURED, jettyServerWrapper.getAddresses(false)));
	}

	@Override
	public void start() throws Exception {
		LOG.info("Starting {}", this);

		if (state != ServerState.STOPPED) {
			throw new IllegalStateException("Can't start Jetty server controller in state " + state);
		}

		jettyServerWrapper.start();

		state = ServerState.STARTED;
		notifyListeners(new ServerEvent(ServerEvent.State.STARTED, jettyServerWrapper.getAddresses(true)));
	}

	@Override
	public void stop() throws Exception {
		LOG.info("Stopping {}", this);

		if (state != ServerState.STARTED) {
			throw new IllegalStateException("Can't stop Jetty server controller in state " + state);
		}

		jettyServerWrapper.stop();

		state = ServerState.STOPPED;
		notifyListeners(new ServerEvent(ServerEvent.State.STOPPED, null));
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
		if (state == ServerState.STOPPED) {
			listener.stateChanged(new ServerEvent(ServerEvent.State.CONFIGURED, jettyServerWrapper.getAddresses(false)));
		} else if (state == ServerState.STARTED) {
			listener.stateChanged(new ServerEvent(ServerEvent.State.STARTED, jettyServerWrapper.getAddresses(true)));
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
	public void sendBatch(Batch batch) {
		LOG.info("Receiving {}", batch);

		if (state == ServerState.UNCONFIGURED) {
			throw new IllegalStateException("Can't process batch in Jetty server controller in state " + state);
		}

		batch.accept(jettyServerWrapper);
	}

	@Override
	public Servlet createResourceServlet(final URL urlBase, final String base) {
		final PathResource baseUrlResource;
		try {
			baseUrlResource = urlBase == null ? null : new PathResource(urlBase);
		} catch (IOException | URISyntaxException notPossbleButStill) {
			throw new IllegalArgumentException(notPossbleButStill.getMessage(), notPossbleButStill);
		}
		String chroot = baseUrlResource == null ? base : null;

		return new JettyResourceServlet(baseUrlResource, chroot);
	}

	@Override
	public String toString() {
		return "JettyServerController{configuration=" + configuration.id() + ",state=" + state + "}";
	}

	public void setHandlers(Set<PriorityValue<Handler>> handlers) {
		jettyServerWrapper.setHandlers(handlers);
	}

	public void setCustomizers(Set<PriorityValue<HttpConfiguration.Customizer>> customizers) {
		jettyServerWrapper.setCustomizers(customizers);
	}

	public void removeCustomizer(HttpConfiguration.Customizer customizer) {
		jettyServerWrapper.removeCustomizer(customizer);
	}

}
