/*
 * Copyright 2012 Romain Gilles
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
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.AccessLog;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AccessLogAdapter;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.loader.ParallelWebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.apache.tomcat.util.digester.Digester;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.LogConfiguration;
import org.ops4j.pax.web.service.spi.config.SessionConfiguration;
import org.ops4j.pax.web.service.spi.model.ContextMetadataModel;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.servlet.Default404Servlet;
import org.ops4j.pax.web.service.spi.servlet.DynamicRegistrations;
import org.ops4j.pax.web.service.spi.servlet.OsgiDynamicServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedServlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContextClassLoader;
import org.ops4j.pax.web.service.spi.servlet.RegisteringContainerInitializer;
import org.ops4j.pax.web.service.spi.servlet.SCIWrapper;
import org.ops4j.pax.web.service.spi.task.BatchVisitor;
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
import org.ops4j.pax.web.service.spi.task.WelcomeFileModelChange;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.tomcat.internal.web.TomcatResourceServlet;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * <p>A <em>wrapper</em> or <em>holder</em> of actual Tomcat server. This class perform two kinds of tasks:<ul>
 *     <li>controls the state of Tomcat by configuring, starting and stopping it</li>
 *     <li>translates model changes into registration of Tomcat-specific contexts, holders and handlers</li>
 * </ul></p>
 *
 * <p>This class after Pax Web 8 is modelled after similar class for Jetty (and later - Undertow)</p>
 *
 * @author Romain Gilles
 * @author Grzegorz Grzybek
 */
class TomcatServerWrapper implements BatchVisitor {

	public static final Logger LOG = LoggerFactory.getLogger(TomcatServerWrapper.class);

	private static final String TOMCAT_CATALINA_NAME = "Catalina";

	/** An <em>entry</em> to OSGi runtime to lookup other bundles if needed (to get their ClassLoader) */
	private final Bundle paxWebTomcatBundle;
	/** Outside of OSGi, let's use passed ClassLoader */
	private final ClassLoader classLoader;

	/**
	 * Actual instance of {@link org.apache.catalina.core.StandardServer}. In Jetty we had extended class. Here
	 * we hold direct instance, because it is final.
	 */
	private StandardServer server;

	/** Tomcat's {@link Service} */
	private StandardService service;

	/** Tomcat's {@link Engine} */
	private StandardEngine engine;

	/**
	 * Tomcat's default {@link Host}
	 */
	private Host defaultHost;

	/**
	 * Other virtual hosts. In Tomcat, host is separate entity unlike in Jetty and Undertow, where it's rather
	 * a <em>feature</em> of the context.
	 */
	private final Map<String, Host> hosts = new HashMap<>();

	/** Server's thread pool */
	private Executor serverExecutor;

	private final TomcatFactory tomcatFactory;

	/**
	 * A set of context paths that are being configured within <em>transactions</em> - context is started only at
	 * the end of the transaction.
	 */
	private final Set<String> transactions = new HashSet<>();

	/** Single map of context path to {@link Context} for fast access */
	private final Map<String, PaxWebStandardContext> contextHandlers = new HashMap<>();

	/**
	 * 1:1 mapping between {@link OsgiContextModel} and {@link org.osgi.service.http.context.ServletContextHelper}'s
	 * specific {@link javax.servlet.ServletContext}.
	 */
	private final Map<OsgiContextModel, OsgiServletContext> osgiServletContexts = new HashMap<>();

	/**
	 * When {@link PaxWebStandardContext} is started, it has to be configured according to current, highest-ranked
	 * {@link OsgiContextModel} using specific session and security configuration. These listeners are added
	 * when {@link OsgiContextModel} is visited.
	 */
	private final Map<OsgiContextModel, LifecycleListener> configurationListeners = new HashMap<>();

	/**
	 * 1:N mapping between context path and sorted (by ranking rules) set of {@link OsgiContextModel}. This helps
	 * finding proper {@link org.osgi.service.http.context.ServletContextHelper} (1:1 with {@link OsgiContextModel})
	 * to use for filters, when the invocation chain doesn't contain target servlet (which otherwise would
	 * determine the ServletContextHelper to use).
	 */
	private final Map<String, TreeSet<OsgiContextModel>> osgiContextModels = new HashMap<>();

	/**
	 * Even if Tomcat manages SCIs, we'll manage them here instead - to be able to remove them when needed.
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

	TomcatServerWrapper(Configuration config, TomcatFactory tomcatFactory,
			Bundle paxWebTomcatBundle, ClassLoader classLoader) {
		this.configuration = config;
		this.tomcatFactory = tomcatFactory;
		this.paxWebTomcatBundle = paxWebTomcatBundle;
		this.classLoader = classLoader;
	}

	// --- lifecycle and configuration methods

	/**
	 * One-time configuration of Tomcat. Modelled after
	 * {@code org.ops4j.pax.web.service.jetty.internal.JettyServerWrapper#configure()}
	 */
	public void configure() throws Exception {
		// for now, we have nothing. We can do many things using external jetty-*.xml files, but the creation
		// of Server itself should be done manually here.
		LOG.info("Creating Tomcat server instance using configuration properties.");
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			// TCCL is needed so StringManagers in Tomcat code work
			Thread.currentThread().setContextClassLoader(TomcatServerWrapper.class.getClassLoader());

			// unlike as in Jetty, we can't configure the "template" server to be altered using external XML
			// configuration (at least not without overwriting digester rules like ObjectCreateRule), so we'll first
			// check if there's an XML configuration available and fallback to fully-programmatic server otherwise
			applyTomcatConfiguration();

			// If external configuration added some connectors, we have to ensure they match declaration from
			// PID config: org.osgi.service.http.enabled and org.osgi.service.http.secure.enabled
			verifyConnectorConfiguration();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}

		// Configure NCSA RequestLogHandler if needed
		if (configuration.logging().isLogNCSAFormatEnabled()) {
			configureRequestLog();
		}

		// default session configuration is prepared, but not set in the server instance. It can be set
		// only after first context is created
		this.defaultSessionCookieConfig = configuration.session().getDefaultSessionCookieConfig();
	}

	/**
	 * <p>Create Tomcat server using provided {@link Configuration}.</p>
	 *
	 * @return
	 */
	private void createServer() {
		serverExecutor = tomcatFactory.createThreadPool(configuration);

		// actual org.apache.catalina.core.StandardServer
		StandardServer server = new StandardServer();
		server.setUtilityThreads(2); // that's the default

		// let's not set (for now) "catalina.base" - it's used only if some other files that Tomcat has to load
		// are relative. Setting it to java.io.tmpdir is strange...
//		server.setCatalinaBase(?);

		// service - collection of connectors operating on single container (which is an engine)
		StandardService service = new StandardService();
		service.setName(TOMCAT_CATALINA_NAME);
		service.addExecutor(serverExecutor);
		server.addService(service);

		// engine - a container invoked from many connectors within a service. Here we're not adding any connectors
		StandardEngine engine = new StandardEngine();
		engine.setName(TOMCAT_CATALINA_NAME);
		// this default host will *never* change
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		// single, default virtual host. Other hosts can be added later dynamically.
		StandardHost host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		host.setStartChildren(false);
		engine.addChild(host);

		this.server = server;
		this.service = service;
		this.engine = engine;
		this.defaultHost = host;
	}

	/**
	 * It was very easy and clean in Jetty, Tomcat also has a method, where the embedded Tomcat server can be
	 * configured using external XML files conforming to Tomcat digester configuration but less flexible than Jetty.
	 */
	private void applyTomcatConfiguration() {
		File[] locations = configuration.server().getConfigurationFiles();
		URL tomcatResource = getClass().getResource("/tomcat-server.xml");
		if (locations.length == 0) {
			if (tomcatResource == null) {
				LOG.info("No external Tomcat configuration files specified. Default/PID configuration will be used.");
			} else {
				LOG.info("Found \"tomcat-server.xml\" resource on the classpath: {}.", tomcatResource);
			}
		} else if (locations.length > 1) {
			LOG.warn("Can't specify Tomcat configuration using more than one XML file. Only {} will be used.", locations[0]);
		} else {
			if (tomcatResource != null) {
				LOG.info("Found additional \"tomcat-server.xml\" resource on the classpath: {}, but {} will be used instead.",
						tomcatResource, locations[0]);
			}
			LOG.info("Processing Tomcat configuration from file: {}", locations[0]);
		}

		URL config = null;
		try {
			config = locations.length == 0 ? tomcatResource : locations[0].toURI().toURL();
		} catch (MalformedURLException e) {
			LOG.warn(e.getMessage(), e);
		}

		TomcatFactory.ServerHolder holder = new TomcatFactory.ServerHolder();
		if (config != null) {
			Digester digester = tomcatFactory.createServerDigester();
			digester.push(holder);

			LOG.debug("Parsing {}", config);

			try (InputStream is = config.openStream()) {
				digester.parse(is);
			} catch (IOException | SAXException e) {
				LOG.warn("Problem parsing {}: {}", config, e.getMessage(), e);
			}
		}

		if (holder.getServer() == null) {
			// we have to create the server using non-XML configuration only
			createServer();
		} else {
			// we have to review the Server created by parsing the XML - as it doesn't have to be "complete"
			// Here's full hierarchy of objects/elements of Tomcat-compliant XML (compacting some irrelevant elements):
			// <Server> (one global server)
			//    <GlobalNamingResources>
			//        ...
			//    <Listener>
			//    <Service> (multiple per server)
			//        <Listener>
			//        <Executor> (multiple per service)
			//        <Connector> (multiple per service, reference executor by id)
			//            <SSLHostConfig>
			//                ...
			//            <Listener>
			//            <UpgradeProtocol>
			//        <Engine> (one per service)
			//            <Cluster>
			//            <Listener>
			//            <Realm>
			//                ...
			//            <Valve>
			//            <Host>
			//                <Alias>
			//                <Cluster>
			//                <Listener>
			//                <Realm>
			//                <Valve>
			//                <Context>
			//                    ...
			//
			// To understand a model a bit (https://tomcat.apache.org/tomcat-9.0-doc/config/index.html):
			//  - there's one global <Server>
			//  - <Service> represents a group of <Connector>s that is associated with an <Engine>
			//  - <Connector> is one of the interfaces _receiving_ incoming requests for particular <Service>
			//  - a Container is a component that actually _processes_ an incoming request for a <Service>
			//  - there are 4 different "containers":
			//     - <Engine> handles *all* requests for a <Service> through its <Connector>s
			//     - <Host> handles requests within an <Engine> for particular virtual host
			//     - <Context> handles reuqests within a <Host> for particular web application
			//     - Wrapper (no XML element for it) handles requests within a <Context> for particular servlet
			//
			// The above model, in the context of Pax Web and OSGi means that we need only one <Service>. Period.
			// We'll still handle more connectors and virtual hosts, but in a way that's consistent between
			// Jetty, Tomcat and Undertow.

			// <Server>
			server = (StandardServer) holder.getServer();

			// <Server>/<Service>
			if (server.findServices().length == 0) {
				LOG.info("No Service configured in Tomcat XML configuration. Creating default \"Catalina\" Service.");
				service = new StandardService();
				server.addService(service);
			} else {
				service = (StandardService) this.server.findServices()[0];
				if (server.findServices().length > 1) {
					LOG.warn("More than one Service configured in Tomcat XML configuration." +
							" Using \"{}\" and removing the other ones.", service.getName());
					Service[] findServices = server.findServices();
					for (int i = 1; i < findServices.length; i++) {
						server.removeService(findServices[i]);
					}
				}
			}
			service.setName(TOMCAT_CATALINA_NAME);

			// <Server>/<Service>/<Executor>
			if (service.findExecutors().length == 0) {
				LOG.info("No Executor configured in Tomcat XML configuration. Creating default Executor from configuration.");
				serverExecutor = tomcatFactory.createThreadPool(configuration);
				service.addExecutor(serverExecutor);
			} else {
				serverExecutor = service.findExecutors()[0];
				if (service.findExecutors().length > 1) {
					// don't remove other executors, as they may be referenced from custom connectors.
					// programmatically created connectors however will reference the first executor.
					LOG.warn("More than one Executor configured in Tomcat XML configuration." +
							" Using \"{}\" as the default and keeping the other ones.", serverExecutor.getName());
				}
			}

			// <Server>/<Service>/<Engine>
			engine = (StandardEngine) this.service.getContainer();
			if (engine == null) {
				LOG.info("No Engine configured in Tomcat XML configuration. Creating default \"Catalina\" Engine.");
				engine = new StandardEngine();
				service.setContainer(engine);
			}
			engine.setName(TOMCAT_CATALINA_NAME);
			// this default host will *never* change
			engine.setDefaultHost("localhost");

			// <Server>/<Service>/<Engine>/<Host>
			if (engine.findChildren().length == 0 || engine.findChild("localhost") == null) {
				LOG.info("No \"localhost\" Host configured in Tomcat XML configuration. Creating one.");
				Host host = new StandardHost();
				host.setName("localhost");
				host.setAppBase(".");
				engine.addChild(host);
			}
			for (Container child : engine.findChildren()) {
				((ContainerBase) child).setStartChildren(false);
			}

			defaultHost = (Host) engine.findChild("localhost");
		}
	}

	/**
	 * External configuration may specify connectors but we may have to add default ones if they're missing
	 */
	private void verifyConnectorConfiguration() {
		boolean httpEnabled = configuration.server().isHttpEnabled();
		Integer httpPort = configuration.server().getHttpPort();

		boolean httpsEnabled = configuration.server().isHttpSecureEnabled();
		Integer httpsPort = configuration.server().getHttpSecurePort();

		String[] addresses = configuration.server().getListeningAddresses();

		// review connectors possibly configured from external tomcat-server.xml and check if they match configadmin configuration
		for (String address : addresses) {
			verifyConnector(address, httpPort, httpEnabled, false,
					() -> tomcatFactory.createDefaultConnector(server, address, serverExecutor, configuration));

			verifyConnector(address, httpsPort, httpsEnabled, true,
					() -> tomcatFactory.createSecureConnector(server, address, serverExecutor, configuration));
		}
	}

	/**
	 * Verify if current server configuration, possibly created from external {@code tomcat.xml} matches the
	 * declaration from PID ({@link org.ops4j.pax.web.service.PaxWebConfig#PID_CFG_HTTP_ENABLED} and
	 * {@link org.ops4j.pax.web.service.PaxWebConfig#PID_CFG_HTTP_SECURE_ENABLED}).
	 *
	 * @param address
	 * @param port
	 * @param enabled
	 * @param secure
	 * @param connectorProvider {@link Supplier} used if connector has to be added to match PID configuration
	 */
	private void verifyConnector(String address, Integer port,
			boolean enabled, boolean secure, Supplier<Connector> connectorProvider) {
		Connector expectedConnector = null;

		boolean connectorFound = false;
		Connector backupConnector = null;

		Service service = server.findService(TOMCAT_CATALINA_NAME);
		Connector[] currentConnectors = service.findConnectors();
		if (currentConnectors == null) {
			currentConnectors = new Connector[0];
		}

		for (Connector connector : currentConnectors) {
			if ("org.apache.coyote.http11.Http11Nio2Protocol".equals(connector.getProtocolHandlerClassName())) {
				if (match(address, port, connector)) {
					if (connector.getSecure() == secure) {
						expectedConnector = connector;
						connectorFound = true;
					}
				} else {
					backupConnector = connector;
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
					if (connector.getSecure() == secure) {
						LOG.warn("Connector defined in external configuration will be removed, "
								+ "because it's not enabled: {}", connector);
						service.removeConnector(connector);
					}
				}
			}
		} else if (enabled) {
			LOG.info("Creating {} connector for address {}:{}", (secure ? "secure" : "non secure"), address, port);
			// we have to create a connector
			Connector connector = connectorProvider.get();
			service.addConnector(connector);
		}
	}

	/**
	 * Check if the passed {@link Connector} can be treated as one <em>matching</em> the connector
	 * declared using PID properties.
	 *
	 * @param address
	 * @param port
	 * @param connector
	 * @return
	 */
	private boolean match(String address, Integer port, Connector connector) {
		InetAddress address2 = (InetAddress) connector.getProperty("address");
		int port2 = connector.getPort();

		InetSocketAddress isa1 = address != null ? new InetSocketAddress(address, port)
				: new InetSocketAddress(port);
		InetSocketAddress isa2 = address2 != null ? new InetSocketAddress(address2, port2)
				: new InetSocketAddress(port2);

		return isa1.equals(isa2);
	}

	/**
	 * Configure request logging (AKA <em>NCSA logging</em>) for Tomcat, using configuration properties.
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

		AccessLogValve ncsaLogger = /*lc.isLogNCSAExtended() ? new ExtendedAccessLogValve() : */new AccessLogValve();
		ncsaLogger.setPattern(lc.isLogNCSAExtended() ? "combined" : "common");

		// org.apache.catalina.valves.AccessLogValve.getLogFile
		ncsaLogger.setDirectory(new File(lc.getLogNCSADirectory()).getAbsolutePath());
		ncsaLogger.setPrefix(lc.getLogNCSAFile());
		ncsaLogger.setFileDateFormat("." + lc.getLogNCSAFilenameDateFormat());
		ncsaLogger.setSuffix(".log");
		ncsaLogger.setBuffered(lc.getLogNCSABuffered());

		AccessLogAdapter adapter = null;
		Valve[] valves = engine.getPipeline().getValves();
		for (Valve valve : valves) {
			if (valve instanceof AccessLog) {
				if (adapter == null) {
					adapter = new AccessLogAdapter((AccessLog) valve);
				} else {
					adapter.add((AccessLog) valve);
				}
				engine.getPipeline().removeValve(valve);
			}
		}

		if (adapter != null) {
			adapter.add(ncsaLogger);
			// not adding our NCSA Logger - assuming one is configured with tomcat-server.xml
//			engine.getPipeline().addValve(adapter);
		} else {
			engine.getPipeline().addValve(ncsaLogger);
		}

		LOG.info("NCSARequestlogging is using directory {}", lc.getLogNCSADirectory());
	}

	/**
	 * Simply start Tomcat server
	 * @throws Exception
	 */
	public void start() throws Exception {
		LOG.info("Starting {}", server);

		long t1 = System.currentTimeMillis();
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			TomcatURLStreamHandlerFactory.disable();
			server.start();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
		long t2 = System.currentTimeMillis();
		LOG.info("Tomcat server started in " + (t2 - t1) + " ms");
	}

	public void stop() {
		try {
			LOG.info("Stopping {}", server);
			server.stop();

			LOG.info("Destroying Tomcat server {}", server);
			server.destroy();
		} catch (final Throwable e) {
			LOG.error("Problem stopping Tomcat server {}", e.getMessage(), e);
		}
	}

	/**
	 * If state allows, this methods returns currently configured/started addresses of the listeners.
	 * @param useLocalPort
	 * @return
	 */
	public InetSocketAddress[] getAddresses(boolean useLocalPort) {
		Service service = server.findService(TOMCAT_CATALINA_NAME);
		if (service == null) {
			return null;
		}
		Connector[] currentConnectors = service.findConnectors();
		if (currentConnectors == null) {
			currentConnectors = new Connector[0];
		}
		final List<InetSocketAddress> result = new ArrayList<>(currentConnectors.length);
		for (Connector connector : currentConnectors) {
			InetAddress address = (InetAddress) connector.getProperty("address");
			int port = useLocalPort ? connector.getLocalPort() : connector.getPort();
			if (address == null) {
				result.add(new InetSocketAddress(port));
			} else {
				result.add(new InetSocketAddress(address, port));
			}
		}

		return result.toArray(new InetSocketAddress[0]);
	}

	// --- visitor methods for model changes

	@Override
	public void visit(TransactionStateChange change) {
		String contextPath = change.getContextPath();
		if (change.getKind() == OpCode.ASSOCIATE) {
			if (!transactions.add(contextPath)) {
				throw new IllegalStateException("Context path " + contextPath
						+ " is already associated with config transaction");
			}
		} else if (change.getKind() == OpCode.DISASSOCIATE) {
			if (!transactions.remove(contextPath)) {
				throw new IllegalStateException("Context path " + contextPath
						+ " is not associated with any config transaction");
			} else if (contextHandlers.containsKey(contextPath)) {
				// end of transaction - start the context
				ensureServletContextStarted(contextHandlers.get(contextPath));
			}
		}
	}

	@Override
	public void visit(ServletContextModelChange change) {
		ServletContextModel model = change.getServletContextModel();
		String contextPath = model.getContextPath();

		if (change.getKind() == OpCode.ADD) {
			LOG.info("Creating new Tomcat context for {}", model);

//							Context ctx = new HttpServiceContext(getHost(), accessControllerContext);
			PaxWebStandardContext context = new PaxWebStandardContext(default404Servlet);
			context.setName(model.getId());
			context.setPath("/".equals(contextPath) ? "" : contextPath);
			// TODO: this should really be context specific
			context.setWorkDir(configuration.server().getTemporaryDirectory().getAbsolutePath());

			// in this new context, we need "initial OSGi filter" which will:
			// - call preprocessors
			// - handle security using proper httpContext/servletContextHelper
			// - proceed with the chain that includes filters and target servlet.
			//   the filters should match target servlet's OsgiServletContext
			context.createInitialOsgiFilter();

			// same as Jetty's org.eclipse.jetty.server.handler.ContextHandler.setAllowNullPathInfo(false)
			// to enable redirect from /context-path to /context-path/
			context.setMapperContextRootRedirectEnabled(true);

			// mime mappings from Tomcat's web.xml
			// TODO: I think we should parse it only in pax-web-extender-war and have a default mapping here only
			WebXml webXml = new WebXml();
			WebXmlParser webXmlParser = new WebXmlParser(true, false, true);
			try {
				// default-web.xml is copied from official Tomcat distro and packaged into pax-web-spi
				URL internalXml = OsgiContextModel.class.getResource("/org/ops4j/pax/web/service/spi/model/default-web.xml");
				if (internalXml != null) {
					// resource from pax-web-tomcat bundle added using maven-dependency-plugin - parsed only
					// to get the MIME mappings
					webXmlParser.parseWebXml(internalXml, webXml, false);
				} else {
					// unit test/IDE case - no URL available
					webXml.addMimeMapping("txt", "text/plain");
				}
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}

			webXml.getMimeMappings().forEach(context::addMimeMapping);

			// TODO: handle virtual hosts here. Context should be added to all declared virtual hosts.
			//       Remember - it's much harder in Tomcat than in Jetty and Undertow
			defaultHost.addChild(context);

			// session configuration - based on defaultSessionConfiguration, but may be later overriden in OsgiContext
			SessionConfiguration sc = configuration.session();
			context.setSessionTimeout(sc.getSessionTimeout());
			context.setSessionCookieName(defaultSessionCookieConfig.getName());
			context.setSessionCookieDomain(defaultSessionCookieConfig.getDomain());
			// will default to context path if null
			context.setSessionCookiePath(defaultSessionCookieConfig.getPath());
			context.setUseHttpOnly(defaultSessionCookieConfig.isHttpOnly());
			// false, because that configures the behavior to be the same in Jetty, Tomcat and Undertow
			context.setSessionCookiePathUsesTrailingSlash(false);
			context.setValidateClientProvidedNewSessionId(true);

			if (sc.getSessionStoreDirectory() != null) {
				StandardManager manager = new StandardManager();
				manager.setPathname(new File(sc.getSessionStoreDirectory(), "SESSIONS.ser").getAbsolutePath());
				context.setManager(manager);
			}

			//		// TODO: what about the AccessControlContext?
			//		// TODO: the virtual host section below
			//		// TODO: what about the VirtualHosts?
			//		// TODO: what about the tomcat-web.xml config?
			//		// TODO: connectors are needed for virtual host?
			//
			//		final Context context = server.addContext(
			//				contextModel.getContextParams(),
			//				getContextAttributes(bundleContext),
			//				contextModel.getContextName(), contextModel.getHttpContext(),
			//				contextModel.getAccessControllerContext(),
			//				contextModel.getContainerInitializers(),
			//				contextModel.getJettyWebXmlURL(),
			//				contextModel.getVirtualHosts(), null /*contextModel.getConnectors() */,
			//				server.getBasedir());
			//
			//		// support default context.xml in configurationDir or config fragment
			//		URL defaultContextUrl = getDefaultContextXml();
			//		// support MTA-INF/context.xml in war
			//		URL configFile = bundle.getEntry(org.apache.catalina.startup.Constants.ApplicationContextXml);
			//		if (defaultContextUrl != null || configFile != null) {
			//			Digester digester = createContextDigester();
			//			if (defaultContextUrl != null) {
			//				processContextConfig(context, digester, defaultContextUrl);
			//			}
			//			if (configFile != null) {
			//				context.setConfigFile(configFile);
			//				processContextConfig(context, digester, configFile);
			//			}
			//		}

			// explicit no check for existing mapping under given physical context path
			contextHandlers.put(contextPath, context);
			osgiContextModels.put(contextPath, new TreeSet<>());

			// configure ordered map of initializers
			initializers.put(contextPath, new LinkedHashMap<>());
			dynamicRegistrations.put(contextPath, new DynamicRegistrations());
		} else if (change.getKind() == OpCode.DELETE) {
			dynamicRegistrations.remove(contextPath);
			initializers.remove(contextPath);
			osgiContextModels.remove(contextPath);
			PaxWebStandardContext context = contextHandlers.remove(contextPath);

			// Note: for WAB deployments, this is the last operation of the undeployment batch and all web element
			// removals are delayed until this step.
			// This is important to ensure proper order of destruction ended with contextDestroyed() calls

			if (isStarted(context)) {
				LOG.info("Stopping Tomcat context \"{}\"", contextPath);
				try {
					context.stop();
				} catch (Exception e) {
					LOG.warn("Error stopping Tomcat context \"{}\": {}", contextPath, e.getMessage(), e);
				}
			}

			defaultHost.removeChild(context);
		}
	}

	@Override
	public void visit(OsgiContextModelChange change) {
		if (change.getKind() == OpCode.ASSOCIATE || change.getKind() == OpCode.DISASSOCIATE) {
			return;
		}

		OsgiContextModel osgiModel = change.getOsgiContextModel();
		ServletContextModel servletContextModel = change.getServletContextModel();

		String contextPath = osgiModel.getContextPath();
		PaxWebStandardContext realContext = contextHandlers.get(contextPath);

		if (realContext == null) {
			throw new IllegalStateException(osgiModel + " refers to unknown ServletContext for path " + contextPath);
		}

		if (change.getKind() == OpCode.ADD) {
			LOG.info("Adding {} to {}", osgiModel, realContext);

			// as with Jetty (JettyServerWrapper.visit(task.OsgiContextModelChange),
			// each unique OsgiServletContext (ServletContextHelper or HttpContext) is a facade for some, sometimes
			// shared by many osgi contexts, real ServletContext
			if (osgiServletContexts.containsKey(osgiModel)) {
				throw new IllegalStateException(osgiModel + " is already registered");
			}

			// this (and similar Jetty and Undertow places) should be the only place where
			// org.ops4j.pax.web.service.spi.servlet.OsgiServletContext is created and we have everything ready
			// to create proper classloader for this OsgiServletContext
			ClassLoader classLoader = null;
			if (osgiModel.getClassLoader() != null) {
				// WAB scenario - the classloader was already prepared earlier when the WAB was processed..
				// The classloader already includes several reachable bundles
				classLoader = osgiModel.getClassLoader();
			}
			if (paxWebTomcatBundle != null) {
				// it may not be the case in Test scenario
				OsgiServletContextClassLoader loader = classLoader != null
						? (OsgiServletContextClassLoader) classLoader : new OsgiServletContextClassLoader();
				loader.addBundle(osgiModel.getOwnerBundle());
				loader.addBundle(paxWebTomcatBundle);
				loader.addBundle(Utils.getPaxWebJspBundle(paxWebTomcatBundle));
				loader.makeImmutable();
				classLoader = loader;
			} else if (classLoader == null) {
				classLoader = this.classLoader;
			}
			OsgiServletContext osgiContext = new OsgiServletContext(realContext.getServletContext(), osgiModel, servletContextModel,
					defaultSessionCookieConfig, classLoader);
			osgiServletContexts.put(osgiModel, osgiContext);
			osgiContextModels.get(contextPath).add(osgiModel);

			configurationListeners.put(osgiModel, new OsgiContextConfiguration(osgiModel, configuration));
		}

		if (change.getKind() == OpCode.DELETE) {
			LOG.info("Removing {} from {}", osgiModel, realContext);

			// TOCHECK: are there web elements associated with removed mapping for OsgiServletContext?
			OsgiServletContext removedOsgiServletContext = osgiServletContexts.remove(osgiModel);
			osgiContextModels.get(contextPath).remove(osgiModel);

			removedOsgiServletContext.unregister();

			configurationListeners.remove(osgiModel);
		}

		// there may be a change in what's the "best" (highest ranked) OsgiContextModel for given
		// ServletContextModel. This will became the "fallback" OsgiContextModel for chains without
		// target servlet (with filters only)
		OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
		if (highestRankedModel != null) {
			OsgiServletContext highestRankedContext = osgiServletContexts.get(highestRankedModel);
			realContext.setDefaultOsgiContextModel(highestRankedModel);
			realContext.setDefaultServletContext(highestRankedContext);

			// we have to ensure that non-highest ranked contexts are unregistered
			osgiServletContexts.forEach((ocm, osc) -> {
				if (ocm.getContextPath().equals(contextPath) && osc != highestRankedContext) {
					osc.unregister();
				}
			});

			// and the highest ranked context should be registered as OSGi service (if it wasn't registered)
			highestRankedContext.register();
		} else {
			// TOCHECK: there should be no more web elements in the context, no OSGi mechanisms, just 404 all the time
			realContext.setDefaultOsgiContextModel(null);
			realContext.setDefaultServletContext(null);
		}
	}

	@Override
	public void visit(ContextMetadataModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			OsgiContextModel ocm = change.getOsgiContextModel();
			ContextMetadataModel meta = change.getMetadata();

			String contextPath = ocm.getContextPath();
			PaxWebStandardContext context = contextHandlers.get(contextPath);

			if (context == null) {
				throw new IllegalStateException(ocm + " refers to unknown ServletContext for path " + contextPath);
			}

			OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
			if (ocm == highestRankedModel) {
				LOG.info("Configuring metadata of {}", ocm);

				// only in this case we'll configure the metadata
				context.setEffectiveMajorVersion(meta.getMajorVersion());
				context.setEffectiveMinorVersion(meta.getMinorVersion());
				context.setDisplayName(meta.getDisplayName());
				context.setDistributable(meta.getDistributable());
				context.setIgnoreAnnotations(meta.isMetadataComplete());
				context.setPublicId(meta.getPublicId());

				context.setRequestCharacterEncoding(meta.getRequestCharacterEncoding());
				context.setResponseCharacterEncoding(meta.getResponseCharacterEncoding());

				context.setDenyUncoveredHttpMethods(meta.isDenyUncoveredHttpMethods());
			}
		}
	}

	@Override
	public void visit(MimeAndLocaleMappingChange change) {
		if (change.getKind() == OpCode.ADD) {
			OsgiContextModel ocm = change.getOsgiContextModel();

			String contextPath = ocm.getContextPath();
			PaxWebStandardContext context = contextHandlers.get(contextPath);

			if (context == null) {
				throw new IllegalStateException(ocm + " refers to unknown ServletContext for path " + contextPath);
			}

			OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
			if (ocm == highestRankedModel) {
				LOG.info("Configuring MIME and Locale Encoding mapping of {}", ocm);

				change.getMimeMapping().forEach(context::addMimeMapping);
				change.getLocaleEncodingMapping().forEach(context::addLocaleEncodingMappingParameter);
			}
		}
	}

	@Override
	public void visit(ServletModelChange change) {
		// org.apache.catalina.core.StandardContext.addChild() accepts only instances of org.apache.catalina.Wrapper
		// - org.apache.catalina.core.ContainerBase.addChildInternal() fires org.apache.catalina.Container.ADD_CHILD_EVENT
		// - org.apache.catalina.mapper.Mapper.addWrappers() is called for mapper
		// - mapper is kept in org.apache.catalina.core.StandardService.mapper

		if ((change.getKind() == OpCode.ADD && !change.isDisabled()) || change.getKind() == OpCode.ENABLE) {
			ServletModel model = change.getServletModel();
			if (change.getNewModelsInfo() == null) {
				LOG.info("Adding servlet {}", model);
			} else {
				LOG.info("Adding servlet {} to new contexts {}", model, change.getNewModelsInfo());
			}

			// see implementation requirements in Jetty version of this visit() method

			Set<String> done = new HashSet<>();

			change.getContextModels().forEach(osgiContext -> {
				String contextPath = osgiContext.getContextPath();
				if (!done.add(contextPath)) {
					return;
				}

				LOG.debug("Adding servlet {} to {}", model.getName(), contextPath);

				// there should already be a context for this path
				PaxWebStandardContext realContext = contextHandlers.get(contextPath);

				// <servlet> - always associated with one of ServletModel's OsgiContextModels
				OsgiServletContext context = osgiServletContexts.get(osgiContext);
				PaxWebStandardWrapper wrapper = new PaxWebStandardWrapper(model, osgiContext, context, realContext);

				boolean isDefaultResourceServlet = model.isResourceServlet();
				for (String pattern : model.getUrlPatterns()) {
					isDefaultResourceServlet &= "/".equals(pattern);
				}
				if (model.isResourceServlet()) {
					wrapper.addInitParameter("pathInfoOnly", Boolean.toString(!isDefaultResourceServlet));
				}

				realContext.addChild(wrapper);

				// <servlet-mapping>
				String name = model.getName();
				for (String pattern : model.getUrlPatterns()) {
					realContext.addServletMappingDecoded(pattern, name, false);
				}

				// are there any error page declarations in the model?
				ErrorPageModel epm = model.getErrorPageModel();
				if (epm != null) {
					String location = epm.getLocation();
					for (String ex : epm.getExceptionClassNames()) {
						ErrorPage errorPage = new ErrorPage();
						errorPage.setExceptionType(ex);
						errorPage.setLocation(location);
						realContext.addErrorPage(errorPage);
					}
					for (int code : epm.getErrorCodes()) {
						ErrorPage errorPage = new ErrorPage();
						errorPage.setErrorCode(code);
						errorPage.setLocation(location);
						realContext.addErrorPage(errorPage);
					}
					if (epm.isXx4()) {
						for (int c = 400; c < 500; c++) {
							ErrorPage errorPage = new ErrorPage();
							errorPage.setErrorCode(c);
							errorPage.setLocation(location);
							realContext.addErrorPage(errorPage);
						}
					}
					if (epm.isXx5()) {
						for (int c = 500; c < 600; c++) {
							ErrorPage errorPage = new ErrorPage();
							errorPage.setErrorCode(c);
							errorPage.setLocation(location);
							realContext.addErrorPage(errorPage);
						}
					}
				}

				ensureServletContextStarted(realContext);
			});
			return;
		}

		if (change.getKind() == OpCode.DISABLE || change.getKind() == OpCode.DELETE) {
			for (Map.Entry<ServletModel, Boolean> entry : change.getServletModels().entrySet()) {
				ServletModel model = entry.getKey();
				if (!entry.getValue()) {
					continue;
				}

				Set<String> done = new HashSet<>();

				// proper order ensures that (assuming above scenario), for /c1, ocm2 will be chosen and ocm1 skipped
				model.getContextModels().forEach(osgiContextModel -> {
					String contextPath = osgiContextModel.getContextPath();
					if (!done.add(contextPath)) {
						return;
					}

					if (pendingTransaction(contextPath)) {
						LOG.debug("Delaying removal of servlet {}", model);
						return;
					}

					LOG.info("Removing servlet {}", model);
					LOG.debug("Removing servlet {} from context {}", model.getName(), contextPath);

					// there should already be a ServletContextHandler
					Context realContext = contextHandlers.get(contextPath);

					Container child = realContext.findChild(model.getName());
					if (child != null) {
						realContext.removeChild(child);
						for (String pattern : model.getUrlPatterns()) {
							realContext.removeServletMapping(pattern);
						}
					}

					// are there any error page declarations in the model?
					ErrorPageModel epm = model.getErrorPageModel();
					if (epm != null) {
						for (ErrorPage ep : realContext.findErrorPages()) {
							if (ep.getExceptionType() != null && epm.getExceptionClassNames().contains(ep.getExceptionType())) {
								realContext.removeErrorPage(ep);
							}
							if (ep.getErrorCode() > 0) {
								if (epm.getErrorCodes().contains(ep.getErrorCode())
										|| (epm.isXx4() && ep.getErrorCode() >= 400 && ep.getErrorCode() < 500)
										|| (epm.isXx5() && ep.getErrorCode() >= 500 && ep.getErrorCode() < 600)) {
									realContext.removeErrorPage(ep);
								}
							}
						}
					}
				});
			}
		}
	}

	@Override
	public void visit(FilterModelChange change) {
		// only handle dynamic filter registration here - filter added only as last filter
		FilterModel model = change.getFilterModel();
		Set<String> done = new HashSet<>();
		if (change.getKind() == OpCode.ADD && model.isDynamic()) {
			for (OsgiContextModel ocm : change.getContextModels()) {
				String contextPath = ocm.getContextPath();
				if (!done.add(contextPath)) {
					continue;
				}

				LOG.info("Adding dynamic filter to context {}", contextPath);

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

				PaxWebStandardContext context = contextHandlers.get(contextPath);
				OsgiServletContext osgiContext = osgiServletContexts.get(highestRankedModel);

				context.addFilterDef(new PaxWebFilterDef(model, false, osgiContext));
				configureFilterMappings(model, context);
			}
		}
	}

	@Override
	public void visit(FilterStateChange change) {
		Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> contextFilters = change.getContextFilters();

		for (Map.Entry<String, TreeMap<FilterModel, List<OsgiContextModel>>> entry : contextFilters.entrySet()) {
			String contextPath = entry.getKey();
			Map<FilterModel, List<OsgiContextModel>> filtersMap = entry.getValue();
			Set<FilterModel> filters = filtersMap.keySet();

			LOG.info("Changing filter configuration for context {}", contextPath);

			PaxWebStandardContext context = contextHandlers.get(contextPath);

			ensureServletContextStarted(context);

			// see implementation requirements in Jetty version of this visit() method
			// here in Tomcat we have to remember about "initial OSGi filter"

			FilterDef[] filterDefs = context.findFilterDefs();
			FilterMap[] filterMaps = context.findFilterMaps();

			// 2020-06-02: it's not possible to simply add a filter to Tomcat and init() it without init()ing
			// existing filters
//			if (true || !quickFilterChange(context, filterDefs, filters, contextPath)) {
			// the hard way - recreate entire array of filters/filter-mappings
			context.filterStop();
			// remove all but "initial OSGi filter"
			for (FilterDef def : filterDefs) {
				if (!(def instanceof PaxWebFilterDef && ((PaxWebFilterDef) def).isInitial())) {
					context.removeFilterDef(def);
				}
			}
			for (FilterMap map : filterMaps) {
				if (!(map instanceof PaxWebFilterMap && ((PaxWebFilterMap) map).isInitial())) {
					context.removeFilterMap(map);
				}
			}

			for (FilterModel model : filters) {
				List<OsgiContextModel> contextModels = filtersMap.get(model) != null
						? filtersMap.get(model) : model.getContextModels();
				OsgiServletContext osgiContext = getHighestRankedContext(contextPath, model, contextModels);

				context.addFilterDef(new PaxWebFilterDef(model, false, osgiContext));
				configureFilterMappings(model, context);
			}

			if (isStarted(context) && !pendingTransaction(contextPath)) {
				context.filterStart();
			}
//			}
		}
	}

	@Override
	public void visit(EventListenerModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			EventListenerModel eventListenerModel = change.getEventListenerModel();
			List<OsgiContextModel> contextModels = change.getContextModels();
			Set<String> done = new HashSet<>();
			contextModels.forEach((context) -> {
				String contextPath = context.getContextPath();
				if (!done.add(contextPath)) {
					return;
				}

				PaxWebStandardContext standardContext = contextHandlers.get(contextPath);
				EventListener eventListener = eventListenerModel.resolveEventListener();
				if (eventListener instanceof ServletContextAttributeListener) {
					// add it to accessible list to fire per-OsgiContext attribute changes
					OsgiServletContext c = osgiServletContexts.get(context);
					c.addServletContextAttributeListener((ServletContextAttributeListener)eventListener);
				}

				// add the listener to real context - even ServletContextAttributeListener (but only once - even
				// if there are many OsgiServletContexts per ServletContext)
				if (eventListener instanceof HttpSessionListener || eventListener instanceof ServletContextListener) {
					standardContext.addApplicationLifecycleListener(eventListener);
				} else {
					standardContext.addApplicationEventListener(eventListener);
				}
			});
		}

		if (change.getKind() == OpCode.DELETE) {
			List<EventListenerModel> eventListenerModels = change.getEventListenerModels();
			for (EventListenerModel eventListenerModel : eventListenerModels) {
				List<OsgiContextModel> contextModels = eventListenerModel.getContextModels();
				contextModels.forEach((context) -> {
					PaxWebStandardContext standardContext = contextHandlers.get(context.getContextPath());
					EventListener eventListener = eventListenerModel.resolveEventListener();
					if (eventListener instanceof ServletContextAttributeListener) {
						// remove it from per-OsgiContext list
						OsgiServletContext c = osgiServletContexts.get(context);
						if (c != null) {
							c.removeServletContextAttributeListener((ServletContextAttributeListener)eventListener);
						}
					}

					if (pendingTransaction(context.getContextPath())) {
						LOG.debug("Delaying removal of event listener {}", eventListenerModel);
						return;
					}

					// remove the listener from real context - even ServletContextAttributeListener
					if (standardContext != null) {
						// this may be null in case of WAB where we keep event listeners so they get contextDestroyed
						// event properly
						Object[] elisteners = standardContext.getApplicationEventListeners();
						Object[] llisteners = standardContext.getApplicationLifecycleListeners();
						List<Object> newEListeners = new ArrayList<>();
						List<Object> newLListeners = new ArrayList<>();
						for (Object l : elisteners) {
							if (l != eventListener) {
								newEListeners.add(l);
							}
						}
						for (Object l : llisteners) {
							if (l != eventListener) {
								newLListeners.add(l);
							}
						}
						standardContext.setApplicationEventListeners(newEListeners.toArray(new Object[0]));
						standardContext.setApplicationLifecycleListeners(newLListeners.toArray(new Object[0]));
					}
//					eventListenerModel.ungetEventListener(eventListener);
				});
			}
		}
	}

	@Override
	public void visit(WelcomeFileModelChange change) {
		WelcomeFileModel model = change.getWelcomeFileModel();

		OpCode op = change.getKind();
		if (op == OpCode.ADD || op == OpCode.DELETE) {
			List<OsgiContextModel> contextModels = op == OpCode.ADD ? change.getContextModels()
					: model.getContextModels();
			contextModels.forEach((context) -> {
				OsgiServletContext osgiServletContext = osgiServletContexts.get(context);
				PaxWebStandardContext realContext = contextHandlers.get(context.getContextPath());

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

				LOG.info("Reconfiguration of welcome files for all resource servlet in context \"{}\"", context);

				// reconfigure welcome files in resource servlets
				for (Container child : realContext.findChildren()) {
					if (child instanceof PaxWebStandardWrapper) {
						ServletModel servletModel = ((PaxWebStandardWrapper) child).getServletModel();
						if (servletModel != null && servletModel.isResourceServlet()
								&& context == ((PaxWebStandardWrapper) child).getOsgiContextModel()) {
							Servlet servlet = ((PaxWebStandardWrapper) child).getServlet();
							if (servlet instanceof TomcatResourceServlet) {
								((TomcatResourceServlet) servlet).setWelcomeFiles(newWelcomeFiles);
								((TomcatResourceServlet) servlet).setWelcomeFilesRedirect(model.isRedirect());
							} else if (servlet instanceof OsgiInitializedServlet) {
								((TomcatResourceServlet) ((OsgiInitializedServlet) servlet).getDelegate()).setWelcomeFiles(newWelcomeFiles);
								((TomcatResourceServlet) ((OsgiInitializedServlet) servlet).getDelegate()).setWelcomeFilesRedirect(model.isRedirect());
							}
						}
					}
				}
			});
		}
	}

	@Override
	public void visit(ErrorPageModelChange change) {
		// no op here
	}

	@Override
	public void visit(ErrorPageStateChange change) {
		Map<String, TreeMap<ErrorPageModel, List<OsgiContextModel>>> contextErrorPages = change.getContextErrorPages();

		for (Map.Entry<String, TreeMap<ErrorPageModel, List<OsgiContextModel>>> entry : contextErrorPages.entrySet()) {
			String contextPath = entry.getKey();
			TreeMap<ErrorPageModel, List<OsgiContextModel>> errorPageModelsMap = entry.getValue();
			Set<ErrorPageModel> errorPageModels = errorPageModelsMap.keySet();

			LOG.info("Changing error page configuration for context {}", contextPath);

			PaxWebStandardContext context = contextHandlers.get(contextPath);
			for (ErrorPage ep : context.findErrorPages()) {
				context.removeErrorPage(ep);
			}

			for (ErrorPageModel model : errorPageModels) {
				String location = model.getLocation();
				for (String ex : model.getExceptionClassNames()) {
					ErrorPage errorPage = new ErrorPage();
					errorPage.setExceptionType(ex);
					errorPage.setLocation(location);
					context.addErrorPage(errorPage);
				}
				for (int code : model.getErrorCodes()) {
					ErrorPage errorPage = new ErrorPage();
					errorPage.setErrorCode(code);
					errorPage.setLocation(location);
					context.addErrorPage(errorPage);
				}
				if (model.isXx4()) {
					for (int c = 400; c < 500; c++) {
						ErrorPage errorPage = new ErrorPage();
						errorPage.setErrorCode(c);
						errorPage.setLocation(location);
						context.addErrorPage(errorPage);
					}
				}
				if (model.isXx5()) {
					for (int c = 500; c < 600; c++) {
						ErrorPage errorPage = new ErrorPage();
						errorPage.setErrorCode(c);
						errorPage.setLocation(location);
						context.addErrorPage(errorPage);
					}
				}
			}
		}
	}

	@Override
	public void visit(ContainerInitializerModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			ContainerInitializerModel model = change.getContainerInitializerModel();
			List<OsgiContextModel> contextModels = change.getContextModels();
			contextModels.forEach((context) -> {
				String path = context.getContextPath();
				PaxWebStandardContext ctx = contextHandlers.get(context.getContextPath());
				if (isStarted(ctx)) {
					LOG.warn("ServletContainerInitializer {} can't be added, as the context {} is already started",
							model.getContainerInitializer(), ctx);
				} else {
					// even if there's org.apache.catalina.core.StandardContext.addServletContainerInitializer(),
					// there's no "remove" equivalent and also we want to be able to pass correct implementation
					// of ServletContext there
					DynamicRegistrations registrations = this.dynamicRegistrations.get(path);
					OsgiDynamicServletContext dynamicContext = new OsgiDynamicServletContext(osgiServletContexts.get(context), registrations);
					SCIWrapper wrapper = new SCIWrapper(dynamicContext, model);
					initializers.get(path).put(System.identityHashCode(model.getContainerInitializer()), wrapper);
				}
			});
		}

		if (change.getKind() == OpCode.DELETE) {
			List<ContainerInitializerModel> models = change.getContainerInitializerModels();
			for (ContainerInitializerModel model : models) {
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

	/**
	 * <p>Registration of <em>active web element</em> should always start the context. On the other hand,
	 * registration of <em>passive web element</em> should <strong>not</strong> start the context.</p>
	 *
	 * <p>This method is always (should be) called withing the "configuration thread" of Pax Web Runtime, because
	 * it's called in visit() methods for servlets (including resources) and filters, so we can safely access
	 * {@link org.ops4j.pax.web.service.spi.model.ServerModel}.</p>
	 * @param context
	 */
	private void ensureServletContextStarted(PaxWebStandardContext context) {
		String contextPath = context.getPath().equals("") ? "/" : context.getPath();
		if (isStarted(context) || pendingTransaction(contextPath)) {
			return;
		}
		try {
			OsgiContextModel highestRanked = context.getDefaultOsgiContextModel();
			OsgiServletContext highestRankedContext = context.getDefaultServletContext();

			LOG.info("Starting Tomcat context \"{}\" with default Osgi Context {}", context, highestRanked);

			// first thing - only NOW we can set ServletContext's class loader! It affects many things, including
			// the TCCL used for example by javax.el.ExpressionFactory.newInstance()

			// org.apache.catalina.core.ContainerBase.setParentClassLoader() sets the parent that
			// will be used as parent of org.apache.catalina.loader.ParallelWebappClassLoader returned
			// (by default) from org.apache.catalina.core.ApplicationContext.getClassLoader(), which
			// internally calls org.apache.catalina.core.StandardContext.getLoader().getClassLoader()
			// here's everything done manually/explicitly
			WebappLoader tomcatLoader = new WebappLoader();
			tomcatLoader.setLoaderInstance(new ParallelWebappClassLoader(highestRankedContext.getClassLoader()) {
				@Override
				protected void clearReferences() {
					// skip, because we're managing "deployments" differently
				}
			});
			context.setParentClassLoader(highestRankedContext.getClassLoader());
			context.setLoader(tomcatLoader);
			context.setOsgiServletContext(highestRankedContext);

			Collection<SCIWrapper> initializers = new LinkedList<>(this.initializers.get(contextPath).values());
			// Initially I thought we should take only these SCIs, which are associated with highest ranked OCM,
			// but it turned out that just as we take servlets registered to different OsgiContextModels, but
			// the same ServletContextModel, we have to do the same with SCIs.
			// otherwise, by default (with HttpService scenario), SCIs from the OsgiContextModel related to
			// pax-web-extender-whiteboard would be taken (probably 0), simply because this bundle is usually
			// the first that grabs an instance of bundle-scoped HttpService
			// so please do not uncomment and keep for educational purposes!
//			initializers.removeIf(w -> !w.getModel().getContextModels().contains(highestRanked));

			if (initializers.size() > 0) {
				initializers.add(new RegisteringContainerInitializer(this.dynamicRegistrations.get(contextPath)));
				context.setServletContainerInitializers(initializers);
			}

			// org.apache.catalina.startup.Tomcat.FixContextListener() sets context as configured and is
			// required for embedded usage. But it does a bit more than it should, so we'll do everything more
			// cleverly

			// remove existing OsgiContextConfiguration listeners
			for (LifecycleListener listener : context.findLifecycleListeners()) {
				if (listener instanceof OsgiContextConfiguration) {
					context.removeLifecycleListener(listener);
					Valve authenticationValve = ((OsgiContextConfiguration) listener).getAuthenticationValve();
					if (authenticationValve != null) {
						// remove the auth valve, because next time, an auth valve for different
						// OsgiContextModel may have to be used
						context.getPipeline().removeValve(authenticationValve);
					}
				}
			}

			// and add the one related to highest-ranked OsgiContextModel
			context.addLifecycleListener(this.configurationListeners.get(highestRanked));

			context.start();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private boolean pendingTransaction(String contextPath) {
		return transactions.contains(contextPath);
	}

	public boolean isStarted(PaxWebStandardContext context) {
		return context.getState() == LifecycleState.STARTED
						|| context.getState() == LifecycleState.STARTING
						|| context.getState() == LifecycleState.STARTING_PREP
						|| context.getState() == LifecycleState.INITIALIZING;
	}

	private OsgiServletContext getHighestRankedContext(String contextPath, FilterModel model,
			List<OsgiContextModel> contextModels) {
		OsgiContextModel highestRankedModel = null;
		// remember, this contextModels list is properly sorted - and it comes either from model or
		// (if configured) from associated list of models which are being changed in the model
		if (contextModels == null) {
			contextModels = model.getContextModels();
		}
		for (OsgiContextModel ocm : contextModels) {
			if (ocm.getContextPath().equals(contextPath)) {
				highestRankedModel = ocm;
				break;
			}
		}
		if (highestRankedModel == null) {
			LOG.warn("(dev) Can't find proper OsgiContextModel for the filter. Falling back to "
					+ "highest ranked OsgiContextModel for given ServletContextModel");
			highestRankedModel = osgiContextModels.get(contextPath).iterator().next();
		}

		return osgiServletContexts.get(highestRankedModel);
	}

	/**
	 * <p>As with Jetty, there's some chance that there's only one new filter to be added to existing set of filters
	 * <em>at the end of the list</em>. If it's not possible, filters have to be recreated (destroy + init)
	 * entirely.</p>
	 *
	 * <p>TODO: Not possible to initialize only the added filter...</p>
	 *
	 * <p>Here we have to test for the <em>initial filter</em>.</p>
	 *
	 * @param context
	 * @param existingFilterDefs
	 * @param filters
	 * @param contextPath
	 * @return
	 */
	private boolean quickFilterChange(PaxWebStandardContext context, FilterDef[] existingFilterDefs,
			Set<FilterModel> filters, String contextPath) {
		int pos = 0;
		FilterModel[] newModels = filters.toArray(new FilterModel[0]);
		boolean quick = newModels.length >= existingFilterDefs.length - 1;

		// by "quick" we mean - there are no removed filters and new filters come last
		while (quick) {
			if (pos >= existingFilterDefs.length - 1) {
				break;
			}
			if (!(existingFilterDefs[pos + 1] instanceof PaxWebFilterDef
					&& ((PaxWebFilterDef) existingFilterDefs[pos + 1]).getFilterModel().equals(newModels[pos]))) {
				quick = false;
				break;
			}
			pos++;
		}

		if (quick) {
			for (int i = pos; i < newModels.length; i++) {
				OsgiServletContext osgiContext = getHighestRankedContext(contextPath, newModels[pos], null);

				context.addFilterDef(new PaxWebFilterDef(newModels[pos], false, osgiContext));
				configureFilterMappings(newModels[pos], context);
			}
			context.filterStart();
			return true;
		}

		return false;
	}

	private void configureFilterMappings(FilterModel model, PaxWebStandardContext context) {
		if (model.getDynamicServletNames().size() > 0 || model.getDynamicUrlPatterns().size() > 0) {
			model.getDynamicServletNames().forEach(dm -> {
				if (!dm.isAfter()) {
					context.addFilterMap(new PaxWebFilterMap(model, dm));
				}
			});
			model.getDynamicUrlPatterns().forEach(dm -> {
				if (!dm.isAfter()) {
					context.addFilterMap(new PaxWebFilterMap(model, dm));
				}
			});
			model.getDynamicServletNames().forEach(dm -> {
				if (dm.isAfter()) {
					context.addFilterMap(new PaxWebFilterMap(model, dm));
				}
			});
			model.getDynamicUrlPatterns().forEach(dm -> {
				if (dm.isAfter()) {
					context.addFilterMap(new PaxWebFilterMap(model, dm));
				}
			});
		} else {
			// normal OSGi mapping
			for (FilterModel.Mapping map : model.getMappingsPerDispatcherTypes()) {
				context.addFilterMap(new PaxWebFilterMap(model, map));
			}
		}
	}

//	private URL getDefaultContextXml() {
//		// get the resource URL from the config fragment
//		URL defaultContextUrl = getClass().getResource("/context.xml");
//		// overwrite with context xml from configuration directory if it exists
//		File configurationFile = new File(server.getConfigurationDir(), "context.xml");
//		if (configurationFile.exists()) {
//			try {
//				defaultContextUrl = configurationFile.toURI().toURL();
//			} catch (MalformedURLException e) {
//				LOG.error("cannot access default context file", e);
//			}
//		}
//		return defaultContextUrl;
//	}
//
//    private void processContextConfig(Context context, Digester digester, URL contextXml) {
//
//        if (LOG.isDebugEnabled()) {
//        	LOG.debug("Processing context [" + context.getName()
//                    + "] configuration file [" + contextXml + "]");
//        }
//
//        InputSource source = null;
//        InputStream stream = null;
//
//        try {
//            source = new InputSource(contextXml.toString());
//            URLConnection xmlConn = contextXml.openConnection();
//            xmlConn.setUseCaches(false);
//            stream = xmlConn.getInputStream();
//        } catch (Exception e) {
//            LOG.error("Cannot read context file" , e);
//        }
//
//        if (source == null) {
//            return;
//        }
//
//        try {
//            source.setByteStream(stream);
//            digester.setClassLoader(this.getClass().getClassLoader());
//            digester.setUseContextClassLoader(false);
//            digester.push(context.getParent());
//            digester.push(context);
//            XmlErrorHandler errorHandler = new XmlErrorHandler();
//            digester.setErrorHandler(errorHandler);
//            digester.parse(source);
//            if (errorHandler.getWarnings().size() > 0 ||
//                    errorHandler.getErrors().size() > 0) {
//                for (SAXParseException e : errorHandler.getWarnings()) {
//                    LOG.warn("Warning in XML processing", e.getMessage(), source);
//                }
//                for (SAXParseException e : errorHandler.getErrors()) {
//                    LOG.warn("Error in XML processing", e.getMessage(), source);
//                }
//            }
//            if (LOG.isDebugEnabled()) {
//                LOG.debug("Successfully processed context [" + context.getName()
//                        + "] configuration file [" + contextXml + "]");
//            }
//        } catch (SAXParseException e) {
//            LOG.error("Cannot parse config file {}", context.getName(), e);
//            LOG.error("at {} {}",
//                             "" + e.getLineNumber(),
//                             "" + e.getColumnNumber());
//        } catch (Exception e) {
//        	LOG.error("Cannot parse context {}",
//                    context.getName(), e);
//        } finally {
//            try {
//                if (stream != null) {
//                    stream.close();
//                }
//            } catch (IOException e) {
//                LOG.error("Cannot close context configuration", e);
//            }
//        }
//    }

}
