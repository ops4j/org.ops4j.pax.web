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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerEx;
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
import org.ops4j.pax.web.service.spi.model.WelcomeFileModel;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerControllerImpl implements ServerController, ServerControllerEx {

	private static final Logger LOG = LoggerFactory
			.getLogger(ServerControllerImpl.class);

	private Configuration configuration;
	private State state;
	private final JettyFactory jettyFactory;
	JettyServer jettyServer;
	private final Set<ServerListener> listeners;
	private ServerConnector httpConnector;
	private ServerConnector httpSecureConnector;
	private final Comparator<?> priorityComparator;

	ServerControllerImpl(final JettyFactory jettyFactory, Comparator<?> priorityComparator) {
		this.jettyFactory = jettyFactory;
		this.configuration = null;
		this.state = new Unconfigured();
		this.listeners = new CopyOnWriteArraySet<>();
		this.priorityComparator = priorityComparator;
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
	public synchronized void configure(final Configuration config) {
		LOG.debug("Configuring server [{}] -> [{}] ", this, config);
		if (config == null) {
			throw new IllegalArgumentException("configuration == null");
		}
		configuration = config;
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
	public void addWelcomFiles(final WelcomeFileModel model) {
		state.addWelcomeFiles(model);
	}

	@Override
	public void removeWelcomeFiles(final WelcomeFileModel model) {
		state.removeWelcomeFiles(model);
	}

	@Override
	public LifeCycle getContext(final ContextModel model) {
		return state.getContext(model);
	}

	@Override
	public void addSecurityConstraintMapping(SecurityConstraintMappingModel model) {
		state.addSecurityConstraintMapping(model);
	}

	@Override
	public void removeSecurityConstraintMapping(SecurityConstraintMappingModel model) {
		state.removeSecurityConstraintMappings(model);
	}

	@Override
	public void addContainerInitializerModel(ContainerInitializerModel model) {
		state.addContainerInitializerModel(model);
	}

	public void addCustomizers(Collection<Customizer> customizers) {
		state.addCustomizers(customizers);
	}

	public void removeCustomizers(Collection<Customizer> customizers) {
		state.removeCustomizers(customizers);
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

		LifeCycle getContext(ContextModel model);
		
		void addCustomizers(Collection<Customizer> customizers);
		
		void removeCustomizers(Collection<Customizer> customizers);

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
			jettyServer.removeContext(httpContext, true);
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

		@Override
		public void removeWelcomeFiles(WelcomeFileModel model) {
			jettyServer.removeWelcomeFiles(model);
		}

		@Override
		public void addWelcomeFiles(WelcomeFileModel model) {
			jettyServer.addWelcomeFiles(model);
		}

		@Override
		public void addCustomizers(Collection<Customizer> customizers) {
			Connector[] connectors = jettyServer.getConnectors();
			for (Connector connector : connectors) {
				Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
				for (ConnectionFactory connectionFactory : connectionFactories) {
					if (connectionFactory instanceof HttpConnectionFactory) {
						HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) connectionFactory;
						HttpConfiguration httpConfiguration = httpConnectionFactory.getHttpConfiguration();
						if (priorityComparator == null) {
							for (Customizer customizer : customizers) {
								httpConfiguration.addCustomizer(customizer);
							}
						} else {
    						List<Customizer> httpConfigurationCustomizers = httpConfiguration.getCustomizers();
    						httpConfigurationCustomizers.addAll(customizers);
    						@SuppressWarnings("unchecked")
    						Comparator<Customizer> comparator = (Comparator<Customizer>) priorityComparator;
    						Collections.sort(httpConfigurationCustomizers, comparator);
						}
					}
				}
			}
		}

		@Override
		public void removeCustomizers(Collection<Customizer> customizers)	{
			Connector[] connectors = jettyServer.getConnectors();
			for (Connector connector : connectors) {
				Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
				for (ConnectionFactory connectionFactory : connectionFactories) {
					if (connectionFactory instanceof HttpConnectionFactory) {
						HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) connectionFactory;
						HttpConfiguration httpConfiguration = httpConnectionFactory.getHttpConfiguration();
						List<Customizer> httpConfigurationCustomizers = httpConfiguration.getCustomizers();
						httpConfigurationCustomizers.removeAll(customizers);
					}
				}
			}
		}

	}

	private class Stopped implements State {

		Stopped() {
			httpConnector = null;
			httpSecureConnector = null;
		}

		@Override
		public void start() {
			jettyServer = jettyFactory.createServer(configuration.getServerMaxThreads(), configuration.getServerMinThreads(), configuration.getServerIdleTimeout());

			httpConnector = null;
			httpSecureConnector = null;
			String[] addresses = configuration.getListeningAddresses();
			if (addresses == null || addresses.length == 0) {
				addresses = new String[]{null};
			}
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("javax.servlet.context.tempdir",
					configuration.getTemporaryDirectory());

			// Fix for PAXWEB-193
			jettyServer.setServerConfigDir(configuration.getConfigurationDir());
			jettyServer.setServerConfigURL(configuration.getConfigurationURL());
			jettyServer.setDefaultAuthMethod(configuration.getDefaultAuthMethod());
			jettyServer.setDefaultRealmName(configuration.getDefaultRealmName());
			jettyServer.configureContext(attributes,
					configuration.getSessionTimeout(),
					configuration.getSessionCookie(),
					configuration.getSessionDomain(),
					configuration.getSessionPath(),
					configuration.getSessionUrl(),
					configuration.getSessionCookieHttpOnly(),
					configuration.getSessionCookieSecure(),
					configuration.getWorkerName(),
					configuration.getSessionLazyLoad(),
					configuration.getSessionStoreDirectory(),
					configuration.getSessionCookieMaxAge());

			// Configure NCSA RequestLogHandler
			if (configuration.isLogNCSAFormatEnabled()) {
				jettyServer.configureRequestLog(
						new ConfigureRequestLogParameter(configuration.getLogNCSAFormat(), configuration.getLogNCSARetainDays(),
								configuration.isLogNCSAAppend(), configuration.isLogNCSAExtended(), configuration.isLogNCSADispatch(), configuration.getLogNCSATimeZone(),
								configuration.getLogNCSADirectory(), configuration.isLogNCSALatency(), configuration.isLogNCSACookies(), configuration.isLogNCSAServer()));
			}

			jettyServer.start();
			for (String address : addresses) {
				Integer httpPort = configuration.getHttpPort();
				// Boolean useNIO = configuration.useNIO();
				Integer httpSecurePort = configuration.getHttpSecurePort();

				// Server should listen to std. http.
				if (configuration.isHttpEnabled()) {
					Connector[] connectors = jettyServer.getConnectors();
					// Flag is set if the same connector has been found
					// through xml config and properties
					boolean masterConnectorFound = false;
					if (connectors != null && connectors.length > 0) {
						// Combine the configurations if they do match
						ServerConnector backupConnector = null;

						for (Connector connector : connectors) {
							if ((connector instanceof ServerConnector)
									&& (connector
									.getConnectionFactory(SslConnectionFactory.class)) == null) {
								if (match(address, httpPort, connector)) {
									// the same connection as configured through
									// property/config-admin already is
									// configured through jetty.xml
									// therefore just use it as the one if not
									// already done so.
									//CHECKSTYLE:OFF
									if (httpConnector == null) {
										httpConnector = (ServerConnector) connector;
									}
									//CHECKSTYLE:ON
									masterConnectorFound = true;
								} else {
									//CHECKSTYLE:OFF
									if (backupConnector == null) {
										backupConnector = (ServerConnector) connector;
									}
									//CHECKSTYLE:ON
								}
							}
						}
						if (httpConnector == null && backupConnector != null) {
							httpConnector = backupConnector;
						}
					}
					if (!masterConnectorFound) {
						final Connector connector = jettyFactory
								.createConnector(jettyServer.getServer(),
										configuration.getHttpConnectorName(),
										httpPort, configuration.getConnectorIdleTimeout(), httpSecurePort, address, configuration.checkForwardedHeaders());
						if (httpConnector == null) {
							httpConnector = (ServerConnector) connector;
						}
						jettyServer.addConnector(connector);
					}
				} else {
					// remove maybe already configured connectors through
					// jetty.xml, the config-property/config-admin service is
					// master configuration
					Connector[] connectors = jettyServer.getConnectors();
					if (connectors != null) {
						for (Connector connector : connectors) {
							if ((connector instanceof Connector)
									&& (connector
									.getConnectionFactory(SslConnectionFactory.class)) == null) {
								LOG.warn(String
										.format("HTTP is not enabled in Pax Web configuration - removing connector: %s",
												connector));
								jettyServer.removeConnector(connector);
							}
						}
					}
				}
				if (configuration.isHttpSecureEnabled()) {
					final String sslKeystorePassword = configuration.getSslKeystorePassword();
					final String sslKeyPassword = configuration.getSslKeyPassword();

					Connector[] connectors = jettyServer.getConnectors();
					boolean masterSSLConnectorFound = false;
					if (connectors != null && connectors.length > 0) {
						// Combine the configurations if they do match
						ServerConnector backupConnector = null;
						for (Connector connector : connectors) {
							if (connector
									.getConnectionFactory(SslConnectionFactory.class) != null) {
								ServerConnector sslCon = (ServerConnector) connector;
								String[] split = connector.getName().split(":");
								if (split.length == 2 && httpSecurePort == Integer.valueOf(split[1])
										.intValue()
										&& address.equalsIgnoreCase(split[0])) {
									httpSecureConnector = sslCon;
									masterSSLConnectorFound = true;
								} else {
									// default behavior
									//CHECKSTYLE:OFF
									if (backupConnector == null) {
										backupConnector = (ServerConnector) connector;
									}
									//CHECKSTYLE:ON
								}
							}
						}
						if (httpSecureConnector == null
								&& backupConnector != null) {
							httpSecureConnector = backupConnector;
						}
					}

					if (!masterSSLConnectorFound) {
						// no combination of jetty.xml and
						// config-admin/properties needed
						if (sslKeystorePassword != null && sslKeyPassword != null) {
							final Connector secureConnector = jettyFactory
									.createSecureConnector(jettyServer
													.getServer(),
											configuration.getHttpSecureConnectorName(),
											httpSecurePort,
											configuration.getConnectorIdleTimeout(),
											configuration.getSslKeystore(),
											sslKeystorePassword,
											sslKeyPassword,
											address,
											configuration.getSslKeystoreType(),
											configuration.getSslKeyAlias(),
											configuration.getTrustStore(),
											configuration.getTrustStorePassword(),
											configuration.getTrustStoreType(),
											configuration.isClientAuthNeeded(),
											configuration.isClientAuthWanted(),
											configuration.getCiphersuiteIncluded(),
											configuration.getCiphersuiteExcluded(),
											configuration.getProtocolsIncluded(),
											configuration.getProtocolsExcluded(),
											configuration.isSslRenegotiationAllowed(),
											configuration.getCrlPath(),
											configuration.isEnableCRLDP(),
											configuration.isValidateCerts(),
											configuration.isValidatePeerCerts(),
											configuration.isEnableOCSP(),
											configuration.getOcspResponderURL(),
											configuration.checkForwardedHeaders());
							if (httpSecureConnector == null) {
								httpSecureConnector = (ServerConnector) secureConnector;
							}
							jettyServer.addConnector(secureConnector);
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
							if (connector
									.getConnectionFactory(SslConnectionFactory.class) != null) {
								LOG.warn(String
										.format("HTTPS is not enabled in Pax Web configuration - removing connector: %s",
												connector));
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
			InetSocketAddress isa2 = ((ServerConnector) connector).getHost() != null ? new InetSocketAddress(
					((ServerConnector) connector).getHost(),
					((ServerConnector) connector).getPort())
					: new InetSocketAddress(
					((ServerConnector) connector).getPort());
			return isa1.equals(isa2);
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

		@SuppressWarnings("unused")
		public void addLoginConfig(LoginConfigModel model) {
			// do nothing if server is not started
		}

		@SuppressWarnings("unused")
		public void removeLoginConfig(LoginConfigModel model) {
			// do nothing if server is not started
		}

		@Override
		public void addSecurityConstraintMapping(
				SecurityConstraintMappingModel model) {
			// do nothing if server is not started
		}

		@Override
		public void addCustomizers(Collection<Customizer> customizers) {
			// do nothing if server is not started
		}

		@Override
		public void removeCustomizers(Collection<Customizer> customizers)	{
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

		@Override
		public void removeWelcomeFiles(WelcomeFileModel model) {
			// do nothing if server is not started
		}

		@Override
		public void addWelcomeFiles(WelcomeFileModel model) {
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
