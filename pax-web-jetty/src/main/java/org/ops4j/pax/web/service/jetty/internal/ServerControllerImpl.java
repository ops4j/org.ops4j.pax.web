/* Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.jetty.internal;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerControllerImpl implements ServerController {

	private static final Logger LOG = LoggerFactory
			.getLogger(ServerControllerImpl.class);

	private Configuration configuration;
	private State state;
	private final JettyFactory jettyFactory;
	private JettyServer jettyServer;
	private final Set<ServerListener> listeners;
	private Connector httpConnector;
	private Connector httpSecureConnector;

	ServerControllerImpl(final JettyFactory jettyFactory) {
		this.jettyFactory = jettyFactory;
		this.configuration = null;
		this.state = new Unconfigured();
		this.listeners = new CopyOnWriteArraySet<ServerListener>();
	}

	@Override
	public synchronized void start() {
		LOG.debug("Starting server [{}]", this);
		state.start();
	}

	@Override
	public synchronized void stop() {
		LOG.debug("Stopping server [{}]", this);
		state.stop();
	}

	@Override
	public synchronized void configure(final Configuration configuration) {
		LOG.debug("Configuring server [{}] -> [{}] ", this, configuration);
		if (configuration == null) {
			throw new IllegalArgumentException("configuration == null");
		}
		this.configuration = configuration;
		state.configure();
	}

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

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

	@Override
	public void addServlet(final ServletModel model) {
		state.addServlet(model);
	}

	@Override
	public void removeServlet(final ServletModel model) {
		state.removeServlet(model);
	}

	@Override
	public boolean isStarted() {
		return state instanceof Started;
	}

	@Override
	public boolean isConfigured() {
		return !(state instanceof Unconfigured);
	}

	@Override
	public void addEventListener(final EventListenerModel eventListenerModel) {
		state.addEventListener(eventListenerModel);
	}

	@Override
	public void removeEventListener(final EventListenerModel eventListenerModel) {
		state.removeEventListener(eventListenerModel);
	}

	@Override
	public void removeContext(HttpContext httpContext) {
		state.removeContext(httpContext);
	}

	@Override
	public void addFilter(final FilterModel filterModel) {
		state.addFilter(filterModel);
	}

	@Override
	public void removeFilter(final FilterModel filterModel) {
		state.removeFilter(filterModel);
	}

	@Override
	public void addErrorPage(final ErrorPageModel model) {
		state.addErrorPage(model);
	}

	@Override
	public void removeErrorPage(final ErrorPageModel model) {
		state.removeErrorPage(model);
	}

	@Override
	public LifeCycle getContext(final ContextModel model) {
		return state.getContext(model);
	}

	@Override
	public void addSecurityConstraintMapping(
			SecurityConstraintMappingModel model) {
		state.addSecurityConstraintMapping(model);
	}

	@Override
	public void addContainerInitializerModel(ContainerInitializerModel model) {
		state.addContainerInitializerModel(model);
	}

	@Override
	public Integer getHttpPort() {
		if (httpConnector != null && httpConnector.isStarted()) {
			return httpConnector.getLocalPort();
		}
		return configuration.getHttpPort();
	}

	@Override
	public Integer getHttpSecurePort() {
		if (httpSecureConnector != null && httpSecureConnector.isStarted()) {
			return httpSecureConnector.getLocalPort();
		}
		return configuration.getHttpSecurePort();
	}

	@Override
	public Servlet createResourceServlet(ContextModel contextModel,
			String alias, String name) {
		return new ResourceServlet(contextModel.getHttpContext(),
				contextModel.getContextName(), alias, name);
	}

	void notifyListeners(ServerEvent event) {
		for (ServerListener listener : listeners) {
			listener.stateChanged(event);
		}
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append(ServerControllerImpl.class.getSimpleName()).append("{")
				.append("state=").append(state).append("}").toString();
	}

	private interface State {

		void start();

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

		LifeCycle getContext(ContextModel model);

	}

	private class Started implements State {

		@Override
		public void start() {
			throw new IllegalStateException(
					"server is already started. must be stopped first.");
		}

		@Override
		public void stop() {
			jettyServer.stop();
			state = new Stopped();
			notifyListeners(ServerEvent.STOPPED);
		}

		@Override
		public void configure() {
			ServerControllerImpl.this.stop();
			ServerControllerImpl.this.start();
		}

		@Override
		public void addServlet(final ServletModel model) {
			jettyServer.addServlet(model);
		}

		@Override
		public void removeServlet(final ServletModel model) {
			jettyServer.removeServlet(model);
		}

		@Override
		public void addEventListener(EventListenerModel eventListenerModel) {
			jettyServer.addEventListener(eventListenerModel);
		}

		@Override
		public void removeEventListener(EventListenerModel eventListenerModel) {
			jettyServer.removeEventListener(eventListenerModel);
		}

		@Override
		public void removeContext(HttpContext httpContext) {
			jettyServer.removeContext(httpContext);
		}

		@Override
		public void addFilter(FilterModel filterModel) {
			jettyServer.addFilter(filterModel);
		}

		@Override
		public void removeFilter(FilterModel filterModel) {
			jettyServer.removeFilter(filterModel);
		}

		@Override
		public void addErrorPage(ErrorPageModel model) {
			jettyServer.addErrorPage(model);
		}

		@Override
		public void removeErrorPage(ErrorPageModel model) {
			jettyServer.removeErrorPage(model);
		}

		@Override
		public void removeSecurityConstraintMappings(
				SecurityConstraintMappingModel model) {
			jettyServer.removeSecurityConstraintMappings(model);
		}

		@Override
		public void addSecurityConstraintMapping(
				SecurityConstraintMappingModel model) {
			jettyServer.addSecurityConstraintMappings(model);
		}

		@Override
		public LifeCycle getContext(ContextModel model) {
			return jettyServer.getContext(model);
		}

		@Override
		public String toString() {
			return "STARTED";
		}

		@Override
		public void addContainerInitializerModel(ContainerInitializerModel model) {
			jettyServer.addServletContainerInitializer(model);
		}
	}

	private class Stopped implements State {

		Stopped() {
			httpConnector = null;
			httpSecureConnector = null;
		}

		@Override
		public void start() {
			jettyServer = jettyFactory.createServer();
			httpConnector = null;
			httpSecureConnector = null;
			String[] addresses = configuration.getListeningAddresses();
			if (addresses == null || addresses.length == 0) {
				addresses = new String[] { null };
			}
			Map<String, Object> attributes = new HashMap<String, Object>();
			attributes.put("javax.servlet.context.tempdir",
					configuration.getTemporaryDirectory());

			jettyServer.setServerConfigDir(configuration
					.getConfigurationDir()); // Fix for PAXWEB-193
			jettyServer.setServerConfigURL(configuration
					.getConfigurationURL());
			jettyServer.configureContext(attributes,
					configuration.getSessionTimeout(),
					configuration.getSessionCookie(),
					configuration.getSessionUrl(),
					configuration.getSessionCookieHttpOnly(),
					configuration.getWorkerName(),
					configuration.getSessionLazyLoad(),
					configuration.getSessionStoreDirectory());

			// Configure NCSA RequestLogHandler

			if (configuration.isLogNCSAFormatEnabled()) {
				jettyServer.configureRequestLog(
						configuration.getLogNCSAFormat(),
						configuration.getLogNCSARetainDays(),
						configuration.isLogNCSAAppend(),
						configuration.isLogNCSAExtended(),
						configuration.isLogNCSADispatch(),
						configuration.getLogNCSATimeZone(),
						configuration.getLogNCSADirectory());
			}

			jettyServer.start();
			for (String address : addresses) {
				Integer httpPort = configuration.getHttpPort();
				Boolean useNIO = configuration.useNIO();
				Integer httpSecurePort = configuration.getHttpSecurePort();

				if (configuration.isHttpEnabled()) {
					Connector[] connectors = jettyServer.getConnectors();
					boolean masterConnectorFound = false; // Flag is set if the
															// same connector
															// has been found
															// through xml
															// config and
															// properties
					if (connectors != null && connectors.length > 0) {
						// Combine the configurations if they do match
						Connector backupConnector = null;

						for (Connector connector : connectors) {
							if ((connector instanceof Connector)
									&& !(connector instanceof SslConnector)) {
								if (match(address, httpPort, connector)) {
									// the same connection as configured through
									// property/config-admin already is
									// configured through jetty.xml
									// therefore just use it as the one if not
									// already done so.
									if (httpConnector == null) {
										httpConnector = connector;
									}
									if (!connector.isStarted()) {
										startConnector(connector);
									}
									masterConnectorFound = true;
								} else {
									if (backupConnector == null) {
										backupConnector = connector;
									}
									if (!connector.isStarted()) {
										startConnector(connector);
									}
								}
							}
						}

						if (httpConnector == null && backupConnector != null) {
							httpConnector = backupConnector;
						}
					}

					if (!masterConnectorFound) {
						final Connector connector = jettyFactory
								.createConnector(
										configuration.getHttpConnectorName(),
										httpPort, address, useNIO);
						if (httpConnector == null) {
							httpConnector = connector;
						}
						jettyServer.addConnector(connector);
						startConnector(connector);
					}
				} else {
					// remove maybe already configured connectors throuhg
					// jetty.xml, the config-property/config-admin service is
					// master configuration
					Connector[] connectors = jettyServer.getConnectors();
					if (connectors != null) {
						for (Connector connector : connectors) {
							if ((connector instanceof Connector)
									&& !(connector instanceof SslConnector)) {
								jettyServer.removeConnector(connector);
							}
						}
					}
				}
				if (configuration.isHttpSecureEnabled()) {
					final String sslPassword = configuration.getSslPassword();
					final String sslKeyPassword = configuration
							.getSslKeyPassword();

					Connector[] connectors = jettyServer.getConnectors();
					boolean masterSSLConnectorFound = false;
					if (connectors != null && connectors.length > 0) {
						// Combine the configurations if they do match
						Connector backupConnector = null;

						for (Connector connector : connectors) {
							if (connector instanceof SslConnector) {
								SslConnector sslCon = (SslConnector) connector;
								String[] split = connector.getName().split(":");
								if (httpSecurePort == Integer.valueOf(split[1])
										.intValue()
										&& address.equalsIgnoreCase(split[0])) {
									httpSecureConnector = sslCon;

									if (!sslCon.isStarted()) {
										startConnector(sslCon);
									}
									masterSSLConnectorFound = true;

								} else {
									// default behavior
									if (backupConnector == null) {
										backupConnector = connector;
									}

									if (!connector.isStarted()) {
										startConnector(connector);
									}
								}
							}
						}
						if (httpSecureConnector == null && backupConnector != null) {
							httpSecureConnector = backupConnector;
						}
					}

					if (!masterSSLConnectorFound) {
						// no combination of jetty.xml and
						// config-admin/properties needed
						if (sslPassword != null && sslKeyPassword != null) {
							final Connector secureConnector = jettyFactory
									.createSecureConnector(configuration
											.getHttpSecureConnectorName(),
											httpSecurePort, configuration
													.getSslKeystore(),
											sslPassword, sslKeyPassword,
											address, configuration
													.getSslKeystoreType(),
											configuration
													.isClientAuthNeeded(),
											configuration
													.isClientAuthWanted());
							if (httpSecureConnector == null) {
								httpSecureConnector = secureConnector;
							}
							jettyServer.addConnector(secureConnector);
							startConnector(secureConnector);
						} else {
							LOG.warn("SSL password and SSL keystore password must be set in order to enable SSL.");
							LOG.warn("SSL connector will not be started");
						}
					}
				} else {
					// remove maybe already configured connectors through
					// jetty.xml, the config-property/config-admin service is
					// master configuration
					Connector[] connectors = jettyServer.getConnectors();
					if (connectors != null) {
						for (Connector connector : connectors) {
							if (connector instanceof SslConnector) {
								jettyServer.removeConnector(connector);
							}
						}
					}
				}
			}
			state = new Started();
			notifyListeners(ServerEvent.STARTED);
		}

		private boolean match(String address, Integer httpPort,
				Connector connector) {
			InetSocketAddress isa1 = address != null ? new InetSocketAddress(
					address, httpPort) : new InetSocketAddress(httpPort);
			InetSocketAddress isa2 = connector.getHost() != null ? new InetSocketAddress(
					connector.getHost(), connector.getPort())
					: new InetSocketAddress(connector.getPort());
			return isa1.equals(isa2);
		}

		private void startConnector(Connector connector) {
			try {
				connector.start();
			} catch (Exception e) { //CHECKSTYLE:SKIP
				LOG.warn("Http connector will not be started", e);
			}
		}

		@Override
		public void stop() {
			// do nothing. already stopped
		}

		@Override
		public void configure() {
			notifyListeners(ServerEvent.CONFIGURED);
		}

		@Override
		public void addServlet(final ServletModel model) {
			// do nothing if server is not started
		}

		@Override
		public void removeServlet(final ServletModel model) {
			// do nothing if server is not started
		}

		@Override
		public void addEventListener(EventListenerModel eventListenerModel) {
			// do nothing if server is not started
		}

		@Override
		public void removeEventListener(EventListenerModel eventListenerModel) {
			// do nothing if server is not started
		}

		@Override
		public void removeContext(HttpContext httpContext) {
			// do nothing if server is not started
		}

		@Override
		public void addFilter(FilterModel filterModel) {
			// do nothing if server is not started
		}

		@Override
		public void removeFilter(FilterModel filterModel) {
			// do nothing if server is not started
		}

		@Override
		public void addErrorPage(ErrorPageModel model) {
			// do nothing if server is not started
		}

		@Override
		public void removeErrorPage(ErrorPageModel model) {
			// do nothing if server is not started
		}

		@Override
		public void removeSecurityConstraintMappings(
				SecurityConstraintMappingModel model) {
			// do nothing if server is not started
		}

		public void addLoginConfig(LoginConfigModel model) {
			// do nothing if server is not started
		}

		public void removeLoginConfig(LoginConfigModel model) {
			// do nothing if server is not started
		}

		@Override
		public void addSecurityConstraintMapping(
				SecurityConstraintMappingModel model) {
			// do nothing if server is not started
		}

		@Override
		public LifeCycle getContext(ContextModel model) {
			return null;
		}

		@Override
		public String toString() {
			return "STOPPED";
		}

		@Override
		public void addContainerInitializerModel(ContainerInitializerModel model) {
			// do nothing if server is not started
		}

	}

	private class Unconfigured extends Stopped {

		@Override
		public void start() {
			throw new IllegalStateException("server is not yet configured.");
		}

		@Override
		public void configure() {
			state = new Stopped();
			notifyListeners(ServerEvent.CONFIGURED);
			ServerControllerImpl.this.start();
		}

		@Override
		public String toString() {
			return "UNCONFIGURED";
		}
	}

}
