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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.servlet.Servlet;

import io.undertow.security.idm.IdentityManager;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerState;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.events.ServerEvent;
import org.ops4j.pax.web.service.spi.model.events.ServerListener;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.undertow.internal.web.UndertowResourceServlet;
import org.osgi.framework.Bundle;
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

	private final Set<ServerListener> listeners;

	private final UndertowFactory undertowFactory;

	/**
	 * An instance of Undertow Wrapper (and wrapped {@link io.undertow.Undertow} inside) that's
	 * managed by this {@link ServerController}.
	 */
	private UndertowServerWrapper undertowServerWrapper;

				private IdentityManager identityManager;

	public UndertowServerController(Bundle paxWebUndertowBundle, ClassLoader classLoader,
			UndertowFactory undertowFactory, Configuration configuration) {
		this.paxWebUndertowBundle = paxWebUndertowBundle;
		this.classLoader = classLoader;
		this.undertowFactory = undertowFactory;
		this.configuration = configuration;
		this.state = ServerState.UNCONFIGURED;

		this.listeners = Collections.synchronizedSet(new LinkedHashSet<>());
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
		notifyListeners(new ServerEvent(ServerEvent.State.CONFIGURED, undertowServerWrapper.getAddresses(false)));
	}

	@Override
	public synchronized void start() throws Exception {
		LOG.info("Starting {}", this);

		if (state != ServerState.STOPPED) {
			throw new IllegalStateException("Can't start Undertow server controller in state " + state);
		}

		undertowServerWrapper.start();

		state = ServerState.STARTED;
		notifyListeners(new ServerEvent(ServerEvent.State.STARTED, undertowServerWrapper.getAddresses(true)));
	}

	@Override
	public void stop() throws Exception {
		LOG.info("Stopping {}", this);

		if (state != ServerState.STARTED) {
			throw new IllegalStateException("Can't stop Undertow server controller in state " + state);
		}

		undertowServerWrapper.stop();

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
			listener.stateChanged(new ServerEvent(ServerEvent.State.CONFIGURED, undertowServerWrapper.getAddresses(false)));
		} else if (state == ServerState.STARTED) {
			listener.stateChanged(new ServerEvent(ServerEvent.State.STARTED, undertowServerWrapper.getAddresses(true)));
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

		UndertowResourceServlet undertowResourceServlet = new UndertowResourceServlet(baseDirectory, chroot);

		// acces via "web root directory" - Pax Web special
		Integer maxSize = configuration.resources().maxTotalCacheSize();
		if (maxSize == null) {
			// no special default in Undertow, so take value from Jetty default 256 * 1024 * 1024 / 64
			// and be aware that there may be many such resource servlets
			maxSize = 256 * 1024 * 1024 / 64;
		}
		Integer maxEntrySize = configuration.resources().maxCacheEntrySize();
		if (maxEntrySize == null) {
			// no special default in Undertow, so take value from Jetty default 128 * 1024 * 1024 / 64
			maxEntrySize = 128 * 1024 * 1024 / 64;
		}
		Integer maxEntries = configuration.resources().maxCacheEntries();
		if (maxEntries == null) {
			// no special default in Undertow, so take value from Jetty default 2048
			maxEntries = 2048;
		}
		Integer maxAge = configuration.resources().maxCacheTTL();
		if (maxAge == null) {
			// arbitrary number
			maxAge = 60000 /* ms */;
		}

		// io.undertow.server.handlers.cache.LRUCache.maxEntries
		int metadataCacheSize = maxEntries;

		undertowResourceServlet.setCachingConfiguration(metadataCacheSize, maxEntrySize, maxSize, maxAge);

		return undertowResourceServlet;
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

}
