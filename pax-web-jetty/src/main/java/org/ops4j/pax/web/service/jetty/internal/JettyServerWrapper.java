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

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.http.HttpSessionAttributeListener;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.security.ConstraintAware;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
import org.eclipse.jetty.security.authentication.ConfigurableSpnegoAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.FileSessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.ops4j.pax.web.service.jetty.internal.web.JettyResourceServlet;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.LogConfiguration;
import org.ops4j.pax.web.service.spi.config.SessionConfiguration;
import org.ops4j.pax.web.service.spi.model.ContextMetadataModel;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.SessionConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.WebSocketModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.model.events.ServerEvent;
import org.ops4j.pax.web.service.spi.servlet.Default404Servlet;
import org.ops4j.pax.web.service.spi.servlet.DynamicRegistrations;
import org.ops4j.pax.web.service.spi.servlet.OsgiDynamicServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedServlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContextClassLoader;
import org.ops4j.pax.web.service.spi.servlet.OsgiSessionAttributeListener;
import org.ops4j.pax.web.service.spi.servlet.PreprocessorFilterConfig;
import org.ops4j.pax.web.service.spi.servlet.RegisteringContainerInitializer;
import org.ops4j.pax.web.service.spi.servlet.SCIWrapper;
import org.ops4j.pax.web.service.spi.task.BatchVisitor;
import org.ops4j.pax.web.service.spi.task.ClearDynamicRegistrationsChange;
import org.ops4j.pax.web.service.spi.task.ContainerInitializerModelChange;
import org.ops4j.pax.web.service.spi.task.ContextMetadataModelChange;
import org.ops4j.pax.web.service.spi.task.ErrorPageModelChange;
import org.ops4j.pax.web.service.spi.task.ErrorPageStateChange;
import org.ops4j.pax.web.service.spi.task.EventListenerModelChange;
import org.ops4j.pax.web.service.spi.task.FilterModelChange;
import org.ops4j.pax.web.service.spi.task.FilterStateChange;
import org.ops4j.pax.web.service.spi.task.MimeAndLocaleMappingChange;
import org.ops4j.pax.web.service.spi.task.OpCode;
import org.ops4j.pax.web.service.spi.task.OsgiContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletModelChange;
import org.ops4j.pax.web.service.spi.task.TransactionStateChange;
import org.ops4j.pax.web.service.spi.task.WebSocketModelChange;
import org.ops4j.pax.web.service.spi.task.WelcomeFileModelChange;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>A <em>wrapper</em> or <em>holder</em> of actual Jetty server. This class perform two kinds of tasks:<ul>
 *     <li>controls the state of Jetty by configuring, starting and stopping it</li>
 *     <li>translates model changes into registration of Jetty-specific contexts, holders and handlers</li>
 * </ul></p>
 *
 * <p>This wrapper implements {@link BatchVisitor} to process batch operations related to model changes.</p>
 */
class JettyServerWrapper implements BatchVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(JettyServerWrapper.class);

	/** An <em>entry</em> to OSGi runtime to lookup other bundles if needed (to get their ClassLoader) */
	private final Bundle paxWebJettyBundle;
	/** Outside of OSGi, let's use passed ClassLoader */
	private final ClassLoader classLoader;

	/** Actual instance of {@link org.eclipse.jetty.server.Server} */
	private Server server;

	/** Server's pool which is added as UNMANAGED */
	private QueuedThreadPool qtp;

	/** If JMX support is enabled, this will be the Jetty bean for JMX management */
	private MBeanContainer mbeanContainer;

	/** Main handler collection for Jetty server */
	private final PrioritizedHandlerCollection mainHandler;

	/** If {@code jetty*.xml} files create instances of {@link HttpConfiguration}, these are collected here. */
	private final Map<String, HttpConfiguration> httpConfigs = new LinkedHashMap<>();

	private final JettyFactory jettyFactory;

	/**
	 * A set of context paths that are being configured within <em>transactions</em> - context is started only at
	 * the end of the transaction.
	 */
	private final Set<String> transactions = new HashSet<>();

	/**
	 * When delaying removal of servlets and listeners (not filters, error pages, ...), we have to ensure that
	 * they are really removed. It is automatic for WABs, where entire context is destroyed, but not necessarily
	 * for HttpService/WebContainer and Whiteboard scenarios. I observed this when working on sample Karaf
	 * commands that allowed me to register/unregister servlets on demand, controlling when the HttpService
	 * instance is <em>unget</em>.
	 */
	private final Map<String, List<ElementModel<?, ?>>> delayedRemovals = new HashMap<>();

	/** Single map of context path to {@link ServletContextHandler} for fast access */
	private final Map<String, PaxWebServletContextHandler> contextHandlers = new HashMap<>();

	/**
	 * 1:1 mapping between {@link OsgiContextModel} and {@link org.osgi.service.http.context.ServletContextHelper}'s
	 * specific {@link javax.servlet.ServletContext}.
	 */
	private final Map<OsgiContextModel, OsgiServletContext> osgiServletContexts = new HashMap<>();

	/**
	 * 1:N mapping between context path and sorted (by ranking rules) set of {@link OsgiContextModel}. This helps
	 * finding proper {@link org.osgi.service.http.context.ServletContextHelper} (1:1 with {@link OsgiContextModel})
	 * to use for filters, when the invocation chain doesn't contain target servlet (which otherwise would
	 * determine the ServletContextHelper to use).
	 */
	private final Map<String, TreeSet<OsgiContextModel>> osgiContextModels = new HashMap<>();

	/**
	 * Jetty somehow doesn't manage {@link ServletContainerInitializer}s so nicely (or I didn't find a way to do
	 * it properly), so we keep an ordered list of initializers per physical context here. Each initializer is
	 * associated with single {@link OsgiContextModel}.
	 */
	private final Map<String, LinkedHashMap<Integer, SCIWrapper>> initializers = new HashMap<>();

	/**
	 * Keep dynamic configuration and use it during startup only.
	 */
	private final Map<String, DynamicRegistrations> dynamicRegistrations = new HashMap<>();

	/**
	 * Global {@link Configuration} passed from pax-web-runtime through
	 * {@link org.ops4j.pax.web.service.spi.ServerController}
	 */
	private final Configuration configuration;

	/** Servlet to use when no servlet is mapped - to ensure that preprocessors and filters are run correctly. */
	private final Default404Servlet default404Servlet = new Default404Servlet();

	private SessionCookieConfig defaultSessionCookieConfig;

	/**
	 * All {@link EventListenerModel} instances for {@link HttpSessionAttributeListener} listeners. They'll be
	 * reviewed in order to propagate session attribute events per {@link OsgiContextModel}.
	 */
	private final List<EventListenerModel> sessionListenerModels = new ArrayList<>();

	private final Set<PriorityValue<HttpConfiguration.Customizer>> registeredCustomizers = new TreeSet<>(JettyServerControllerFactory.priorityComparator);

	JettyServerWrapper(Configuration config, JettyFactory jettyFactory,
			Bundle paxWebJettyBundle, ClassLoader classLoader) {
		this.configuration = config;
		this.jettyFactory = jettyFactory;
		this.paxWebJettyBundle = paxWebJettyBundle;
		this.classLoader = classLoader;

		this.mainHandler = new PrioritizedHandlerCollection();
	}

	// --- lifecycle and configuration methods

	/**
	 * One-time configuration of Jetty
	 */
	public void configure() throws Exception {
		// for now, we have nothing. We can do many things using external jetty-*.xml files, but the creation
		// of Server itself should be done manually here.
		LOG.info("Creating Jetty server instance using configuration properties.");
		createServer();

		server.setHandler(this.mainHandler);

		// No external configuration should replace our "Server" object
		applyJettyConfiguration();

		// If external configuration added some connectors, we have to ensure they match declaration from
		// PID config: org.osgi.service.http.enabled and org.osgi.service.http.secure.enabled
		verifyConnectorConfiguration();

		// PAXWEB-1084 - start QTP before starting the server. When QTP is added as a bean to
		// org.eclipse.jetty.server.Server, it'll become UNMANAGED bean, so we can and have to manage its
		// lifecycle manually, which is exactly what we want.
		// see org.eclipse.jetty.util.component.ContainerLifeCycle for details
		qtp = (QueuedThreadPool) server.getThreadPool();
		LOG.info("Eagerly starting Jetty thread pool {}", qtp);
		qtp.start();

		if (server.getErrorHandler() == null) {
			server.setErrorHandler(new ErrorHandler());
		}

		// Configure NCSA RequestLogHandler if needed
		if (configuration.logging().isLogNCSAFormatEnabled() && server.getRequestLog() == null) {
			configureRequestLog();
		}

		// default session configuration is prepared, but not set in the server instance. It can be set
		// only after first context is created
		this.defaultSessionCookieConfig = configuration.session().getDefaultSessionCookieConfig();

		// global session persistence configuration
		if (configuration.session().getSessionStoreDirectory() != null) {
			FileSessionDataStoreFactory dsFactory = new FileSessionDataStoreFactory();
			dsFactory.setDeleteUnrestorableFiles(true);
			dsFactory.setStoreDir(configuration.session().getSessionStoreDirectory());
			server.addBean(dsFactory);
		}

		configureServerCustomizers();

		mbeanContainer = jettyFactory.enableJmxIfPossible(server);
	}

	/**
	 * <p>Create Jetty server using provided {@link Configuration}. The only <em>bean</em> inside Jetty server
	 * will be {@link QueuedThreadPool}, which can be reconfigured later (using XMLs).</p>
	 *
	 * @return
	 */
	private void createServer() {
		QueuedThreadPool qtp = jettyFactory.createThreadPool(configuration);

		// actual org.eclipse.jetty.server.Server
		this.server = new Server(qtp);
		this.server.setSessionIdManager(new DefaultSessionIdManager(this.server));
	}

	/**
	 * <p>This method parses existing {@code jetty*.xml} files and should <strong>not</strong> create an instance
	 * of {@link Server}. Existing {@link Server} is passed as server object ID.</p>
	 *
	 * <p>Besides the {@link Server}, XML configuration may alter any aspect of Jetty server. Additionally, if
	 * XML configurations create instances of {@link org.eclipse.jetty.server.HttpConfiguration}, these
	 * are collected and remembered for future use (if there's a need to create a {@link ServerConnector}).</p>
	 */
	private void applyJettyConfiguration() throws Exception {
		File[] locations = configuration.server().getConfigurationFiles();
		URL jettyResource = getClass().getResource("/jetty.xml");
		if (locations.length == 0) {
			if (jettyResource == null) {
				LOG.info("No external Jetty configuration files specified. Default/PID configuration will be used.");
			} else {
				LOG.info("Found \"jetty.xml\" resource on the classpath: {}.", jettyResource);
			}
		} else {
			LOG.info("Processing Jetty configuration from files: {}", Arrays.asList(locations));
			if (jettyResource != null) {
				LOG.info("Found additional \"jetty.xml\" resource on the classpath: {}.", jettyResource);
			}
		}

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try {
			// PAXWEB-1112 - classloader leak prevention:
			// first find XmlConfiguration class' CL to set it as TCCL when performing static
			// initialization of XmlConfiguration in order to not leak
			// org.eclipse.jetty.xml.XmlConfiguration.__factoryLoader.loader
			ClassLoader jettyXmlCl = null;
			if (paxWebJettyBundle != null) {
				for (Bundle b : paxWebJettyBundle.getBundleContext().getBundles()) {
					if ("org.eclipse.jetty.xml".equals(b.getSymbolicName())) {
						jettyXmlCl = b.adapt(BundleWiring.class).getClassLoader();
						break;
					}
				}
			}

			// PAXWEB-1112: TCCL to perform static initialization of XmlConfiguration with proper TCCL
			// needed for org.eclipse.jetty.xml.XmlConfiguration.__factoryLoader
			Thread.currentThread().setContextClassLoader(jettyXmlCl);
			URL emptyConfig = getClass().getResource("/jetty-empty.xml");
			if (emptyConfig != null) {
				new XmlConfiguration(Resource.newResource(emptyConfig));
			}

			// When parsing, TCCL will be set to CL of pax-web-jetty bundle
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

			XmlConfiguration previous = null;
			Map<String, Object> objects = new LinkedHashMap<>();

			// the only existing objects. Names as found in JETTY_HOME/etc/jetty*.xml files
			objects.put("Server", server);
			objects.put("threadPool", server.getThreadPool());

			List<XmlConfiguration> configs = new ArrayList<>();
			for (File location : locations) {
				configs.add(new XmlConfiguration(Resource.newResource(location)));
			}
			if (jettyResource != null) {
				configs.add(new XmlConfiguration(Resource.newResource(jettyResource)));
			}

			for (XmlConfiguration cfg : configs) {
				LOG.debug("Parsing {}", cfg);

				processConfiguration(cfg, previous, objects);

				previous = cfg;
			}

			if (locations.length > 0 || jettyResource != null) {
				// the "Server" object should not be redefined
				objects.values().forEach(bean -> {
					if (Server.class.isAssignableFrom(bean.getClass())) {
						if (bean != server) {
							String msg = "Can't create new instance of Jetty server in external configuration files.";
							throw new IllegalArgumentException(msg);
						}
					}
				});

				// summary about found connectors
				Connector[] connectors = server.getConnectors();
				if (connectors != null && connectors.length > 0) {
					for (Connector connector : connectors) {
						String host = ((ServerConnector) connector).getHost();
						if (host == null) {
							host = "0.0.0.0";
						}
						int port = ((ServerConnector) connector).getPort();
						LOG.info("Found configured connector \"{}\": {}:{}", connector.getName(), host, port);
					}
				} else {
					LOG.debug("No connectors configured in external Jetty configuration files.");
				}
			}
		} finally {
			Thread.currentThread().setContextClassLoader(loader);
		}
	}

	/**
	 * External configuration may specify connectors (as in JETTY_HOME/etc/jetty-http.xml)
	 * but we may have to add default ones if they're missing
	 */
	private void verifyConnectorConfiguration() {
		boolean httpEnabled = configuration.server().isHttpEnabled();
		Integer httpPort = configuration.server().getHttpPort();

		boolean httpsEnabled = configuration.server().isHttpSecureEnabled();
		Integer httpsPort = configuration.server().getHttpSecurePort();

		String[] addresses = configuration.server().getListeningAddresses();

		// review connectors possibly configured from jetty.xml and check if they match configadmin configuration
		for (String address : addresses) {
			// If configured (org.osgi.service.http.enabled), Jetty should have a connector
			// with org.eclipse.jetty.server.HttpConnectionFactory
			verifyConnector(address, HttpConnectionFactory.class, httpPort, httpEnabled, false,
					() -> jettyFactory.createDefaultConnector(server, httpConfigs, address, configuration));

			// If configured (org.osgi.service.http.secure.enabled), Jetty should have a connector
			// with org.eclipse.jetty.server.SslConnectionFactory
			verifyConnector(address, SslConnectionFactory.class, httpsPort, httpsEnabled, true,
					() -> jettyFactory.createSecureConnector(server, httpConfigs, address, configuration));
		}
	}

	/**
	 * Verify if current server configuration, possibly created from external {@code jetty.xml} matches the
	 * declaration from PID ({@link org.ops4j.pax.web.service.PaxWebConfig#PID_CFG_HTTP_ENABLED} and
	 * {@link org.ops4j.pax.web.service.PaxWebConfig#PID_CFG_HTTP_SECURE_ENABLED}).
	 *
	 * @param address
	 * @param cfClass {@link ConnectionFactory} class to determine the <em>nature</em> of the connector
	 * @param port
	 * @param enabled
	 * @param secure
	 * @param connectorProvider {@link Supplier} used if connector has to be added to match PID configuration
	 */
	private void verifyConnector(String address, Class<? extends ConnectionFactory> cfClass, Integer port,
			boolean enabled, boolean secure, Supplier<Connector> connectorProvider) {
		ServerConnector expectedConnector = null;

		boolean connectorFound = false;
		ServerConnector backupConnector = null;

		Connector[] currentConnectors = server.getConnectors();
		if (currentConnectors == null) {
			currentConnectors = new Connector[0];
		}

		for (Connector connector : currentConnectors) {
			if (connector.getConnectionFactory(cfClass) != null) {
				if (match(address, port, connector)) {
					if (expectedConnector == null) {
						expectedConnector = (ServerConnector) connector;
					}
					connectorFound = true;
				} else {
					if (backupConnector == null) {
						backupConnector = (ServerConnector) connector;
					}
				}
			}
		}
		if (expectedConnector == null && backupConnector != null) {
			expectedConnector = backupConnector;
		}
		if (connectorFound) {
			if (enabled) {
				LOG.info("Using configured {} as {} connector for address: {}:{}", expectedConnector,
						(secure ? "secure" : "non secure"), address, port);
			} else {
				for (Connector connector : currentConnectors) {
					if (connector.getConnectionFactory(cfClass) != null) {
						LOG.warn("Connector defined in external configuration will be removed, "
								+ "because it's not enabled: {}", connector);
						server.removeConnector(connector);
					}
				}
			}
		} else if (enabled) {
			LOG.info("Creating {} connector for address {}:{}", (secure ? "secure" : "non secure"), address, port);
			// we have to create a connector
			Connector connector = connectorProvider.get();
			server.addConnector(connector);
		}
	}

	/**
	 * Check if the passed {@link Connector} can be treated as one <em>matching</em> the connector
	 * declared using PID properties.
	 *
	 * @param address1
	 * @param port1
	 * @param connector
	 * @return
	 */
	private boolean match(String address1, Integer port1, Connector connector) {
		if (!(connector instanceof ServerConnector)) {
			// strange, but lets do it like it was done before
			LOG.warn(connector + " is not an instance of ServerConnector");
			return false;
		}

		ServerConnector sc = (ServerConnector) connector;

		String address2 = sc.getHost();
		int port2 = sc.getPort();

		InetSocketAddress isa1 = address1 != null ? new InetSocketAddress(address1, port1)
				: new InetSocketAddress(port1);
		InetSocketAddress isa2 = address2 != null ? new InetSocketAddress(address2, port2)
				: new InetSocketAddress(port2);

		return isa1.equals(isa2);
	}

	/**
	 * Configure request logging (AKA <em>NCSA logging</em>) for Jetty, using configuration properties.
	 */
	public void configureRequestLog() {
		LogConfiguration lc = configuration.logging();

		if (lc.getLogNCSADirectory() == null) {
			throw new IllegalArgumentException("Log directory for NCSA logging is not specified. Please set"
					+ " org.ops4j.pax.web.log.ncsa.directory property.");
		}
		File logDir = new File(lc.getLogNCSADirectory());
		if (logDir.isFile()) {
			throw new IllegalArgumentException(logDir + " is not a valid directory to store request logs");
		}

		RequestLogWriter writer = new RequestLogWriter();

		// org.eclipse.jetty.util.RolloverFileOutputStream._append
		writer.setAppend(lc.isLogNCSAAppend());
		// org.eclipse.jetty.util.RolloverFileOutputStream._filename, should contain "yyyy_mm_dd"
		if (lc.getLogNCSAFile() != null) {
			writer.setFilename(new File(lc.getLogNCSADirectory(), lc.getLogNCSAFile()).getAbsolutePath());
		} else {
			writer.setFilename(new File(lc.getLogNCSADirectory(), "yyyy_mm_dd.request.log").getAbsolutePath());
		}
		// org.eclipse.jetty.util.RolloverFileOutputStream._fileDateFormat, defaults to "yyyy_mm_dd"
		writer.setFilenameDateFormat(lc.getLogNCSAFilenameDateFormat());
		// org.eclipse.jetty.util.RolloverFileOutputStream._retainDays
		writer.setRetainDays(lc.getLogNCSARetainDays());
		// org.eclipse.jetty.server.RequestLogWriter._timeZone, defaults to "GMT"
		writer.setTimeZone(lc.getLogNCSATimeZone());

		CustomRequestLog requestLog = new CustomRequestLog(writer,
				lc.isLogNCSAExtended() ? CustomRequestLog.NCSA_FORMAT : CustomRequestLog.EXTENDED_NCSA_FORMAT);

		// original approach from PAXWEB-269 - http://wiki.eclipse.org/Jetty/Howto/Configure_Request_Logs:
//		server.getRootHandlerCollection().addHandler(requestLogHandler);
		// since https://bugs.eclipse.org/bugs/show_bug.cgi?id=446564 we can do better:
		server.setRequestLog(requestLog);

		LOG.info("NCSARequestlogging is using directory {}", lc.getLogNCSADirectory());
	}

	/**
	 * Simply start Jetty server
	 * @throws Exception
	 */
	public void start() throws Exception {
		LOG.info("Starting {}", server);

		server.start();
	}

	/**
	 * One-time operation. After stopping Jetty, we should not be able to start it again, so it has to be
	 * terminal operation with full clean up of resources.
	 *
	 * @throws Exception
	 */
	public void stop() throws Exception {
		LOG.info("Stopping {}", server);

		if (mbeanContainer != null) {
			LOG.info("Destroying Jetty JMX MBean container");
			// see https://github.com/eclipse/jetty.project/issues/851 for explanation
			server.removeBean(mbeanContainer);
			mbeanContainer.destroy();
			mbeanContainer = null;
		}

		server.stop();

		// PAXWEB-1127 - stop qtp after stopping server, as we've started it manually
		LOG.info("Stopping Jetty thread pool {}", qtp);
		qtp.stop();

		Handler[] childHandlers = server.getChildHandlers();
		for (Handler handler : childHandlers) {
			handler.stop();
		}

		LOG.info("Destroying Jetty server {}", server);
		server.destroy();

		dynamicRegistrations.clear();
		initializers.clear();
		osgiContextModels.clear();
		contextHandlers.forEach((path, sch) -> {
			mainHandler.removeHandler(sch);
		});
		contextHandlers.clear();
		mainHandler.mapContexts();

		// I found this necessary, when pax-web-jetty is restarted/refreshed without affecting
		// pax-web-extender-whiteboard
		osgiServletContexts.values().forEach(OsgiServletContext::unregister);
	}

	/**
	 * If state allows, this methods returns currently configured/started addresses of the listeners.
	 * @param useLocalPort
	 * @return
	 */
	public ServerEvent.Address[] getAddresses(boolean useLocalPort) {
		if (server == null || server.getConnectors() == null || server.getConnectors().length == 0) {
			return null;
		}
		final List<ServerEvent.Address> result = new ArrayList<>(server.getConnectors().length);
		for (Connector connector : server.getConnectors()) {
			if (connector instanceof ServerConnector) {
				ServerConnector sc = (ServerConnector)connector;
				int port = useLocalPort ? sc.getLocalPort() : sc.getPort();
				SslConnectionFactory cf = sc.getConnectionFactory(SslConnectionFactory.class);
				if (sc.getHost() == null) {
					result.add(new ServerEvent.Address(new InetSocketAddress(port), cf != null));
				} else {
					result.add(new ServerEvent.Address(new InetSocketAddress(sc.getHost(), port), cf != null));
				}
			}
		}

		return result.toArray(new ServerEvent.Address[0]);
	}

	// --- visitor methods for model changes

	@Override
	public void visitTransactionStateChange(TransactionStateChange change) {
		String contextPath = change.getContextPath();
		if (change.getKind() == OpCode.ASSOCIATE) {
			if (!transactions.add(contextPath)) {
				throw new IllegalStateException("Context path " + contextPath
						+ " is already associated with config transaction");
			}
			delayedRemovals.computeIfAbsent(contextPath, cp -> new ArrayList<>());
		} else if (change.getKind() == OpCode.DISASSOCIATE) {
			if (!transactions.remove(contextPath)) {
				throw new IllegalStateException("Context path " + contextPath
						+ " is not associated with any config transaction");
			} else {
				// check pending removals
				List<ElementModel<?, ?>> toRemove = delayedRemovals.get(contextPath);
				if (toRemove != null) {
					for (Iterator<ElementModel<?, ?>> iterator = toRemove.iterator(); iterator.hasNext(); ) {
						ElementModel<?, ?> model = iterator.next();
						if (model instanceof ServletModel) {
							removeServletModel(contextPath, ((ServletModel) model));
						} else if (model instanceof EventListenerModel) {
							removeEventListenerModel(contextHandlers.get(contextPath), ((EventListenerModel) model));
						}
						iterator.remove();
					}
					delayedRemovals.remove(contextPath);
				}
				if (contextHandlers.containsKey(contextPath)) {
					// end of transaction - start the context
					PaxWebServletContextHandler sch = contextHandlers.get(contextPath);
					if (sch.getServer().isStarted()) {
						ensureServletContextStarted(sch);
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This change deals with Pax Web specific implementaion of Jetty's {@link ServletContextHandler}. When adding
	 * a servlet context, new instance of context handler is created, immediately added to
	 * {@link ContextHandlerCollection} but <strong>not</strong> started yet - it'll be started after first active
	 * web element registration (like servlet) or at the end of WAB's {@link org.ops4j.pax.web.service.spi.task.Batch}.</p>
	 *
	 * @param change
	 */
	@Override
	public void visitServletContextModelChange(ServletContextModelChange change) {
		ServletContextModel model = change.getServletContextModel();
		String contextPath = model.getContextPath();

		if (change.getKind() == OpCode.ADD) {
			LOG.info("Creating new Jetty context for {}", model);

			PaxWebServletContextHandler sch = new PaxWebServletContextHandler(null, contextPath, configuration);
			// special, OSGi-aware org.eclipse.jetty.servlet.ServletHandler
			sch.setServletHandler(new PaxWebServletHandler(default404Servlet, new OsgiSessionAttributeListener(sessionListenerModels)));
			// setting "false" here will trigger 302 redirect when browsing to context without trailing "/"
			sch.setAllowNullPathInfo(false);
			// welcome files will be handled at default/resource servlet level and OsgiServletContext
			sch.setWelcomeFiles(new String[0]);

			// error page handler will be configured later (optionally)
			ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
			errorHandler.setShowStacks(configuration.server().isShowStacks());
			sch.setErrorHandler(errorHandler);

			// for future (optional) resource servlets, let's define some common context init properties
			// which are read in org.eclipse.jetty.servlet.DefaultServlet.init()
			sch.setInitParameter(DefaultServlet.CONTEXT_INIT + "dirAllowed", "false");
			sch.setInitParameter(DefaultServlet.CONTEXT_INIT + "etags", "true");
			// needed to comply with Servlets specification
			sch.setInitParameter(DefaultServlet.CONTEXT_INIT + "welcomeServlets", "true");

			// cache properties for default servlet (see org.eclipse.jetty.server.CachedContentFactory) passed
			// through context init params
			Integer totalCacheSize = configuration.resources().maxTotalCacheSize(); // kB
			Integer maxEntrySize = configuration.resources().maxCacheEntrySize(); // kB
			Integer maxEntries = configuration.resources().maxCacheEntries();
			// the defaults in Jetty are quite high (256MB total, 128MB max entry size), but we can have more
			// resource servlets, so we'll divide the defaults by 64
			sch.setInitParameter(DefaultServlet.CONTEXT_INIT + "maxCacheSize",
					totalCacheSize != null ? Integer.toString(totalCacheSize * 1024) : Integer.toString(256 * 1024 * 1024 / 64));
			sch.setInitParameter(DefaultServlet.CONTEXT_INIT + "maxCachedFileSize",
					maxEntrySize != null ? Integer.toString(maxEntrySize * 1024) : Integer.toString(128 * 1024 * 1024 / 64));
			sch.setInitParameter(DefaultServlet.CONTEXT_INIT + "maxCachedFiles",
					maxEntries != null ? Integer.toString(maxEntries) : "2048");

			mainHandler.addHandler(sch);
			mainHandler.mapContexts();

			// session configuration - based on defaultSessionConfiguration, but may be later overriden in OsgiContext
			SessionHandler sessions = sch.getSessionHandler();
			if (sessions != null) {
				SessionConfiguration sc = configuration.session();
				sessions.setMaxInactiveInterval(sc.getSessionTimeout());
				sessions.setSessionCookie(defaultSessionCookieConfig.getName());
				sessions.getSessionCookieConfig().setDomain(defaultSessionCookieConfig.getDomain());
				// will default to context path if null
				sessions.getSessionCookieConfig().setPath(defaultSessionCookieConfig.getPath());
				sessions.getSessionCookieConfig().setMaxAge(defaultSessionCookieConfig.getMaxAge());
				sessions.getSessionCookieConfig().setHttpOnly(defaultSessionCookieConfig.isHttpOnly());
				sessions.getSessionCookieConfig().setSecure(defaultSessionCookieConfig.isSecure());
				sessions.getSessionCookieConfig().setComment(defaultSessionCookieConfig.getComment());
				if (sc.getSessionUrlPathParameter() != null) {
					sessions.setSessionIdPathParameterName(sc.getSessionUrlPathParameter());
				}
				if (sc.getSessionWorkerName() != null) {
					SessionIdManager sidManager = server.getSessionIdManager();
					if (sidManager instanceof DefaultSessionIdManager) {
						((DefaultSessionIdManager) sidManager).setWorkerName(sc.getSessionWorkerName());
					}
				}
			}

			// security configuration will be configured when context is started and will be based (if needed)
			// on highest ranked OsgiContextModel

			// explicit no check for existing mapping under given physical context path
			contextHandlers.put(contextPath, sch);
			osgiContextModels.put(contextPath, new TreeSet<>());

			// configure ordered map of initializers - Jetty doesn't let us configure it in a "context"...
			initializers.put(contextPath, new LinkedHashMap<>());
			dynamicRegistrations.put(contextPath, new DynamicRegistrations());

			// do NOT start the context here - only after registering first "active" web element
		} else if (change.getKind() == OpCode.DELETE) {
			dynamicRegistrations.remove(contextPath);
			initializers.remove(contextPath);
			osgiContextModels.remove(contextPath);
			PaxWebServletContextHandler sch = contextHandlers.remove(contextPath);

			// Note: for WAB deployments, this is the last operation of the undeployment batch and all web element
			// removals are delayed until this step.
			// This is important to ensure proper order of destruction ended with contextDestroyed() calls
			// No need to clean anything, as the PaxWebServletContextHandler is not reused

			if (sch != null) {
				if (sch.isStarted()) {
					LOG.info("Stopping Jetty context \"{}\"", contextPath);
					try {
						sch.stop();
					} catch (Exception e) {
						LOG.warn("Error stopping Jetty context \"{}\": {}", contextPath, e.getMessage(), e);
					}
				}
				mainHandler.removeHandler(sch);
				mainHandler.mapContexts();
			}
		}
	}

	@Override
	public void visitOsgiContextModelChange(OsgiContextModelChange change) {
		if (change.getKind() == OpCode.ASSOCIATE || change.getKind() == OpCode.DISASSOCIATE) {
			return;
		}

		OsgiContextModel osgiModel = change.getOsgiContextModel();
		ServletContextModel servletContextModel = change.getServletContextModel();

		String contextPath = osgiModel.getContextPath();
		ServletContextHandler sch = contextHandlers.get(contextPath);

		if (sch == null) {
			// by optimizing the process and assuming that removal of last OsgiContextModel for a context
			// means full removal of the context, let's recreated the ServletContext now
//				throw new IllegalStateException(ocm + " refers to unknown ServletContext for path " + contextPath);
			visitServletContextModelChange(new ServletContextModelChange(OpCode.ADD, new ServletContextModel(contextPath)));
			sch = contextHandlers.get(contextPath);
		}

		if (change.getKind() == OpCode.ADD) {
			LOG.info("Adding {} to {}", osgiModel, sch);

			// new OsgiContextModelChange was created 1:1 with some HttpContext/ServletContextHelper.
			// because Whiteboard ServletContext is scoped to single ServletContextHelper and has unique
			// set of attributes, that's the good place to remember such association.
			// Later, when servlets will be registered with such OsgiContextModel, they need to get
			// special facade for ServletContext
			if (osgiServletContexts.containsKey(osgiModel)) {
				throw new IllegalStateException(osgiModel + " is already registered");
			}

			// this (and similar Tomcat and Undertow places) should be the only place where
			// org.ops4j.pax.web.service.spi.servlet.OsgiServletContext is created and we have everything ready
			// to create proper classloader for this OsgiServletContext
			ClassLoader classLoader = null;
			if (osgiModel.getClassLoader() != null) {
				// WAB scenario - the classloader was already prepared earlier when the WAB was processed..
				// The classloader already includes several reachable bundles
				classLoader = osgiModel.getClassLoader();
			}
			if (paxWebJettyBundle != null) {
				// it may not be the case in Test scenario
				OsgiServletContextClassLoader loader = classLoader != null
						? (OsgiServletContextClassLoader) classLoader : new OsgiServletContextClassLoader();
				loader.addBundle(osgiModel.getOwnerBundle());
				loader.addBundle(paxWebJettyBundle);
				loader.addBundle(Utils.getPaxWebJspBundle(paxWebJettyBundle));
				loader.addBundles(Utils.getJettyWebSocketBundle(paxWebJettyBundle));
				loader.makeImmutable();
				classLoader = loader;
			} else if (classLoader == null) {
				classLoader = this.classLoader;
			}
			OsgiServletContext osgiContext = new OsgiServletContext(sch.getServletContext(), osgiModel, servletContextModel,
					defaultSessionCookieConfig, classLoader);

			// that's ideal place to set ServletContext.TEMPDIR attribute - it'll work for HttpService, Whiteboard and WAB
			File tmpLocation = new File(configuration.server().getTemporaryDirectory(), osgiModel.getTemporaryLocation());
			if (!tmpLocation.exists() && !tmpLocation.mkdirs()) {
				LOG.warn("Can't create temporary directory for {}: {}", osgiModel, tmpLocation.getAbsolutePath());
			}
			osgiModel.getInitialContextAttributes().put(ServletContext.TEMPDIR, tmpLocation);
			osgiContext.setAttribute(ServletContext.TEMPDIR, tmpLocation);

			osgiServletContexts.put(osgiModel, osgiContext);

			// a physical context just got a new OSGi context
			osgiContextModels.get(contextPath).add(osgiModel);
		}

		if (change.getKind() == OpCode.DELETE) {
			LOG.info("Removing {} from {}", osgiModel, sch);

			OsgiServletContext removedOsgiServletContext = osgiServletContexts.remove(osgiModel);
			osgiContextModels.get(contextPath).remove(osgiModel);

			removedOsgiServletContext.unregister();
			removedOsgiServletContext.releaseWebContainerContext();

			OsgiServletContext currentHighestRankedContext = ((PaxWebServletHandler) sch.getServletHandler()).getDefaultServletContext();
			if (currentHighestRankedContext == removedOsgiServletContext) {
				// we have to stop the context - it'll be started later
				LOG.info("Stopping Jetty context \"{}\"", contextPath);
				try {
					sch.stop();
					// clear the OSGi context + model - it'll be calculated to next higher ranked ones few lines later
					((PaxWebServletHandler) sch.getServletHandler()).setDefaultOsgiContextModel(null, null);
					((PaxWebServletHandler) sch.getServletHandler()).setDefaultServletContext(null);
				} catch (Exception e) {
					LOG.warn("Error stopping Jetty context \"{}\": {}", contextPath, e.getMessage(), e);
				}
			}
		}

		// there may be a change in what's the "best" (highest ranked) OsgiContextModel for given
		// ServletContextModel. This will became the "fallback" OsgiContextModel for chains without
		// target servlet (with filters only)
		OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
		if (highestRankedModel != null) {
			if (highestRankedModel != ((PaxWebServletHandler) sch.getServletHandler()).getDefaultOsgiContextModel()) {
				LOG.info("Changing default OSGi context model for " + sch);
				OsgiServletContext highestRankedContext = osgiServletContexts.get(highestRankedModel);
				((PaxWebServletHandler) sch.getServletHandler()).setDefaultOsgiContextModel(highestRankedModel, highestRankedContext.getResolvedWebContainerContext());
				((PaxWebServletHandler) sch.getServletHandler()).setDefaultServletContext(highestRankedContext);

				// we have to ensure that non-highest ranked contexts are unregistered
				osgiServletContexts.forEach((ocm, osc) -> {
					if (ocm.getContextPath().equals(contextPath) && osc != highestRankedContext) {
						osc.unregister();
					}
				});

				// and the highest ranked context should be registered as OSGi service (if it wasn't registered)
				highestRankedContext.register();
			}
		} else {
			((PaxWebServletHandler) sch.getServletHandler()).setDefaultOsgiContextModel(null, null);
			((PaxWebServletHandler) sch.getServletHandler()).setDefaultServletContext(null);

			// removing LAST OsgiContextModel for given servlet context (by context path) is almost like if the
			// servlet context was entirely removed. Let's assume that the bundle that lead to creation of given
			// context stopped and it's HttpService instance was removed, which means that given OsgiContextModel
			// was removed
			visitServletContextModelChange(new ServletContextModelChange(OpCode.DELETE, new ServletContextModel(contextPath)));
		}
	}

	@Override
	public void visitContextMetadataModelChange(ContextMetadataModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			OsgiContextModel ocm = change.getOsgiContextModel();
			ContextMetadataModel meta = change.getMetadata();

			String contextPath = ocm.getContextPath();
			ServletContextHandler sch = contextHandlers.get(contextPath);

			if (sch == null) {
				throw new IllegalStateException(ocm + " refers to unknown ServletContext for path " + contextPath);
			}

			OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
			if (ocm == highestRankedModel) {
				LOG.info("Configuring metadata of {}", ocm);

				// only in this case we'll configure the metadata
				sch.getServletContext().setEffectiveMajorVersion(meta.getMajorVersion());
				sch.getServletContext().setEffectiveMinorVersion(meta.getMinorVersion());
				sch.setDisplayName(meta.getDisplayName());
				// org.eclipse.jetty.webapp.WebDescriptor._distributable - doesn't do anything useful (?)
//				meta.getDistributable();
				// nowhere to set these too
//				meta.isMetadataComplete();
//				meta.getPublicId();
//				meta.getRequestCharacterEncoding();
//				meta.getResponseCharacterEncoding();

				if (sch.getSecurityHandler() instanceof ConstraintAware) {
					((ConstraintAware) sch.getSecurityHandler()).setDenyUncoveredHttpMethods(meta.isDenyUncoveredHttpMethods());
				}
			}
		}
	}

	@Override
	public void visitMimeAndLocaleMappingChange(MimeAndLocaleMappingChange change) {
		if (change.getKind() == OpCode.ADD) {
			OsgiContextModel ocm = change.getOsgiContextModel();

			String contextPath = ocm.getContextPath();
			ServletContextHandler sch = contextHandlers.get(contextPath);

			if (sch == null) {
				throw new IllegalStateException(ocm + " refers to unknown ServletContext for path " + contextPath);
			}

			OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
			if (ocm == highestRankedModel) {
				LOG.info("Configuring MIME and Locale Encoding mapping of {}", ocm);

				MimeTypes mimeTypes = new MimeTypes();
				mimeTypes.setMimeMap(change.getMimeMapping());
				sch.setMimeTypes(mimeTypes);

				change.getLocaleEncodingMapping().forEach(sch::addLocaleEncoding);
			}
		}
	}

	@Override
	public void visitServletModelChange(ServletModelChange change) {
		Set<String> done = new HashSet<>();

		if ((change.getKind() == OpCode.ADD && !change.isDisabled()) || change.getKind() == OpCode.ENABLE) {
			ServletModel model = change.getServletModel();
			if (change.getNewModelsInfo() == null) {
				LOG.info("Adding servlet {}", model);
			} else {
				LOG.info("Adding servlet {} to new contexts {}", model, change.getNewModelsInfo());
			}

			// the same servlet should be added to all relevant contexts
			// for each unique ServletContextModel, a servlet should be associated with the "best"
			// OsgiContextModel. Usually there should be one, but here's example of a conflict:
			// ServletContextModel with "/c1"
			//     OsgiContextModel with name "ocm1" and rank 1
			//     OsgiContextModel with name "ocm2" and rank 2
			//     OsgiContextModel with name "ocm3" and rank 3
			// ServletContextModel with "/c2"
			//     OsgiContextModel with name "ocm4" and rank 1 and service.id 1
			//     OsgiContextModel with name "ocm5" and rank 1 and service.id 2
			// ServletContextModel with "/c3"
			//     OsgiContextModel with name "ocm6"
			//
			// now ServletModel with /s1 mapping is associated with ocm1, ocm2, ocm4 and ocm5 osgi contexts. It should
			// be available under:
			//  - /c1/s1 and associated with ocm2 (due to rank of ocm2)
			//  - /c2/s1 and associated with ocm4 (due to service.id of ocm4)
			//
			//    140.2 The Servlet Context
			//    [...] In the case of two Servlet Context Helpers with the same path, the service with the highest
			//    ranking is searched first for a match. In the case of a tie, the lowest service ID is searched first.
			//
			// The above is only about locating "matching servlet or resource" when processing requests, not
			// about initial registration...

			// proper order ensures that (assuming above scenario), for /c1, ocm2 will be chosen and ocm1 skipped
			change.getContextModels().forEach(osgiContextModel -> {
				String contextPath = osgiContextModel.getContextPath();
				if (!done.add(contextPath)) {
					// servlet was already added to given ServletContextHandler
					// in association with the highest ranked OsgiContextModel (and its supporting
					// ServletContextHelper or HttpContext)
					//
					// there may be other servlets registered only to this OsgiContextModel (which, in this case,
					// turned out to be "lower ranked" within given ServletContextModel), so at any moment we
					// may have many "active" OsgiContextModels associated with single physical context path
					// we remember them in this.osgiContextModels
					return;
				}

				LOG.debug("Adding servlet {} to {}", model.getName(), contextPath);

				// there may be many instances of ServletHolder using the same instance of servlet (added to
				// different org.eclipse.jetty.servlet.ServletContextHandler._servletHandler in different context
				// paths, so we have the opportunity to provide specification-defined behavior at _this_ level
				//
				// 140.3 Common Whiteboard Properties says:
				//     ...
				//     If multiple Servlet Context Helper services match the osgi.http.whiteboard.context.select
				//     property the servlet, filter, resource or listener will be registered with all these Servlet
				//     Context Helpers. To avoid multiple init and destroy calls on the same instance, servlets and
				//     filters should be registered as Prototype Service Factory.
				//
				// we just have to create an instance of ServletHolder with proper OsgiContextModel, which in
				// turn may specify related HttpContext/ServletContextHelper directly or as ServiceReference, so
				// actual instance can be obtained as bundle-scoped service if the ServletContextHelper service
				// was registered as service factory
				//
				// same for the servlet itself. Specification recommends registration of Whiteboard servlets as
				// prototype service factories to avoid duplicate init() methods.

				// there should already be a ServletContextHandler
				PaxWebServletContextHandler sch = contextHandlers.get(contextPath);

				// <servlet> - always associated with one of ServletModel's OsgiContextModels
				OsgiServletContext context = osgiServletContexts.get(osgiContextModel);
				PaxWebServletHolder holder = new PaxWebServletHolder(model, osgiContextModel, context);

				// <servlet-mapping>
				ServletMapping mapping = new ServletMapping();
				mapping.setServletName(model.getName());
				mapping.setPathSpecs(model.getUrlPatterns());
				mapping.setDefault(model.isOverridable());

				boolean isDefaultResourceServlet = model.isResourceServlet();
				for (String pattern : model.getUrlPatterns()) {
					isDefaultResourceServlet &= "/".equals(pattern);
				}
				if (model.isResourceServlet()) {
					holder.setInitParameter("pathInfoOnly", Boolean.toString(!isDefaultResourceServlet));
				}

				ServletHandler sh = sch.getServletHandler();
				if (isDefaultResourceServlet && "default".equals(model.getName())) {
					// we have to remove the "default"/"/" servlet
					PaxWebServletHolder defaultHolder = (PaxWebServletHolder) sh.getServlet(model.getName());
					ServletMapping defaultMapping = sh.getServletMapping("/");
					if (defaultHolder != null) {
						sh.setServlets(ArrayUtil.removeFromArray(sh.getServlets(), defaultHolder));
						sh.setServletMappings(ArrayUtil.removeFromArray(sh.getServletMappings(), defaultMapping));
					}
				}

				// role mapping per-servlet
				model.getRoleLinks().forEach(holder::setUserRoleLink);

				holder.setRunAsRole(model.getRunAs());

				((PaxWebServletHandler) sh).addServletWithMapping(holder, mapping);

				// are there any error page declarations in the model?
				ErrorPageModel epm = model.getErrorPageModel();
				if (epm != null && epm.isValid()) {
					// location will be the first URL mapping (even if there may be more)
					// in pure OSGi CMPN Whiteboard case, initially there could be no mapping at all, but in such
					// case a default would be generated for us to use
					String location = epm.getLocation();

					ErrorPageErrorHandler eph = (ErrorPageErrorHandler) sch.getErrorHandler();
					// If there are many servlets (mapped to non conflicting URIs), they still may define
					// conflicting error pages, and these conflicts are NOT resolved at ServletModel
					// resolution time. For now, we simply override existing error pages
					configureErrorPages(location, eph, epm);
				}

				if (!change.isDynamic()) {
					ensureServletContextStarted(sch);
				} else if (model.isServletSecurityPresent() && sch.getSecurityHandler() != null) {
					// let's check the dynamic servlet security constraints - not necessarily from the highest
					// ranked OsgiContextModel, but from OsgiContextModel of the servlet
					List<SecurityConstraintModel> dynamicModels = new ArrayList<>();
					model.getContextModels().forEach(ocm -> {
						for (SecurityConstraintModel sc : ocm.getSecurityConfiguration().getSecurityConstraints()) {
							if (sc.getServletModel() == model) {
								dynamicModels.add(sc);
							}
						}
					});
					ensureSecurityConstraintsConfigured((ConstraintSecurityHandler) sch.getSecurityHandler(), dynamicModels);
				}
			});
			return;
		}

		if (change.getKind() == OpCode.DISABLE || change.getKind() == OpCode.DELETE) {
			for (Map.Entry<ServletModel, Boolean> entry : change.getServletModels().entrySet()) {
				ServletModel model = entry.getKey();
				if (!entry.getValue()) {
					continue;
				}

				// proper order ensures that (assuming above scenario), for /c1, ocm2 will be chosen and ocm1 skipped
				model.getContextModels().forEach(osgiContextModel -> {
					String contextPath = osgiContextModel.getContextPath();
					if (!done.add(contextPath)) {
						return;
					}

					if (pendingTransaction(contextPath)) {
						LOG.debug("Delaying removal of servlet {}", model);
						delayedRemovals.get(contextPath).add(model);
						return;
					}

					removeServletModel(contextPath, model);
				});
			}
		}
	}

	private void removeServletModel(String contextPath, ServletModel model) {
		ServletContextHandler sch = contextHandlers.get(contextPath);

		if (sch == null) {
			return;
		}

		((PaxWebServletHandler) sch.getServletHandler()).removeServletWithMapping(model);

		LOG.info("Removing servlet {}", model);
		LOG.debug("Removing servlet {} from context {}", model.getName(), contextPath);

		// are there any error page declarations in the model?
		ErrorPageModel epm = model.getErrorPageModel();
		if (epm != null) {
			// location will be the first URL mapping (even if there may be more)
			// in pure OSGi CMPN Whiteboard case, initially there could be no mapping at all, but in such
			// case a default would be generated for us to use
			String location = epm.getLocation();

			ErrorPageErrorHandler eph = (ErrorPageErrorHandler) sch.getErrorHandler();
			// If there are many servlets (mapped to non conflicting URIs), they still may define
			// conflicting error pages, and these conflicts are NOT resolved at ServletModel
			// resolution time. For now, we simply remove existing error pages
			Map<String, String> existing = eph.getErrorPages();
			for (String ex : epm.getExceptionClassNames()) {
				existing.entrySet().removeIf(e -> e.getKey().equals(ex) && e.getValue().equals(location));
			}
			for (int code : epm.getErrorCodes()) {
				existing.entrySet().removeIf(e -> e.getKey().equals(Integer.toString(code)) && e.getValue().equals(location));
			}
			if (epm.isXx4() || epm.isXx5()) {
				// hmm, can't change existing ErrorPageErrorHandler
				eph = new ErrorPageErrorHandler();
				sch.setErrorHandler(eph);
				if (epm.isXx4()) {
					existing.entrySet().removeIf(e -> e.getKey().startsWith("4") && e.getKey().length() == 3
							&& e.getValue().equals(location));
				}
				if (epm.isXx5()) {
					existing.entrySet().removeIf(e -> e.getKey().startsWith("5") && e.getKey().length() == 3
							&& e.getValue().equals(location));
				}
			}
			// leave remaining (not removed) mappings
			for (Map.Entry<String, String> e : existing.entrySet()) {
				eph.addErrorPage(e.getKey(), e.getValue());
			}
		}
	}

	@Override
	public void visitFilterModelChange(FilterModelChange change) {
		// only handle dynamic filter registration here - filter added only as last filter
		FilterModel model = change.getFilterModel();
		Set<String> done = new HashSet<>();
		if (change.getKind() == OpCode.ADD && model.isDynamic()) {
			for (OsgiContextModel ocm : change.getContextModels()) {
				String contextPath = ocm.getContextPath();
				if (!done.add(contextPath)) {
					continue;
				}

				LOG.info("Adding dynamic filter {} to context {}", model, contextPath);

				OsgiContextModel highestRankedModel = null;
				for (OsgiContextModel cm : model.getContextModels()) {
					if (cm.getContextPath().equals(contextPath)) {
						highestRankedModel = cm;
						break;
					}
				}
				if (highestRankedModel == null) {
					highestRankedModel = ocm;
				}

				PaxWebServletContextHandler sch = contextHandlers.get(contextPath);
				OsgiServletContext context = osgiServletContexts.get(highestRankedModel);

				ServletHandler servletHandler = sch.getServletHandler();
				List<PaxWebFilterMapping> mapping = configureFilterMappings(model);
				PaxWebFilterHolder holder = new PaxWebFilterHolder(model, context);
				holder.setMapping(mapping);
				for (PaxWebFilterMapping m : mapping) {
					servletHandler.addFilter(holder);
					if (m.isAfter()) {
						servletHandler.addFilterMapping(m);
					} else {
						servletHandler.prependFilterMapping(m);
					}
				}
			}
		}
	}

	@Override
	public void visitFilterStateChange(FilterStateChange change) {
		// there's no separate add filter, add filter, remove filter, ... set of operations
		// everything is passed in single "change"

		if (change.isDynamic()) {
			// dynamic filter may be added when:
			// 1) normal filter is added
			// 2) context is started
			// 3) SCIs are called that register a filter
			// so we can't rely on full "state change" - this is handled by JettyServerWrapper.visitFilterModelChange
			return;
		}

		Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> contextFilters = change.getContextFilters();

		for (Map.Entry<String, TreeMap<FilterModel, List<OsgiContextModel>>> entry : contextFilters.entrySet()) {
			String contextPath = entry.getKey();
			Map<FilterModel, List<OsgiContextModel>> filtersMap = entry.getValue();
			Set<FilterModel> filters = new TreeSet<>(filtersMap.keySet());

			PaxWebServletContextHandler sch = contextHandlers.get(contextPath);
			if (sch == null) {
				// it can happen when unregistering last filters (or rather setting the state with empty set of filters)
				continue;
			}

			LOG.info("Changing filter configuration for context {}", contextPath);

			OsgiContextModel defaultHighestRankedModel = osgiContextModels.get(contextPath).iterator().next();

			// potentially, all existing preprocessors will be removed
			Set<PreprocessorFilterConfig> toDestroy = new HashSet<>(((PaxWebServletHandler) sch.getServletHandler()).getPreprocessors());
			// some new preprocessors may be added - we have to init() them ourselves, because they're not held
			// in PaxWebFilterHolders
			List<PreprocessorFilterConfig> toInit = new LinkedList<>();

			// clear to keep the order of all available preprocessors
			((PaxWebServletHandler) sch.getServletHandler()).getPreprocessors().clear();

			for (Iterator<FilterModel> iterator = filters.iterator(); iterator.hasNext(); ) {
				FilterModel model = iterator.next();
				if (model.isPreprocessor()) {
					PreprocessorFilterConfig filterConfig = new PreprocessorFilterConfig(model, osgiServletContexts.get(defaultHighestRankedModel));
					if (toDestroy.stream().noneMatch(pfc -> pfc.getModel().equals(model))) {
						// new preprocessor - we have to init() it
						toInit.add(filterConfig);
					} else {
						// it was already there - we don't have to destroy() it
						toDestroy.removeIf(pfc -> {
							boolean match = pfc.getModel().equals(model);
							if (match) {
								// there's existing PreprocessorFilterConfig, so copy the instance and
								// potentially the ServiceObjects
								filterConfig.copyFrom(pfc);
							}
							return match;
						});
					}
					((PaxWebServletHandler) sch.getServletHandler()).getPreprocessors().add(filterConfig);
					iterator.remove();
				}
			}

			if (sch.isStarted()) {
				for (PreprocessorFilterConfig fc : toInit) {
					try {
						fc.getInstance().init(fc);
					} catch (ServletException ex) {
						LOG.warn("Problem during preprocessor initialization: {}", ex.getMessage(), ex);
					}
				}
				for (PreprocessorFilterConfig fc : toDestroy) {
					fc.destroy();
				}
			}

			// all the filters should be added to org.ops4j.pax.web.service.jetty.internal.PaxWebServletHandler
			// of ServletContextHandler - regardles of the "OSGi context" with which the filter was registered.
			//
			//    140.5 Registering Servlet Filters
			//    [...] Servlet filters are only applied to servlet requests if they are bound to the same Servlet
			//    Context Helper and the same Http Whiteboard implementation.
			//
			// this means that we don't know upfront which filters are actually needed during request processing.
			// Having some servlet registered with ServletContextHelper sch1 and two filters mapped to '/*' URL but
			// registered with sch1 and sch2, only one filter should be invoked even if both sch1 and sch2 may lead
			// to single ServletContext (and single unique context path)
			// however, when there's no servlet in a chain, both filters should be invoked - at least I think so
			//
			// neither specification, nor felix.http implementation handle single filter scenario - such
			// filter is not called. That's quite logical - ServletContext associated with a filter (through
			// FilterConfig or request.getServletContext()) should be the same as the one associated with target
			// servlet. If there's no servlet, we can still have filters associated with different
			// ServletContextHelpers. If those ServletContextHelpers are associated with single physical
			// context path, we can't tell which actual ServletContext given filter should use.
			//
			// For Pax Web purposes, we'll try to handle such scenario and all the filters in a chain without servlet
			// will use OsgiServletContext which is "best" (wrt service ranking) for given physical context path

			// holders won't include preprocessors
			PaxWebFilterHolder[] newFilterHolders = new PaxWebFilterHolder[filters.size()];

			// filters are sorted by ranking. for Jetty, this order should be reflected in the array of FilterMappings
			// order of FilterHolders is irrelevant
			int pos = 0;
			boolean noQuick = false;
			for (FilterModel model : filters) {
				// <filter> - FilterModel's OsgiContextModels only determine with which servlets such filter may
				// be associated.
				//  - when filter is running in a chain ending with some servlet, it'll get this servlet's OsgiContextModel
				//    and associated HttpContext/ServletContextHelper
				//  - when filter is running in a chain without target servlet, it'll get the "best" OsgiContextModel
				//    for given physical context path taken from this.osgiContextModels - this case (filters without
				//    servlet doesn't seem to be described by Whiteboard Service spec and isn't implemented by felix.http)

				// Filter and servlets conflict related to different target OsgiContextModels. Imagine:
				// ServletContextModel with "/c1"
				//     OsgiContextModel with name "ocm1" and rank 1
				//     OsgiContextModel with name "ocm2" and rank 2
				//     OsgiContextModel with name "ocm3" and rank 3
				//
				// if there's servlet registered under /s with ocm1 and ocm2 and a filter mapped to this servlet
				// (by name or /* path) with ocm1 only, this filter:
				//  - should get an OsgiServletContext associated with ocm1 during filter.init()
				//  - should not be invoked when processing a request targeted at servlet /s, because /s is chosen
				//    to be associated with higher ranked ocm2
				//
				// however, if there was some /s1 servlet associated with ocm1 only, filter should be invoked
				// when targeting /s1 servlet

				// if there's out-of-band list of new filters, there's no way the change will be "quick"
				noQuick |= filtersMap.get(model) != null;

				// we need highest ranked OsgiContextModel for current context path - chosen not among all
				// associated OsgiContextModels, but among OsgiContextModels of the FilterModel
				OsgiContextModel highestRankedModel = null;
				// remember, this contextModels list is properly sorted - and it comes either from model or
				// (if configured) from associated list of models which are being changed in the model
				List<OsgiContextModel> contextModels = filtersMap.get(model) != null
						? filtersMap.get(model) : model.getContextModels();
				for (OsgiContextModel ocm : contextModels) {
					if (ocm.getContextPath().equals(contextPath)) {
						highestRankedModel = ocm;
						break;
					}
				}
				if (highestRankedModel == null) {
					LOG.warn("(dev) Can't find proper OsgiContextModel for the filter. Falling back to "
							+ "highest ranked OsgiContextModel for given ServletContextModel");
					highestRankedModel = defaultHighestRankedModel;
				}

				OsgiServletContext context = osgiServletContexts.get(highestRankedModel);

				PaxWebFilterHolder holder = new PaxWebFilterHolder(model, context);
				holder.setMapping(configureFilterMappings(model));

				newFilterHolders[pos] = holder;
				pos++;
			}

			// we have to preserve dynamically added filters, when a filter was added after the context has started,
			// because the SCIs and ServletContextListeners may have added new filters
			List<PaxWebFilterMapping> newFilterMappingsListBefore = new LinkedList<>();
			List<PaxWebFilterMapping> newFilterMappingsListAfter = new LinkedList<>();

			if (!change.isDynamic() && sch.getServletHandler().getFilters().length > 0) {
				List<PaxWebFilterHolder> newFiltersList = new ArrayList<>();
				for (FilterHolder fh : sch.getServletHandler().getFilters()) {
					if (fh instanceof PaxWebFilterHolder) {
						PaxWebFilterHolder pwfh = (PaxWebFilterHolder) fh;
						if (pwfh.getFilterModel() == null || pwfh.getFilterModel().isDynamic()) {
							// filter added normally
							newFiltersList.add(pwfh);
							// mappings added depending on the flag
							if (pwfh.getMapping() != null) {
								for (PaxWebFilterMapping pwfm : pwfh.getMapping()) {
									if (pwfm.isAfter()) {
										newFilterMappingsListAfter.add(pwfm);
									} else {
										newFilterMappingsListBefore.add(pwfm);
									}
								}
							}
						}
					}
				}
				// don't bother about the order - mapping is relevant
				Collections.addAll(newFiltersList, newFilterHolders);
				newFilterHolders = newFiltersList.toArray(new PaxWebFilterHolder[0]);
			}

			if (noQuick || !quickFilterChange(sch.getServletHandler(), newFilterHolders)) {
				// the hard way - recreate entire array of filters/filter-mappings
				for (FilterHolder holder : sch.getServletHandler().getFilters()) {
					try {
						holder.stop();
					} catch (Exception e) {
						LOG.error(e.getMessage(), e);
					}
				}

				sch.getServletHandler().setFilters(new FilterHolder[0]);
				sch.getServletHandler().setFilterMappings(new FilterMapping[0]);

				for (PaxWebFilterHolder fh : newFilterHolders) {
					if ("Jetty_WebSocketUpgradeFilter".equals(fh.getName())) {
						sch.getServletContext().removeAttribute("org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter");
					}
					sch.getServletHandler().addFilter(fh);
					if (fh.getMapping() != null) {
						for (PaxWebFilterMapping fm : fh.getMapping()) {
							if (fm.isAfter()) {
								newFilterMappingsListAfter.add(fm);
							} else {
								newFilterMappingsListBefore.add(fm);
							}
						}
					}
				}
				newFilterMappingsListBefore.addAll(newFilterMappingsListAfter);
				sch.getServletHandler().setFilters(newFilterHolders);
				sch.getServletHandler().setFilterMappings(newFilterMappingsListBefore.toArray(new PaxWebFilterMapping[0]));
			}

			if (!change.isDynamic()) {
				ensureServletContextStarted(sch);
			}
		}
	}

	@Override
	public void visitEventListenerModelChange(EventListenerModelChange change) {
		Set<String> done = new HashSet<>();

		if (change.getKind() == OpCode.ADD) {
			EventListenerModel eventListenerModel = change.getEventListenerModel();
			List<OsgiContextModel> contextModels = change.getContextModels();
			contextModels.forEach((context) -> {
				String contextPath = context.getContextPath();
				if (!done.add(contextPath)) {
					return;
				}

				PaxWebServletContextHandler servletContextHandler = contextHandlers.get(contextPath);
				EventListener eventListener = eventListenerModel.resolveEventListener();
				if (eventListener instanceof ServletContextAttributeListener) {
					// add it to accessible list to fire per-OsgiContext attribute changes
					OsgiServletContext c = osgiServletContexts.get(context);
					c.addServletContextAttributeListener((ServletContextAttributeListener)eventListener);
				}
				if (eventListener instanceof HttpSessionAttributeListener) {
					// we have to store it separately to propagate OsgiHttpSession specific events
					sessionListenerModels.add(eventListenerModel);
				}

				boolean stopped = false;
				if (servletContextHandler.isStarted() && ServletContextListener.class.isAssignableFrom(eventListener.getClass())) {
					// we have to stop the context, so existing ServletContextListeners are called
					// with contextDestroyed() and new listener is added according to ranking rules of
					// the EventListenerModel
					try {
						LOG.info("Stopping Jetty context \"{}\" before registering a ServletContextListener", contextPath);
						servletContextHandler.stop();
						stopped = true;
					} catch (Exception e) {
						LOG.warn("Problem stopping {}: {}", servletContextHandler, e.getMessage());
					}
				}

				// add the listener to real context - even ServletContextAttributeListener (but only once - even
				// if there are many OsgiServletContexts per ServletContext)
				servletContextHandler.addEventListener(eventListenerModel, eventListener);

				if (stopped) {
					// we have to start it again
					ensureServletContextStarted(servletContextHandler);
				}

				// calling javax.servlet.ServletContextListener.contextInitialized() when server (context) is
				// already started and doing it in separate thread is a tweak to make Aries-CDI + extensions
				// work with CDI/JSF sample. I definitely have to solve it differently.
				// The problems are summarized in https://github.com/ops4j/org.ops4j.pax.web/issues/1622
//				if (servletContextHandler.isStarted() && ServletContextListener.class.isAssignableFrom(eventListener.getClass())) {
//					new Thread(() -> {
//						((ServletContextListener) eventListener).contextInitialized(new ServletContextEvent(osgiServletContexts.get(context)));
//					}).start();
//				}
			});
		}

		if (change.getKind() == OpCode.DELETE) {
			List<EventListenerModel> eventListenerModels = change.getEventListenerModels();
			for (EventListenerModel eventListenerModel : eventListenerModels) {
				List<OsgiContextModel> contextModels = eventListenerModel.getContextModels();
				contextModels.forEach((context) -> {
					String contextPath = context.getContextPath();
					if (!done.add(contextPath)) {
						return;
					}

					PaxWebServletContextHandler servletContextHandler = contextHandlers.get(contextPath);
					EventListener eventListener = eventListenerModel.resolveEventListener();
					if (eventListener instanceof ServletContextAttributeListener) {
						// remove it from per-OsgiContext list
						OsgiServletContext c = osgiServletContexts.get(context);
						if (c != null) {
							c.removeServletContextAttributeListener((ServletContextAttributeListener)eventListener);
						}
					}
					if (eventListener instanceof HttpSessionAttributeListener) {
						sessionListenerModels.remove(eventListenerModel);
					}

					if (pendingTransaction(contextPath)) {
						LOG.debug("Delaying removal of event listener {}", eventListenerModel);
						return;
					}

					removeEventListenerModel(servletContextHandler, eventListenerModel);
				});
			}
		}
	}

	private void removeEventListenerModel(PaxWebServletContextHandler servletContextHandler, EventListenerModel eventListenerModel) {
		if (servletContextHandler != null) {
			LOG.info("Removing event listener {}", eventListenerModel);

			// remove the listener from real context - even ServletContextAttributeListener
			// this may be null in case of WAB where we keep event listeners so they get contextDestroyed
			// event properly
			servletContextHandler.removeEventListener(eventListenerModel, eventListenerModel.resolveEventListener());
			eventListenerModel.releaseEventListener();
		}
	}

	@Override
	public void visitWelcomeFileModelChange(WelcomeFileModelChange change) {
		WelcomeFileModel model = change.getWelcomeFileModel();

		OpCode op = change.getKind();
		if (op == OpCode.ADD || op == OpCode.DELETE) {
			List<OsgiContextModel> contextModels = op == OpCode.ADD ? change.getContextModels()
					: model.getContextModels();
			// we have to configure all contexts, or rather - all resource servlets in all the contexts.
			// for Tomcat and Undertow we had to implement welcome file handling in "resource servlets" ourselves,
			// so we could have them settable.
			// in Jetty, initially we had no choice but to re-init the resource servlets after changing welcome files
			// in context handler, but eventually we've shaded the DefaultServlet, so welcome files are
			// settable
			contextModels.forEach((context) -> {
				// this time we don't alter single ServletContext for path of the highest ranked OsgiContextModel -
				// - we have to update all OsgiServletContexts because that's where welcome files are stored and
				// that's where "resource servlets" take the welcome files from
				OsgiServletContext osgiServletContext = osgiServletContexts.get(context);
				ServletContextHandler servletContextHandler = contextHandlers.get(context.getContextPath());

				if (osgiServletContext == null) {
					// may happen when cleaning things out
					return;
				}

				Set<String> currentWelcomeFiles = osgiServletContext.getWelcomeFiles() == null
						? new LinkedHashSet<>()
						: new LinkedHashSet<>(Arrays.asList(osgiServletContext.getWelcomeFiles()));

				if (op == OpCode.ADD) {
					currentWelcomeFiles.addAll(Arrays.asList(model.getWelcomeFiles()));
				} else {
					if (model.getWelcomeFiles().length == 0) {
						// special case of "remove all welcome files"
						currentWelcomeFiles.clear();
					} else {
						for (String s : model.getWelcomeFiles()) {
							currentWelcomeFiles.remove(s);
						}
					}
				}

				// set welcome files at OsgiServletContext level. NOT at ServletContextHandler level
				String[] newWelcomeFiles = currentWelcomeFiles.toArray(new String[0]);
				osgiServletContext.setWelcomeFiles(newWelcomeFiles);
				osgiServletContext.setWelcomeFilesRedirect(model.isRedirect());

				LOG.info("Reconfiguration of welcome files for all resource servlets in context \"{}\"", context);

				// reconfigure welcome files in resource servlets without reinitialization (Pax Web 8 change)
				for (ServletHolder sh : servletContextHandler.getServletHandler().getServlets()) {
					PaxWebServletHolder pwsh = (PaxWebServletHolder) sh;
					// reconfigure the servlet ONLY if its holder uses given OsgiContextModel
					if (pwsh.getServletModel() != null && pwsh.getServletModel().isResourceServlet()
							&& context == pwsh.getOsgiContextModel()) {
						try {
							Servlet servlet = sh.getServlet();
							if (servlet instanceof JettyResourceServlet) {
								((JettyResourceServlet) servlet).setWelcomeFiles(newWelcomeFiles);
								((JettyResourceServlet) servlet).setWelcomeFilesRedirect(model.isRedirect());
							} else if (servlet instanceof OsgiInitializedServlet) {
								((JettyResourceServlet) ((OsgiInitializedServlet) servlet).getDelegate()).setWelcomeFiles(newWelcomeFiles);
								((JettyResourceServlet) ((OsgiInitializedServlet) servlet).getDelegate()).setWelcomeFilesRedirect(model.isRedirect());
							}
						} catch (Exception e) {
							LOG.warn("Problem reconfiguring welcome files in servlet {}", sh, e);
						}
					}
				}
			});
		}
	}

	@Override
	public void visitErrorPageModelChange(ErrorPageModelChange change) {
		// no op here
	}

	@Override
	public void visitErrorPageStateChange(ErrorPageStateChange change) {
		Map<String, TreeMap<ErrorPageModel, List<OsgiContextModel>>> contextErrorPages = change.getContextErrorPages();

		for (Map.Entry<String, TreeMap<ErrorPageModel, List<OsgiContextModel>>> entry : contextErrorPages.entrySet()) {
			String contextPath = entry.getKey();
			TreeMap<ErrorPageModel, List<OsgiContextModel>> errorPageModelsMap = entry.getValue();
			Set<ErrorPageModel> errorPageModels = errorPageModelsMap.keySet();

			LOG.info("Changing error page configuration for context {}", contextPath);

			// there should already be a ServletContextHandler
			ServletContextHandler sch = contextHandlers.get(contextPath);

			if (sch == null) {
				// may happen when cleaning things out
				return;
			}

			ErrorPageErrorHandler eph = (ErrorPageErrorHandler) sch.getErrorHandler();
			eph.getErrorPages().clear();

			for (ErrorPageModel model : errorPageModels) {
				String location = model.getLocation();
				configureErrorPages(location, eph, model);
			}
		}
	}

	@Override
	public void visitContainerInitializerModelChange(ContainerInitializerModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			ContainerInitializerModel model = change.getContainerInitializerModel();
			if (!model.isForAnyRuntime() && !model.isForJetty()) {
				return;
			}
			List<OsgiContextModel> contextModels = change.getContextModels();
			contextModels.forEach((context) -> {
				String path = context.getContextPath();
				ServletContextHandler sch = contextHandlers.get(context.getContextPath());
				if (sch.isStarted()) {
					// we have to stop it. This operation should follow ClearDynamicRegistrationsChange that
					// clears possible dynamic registrations
					// also this operation is always followed by active web element registration (servlet or websocket)
					// because there's no need to just start the container with only one SCI
					LOG.info("Stopping Jetty context \"{}\" before registering a ServletContextInitializer", path);
					try {
						sch.stop();
					} catch (Exception e) {
						LOG.warn("Error stopping Jetty context \"{}\": {}", path, e.getMessage(), e);
					}
				}

				// no need to check whether there are more OsgiContextModels associated with
				// ContainerInitializerModel, because (for now) there's no Whiteboard support, thus there can
				// be only one OsgiContextModel

				// Jetty doesn't handle ServletContainerInitializers directly... There's something in
				// org.eclipse.jetty.annotations.AnnotationConfiguration and
				// org.eclipse.jetty.servlet.listener.ContainerInitializer which turns initializer into a
				// listener, but looks like in this case, ordering should be somehow managed manually
				//
				// but that's actually good, because we don't want Jetty to call the initializers - we want to
				// call them ourselves - we want to pass correct ServletContext implementation there
				DynamicRegistrations registrations = this.dynamicRegistrations.get(path);
				OsgiDynamicServletContext dynamicContext = new OsgiDynamicServletContext(osgiServletContexts.get(context), registrations);
				SCIWrapper wrapper = new SCIWrapper(dynamicContext, model);
				initializers.get(path).put(System.identityHashCode(model.getContainerInitializer()), wrapper);
			});
		}

		if (change.getKind() == OpCode.DELETE) {
			List<ContainerInitializerModel> models = change.getContainerInitializerModels();
			for (ContainerInitializerModel model : models) {
				if (!model.isForAnyRuntime() && !model.isForJetty()) {
					continue;
				}
				List<OsgiContextModel> contextModels = model.getContextModels();
				contextModels.forEach((context) -> {
					String path = context.getContextPath();
					ServletContainerInitializer initializer = model.getContainerInitializer();
					LinkedHashMap<Integer, SCIWrapper> wrappers = this.initializers.get(path);
					if (wrappers != null) {
						wrappers.remove(System.identityHashCode(initializer));
					}
				});
			}
		}
	}

	@Override
	public void visitWebSocketModelChange(WebSocketModelChange change) {
		if ((change.getKind() == OpCode.ADD && !change.isDisabled()) || change.getKind() == OpCode.ENABLE) {
			WebSocketModel model = change.getWebSocketModel();

			Set<String> done = new HashSet<>();

			change.getContextModels().forEach(osgiContextModel -> {
				String contextPath = osgiContextModel.getContextPath();
				if (!done.add(contextPath)) {
					return;
				}

				LOG.info("Adding web socket {} to {}", model, contextPath);

				// actually the web socket is already part of (known to) relevant SCI that'll register it when context
				// is started.
				// - when WebSocket is added to fresh (not started) context, the context will be started here
				// - when WebSocket is added to already started context, restart will be handled in
				//   visit(ContainerInitializerModelChange) method
				// so in both cases we simply have to start the server if it's not yet started

				ensureServletContextStarted(contextHandlers.get(contextPath));
			});
			return;
		}

		if (change.getKind() == OpCode.DISABLE || change.getKind() == OpCode.DELETE) {
			for (Map.Entry<WebSocketModel, Boolean> entry : change.getWebSocketModels().entrySet()) {
				WebSocketModel model = entry.getKey();
				if (!entry.getValue()) {
					continue;
				}

				Set<String> done = new HashSet<>();

				model.getContextModels().forEach(osgiContextModel -> {
					String contextPath = osgiContextModel.getContextPath();
					if (!done.add(contextPath)) {
						return;
					}

					LOG.info("Removing web socket {} from context {}", model, contextPath);

					// just as when adding WebSockets, we only have to ensure that context is started if it was
					// stopped. Restart is handled in visit(ContainerInitializerModelChange) method

					ensureServletContextStarted(contextHandlers.get(contextPath));
				});
			}
		}
	}

	@Override
	public void visitClearDynamicRegistrationsChange(ClearDynamicRegistrationsChange change) {
		Set<String> done = new HashSet<>();

		// the contexts related to the change will be (re)started in following batch operations. We have to clear
		// existing dynamic elements first
		change.getContextModels().forEach(context -> {
			String contextPath = context.getContextPath();
			if (!done.add(contextPath)) {
				return;
			}

			clearDynamicRegistrations(contextPath, context);
		});
	}

	private void clearDynamicRegistrations(String contextPath, OsgiContextModel context) {
		// there should already be a ServletContextHandler
		PaxWebServletContextHandler sch = contextHandlers.get(contextPath);

		// we can safely stop the context
		if (sch.isStarted()) {
			LOG.info("Stopping Jetty context \"{}\"", contextPath);
			try {
				sch.stop();
			} catch (Exception e) {
				LOG.warn("Error stopping Jetty context \"{}\": {}", contextPath, e.getMessage(), e);
			}
		}

		final int[] removed = { 0 };

		// servlets
		ServletHolder[] allServlets = sch.getServletHandler().getServlets();
		Map<ServletModel, Boolean> toRemove = new HashMap<>();
		for (ServletHolder sh : allServlets) {
			if (sh instanceof PaxWebServletHolder) {
				ServletModel model = ((PaxWebServletHolder) sh).getServletModel();
				if (model != null && model.isDynamic()) {
					toRemove.put(model, Boolean.TRUE);
					removed[0]++;
				}
			}
		}
		if (!toRemove.isEmpty()) {
			// it's safe, because dynamic servlets can target only one osgi/servlet context
			visitServletModelChange(new ServletModelChange(OpCode.DELETE, toRemove));
		}

		// filters
		FilterHolder[] allFilters = sch.getServletHandler().getFilters();
		List<FilterHolder> filtersToRemove = new ArrayList<>();
		// the map should contain all but the dynamic ones
		for (FilterHolder fh : allFilters) {
			if (fh instanceof PaxWebFilterHolder) {
				FilterModel model = ((PaxWebFilterHolder) fh).getFilterModel();
				// org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter.configure() deals directly
				// with Jetty context, so we can't intercept the registration and the model is null.
				// we'll treat such registration as dynamic that has to be removed
				if (model == null || model.isDynamic()) {
					filtersToRemove.add(fh);
					removed[0]++;
				}
			}
		}
		if (!filtersToRemove.isEmpty()) {
			List<PaxWebFilterHolder> newFilters = new ArrayList<>();
			List<PaxWebFilterMapping> newFilterMappings = new ArrayList<>();
			for (FilterHolder fh : allFilters) {
				if (fh instanceof PaxWebFilterHolder && !filtersToRemove.contains(fh)) {
					PaxWebFilterHolder pwfh = (PaxWebFilterHolder) fh;
					newFilters.add(pwfh);
					newFilterMappings.addAll(pwfh.getMapping());
				}
			}
			sch.getServletHandler().setFilters(newFilters.toArray(new PaxWebFilterHolder[0]));
			sch.getServletHandler().setFilterMappings(newFilterMappings.toArray(new PaxWebFilterMapping[0]));
		}

		// listeners - it's easier, because we remember them in dynamic registrations
		DynamicRegistrations contextRegistrations = this.dynamicRegistrations.get(contextPath);
		if (contextRegistrations != null) {
			contextRegistrations.getDynamicListenerModels().forEach((l, model) -> {
				if (model.isDynamic()) {
					// yes it always should be
					removed[0]++;

					if (l instanceof ServletContextAttributeListener) {
						// remove it from per-OsgiContext list
						OsgiServletContext c = osgiServletContexts.get(context);
						if (c != null) {
							c.removeServletContextAttributeListener((ServletContextAttributeListener) l);
						}
					}

					sch.removeEventListener(l);
				}
			});
			// it'll be prepared for new dynamic registrations when SCIs are started again
			contextRegistrations.getDynamicListenerModels().clear();

			// additionally clear pending registrations
			contextRegistrations.getDynamicServletRegistrations().clear();
			contextRegistrations.getDynamicFilterRegistrations().clear();
			contextRegistrations.getDynamicListenerRegistrations().clear();
		}

		if (removed[0] > 0) {
			LOG.debug("Removed {} dynamically registered servlets/filters/listeners from context {}", removed[0], contextPath);
		}
	}

	/**
	 * <p>Registration of <em>active web element</em> should always start the context. On the other hand,
	 * registration of <em>passive web element</em> should <strong>not</strong> start the context.</p>
	 *
	 * <p>This method is always (should be) called withing the "configuration thread" of Pax Web Runtime, because
	 * it's called in visit() methods for servlets (including resources) and filters, so we can safely access
	 * {@link org.ops4j.pax.web.service.spi.model.ServerModel}.</p>
	 * @param sch
	 */
	private void ensureServletContextStarted(PaxWebServletContextHandler sch) {
		String contextPath = sch.getContextPath().equals("") ? "/" : sch.getContextPath();
		if (sch.isStarted() || pendingTransaction(contextPath)) {
			return;
		}
		try {
			OsgiContextModel highestRanked = ((PaxWebServletHandler) sch.getServletHandler()).getDefaultOsgiContextModel();
			OsgiServletContext highestRankedContext = ((PaxWebServletHandler) sch.getServletHandler()).getDefaultServletContext();
			highestRankedContext.allowServletContextListeners();

			LOG.info("Starting Jetty context \"{}\" with default Osgi Context {}", contextPath, highestRanked);

			// first thing - only NOW we can set ServletContext's class loader! It affects many things, including
			// the TCCL used for example by javax.el.ExpressionFactory.newInstance()
			sch.setClassLoader(highestRankedContext.getClassLoader());

			highestRankedContext.clearAttributesFromPreviousCycle();
			clearDynamicRegistrations(contextPath, highestRanked);

			DynamicRegistrations registrations = this.dynamicRegistrations.get(contextPath);
			// allow dynamic registration, which will be restricted by RegisteringContainerInitializer
			OsgiDynamicServletContext dynamicContext = new OsgiDynamicServletContext(highestRankedContext, registrations);
			sch.setOsgiServletContext(dynamicContext);

			// this is when already collected initializers may be added as ordered collection to the servlet context
			// handler (Pax Web specific) - we need control over them, because we have to pass correct
			// ServletContext implementation there
			Collection<SCIWrapper> initializers = new LinkedList<>(this.initializers.get(contextPath).values());
			// Initially I thought we should take only these SCIs, which are associated with highest ranked OCM,
			// but it turned out that just as we take servlets registered to different OsgiContextModels, but
			// the same ServletContextModel, we have to do the same with SCIs.
			// otherwise, by default (with HttpService scenario), SCIs from the OsgiContextModel related to
			// pax-web-extender-whiteboard would be taken (probably 0), simply because this bundle is usually
			// the first that grabs an instance of bundle-scoped HttpService
			// so please do not uncomment and keep for educational purposes!
//			initializers.removeIf(w -> !w.getModel().getContextModels().contains(highestRanked));

			// add a final initializer that will take care of actual registration of potentially collected
			// dynamic servlets, filters and listeners and that will mark the OsgiServletContext as no longer
			// accepting registration of additional ServletContextListeners
			initializers.add(new RegisteringContainerInitializer(highestRankedContext, registrations));
			sch.setServletContainerInitializers(initializers);

			// alter session configuration
			SessionHandler sessionHandler = sch.getSessionHandler();
			SessionConfigurationModel sessionConfig = highestRanked.getSessionConfiguration();
			if (sessionConfig != null) {
				if (sessionConfig.getSessionTimeout() != null) {
					sessionHandler.setMaxInactiveInterval(sessionConfig.getSessionTimeout() * 60);
				}
				SessionCookieConfig scc = sessionConfig.getSessionCookieConfig();
				if (scc != null) {
					if (scc.getName() != null) {
						sessionHandler.setSessionCookie(scc.getName());
					}
					if (scc.getDomain() != null) {
						sessionHandler.getSessionCookieConfig().setDomain(scc.getDomain());
					}
					if (scc.getPath() != null) {
						sessionHandler.getSessionCookieConfig().setPath(scc.getPath());
					}
					sessionHandler.getSessionCookieConfig().setMaxAge(scc.getMaxAge());
					sessionHandler.getSessionCookieConfig().setHttpOnly(scc.isHttpOnly());
					sessionHandler.getSessionCookieConfig().setSecure(scc.isSecure());
					sessionHandler.getSessionCookieConfig().setComment(scc.getComment());

					if (sessionConfig.getTrackingModes().size() > 0) {
						sessionHandler.setSessionTrackingModes(sessionConfig.getTrackingModes());
					}
				}
			}

			// security configuration - as with sessions, it's taken from OsgiContextModel
			SecurityConfigurationModel securityConfig = highestRanked.getSecurityConfiguration();
			LoginConfigModel loginConfig = securityConfig.getLoginConfig();
			if (loginConfig != null) {
				// only in this case there's a need to configure anything
				ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
				sch.setSecurityHandler(securityHandler);

				securityHandler.setRealmName(loginConfig.getRealmName());
				// realm name is needed even for FORM based authentication if there are more than 1 login services
				// defined for a server
				if (securityHandler.getRealmName() == null) {
					securityHandler.setRealmName("default");
				}

				switch (loginConfig.getAuthMethod().toUpperCase()) {
					case Constraint.__BASIC_AUTH:
						securityHandler.setAuthenticator(new BasicAuthenticator());
						break;
					case Constraint.__DIGEST_AUTH:
						DigestAuthenticator digestAuthenticator = new DigestAuthenticator();
						digestAuthenticator.setMaxNonceAge(configuration.security().getDigestAuthMaxNonceAge());
						digestAuthenticator.setMaxNonceCount(configuration.security().getDigestAuthMaxNonceCount());
						securityHandler.setAuthenticator(digestAuthenticator);
						break;
					case Constraint.__CERT_AUTH:
					case Constraint.__CERT_AUTH2:
						securityHandler.setAuthenticator(new ClientCertAuthenticator());
						break;
					case Constraint.__FORM_AUTH:
						FormAuthenticator formAuthenticator = new FormAuthenticator(loginConfig.getFormLoginPage(),
								loginConfig.getFormErrorPage(), !configuration.security().getFormAuthRedirect());
						securityHandler.setInitParameter(FormAuthenticator.__FORM_LOGIN_PAGE, loginConfig.getFormLoginPage());
						securityHandler.setInitParameter(FormAuthenticator.__FORM_ERROR_PAGE, loginConfig.getFormErrorPage());
						securityHandler.setInitParameter(FormAuthenticator.__FORM_DISPATCH, Boolean.toString(!configuration.security().getFormAuthRedirect()));
						securityHandler.setAuthenticator(formAuthenticator);
						break;
					case Constraint.__NEGOTIATE_AUTH:
						// TODO: create configuration options
						securityHandler.setAuthenticator(new ConfigurableSpnegoAuthenticator());
						break;
					default:
						// TODO: discover (OSGi, ServiceLoader) an authenticator, e.g., Keycloak
				}

				for (String role : securityConfig.getSecurityRoles()) {
					securityHandler.addRole(role);
				}

				ensureSecurityConstraintsConfigured(securityHandler, securityConfig.getSecurityConstraints());
			}

			// taking virtual host / connector configuration from OsgiContextModel
			// see http://www.eclipse.org/jetty/documentation/jetty-9/index.html#configuring-virtual-hosts
			// Jetty supports the following styles of virtual host name:
			//  - www.hostname.com
			//  - *.hostname.com (one level wildcarded)
			//  - 10.20.30.40 (IP address)
			//  - @ConnectorName - not strictly a virtual host, but handled in the same way as VH, but not by matching
			//    the "Host" header, but incomming connector's name
			//  - actually, looks like it can also be "virtualHost@connectorName" format, where both must match
			List<String> allVirtualHosts = new ArrayList<>();
			List<String> vhosts = new ArrayList<>(highestRanked.getVirtualHosts());
			if (vhosts.isEmpty()) {
				vhosts.addAll(Arrays.asList(configuration.server().getVirtualHosts()));
			}
			List<String> connectors = new ArrayList<>(highestRanked.getConnectors());
			if (connectors.isEmpty()) {
				connectors.addAll(Arrays.asList(configuration.server().getConnectors()));
			}
			for (String vhost : vhosts) {
				if (vhost == null || "".equals(vhost.trim())) {
					continue;
				}
				if (vhost.startsWith("@")) {
					// it is a connector
					allVirtualHosts.add(vhost);
				} else {
					// it is a normal virtual host (yes - don't process it anyway)
					allVirtualHosts.add(vhost);
				}
			}
			for (String c : connectors) {
				if (c == null || "".equals(c.trim())) {
					continue;
				}
				if (c.startsWith("@")) {
					// it is a connector, but should be specified as special Jetty's VHost - add without processing
					allVirtualHosts.add(c);
				} else {
					// it is a connector, but should be added as "@" prefixed VHost
					allVirtualHosts.add("@" + c);
				}
			}
			boolean connectorOnly = false;
			boolean vhostOnly = false;
			boolean vhostAndConnector = false;
			for (String vh : allVirtualHosts) {
				if (!vh.contains("@")) {
					vhostOnly = true;
				} else if (vh.startsWith("@")) {
					connectorOnly = true;
				} else {
					vhostAndConnector = true;
				}
			}
			if (vhostOnly && connectorOnly && !vhostAndConnector) {
				// add artificial handler@connector to prevent filtering out the host-only VHosts
				// see https://github.com/eclipse/jetty.project/issues/1785
				allVirtualHosts.add("_@_");
			}

			sch.setVirtualHosts(allVirtualHosts.toArray(new String[0]));

			// and finally - XML context configuration which should be treated as highest priority (overriding
			// the above setup)
			XmlConfiguration previous = null;
			Map<String, Object> objects = new LinkedHashMap<>();
			objects.put("Context", sch);

			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(sch.getClassLoader());
			try {
				for (URL url : highestRanked.getServerSpecificDescriptors()) {
					String path = url.getPath();
					if (path.startsWith("/WEB-INF/") && path.endsWith(".xml") && path.contains("jetty")) {
						XmlConfiguration cfg = new XmlConfiguration(Resource.newResource(url));
						LOG.info("Processing context specific {} for {}", url, contextPath);

						processConfiguration(cfg, previous, objects);

						previous = cfg;
					}
				}
			} finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}

			// There's a deadlock, when starting the context in single paxweb-config thread:
			// 1. org.apache.aries.cdi.weld.WeldCDIContainerInitializer.initialize() is called in Aries CCR thread
			// 2. locking <0x1e70> (a org.jboss.weld.Container) in Aries CCR thread
			// 3. org.apache.aries.cdi.extension.servlet.weld.WeldServletExtension.afterDeploymentValidation() is called in Aries CCR thread
			// 4. org.jboss.weld.module.web.servlet.WeldInitialListener is whiteboard-registered in Aries CCR thread
			// 5. Pax Web's listener tracker passes the registration to single paxweb-config thread
			// 6. Pax Web's JettyServerWrapper registers the listener in already started context (so it restarts it) in
			//    paxweb-config thread
			// 7. WeldInitialListener.contextInitialized() is called in paxweb-config thread
			// 8. org.jboss.weld.module.web.servlet.HttpContextLifecycle#fireEventForApplicationScope() has
			//    synchronized block on the org.jboss.weld.Container instance - deadlock

			// TODO: do it better
//			new Thread(() -> {
//				try {
//					sch.start();
//				} catch (Exception e) {
//					LOG.error(e.getMessage(), e);
//				}
//			}).start();
			sch.start();

			dynamicContext.rememberAttributesFromSCIs();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void ensureSecurityConstraintsConfigured(ConstraintSecurityHandler securityHandler, List<SecurityConstraintModel> models) {
		// see org.eclipse.jetty.webapp.StandardDescriptorProcessor.visitSecurityConstraint()
		for (SecurityConstraintModel constraint : models) {
			Constraint base = new Constraint();
			if (constraint.isAuthRolesSet()) {
				base.setAuthenticate(true);
				base.setRoles(constraint.getAuthRoles().toArray(new String[0]));
				for (String role : constraint.getAuthRoles()) {
					// in case it wasn't declared in <security-role>
					securityHandler.addRole(role);
				}
			}
			if (constraint.getTransportGuarantee() == ServletSecurity.TransportGuarantee.NONE) {
				base.setDataConstraint(Constraint.DC_NONE);
			} else {
				// DC_CONFIDENTIAL and DC_INTEGRAL are handled equally and effectively mean "use TLS"
				base.setDataConstraint(Constraint.DC_CONFIDENTIAL);
			}

			for (SecurityConstraintModel.WebResourceCollection wrc : constraint.getWebResourceCollections()) {
				Constraint sc = null;
				try {
					sc = (Constraint) base.clone();
					sc.setName(wrc.getName());
				} catch (CloneNotSupportedException e) {
					LOG.warn(e.getMessage(), e);
				}

				if (wrc.getMethods().size() > 0 && wrc.getOmittedMethods().size() > 0) {
					LOG.warn("Both methods and method omissions specified in the descriptor. Using methods only");
					wrc.getOmittedMethods().clear();
				}
				for (String url : wrc.getPatterns()) {
					boolean hit = false;
					for (String method : wrc.getMethods()) {
						ConstraintMapping mapping = new ConstraintMapping();
						mapping.setMethod(method);
						mapping.setPathSpec(url);
						mapping.setConstraint(sc);
						securityHandler.addConstraintMapping(mapping);
						hit = true;
					}
					for (String method : wrc.getOmittedMethods()) {
						ConstraintMapping mapping = new ConstraintMapping();
						// yes - one-element array as in
						// org.eclipse.jetty.webapp.StandardDescriptorProcessor.visitSecurityConstraint()
						mapping.setMethodOmissions(new String[] { method });
						mapping.setPathSpec(url);
						mapping.setConstraint(sc);
						securityHandler.addConstraintMapping(mapping);
						hit = true;
					}
					if (!hit) {
						// all-method constraint
						ConstraintMapping mapping = new ConstraintMapping();
						mapping.setPathSpec(url);
						mapping.setConstraint(sc);
						securityHandler.addConstraintMapping(mapping);
					}
				}
			}
		}
	}

	private boolean pendingTransaction(String contextPath) {
		return transactions.contains(contextPath);
	}

	private void configureErrorPages(String location, ErrorPageErrorHandler eph, ErrorPageModel epm) {
		for (String ex : epm.getExceptionClassNames()) {
			eph.addErrorPage(ex, location);
		}
		for (int code : epm.getErrorCodes()) {
			eph.addErrorPage(code, location);
		}
		if (epm.isXx4()) {
			eph.addErrorPage(400, 499, location);
		}
		if (epm.isXx5()) {
			eph.addErrorPage(500, 599, location);
		}
	}

	/**
	 * <p>This method tries to check if it's ok to just add new filters from {@code newFilterHolders} <em>at the end</em>
	 * of current list of filters. This is quite special case, but not that uncommon - when new filters are
	 * lower-ranked than all existing ones and there are no filters to be removed.</p>
	 *
	 * @param servletHandler
	 * @param newFilterHolders
	 * @return
	 */
	private boolean quickFilterChange(ServletHandler servletHandler, PaxWebFilterHolder[] newFilterHolders) {
		FilterHolder[] existingFilterHolders = servletHandler.getFilters();

		int pos = 0;
		boolean quick = newFilterHolders.length >= existingFilterHolders.length;

		// by "quick" we mean - there are no removed filters and new filters come last
		while (quick) {
			if (pos >= existingFilterHolders.length) {
				break;
			}
			if ((((PaxWebFilterHolder) existingFilterHolders[pos]).getFilterModel() == null
					|| newFilterHolders[pos].getFilterModel() == null)) {
				if (existingFilterHolders[pos].getName() != null) {
					if (!existingFilterHolders[pos].getName().equals(newFilterHolders[pos].getName())) {
						quick = false;
						break;
					}
				}
				if (existingFilterHolders[pos].getHeldClass() != null) {
					if (!existingFilterHolders[pos].getHeldClass().equals(newFilterHolders[pos].getHeldClass())) {
						quick = false;
						break;
					}
				}
			} else if (!((PaxWebFilterHolder)existingFilterHolders[pos]).getFilterModel().equals(newFilterHolders[pos].getFilterModel())) {
				quick = false;
				break;
			}
			pos++;
		}

		if (quick) {
			for (int i = pos; i < newFilterHolders.length; i++) {
				// each holder may have many mappings
				PaxWebFilterHolder fh = newFilterHolders[i];
				servletHandler.addFilter(fh);
				for (PaxWebFilterMapping paxWebFilterMapping : fh.getMapping()) {
					if (paxWebFilterMapping.isAfter()) {
						servletHandler.addFilterMapping(paxWebFilterMapping);
					} else {
						servletHandler.prependFilterMapping(paxWebFilterMapping);
					}
				}
			}
			return true;
		}

		return false;
	}

	private List<PaxWebFilterMapping> configureFilterMappings(FilterModel model) {
		List<PaxWebFilterMapping> mappings = new LinkedList<>();

		if (model.getDynamicServletNames().size() > 0 || model.getDynamicUrlPatterns().size() > 0) {
			// this FilterModel was created in SCI using ServletContext.addFilter(), so it has ONLY
			// dynamic mappings (potentially more than one)
			model.getDynamicServletNames().forEach(dm -> {
				if (!dm.isAfter()) {
					mappings.add(new PaxWebFilterMapping(model, dm));
				}
			});
			model.getDynamicUrlPatterns().forEach(dm -> {
				if (!dm.isAfter()) {
					mappings.add(new PaxWebFilterMapping(model, dm));
				}
			});
			model.getDynamicServletNames().forEach(dm -> {
				if (dm.isAfter()) {
					mappings.add(new PaxWebFilterMapping(model, dm));
				}
			});
			model.getDynamicUrlPatterns().forEach(dm -> {
				if (dm.isAfter()) {
					mappings.add(new PaxWebFilterMapping(model, dm));
				}
			});
		} else {
			// normal OSGi mapping
			for (FilterModel.Mapping map : model.getMappingsPerDispatcherTypes()) {
				mappings.add(new PaxWebFilterMapping(model, map));
			}
		}

		return mappings;
	}

	/**
	 * Helper method that calls {@link XmlConfiguration#configure()}
	 * @param cfg
	 * @param previous
	 * @param objects
	 */
	private void processConfiguration(XmlConfiguration cfg, XmlConfiguration previous, Map<String, Object> objects) {
		// add objects created in previous file, so they're available when parsing next one
		cfg.getIdMap().putAll(previous == null ? objects : previous.getIdMap());
		// configuration will be available for Jetty when using <Property />
		cfg.getProperties().putAll(this.configuration.all());

		try {
			cfg.configure();
		} catch (Exception e) {
			LOG.warn("Problem parsing {}: {}", cfg, e.getMessage(), e);
		}

		// collect all created objects
		objects.putAll(cfg.getIdMap());

		// collect all created HttpConfigurations
		cfg.getIdMap().forEach((id, v) -> {
			if (HttpConfiguration.class.isAssignableFrom(v.getClass())) {
				httpConfigs.put(id, (HttpConfiguration) v);
			}
		});
	}

	// --- handler/connector/customizer configuration
	//     only customizers are added/removed to/from "live" server

	public void setHandlers(Set<PriorityValue<Handler>> handlers) {
		mainHandler.setPriorityHandlers(handlers);
	}

	public void setCustomizers(Set<PriorityValue<HttpConfiguration.Customizer>> customizers) {
		registeredCustomizers.clear();
		registeredCustomizers.addAll(customizers);
		if (server != null) {
			configureServerCustomizers();
		}
	}

	private void configureServerCustomizers() {
		Connector[] connectors = server.getConnectors();
		for (Connector connector : connectors) {
			Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
			for (ConnectionFactory connectionFactory : connectionFactories) {
				if (connectionFactory instanceof HttpConnectionFactory) {
					// same for secure and non-secure connectors. Secure connectors simply have more
					// connection factories inside
					HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) connectionFactory;
					HttpConfiguration httpConfiguration = httpConnectionFactory.getHttpConfiguration();
					for (PriorityValue<HttpConfiguration.Customizer> customizer : registeredCustomizers) {
						httpConfiguration.addCustomizer(customizer.getValue());
					}
				}
			}
		}
	}

	public void removeCustomizer(HttpConfiguration.Customizer customizer) {
		if (server != null) {
			Connector[] connectors = server.getConnectors();
			for (Connector connector : connectors) {
				Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
				for (ConnectionFactory connectionFactory : connectionFactories) {
					if (connectionFactory instanceof HttpConnectionFactory) {
						HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) connectionFactory;
						HttpConfiguration httpConfiguration = httpConnectionFactory.getHttpConfiguration();
						List<HttpConfiguration.Customizer> httpConfigurationCustomizers = httpConfiguration.getCustomizers();
						httpConfigurationCustomizers.remove(customizer);
					}
				}
			}
		}
		registeredCustomizers.removeIf(pv -> pv.getValue() == customizer);
	}

}
