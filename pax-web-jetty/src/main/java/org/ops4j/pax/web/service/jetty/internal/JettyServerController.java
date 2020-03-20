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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.ServerState;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
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

	private final List<ServerListener> listeners;

	private final JettyFactory jettyFactory;

	/**
	 * An instance of Jetty Wrapper (and wrapped {@link org.eclipse.jetty.server.Server} inside) that's
	 * managed by this {@link ServerController}.
	 */
	private JettyServerWrapper jettyServerWrapper;

						private Comparator<?> priorityComparator;

	JettyServerController(Bundle paxWebJettyBundle, ClassLoader classLoader, JettyFactory jettyFactory, Configuration configuration) {
		this.paxWebJettyBundle = paxWebJettyBundle;
		this.classLoader = classLoader;
		this.jettyFactory = jettyFactory;
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
			throw new IllegalStateException("Can't configure server controller in state " + state);
		}

		jettyServerWrapper = new JettyServerWrapper(configuration, jettyFactory, paxWebJettyBundle, classLoader);
		jettyServerWrapper.configure();

		state = ServerState.STOPPED;
		notifyListeners(ServerEvent.CONFIGURED);
	}

	@Override
	public void start() throws Exception {
		LOG.info("Starting {}", this);

		if (state != ServerState.STOPPED) {
			throw new IllegalStateException("Can't start server controller in state " + state);
		}

		jettyServerWrapper.start();

		state = ServerState.STARTED;
		notifyListeners(ServerEvent.STARTED);
	}

	@Override
	public void stop() throws Exception {
		LOG.info("Stopping {}", this);

		if (state != ServerState.STARTED) {
			throw new IllegalStateException("Can't stop server controller in state " + state);
		}

		jettyServerWrapper.stop();

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
			throw new IllegalArgumentException("listener == null");
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
			throw new IllegalStateException("Can't process batch in server controller in state " + state);
		}

		batch.accept(jettyServerWrapper);
	}

//	@Override
//	public void addServlet(final ServletModel model) {
//		state.addServlet(model);
//	}
//
//	@Override
//	public void removeServlet(final ServletModel model) {
//		state.removeServlet(model);
//	}
//
//	@Override
//	public boolean isStarted() {
//		return state instanceof Started;
//	}
//
//	@Override
//	public boolean isConfigured() {
//		return !(state instanceof Unconfigured);
//	}
//
//	@Override
//	public void addEventListener(final EventListenerModel eventListenerModel) {
//		state.addEventListener(eventListenerModel);
//	}
//
//	@Override
//	public void removeEventListener(final EventListenerModel eventListenerModel) {
//		state.removeEventListener(eventListenerModel);
//	}
//
//	@Override
//	public void removeContext(HttpContext httpContext) {
//		state.removeContext(httpContext);
//	}
//
//	@Override
//	public void addFilter(final FilterModel filterModel) {
//		state.addFilter(filterModel);
//	}
//
//	@Override
//	public void removeFilter(final FilterModel filterModel) {
//		state.removeFilter(filterModel);
//	}
//
//	@Override
//	public void addErrorPage(final ErrorPageModel model) {
//		state.addErrorPage(model);
//	}
//
//	@Override
//	public void removeErrorPage(final ErrorPageModel model) {
//		state.removeErrorPage(model);
//	}
//
//	@Override
//	public void addWelcomFiles(final WelcomeFileModel model) {
//		state.addWelcomeFiles(model);
//	}
//
//	@Override
//	public void removeWelcomeFiles(final WelcomeFileModel model) {
//		state.removeWelcomeFiles(model);
//	}
//
//	@Override
//	public LifeCycle getContext(final OsgiContextModel model) {
//		return state.getContext(model);
//	}
//
//	@Override
//	public void addSecurityConstraintMapping(SecurityConstraintMappingModel model) {
//		state.addSecurityConstraintMapping(model);
//	}
//
//	@Override
//	public void removeSecurityConstraintMapping(SecurityConstraintMappingModel model) {
//		state.removeSecurityConstraintMappings(model);
//	}
//
//	@Override
//	public void addContainerInitializerModel(ContainerInitializerModel model) {
//		state.addContainerInitializerModel(model);
//	}
//
//	public void addCustomizers(Collection<Customizer> customizers) {
//		state.addCustomizers(customizers);
//	}
//
//	public void removeCustomizers(Collection<Customizer> customizers) {
//		state.removeCustomizers(customizers);
//	}
//
//	@Override
//	public Integer getHttpPort() {
//		if (httpConnector != null && httpConnector.isStarted()) {
//			return httpConnector.getLocalPort();
//		}
//		return configuration.server().getHttpPort();
//	}
//
//	@Override
//	public Integer getHttpSecurePort() {
//		if (httpSecureConnector != null && httpSecureConnector.isStarted()) {
//			return httpSecureConnector.getLocalPort();
//		}
//		return configuration.server().getHttpSecurePort();
//	}
//
//	@Override
//	public Servlet createResourceServlet(OsgiContextModel contextModel,
//										 String alias, String name) {
//		return new ResourceServlet(contextModel.getHttpContext(),
//				/*contextModel.getContextName()*/"", alias, name);
//	}

	@Override
	public String toString() {
		return "JettyServerController{configuration=" + configuration.id() + ",state=" + state + "}";
	}

	private interface State {

		void start();

		void removeWelcomeFiles(WelcomeFileModel model);

		void addWelcomeFiles(WelcomeFileModel model);

		void addContainerInitializerModel(ContainerInitializerModel model);

		void addSecurityConstraintMapping(SecurityConstraintMappingModel model);

		void removeSecurityConstraintMappings(
				SecurityConstraintMappingModel model);

		void stop();

		void configure();

		void addServlet(ServletModel model);

		void removeServlet(ServletModel model);

		void addEventListener(EventListenerModel eventListenerModel);

		void removeEventListener(EventListenerModel eventListenerModel);

		void removeContext(HttpContext httpContext);

		void addFilter(FilterModel filterModel);

		void removeFilter(FilterModel filterModel);

		void addErrorPage(ErrorPageModel model);

		void removeErrorPage(ErrorPageModel model);

		LifeCycle getContext(OsgiContextModel model);
		
		void addCustomizers(Collection<Customizer> customizers);
		
		void removeCustomizers(Collection<Customizer> customizers);

	}

//	private class Started implements State {
//
//		@Override
//		public void stop() {
//			jettyServer.stop();
//			state = new Stopped();
//			notifyListeners(ServerEvent.STOPPED);
//		}
//
//		@Override
//		public void addCustomizers(Collection<Customizer> customizers) {
//			Connector[] connectors = jettyServer.getConnectors();
//			for (Connector connector : connectors) {
//				Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
//				for (ConnectionFactory connectionFactory : connectionFactories) {
//					if (connectionFactory instanceof HttpConnectionFactory) {
//						HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) connectionFactory;
//						HttpConfiguration httpConfiguration = httpConnectionFactory.getHttpConfiguration();
//						if (priorityComparator == null) {
//							for (Customizer customizer : customizers) {
//								httpConfiguration.addCustomizer(customizer);
//							}
//						} else {
//    						List<Customizer> httpConfigurationCustomizers = httpConfiguration.getCustomizers();
//    						httpConfigurationCustomizers.addAll(customizers);
//    						@SuppressWarnings("unchecked")
//    						Comparator<Customizer> comparator = (Comparator<Customizer>) priorityComparator;
//    						Collections.sort(httpConfigurationCustomizers, comparator);
//						}
//					}
//				}
//			}
//		}
//
//		@Override
//		public void removeCustomizers(Collection<Customizer> customizers)	{
//			Connector[] connectors = jettyServer.getConnectors();
//			for (Connector connector : connectors) {
//				Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
//				for (ConnectionFactory connectionFactory : connectionFactories) {
//					if (connectionFactory instanceof HttpConnectionFactory) {
//						HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) connectionFactory;
//						HttpConfiguration httpConfiguration = httpConnectionFactory.getHttpConfiguration();
//						List<Customizer> httpConfigurationCustomizers = httpConfiguration.getCustomizers();
//						httpConfigurationCustomizers.removeAll(customizers);
//					}
//				}
//			}
//		}
//
//	}

}
