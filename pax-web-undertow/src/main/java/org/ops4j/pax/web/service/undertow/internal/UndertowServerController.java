/*
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
package org.ops4j.pax.web.service.undertow.internal;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.undertow.Handlers;
import io.undertow.security.idm.IdentityManager;
import io.undertow.servlet.api.SessionPersistenceManager;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.ServerState;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume Nodet
 * @author Grzegorz Grzybek
 */
public class UndertowServerController implements ServerController/*, IdentityManager*/ {

	private static final Logger LOG = LoggerFactory.getLogger(UndertowServerController.class);

	private final Bundle paxWebUndertowBundle;
	private final ClassLoader classLoader;

	private final Configuration configuration;
	private ServerState state;

	private final List<ServerListener> listeners;

	private final UndertowFactory undertowFactory;

	/**
	 * An instance of Undertow Wrapper (and wrapped {@link io.undertow.Undertow} inside) that's
	 * managed by this {@link ServerController}.
	 */
	private UndertowServerWrapper undertowServerWrapper;

				private IdentityManager identityManager;
				private SessionPersistenceManager sessionPersistenceManager;
				private int defaultSessionTimeoutInMinutes;
			
				// Standard URI -> HttpHandler map - may be wrapped by access log, filters, etc. later
				private final ContextAwarePathHandler path = new ContextAwarePathHandler(Handlers.path());
				private final ConcurrentMap<HttpContext, Context> contextMap = new ConcurrentHashMap<>();

	public UndertowServerController(Bundle paxWebUndertowBundle, ClassLoader classLoader,
			UndertowFactory undertowFactory, Configuration configuration) {
		this.paxWebUndertowBundle = paxWebUndertowBundle;
		this.classLoader = classLoader;
		this.undertowFactory = undertowFactory;
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
			throw new IllegalStateException("Can't configure Undertow server controller in state " + state);
		}

		undertowServerWrapper = new UndertowServerWrapper(configuration, undertowFactory, paxWebUndertowBundle, classLoader);
		undertowServerWrapper.configure();

		state = ServerState.STOPPED;
		notifyListeners(ServerEvent.CONFIGURED);
	}

	@Override
	public synchronized void start() throws Exception {
		LOG.info("Starting {}", this);

		if (state != ServerState.STOPPED) {
			throw new IllegalStateException("Can't start Undertow server controller in state " + state);
		}

		undertowServerWrapper.start();

		state = ServerState.STARTED;
		notifyListeners(ServerEvent.STARTED);
	}

	@Override
	public void stop() throws Exception {
		LOG.info("Stopping {}", this);

		if (state != ServerState.STARTED) {
			throw new IllegalStateException("Can't stop Undertow server controller in state " + state);
		}

		undertowServerWrapper.stop();

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
	public void sendBatch(Batch batch) {
		LOG.info("Receiving {}", batch);

		if (state == ServerState.UNCONFIGURED) {
			throw new IllegalStateException("Can't process batch in Undertow server controller in state " + state);
		}

		batch.accept(undertowServerWrapper);
	}

	@Override
	public String toString() {
		return "UndertowServerController{configuration=" + configuration.id() + ",state=" + state + "}";
	}

	/**
	 * Kind of configuration used
	 */
	private enum ConfigSource {
		/** Configuration in undertow.xml */
		XML,
		/** Additional (merged with PID) configuration in undertow.properties */
		PROPERTIES,
		/** Configuration purely from Configadmin */
		PID;

		/**
		 * Detect {@link ConfigSource} by the type of URL
		 * @param undertowResource
		 * @return
		 */
		public static ConfigSource kind(URL undertowResource) {
			if (undertowResource == null) {
				return PID;
			}
			String path = undertowResource.getPath();
			if (path == null) {
				return PID;
			}
			String name = new File(path).getName();
			if (name.endsWith(".properties")) {
				return PROPERTIES;
			} else if (name.endsWith(".xml")) {
				return XML;
			}
			return PID;
		}
	}

}
