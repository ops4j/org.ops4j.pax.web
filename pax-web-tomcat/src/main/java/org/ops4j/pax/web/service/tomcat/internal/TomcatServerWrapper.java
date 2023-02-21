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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpSessionAttributeListener;
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
import org.apache.catalina.util.ToStringUtil;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;
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
import org.ops4j.pax.web.service.spi.task.ContextParamsChange;
import org.ops4j.pax.web.service.spi.task.ContextStartChange;
import org.ops4j.pax.web.service.spi.task.ContextStopChange;
import org.ops4j.pax.web.service.spi.task.ErrorPageModelChange;
import org.ops4j.pax.web.service.spi.task.ErrorPageStateChange;
import org.ops4j.pax.web.service.spi.task.EventListenerModelChange;
import org.ops4j.pax.web.service.spi.task.FilterModelChange;
import org.ops4j.pax.web.service.spi.task.FilterStateChange;
import org.ops4j.pax.web.service.spi.task.MimeAndLocaleMappingChange;
import org.ops4j.pax.web.service.spi.task.OpCode;
import org.ops4j.pax.web.service.spi.task.OsgiContextModelChange;
import org.ops4j.pax.web.service.spi.task.SecurityConfigChange;
import org.ops4j.pax.web.service.spi.task.ServletContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletModelChange;
import org.ops4j.pax.web.service.spi.task.TransactionStateChange;
import org.ops4j.pax.web.service.spi.task.WebSocketModelChange;
import org.ops4j.pax.web.service.spi.task.WelcomeFileModelChange;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.tomcat.internal.web.TomcatResourceServlet;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * <p>A <em>wrapper</em> or <em>holder</em> of actual Tomcat server. This class perform two kinds of tasks:<ul>
 * <li>controls the state of Tomcat by configuring, starting and stopping it</li>
 * <li>translates model changes into registration of Tomcat-specific contexts, holders and handlers</li>
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

	/**
	 * An <em>entry</em> to OSGi runtime to lookup other bundles if needed (to get their ClassLoader)
	 */
	private final Bundle paxWebTomcatBundle;
	/**
	 * Outside of OSGi, let's use passed ClassLoader
	 */
	private final ClassLoader classLoader;

	/**
	 * Actual instance of {@link org.apache.catalina.core.StandardServer}. In Jetty we had extended class. Here
	 * we hold direct instance, because it is final.
	 */
	private StandardServer server;

	/**
	 * Tomcat's {@link Service}
	 */
	private StandardService service;

	/**
	 * Tomcat's {@link Engine}
	 */
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

	/**
	 * Server's thread pool
	 */
	private Executor serverExecutor;

	private final TomcatFactory tomcatFactory;

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

	/**
	 * Single map of context path to {@link Context} for fast access
	 */
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
	private final Map<String, TreeSet<SCIWrapper>> initializers = new HashMap<>();

	/**
	 * Keep dynamic configuration and use it during startup only.
	 */
	private final Map<String, DynamicRegistrations> dynamicRegistrations = new HashMap<>();

	/**
	 * Global {@link Configuration} passed from pax-web-runtime through
	 * {@link org.ops4j.pax.web.service.spi.ServerController}
	 */
	private final Configuration configuration;

	/**
	 * Servlet to use when no servlet is mapped - to ensure that preprocessors and filters are run correctly.
	 */
	private final Default404Servlet default404Servlet = new Default404Servlet();

	private SessionCookieConfig defaultSessionCookieConfig;

	/**
	 * All {@link EventListenerModel} instances for {@link HttpSessionAttributeListener} listeners. They'll be
	 * reviewed in order to propagate session attribute events per {@link OsgiContextModel}.
	 */
	private final List<EventListenerModel> sessionListenerModels = new ArrayList<>();

	private final Map<String, TreeMap<OsgiContextModel, SecurityConfigurationModel>> contextSecurityConstraints = new HashMap<>();

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

		// to load org.apache.catalina.util.ToStringUtil class - needed when stopping the bundle
		LOG.info("Created {}", ToStringUtil.toString(this, this.engine));
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
			Digester digester = tomcatFactory.createServerDigester(configuration);
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
			if ("org.apache.coyote.http11.Http11Nio2Protocol".equals(connector.getProtocolHandlerClassName())
					|| "org.apache.coyote.http11.Http11NioProtocol".equals(connector.getProtocolHandlerClassName())
					|| "org.ops4j.pax.web.service.tomcat.internal.PaxWebHttp11Nio2Protocol".equals(connector.getProtocolHandlerClassName())) {
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
				if (expectedConnector.getProperty("name") == null) {
					if (secure) {
						expectedConnector.setProperty("name", configuration.server().getHttpSecureConnectorName());
					} else {
						expectedConnector.setProperty("name", configuration.server().getHttpConnectorName());
					}
				}
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
	 *
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

			// I found this necessary, when pax-web-tomcat is restarted/refreshed without affecting
			// pax-web-extender-whiteboard
			osgiServletContexts.values().forEach(OsgiServletContext::unregister);
		} catch (final Throwable e) {
			LOG.error("Problem stopping Tomcat server {}", e.getMessage(), e);
		}
	}

	/**
	 * If state allows, this methods returns currently configured/started addresses of the listeners.
	 *
	 * @param useLocalPort
	 * @return
	 */
	public ServerEvent.Address[] getAddresses(boolean useLocalPort) {
		Service service = server.findService(TOMCAT_CATALINA_NAME);
		if (service == null) {
			return new ServerEvent.Address[0];
		}
		Connector[] currentConnectors = service.findConnectors();
		if (currentConnectors == null) {
			currentConnectors = new Connector[0];
		}
		final List<ServerEvent.Address> result = new ArrayList<>(currentConnectors.length);
		for (Connector connector : currentConnectors) {
			InetAddress address = (InetAddress) connector.getProperty("address");
			int port = useLocalPort ? connector.getLocalPort() : connector.getPort();
			if (address == null) {
				result.add(new ServerEvent.Address(new InetSocketAddress(port), connector.getSecure()));
			} else {
				result.add(new ServerEvent.Address(new InetSocketAddress(address, port), connector.getSecure()));
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
				if (toRemove != null && contextHandlers.containsKey(contextPath)) {
					for (Iterator<ElementModel<?, ?>> iterator = toRemove.iterator(); iterator.hasNext(); ) {
						ElementModel<?, ?> model = iterator.next();
						if (model instanceof ServletModel) {
							removeServletModel(contextPath, ((ServletModel) model));
						} else if (model instanceof EventListenerModel) {
							removeEventListenerModel(contextHandlers.get(contextPath), ((EventListenerModel) model),
									((EventListenerModel) model).getResolvedListener());
						}
						iterator.remove();
					}
				}
				delayedRemovals.remove(contextPath);
				if (contextHandlers.containsKey(contextPath)) {
					// end of transaction - start the context
					PaxWebStandardContext context = contextHandlers.get(contextPath);
					if (server.getState() == LifecycleState.STARTED) {
						ensureServletContextStarted(context);
					}
				}
			}
		}
	}

	@Override
	public void visitServletContextModelChange(ServletContextModelChange change) {
		ServletContextModel model = change.getServletContextModel();
		String contextPath = model.getContextPath();

		if (change.getKind() == OpCode.ADD) {
			if (contextHandlers.containsKey(contextPath)) {
				// quiet return, because of how ServletContextModels for WABs are created.
				return;
			}

			LOG.info("Creating new Tomcat context for {}", model);

//							Context ctx = new HttpServiceContext(getHost(), accessControllerContext);
			PaxWebStandardContext context = new PaxWebStandardContext(default404Servlet, new OsgiSessionAttributeListener(sessionListenerModels));
			context.setWhiteboardTCCL("whiteboard".equalsIgnoreCase(configuration.server().getTCCLType()));

			context.setPath("/".equals(contextPath) ? "" : contextPath);
			// name is used in final toString(), so better to have it clearer
			context.setName(contextPath);

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
			// #1727 - SameSite attribute handling - only from configuration (unset, none, lax, strict)
			Rfc6265CookieProcessor cookieProcessor = new Rfc6265CookieProcessor();
			String sameSiteValue = sc.getSessionCookieSameSite();
			if (sameSiteValue != null) {
				// in Tomcat, "unset" is real value
				cookieProcessor.setSameSiteCookies(sameSiteValue);
			} else {
				cookieProcessor.setSameSiteCookies(SameSiteCookies.UNSET.getValue());
			}
			context.setCookieProcessor(cookieProcessor);

			StandardManager manager = new PaxWebSessionManager();
			manager.setSessionIdGenerator(new PaxWebSessionIdGenerator());
			if (sc.getSessionStoreDirectory() != null) {
				manager.setPathname(new File(sc.getSessionStoreDirectory(), "SESSIONS.ser").getAbsolutePath());
			}
			context.setManager(manager);

			//		// TODO: what about the AccessControlContext?
			//
			//		final Context context = server.addContext(
			//				contextModel.getAccessControllerContext(),

			// explicit no check for existing mapping under given physical context path
			contextHandlers.put(contextPath, context);
			osgiContextModels.put(contextPath, new TreeSet<>());

			// configure ordered map of initializers
			initializers.put(contextPath, new TreeSet<>());
			dynamicRegistrations.put(contextPath, new DynamicRegistrations());
		} else if (change.getKind() == OpCode.DELETE) {
			OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
			if (highestRankedModel != null) {
				// there are still OsgiContextModel(s) for given ServletContextModel, so maybe after uninstallation
				// of a WAB, HttpService-based web apps have to stay running?
				return;
			}

			dynamicRegistrations.remove(contextPath);
			initializers.remove(contextPath);
			osgiContextModels.remove(contextPath);
			PaxWebStandardContext context = contextHandlers.remove(contextPath);

			// Note: for WAB deployments, this is the last operation of the undeployment batch and all web element
			// removals are delayed until this step.
			// This is important to ensure proper order of destruction ended with contextDestroyed() calls

			if (context != null && context.isStarted()) {
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
	public void visitOsgiContextModelChange(OsgiContextModelChange change) {
		if (change.getKind() == OpCode.ASSOCIATE || change.getKind() == OpCode.DISASSOCIATE) {
			return;
		}

		OsgiContextModel osgiModel = change.getOsgiContextModel();
		ServletContextModel servletContextModel = change.getServletContextModel();

		String contextPath = osgiModel.getContextPath();
		PaxWebStandardContext realContext = contextHandlers.get(contextPath);

		if (realContext == null) {
			if (change.getKind() == OpCode.DELETE) {
				return;
			}
			visitServletContextModelChange(new ServletContextModelChange(OpCode.ADD, new ServletContextModel(contextPath)));
			realContext = contextHandlers.get(contextPath);
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
				loader.addBundle(Utils.getTomcatWebSocketBundle(paxWebTomcatBundle));
				loader.makeImmutable();
				classLoader = loader;
			} else if (classLoader == null) {
				classLoader = this.classLoader;
			}
			OsgiServletContext osgiContext = new OsgiServletContext(realContext.getServletContext(), osgiModel, servletContextModel,
					defaultSessionCookieConfig, classLoader);

			// that's ideal place to set ServletContext.TEMPDIR attribute - it'll work for HttpService, Whiteboard and WAB
			File tmpLocation = new File(configuration.server().getTemporaryDirectory(), osgiModel.getTemporaryLocation());
			if (!tmpLocation.exists() && !tmpLocation.mkdirs()) {
				LOG.warn("Can't create temporary directory for {}: {}", osgiModel, tmpLocation.getAbsolutePath());
			}
			osgiModel.getInitialContextAttributes().put(ServletContext.TEMPDIR, tmpLocation);
			osgiContext.setAttribute(ServletContext.TEMPDIR, tmpLocation);

			osgiServletContexts.put(osgiModel, osgiContext);
			osgiContextModels.get(contextPath).add(osgiModel);

			configurationListeners.put(osgiModel, new OsgiContextConfiguration(osgiModel, configuration, tomcatFactory, contextSecurityConstraints));
		}

		boolean hasStopped = false;

		if (change.getKind() == OpCode.DELETE) {
			LOG.info("Removing {} from {}", osgiModel, realContext);

			OsgiServletContext removedOsgiServletContext = osgiServletContexts.remove(osgiModel);
			TreeSet<OsgiContextModel> models = osgiContextModels.get(contextPath);
			if (models != null) {
				models.remove(osgiModel);
			}

			if (removedOsgiServletContext != null) {
				removedOsgiServletContext.unregister();
				removedOsgiServletContext.releaseWebContainerContext();
			}

			OsgiServletContext currentHighestRankedContext = realContext.getDefaultServletContext();
			if (currentHighestRankedContext == removedOsgiServletContext || pendingTransaction(contextPath)) {
				// we have to stop the context - it'll be started later
				if (realContext.isStarted()) {
					LOG.info("Stopping Tomcat context \"{}\"", contextPath);
					try {
						if (realContext.isStarted()) {
							realContext.stop();
							hasStopped = true;
						}
					} catch (Exception e) {
						LOG.warn("Error stopping Tomcat context \"{}\": {}", contextPath, e.getMessage(), e);
					}
				}
				// clear the OSGi context + model - it'll be calculated to next higher ranked ones few lines later
				realContext.setDefaultOsgiContextModel(null, null);
				realContext.setDefaultServletContext(null);
			}

			configurationListeners.remove(osgiModel);
		}

		// there may be a change in what's the "best" (highest ranked) OsgiContextModel for given
		// ServletContextModel. This will became the "fallback" OsgiContextModel for chains without
		// target servlet (with filters only)
		OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
		if (highestRankedModel != null) {
			if (highestRankedModel != realContext.getDefaultOsgiContextModel()) {
				LOG.info("Changing default OSGi context model for " + realContext);
				OsgiServletContext highestRankedContext = osgiServletContexts.get(highestRankedModel);
				realContext.setDefaultOsgiContextModel(highestRankedModel, highestRankedContext.getResolvedWebContainerContext());
				realContext.setDefaultServletContext(highestRankedContext);

				// we have to ensure that non-highest ranked contexts are unregistered
				osgiServletContexts.forEach((ocm, osc) -> {
					if (ocm.getContextPath().equals(contextPath) && osc != highestRankedContext) {
						osc.unregister();
					}
				});

				if (!hasStopped && realContext.isStarted()) {
					// we have to stop the context - because its OsgiContextModel has changed, so we may
					// have to reconfigure e.g., virtual hosts
					LOG.info("Stopping Tomcat context \"{}\"", contextPath);
					try {
						if (realContext.isStarted()) {
							realContext.stop();
							hasStopped = true;
						}
					} catch (Exception e) {
						LOG.warn("Error stopping Tomcat context \"{}\": {}", contextPath, e.getMessage(), e);
					}
				}

				// and the highest ranked context should be registered as OSGi service (if it wasn't registered)
				highestRankedContext.register();
			}

			if (hasStopped) {
				if (pendingTransaction(contextPath)) {
					change.registerBatchCompletedAction(new ContextStartChange(OpCode.MODIFY, contextPath));
				} else {
					ensureServletContextStarted(realContext);
				}
			}
		} else {
			realContext.setDefaultOsgiContextModel(null, null);
			realContext.setDefaultServletContext(null);

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
	public void visitMimeAndLocaleMappingChange(MimeAndLocaleMappingChange change) {
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
	public void visitServletModelChange(ServletModelChange change) {
		// org.apache.catalina.core.StandardContext.addChild() accepts only instances of org.apache.catalina.Wrapper
		// - org.apache.catalina.core.ContainerBase.addChildInternal() fires org.apache.catalina.Container.ADD_CHILD_EVENT
		// - org.apache.catalina.mapper.Mapper.addWrappers() is called for mapper
		// - mapper is kept in org.apache.catalina.core.StandardService.mapper

		Set<String> done = new HashSet<>();

		if ((change.getKind() == OpCode.ADD && !change.isDisabled()) || change.getKind() == OpCode.ENABLE) {
			ServletModel model = change.getServletModel();
			if (change.getNewModelsInfo() == null) {
				LOG.info("Adding servlet {}", model);
			} else {
				LOG.info("Adding servlet {} to new contexts {}", model, change.getNewModelsInfo());
			}

			// see implementation requirements in Jetty version of this visit() method

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
				wrapper.setWhiteboardTCCL("whiteboard".equalsIgnoreCase(configuration.server().getTCCLType()));

				boolean isDefaultResourceServlet = model.isResourceServlet();
				for (String pattern : model.getUrlPatterns()) {
					isDefaultResourceServlet &= "/".equals(pattern);
				}
				if (model.isResourceServlet()) {
					wrapper.addInitParameter("pathInfoOnly", Boolean.toString(!isDefaultResourceServlet));
					OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
					wrapper.setHighestRankedContext(osgiServletContexts.get(highestRankedModel));
				}

				// role mapping per-servlet
				model.getRoleLinks().forEach(wrapper::addSecurityReference);

				wrapper.setRunAs(model.getRunAs());

				realContext.addChild(wrapper);

				// <servlet-mapping>
				String name = model.getName();
				for (String pattern : model.getUrlPatterns()) {
					realContext.addServletMappingDecoded(pattern, name, false);
				}

				// are there any error page declarations in the model?
				ErrorPageModel epm = model.getErrorPageModel();
				if (epm != null && epm.isValid()) {
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

				if (!change.isDynamic()) {
					ensureServletContextStarted(realContext);
				} else if (model.isServletSecurityPresent()) {
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

					// add the dynamic security constraints here (while static ones are added in OsgiContextConfiguration
					// listener
					Set<String> potentiallyNewRoles = new HashSet<>();

					for (SecurityConstraintModel scm : dynamicModels) {
						SecurityConstraint constraint = new SecurityConstraint();
						constraint.setDisplayName(scm.getName());
						constraint.setUserConstraint(scm.getTransportGuarantee().name());

						constraint.setAuthConstraint(scm.isAuthRolesSet());
						for (String role : scm.getAuthRoles()) {
							constraint.addAuthRole(role);
							potentiallyNewRoles.add(role);
						}

						// <web-resource-collection> elements
						for (SecurityConstraintModel.WebResourceCollection col : scm.getWebResourceCollections()) {
							SecurityCollection wrc = new SecurityCollection();
							wrc.setName(col.getName());
							boolean methodSet = false;
							for (String method : col.getMethods()) {
								wrc.addMethod(method);
								methodSet = true;
							}
							if (!methodSet) {
								for (String method : col.getOmittedMethods()) {
									wrc.addOmittedMethod(method);
								}
							}
							for (String pattern : col.getPatterns()) {
								wrc.addPattern(pattern);
							}
							constraint.addCollection(wrc);
						}
						realContext.addConstraint(constraint);
					}

					// add missing roles
					Set<String> currentRoles = new HashSet<>(Arrays.asList(realContext.findSecurityRoles()));

					for (String role : potentiallyNewRoles) {
						if (!currentRoles.contains(role)) {
							realContext.addSecurityRole(role);
						}
					}
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
		LOG.info("Removing servlet {}", model);
		LOG.debug("Removing servlet {} from context {}", model.getName(), contextPath);

		// there should already be a ServletContextHandler
		Context realContext = contextHandlers.get(contextPath);

		Container child = realContext.findChild(model.getName());
		if (child != null) {
			for (String pattern : model.getUrlPatterns()) {
				realContext.removeServletMapping(pattern);
			}
			realContext.removeChild(child);
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

				PaxWebStandardContext context = contextHandlers.get(contextPath);
				OsgiServletContext osgiContext = osgiServletContexts.get(highestRankedModel);

				PaxWebFilterDef filterDef = new PaxWebFilterDef(model, false, osgiContext);
				filterDef.setWhiteboardTCCL("whiteboard".equalsIgnoreCase(configuration.server().getTCCLType()));
				context.addFilterDef(filterDef);
				configureFilterMappings(model, context);
			}
		}
	}

	@Override
	public void visitFilterStateChange(FilterStateChange change) {
		if (change.isDynamic()) {
			// dynamic filter may be added when:
			// 1) normal filter is added
			// 2) context is started
			// 3) SCIs are called that register a filter
			// so we can't rely on full "state change"
			return;
		}

		Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> contextFilters = change.getContextFilters();

		for (Map.Entry<String, TreeMap<FilterModel, List<OsgiContextModel>>> entry : contextFilters.entrySet()) {
			String contextPath = entry.getKey();
			Map<FilterModel, List<OsgiContextModel>> filtersMap = entry.getValue();
			Set<FilterModel> filters = new TreeSet<>(filtersMap.keySet());

			LOG.info("Changing filter configuration for context {}", contextPath);

			OsgiContextModel defaultHighestRankedModel = osgiContextModels.containsKey(contextPath)
					? osgiContextModels.get(contextPath).iterator().next() : null;

			PaxWebStandardContext context = contextHandlers.get(contextPath);
			if (context == null) {
				// it can happen when unregistering last filters (or rather setting the state with empty set of filters)
				continue;
			}

			ensureServletContextStarted(context);

			// see implementation requirements in Jetty version of this visit() method
			// here in Tomcat we have to remember about "initial OSGi filter"

			FilterDef[] filterDefs = context.findFilterDefs();
			FilterMap[] filterMaps = context.findFilterMaps();

			// 2020-06-02: it's not possible to simply add a filter to Tomcat and init() it without init()ing
			// existing filters, so no way to do quick change
			context.filterStop();

			// remove all but "initial OSGi filter" and dynamically added filters
			for (FilterDef def : filterDefs) {
				if (def instanceof PaxWebFilterDef) {
					if (((PaxWebFilterDef) def).isInitial()
							|| (((PaxWebFilterDef) def).getFilterModel() != null && ((PaxWebFilterDef) def).getFilterModel().isDynamic())) {
						continue;
					}
				}
				context.removeFilterDef(def);
			}
			for (FilterMap map : filterMaps) {
				if (map instanceof PaxWebFilterMap) {
					if (((PaxWebFilterMap) map).isInitial()
							|| (((PaxWebFilterMap) map).getFilterModel() != null && ((PaxWebFilterMap) map).getFilterModel().isDynamic())) {
						continue;
					}
				}
				context.removeFilterMap(map);
			}

			// clear to keep the order of all available preprocessors
			context.getPreprocessors().clear();

			for (Iterator<FilterModel> iterator = filters.iterator(); iterator.hasNext(); ) {
				FilterModel model = iterator.next();
				if (model.isPreprocessor()) {
					context.getPreprocessors()
							.add(new PreprocessorFilterConfig(model, osgiServletContexts.get(defaultHighestRankedModel)));
					iterator.remove();
				}
			}

			for (FilterModel model : filters) {
				List<OsgiContextModel> contextModels = filtersMap.get(model) != null
						? filtersMap.get(model) : model.getContextModels();
				OsgiServletContext osgiContext = getHighestRankedContext(contextPath, model, contextModels);

				PaxWebFilterDef filterDef = new PaxWebFilterDef(model, false, osgiContext);
				filterDef.setWhiteboardTCCL("whiteboard".equalsIgnoreCase(configuration.server().getTCCLType()));
				context.addFilterDef(filterDef);
				configureFilterMappings(model, context);
			}

			if (context.isStarted() && !pendingTransaction(contextPath)) {
				context.filterStart();
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

				PaxWebStandardContext standardContext = contextHandlers.get(contextPath);
				EventListener eventListener = eventListenerModel.resolveEventListener();
				if (eventListener instanceof ServletContextAttributeListener) {
					// add it to accessible list to fire per-OsgiContext attribute changes
					OsgiServletContext c = osgiServletContexts.get(context);
					c.addServletContextAttributeListener((ServletContextAttributeListener) eventListener);
				}
				if (eventListener instanceof HttpSessionAttributeListener) {
					// we have to store it separately to propagate OsgiHttpSession specific events
					sessionListenerModels.add(eventListenerModel);
				}

				boolean stopped = false;
				if (standardContext.isStarted() && standardContext.getState() != LifecycleState.STARTING_PREP
						&& ServletContextListener.class.isAssignableFrom(eventListener.getClass())) {
					// we have to stop the context, so existing ServletContextListeners are called
					// with contextDestroyed() and new listener is added according to ranking rules of
					// the EventListenerModel
					LOG.info("Stopping Tomcat context \"{}\" before registering a ServletContextListener", contextPath);
					try {
						standardContext.stop();
						stopped = true;
					} catch (Exception e) {
						LOG.warn("Problem stopping {}: {}", standardContext, e.getMessage());
					}
				}

				// add the listener to real context - even ServletContextAttributeListener (but only once - even
				// if there are many OsgiServletContexts per ServletContext)
				if (eventListener instanceof HttpSessionListener || eventListener instanceof ServletContextListener) {
					// we're adding these listeners to overriden method, so Tomcat doesn't know about
					// "no pluggability listeners"
					standardContext.addApplicationLifecycleListener(eventListenerModel, eventListener);
				} else {
					// org.apache.catalina.core.StandardContext.addApplicationEventListener() is called from
					// org.apache.catalina.startup.ContextConfig.configureContext() and ALL listener class names
					// from web.xml, web-fragment.xml and @WebListener are added to the context using this method.
					// here we're calling specialized method that adds a listener with associated model
					standardContext.addApplicationEventListener(eventListenerModel, eventListener);
				}

				if (stopped) {
					// we have to start it again
					// register a "callback batch operation", which will be submitted within a new batch
					// as new task in single paxweb-config thread pool's thread
					LOG.info("Scheduling start of the {} context after listener registration for already started context", contextPath);
					change.registerBatchCompletedAction(new ContextStartChange(OpCode.MODIFY, contextPath));
				}
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

					PaxWebStandardContext standardContext = contextHandlers.get(contextPath);
					EventListener eventListener = eventListenerModel.resolveEventListener();
					if (eventListener instanceof ServletContextAttributeListener) {
						// remove it from per-OsgiContext list
						OsgiServletContext c = osgiServletContexts.get(context);
						if (c != null) {
							c.removeServletContextAttributeListener((ServletContextAttributeListener) eventListener);
						}
					}
					if (eventListener instanceof HttpSessionAttributeListener) {
						sessionListenerModels.remove(eventListenerModel);
					}

					if (pendingTransaction(contextPath)) {
						LOG.debug("Delaying removal of event listener {}", eventListenerModel);
						return;
					}

					removeEventListenerModel(standardContext, eventListenerModel, eventListener);
				});
			}
		}
	}

	private void removeEventListenerModel(PaxWebStandardContext standardContext, EventListenerModel eventListenerModel, EventListener eventListener) {
		// remove the listener from real context - even ServletContextAttributeListener
		if (standardContext != null) {
			// this may be null in case of WAB where we keep event listeners so they get contextDestroyed
			// event properly
			Object[] evListeners = standardContext.getApplicationEventListeners();
			Object[] lcListeners = standardContext.getApplicationLifecycleListeners();
			List<Object> newEvListeners = new ArrayList<>();
			List<Object> newLcListeners = new ArrayList<>();
			for (Object l : evListeners) {
				if (l != eventListener) {
					newEvListeners.add(l);
				}
			}
			for (Object l : lcListeners) {
				if (l != eventListener) {
					newLcListeners.add(l);
				}
			}
			if (eventListener != null) {
				standardContext.removeListener(eventListenerModel, eventListener);
			}
			standardContext.setApplicationEventListeners(newEvListeners.toArray(new Object[0]));
			standardContext.setApplicationLifecycleListeners(newLcListeners.toArray(new Object[0]));
		}
		eventListenerModel.releaseEventListener();
	}

	@Override
	public void visitWelcomeFileModelChange(WelcomeFileModelChange change) {
		WelcomeFileModel model = change.getWelcomeFileModel();

		OpCode op = change.getKind();
		if (op == OpCode.ADD || op == OpCode.DELETE) {
			List<OsgiContextModel> contextModels = op == OpCode.ADD ? change.getContextModels()
					: model.getContextModels();
			contextModels.forEach((context) -> {
				OsgiServletContext osgiServletContext = osgiServletContexts.get(context);
				PaxWebStandardContext realContext = contextHandlers.get(context.getContextPath());

				if (osgiServletContext == null || realContext == null) {
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

			PaxWebStandardContext context = contextHandlers.get(contextPath);

			if (context == null) {
				// may happen when cleaning things out
				return;
			}

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
	public void visitContainerInitializerModelChange(ContainerInitializerModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			ContainerInitializerModel model = change.getContainerInitializerModel();
			if (!model.isForAnyRuntime() && !model.isForTomcat()) {
				return;
			}
			List<OsgiContextModel> contextModels = change.getContextModels();
			contextModels.forEach((context) -> {
				String path = context.getContextPath();
				PaxWebStandardContext ctx = contextHandlers.get(context.getContextPath());
				if (ctx != null && ctx.isStarted()) {
					// we have to stop it. This operation should follow ClearDynamicRegistrationsChange that
					// clears possible dynamic registrations
					// also this operation is always followed by active web element registration (servlet or websocket)
					// because there's no need to just start the container with only one SCI
					LOG.info("Stopping Tomcat context \"{}\" before registering a ServletContextInitializer", path);
					try {
						ctx.stop();
					} catch (Exception e) {
						LOG.warn("Error stopping Tomcat context \"{}\": {}", path, e.getMessage(), e);
					}
				}

				// even if there's org.apache.catalina.core.StandardContext.addServletContainerInitializer(),
				// there's no "remove" equivalent and also we want to be able to pass correct implementation
				// of ServletContext there
				DynamicRegistrations registrations = this.dynamicRegistrations.get(path);
				OsgiDynamicServletContext dynamicContext = new OsgiDynamicServletContext(osgiServletContexts.get(context), registrations);
				SCIWrapper wrapper = new SCIWrapper(dynamicContext, model);
				initializers.get(path).add(wrapper);
			});
		}

		if (change.getKind() == OpCode.DELETE) {
			List<ContainerInitializerModel> models = change.getContainerInitializerModels();
			for (ContainerInitializerModel model : models) {
				if (!model.isForAnyRuntime() && !model.isForTomcat()) {
					continue;
				}
				List<OsgiContextModel> contextModels = model.getContextModels();
				contextModels.forEach((context) -> {
					String path = context.getContextPath();
					TreeSet<SCIWrapper> wrappers = this.initializers.get(path);
					if (wrappers != null) {
						wrappers.removeIf(w -> w.getModel() == model);
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
		PaxWebStandardContext ctx = contextHandlers.get(contextPath);
		if (ctx == null) {
			return;
		}

		// we can safely stop the context
		if (ctx.isStarted()) {
			LOG.info("Stopping Tomcat context \"{}\"", contextPath);
			try {
				ctx.stop();
			} catch (Exception e) {
				LOG.warn("Error stopping Tomcat context \"{}\": {}", contextPath, e.getMessage(), e);
			}
		}

		final int[] removed = {0};

		// servlets
		Map<ServletModel, Boolean> toRemove = new HashMap<>();
		for (Container child : ctx.findChildren()) {
			if (child instanceof PaxWebStandardWrapper) {
				ServletModel model = ((PaxWebStandardWrapper) child).getServletModel();
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

		// filters - clear all dynamic ones
		FilterDef[] filterDefs = ctx.findFilterDefs();
		FilterMap[] filterMaps = ctx.findFilterMaps();
		for (FilterDef def : filterDefs) {
			if (def instanceof PaxWebFilterDef) {
				if (((PaxWebFilterDef) def).isInitial()
						|| (((PaxWebFilterDef) def).getFilterModel() != null && !((PaxWebFilterDef) def).getFilterModel().isDynamic())) {
					continue;
				}
			}
			ctx.removeFilterDef(def);
			removed[0]++;
		}
		for (FilterMap map : filterMaps) {
			if (map instanceof PaxWebFilterMap) {
				if (((PaxWebFilterMap) map).isInitial()
						|| (((PaxWebFilterMap) map).getFilterModel() != null && !((PaxWebFilterMap) map).getFilterModel().isDynamic())) {
					continue;
				}
			}
			ctx.removeFilterMap(map);
		}

		// listeners - it's easier, because we remember them in dynamic registrations
		DynamicRegistrations contextRegistrations = this.dynamicRegistrations.get(contextPath);
		if (contextRegistrations != null) {
			contextRegistrations.getDynamicListenerModels().forEach((listenerToRemove, model) -> {
				if (model.isDynamic()) {
					removed[0]++;
					if (listenerToRemove instanceof ServletContextAttributeListener) {
						// remove it from per-OsgiContext list
						OsgiServletContext c = osgiServletContexts.get(context);
						if (c != null) {
							c.removeServletContextAttributeListener((ServletContextAttributeListener) listenerToRemove);
						}
					}

					// a bit harder (than in Jetty) way to remove the listeners
					Object[] elisteners = ctx.getApplicationEventListeners();
					Object[] llisteners = ctx.getApplicationLifecycleListeners();
					List<Object> newEListeners = new ArrayList<>();
					List<Object> newLListeners = new ArrayList<>();
					for (Object existing : elisteners) {
						if (existing != listenerToRemove) {
							newEListeners.add(existing);
						}
					}
					for (Object existing : llisteners) {
						if (existing != listenerToRemove) {
							newLListeners.add(existing);
						}
					}
					ctx.removeListener(model, listenerToRemove);
					ctx.setApplicationEventListeners(newEListeners.toArray(new Object[0]));
					ctx.setApplicationLifecycleListeners(newLListeners.toArray(new Object[0]));
				}
			});
			// remove application listeners added to Tomcat
//			ctx.clearApplicationLifecycleListeners();
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

	@Override
	public void visitContextStartChange(ContextStartChange change) {
		String contextPath = change.getContextPath();
		PaxWebStandardContext standardContext = contextHandlers.get(contextPath);
		if (standardContext != null) {
			ensureServletContextStarted(standardContext);
		} else {
			LOG.debug("Not starting unknown context {}.", contextPath);
		}
	}

	@Override
	public void visitContextStopChange(ContextStopChange change) {
		String contextPath = change.getContextPath();
		PaxWebStandardContext standardContext = contextHandlers.get(contextPath);
		if (standardContext != null && standardContext.isStarted()) {
			LOG.info("Stopping Tomcat context \"{}\"", contextPath);
			try {
				standardContext.stop();
			} catch (Exception e) {
				LOG.warn("Error stopping Tomcat context \"{}\": {}", contextPath, e.getMessage(), e);
			}
		}
	}

	@Override
	public void visitContextParamsChange(ContextParamsChange change) {
		// only here we set the parameters
		if (change.getKind() == OpCode.ADD) {
			LOG.info("Adding init parameters to {}: {}", change.getOsgiContextModel(), change.getParams());
			change.getOsgiContextModel().getContextParams().putAll(change.getParams());
		} else {
			LOG.info("Removing init parameters from {}: {}", change.getOsgiContextModel(), change.getParams());
			change.getParams().keySet().forEach(param -> {
				change.getOsgiContextModel().getContextParams().remove(param);
			});
		}
	}

	@Override
	public void visitSecurityConfigChange(SecurityConfigChange change) {
		// we should have the comfort of stopped target context
		LoginConfigModel loginConfigModel = change.getLoginConfigModel();
		List<String> securityRoles = change.getSecurityRoles();
		List<SecurityConstraintModel> securityConstraints = change.getSecurityConstraints();
		OsgiContextModel ocm = change.getOsgiContextModel();
		if (change.getKind() == OpCode.ADD) {
			LOG.info("Adding security configuration to {}", ocm);
			// just operate on the same OsgiContextModel that comes with the change
			// even if it's not highest ranked - it also means we don't have to process the configuration
			// in ServiceModel visitor
			if (loginConfigModel != null) {
				// null is an indication that we're adding security constraints using HttpService approach
				// after previously adding LoginConfig
				ocm.getSecurityConfiguration().setLoginConfig(loginConfigModel);
			}
			ocm.getSecurityConfiguration().getSecurityRoles().addAll(securityRoles);
			ocm.getSecurityConfiguration().getSecurityConstraints().addAll(securityConstraints);

			// https://github.com/ops4j/org.ops4j.pax.web/issues/1720 - but actually let's contribute these constraints
			// to a global pool of per-physical-context constraints.
			// Roles/constraints are additive, but login configuration is ranked-based - taken from highest-ranked
			// context only
			contextSecurityConstraints.computeIfAbsent(ocm.getContextPath(), c -> new TreeMap<>())
					.put(ocm, ocm.getSecurityConfiguration());
		} else {
			LOG.info("Removing security configuration from {}", ocm);
			if (!ocm.hasDirectHttpContextInstance() || loginConfigModel != null) {
				// non-null LoginConfigModel for HttpService based security configuration means
				// that we're removing only login configuration
				ocm.getSecurityConfiguration().setLoginConfig(null);
			}
			if (ocm.hasDirectHttpContextInstance() && loginConfigModel == null) {
				// null LoginConfigModel for HttpService based security configuration means
				// that we clear ALL security constraints and roles
				ocm.getSecurityConfiguration().getSecurityRoles().clear();
				ocm.getSecurityConfiguration().getSecurityConstraints().clear();
			} else {
				// this is the Whiteboard/WAB/HttpContextProcessing case
				securityRoles.forEach(ocm.getSecurityConfiguration().getSecurityRoles()::remove);
				securityConstraints.forEach(sc -> {
					ocm.getSecurityConfiguration().getSecurityConstraints()
							.removeIf(scm -> scm.getName().equals(sc.getName()));
				});
				TreeMap<OsgiContextModel, SecurityConfigurationModel> constraints = contextSecurityConstraints.get(ocm.getContextPath());
				constraints.remove(ocm);
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
	 *
	 * @param context
	 */
	private void ensureServletContextStarted(PaxWebStandardContext context) {
		String contextPath = context == null || context.getPath().equals("") ? "/" : context.getPath();
		if (context == null || context.isStarted() || context.getState() == LifecycleState.DESTROYED || pendingTransaction(contextPath)) {
			return;
		}
		try {
			OsgiContextModel highestRanked = context.getDefaultOsgiContextModel();
			OsgiServletContext highestRankedContext = context.getDefaultServletContext();
			highestRankedContext.allowServletContextListeners();

			LOG.info("Starting Tomcat context \"{}\" with default Osgi Context {}", context, highestRanked);

			// first thing - only NOW we can set ServletContext's class loader! It affects many things, including
			// the TCCL used for example by javax.el.ExpressionFactory.newInstance()

			// org.apache.catalina.core.ContainerBase.setParentClassLoader() sets the parent that
			// will be used as parent of org.apache.catalina.loader.ParallelWebappClassLoader returned
			// (by default) from org.apache.catalina.core.ApplicationContext.getClassLoader(), which
			// internally calls org.apache.catalina.core.StandardContext.getLoader().getClassLoader()
			// here's everything done manually/explicitly
			WebappLoader tomcatLoader = new WebappLoader();
//			ParallelWebappClassLoader loaderInstance = new ParallelWebappClassLoader(highestRankedContext.getClassLoader()) {
//				@Override
//				protected void clearReferences() {
//					// skip, because we're managing "deployments" differently
//					super.clearReferences();
//				}
//			};
			ParallelWebappClassLoader loaderInstance = new ParallelWebappClassLoader(highestRankedContext.getClassLoader());
			loaderInstance.setClearReferencesObjectStreamClassCaches(false);
			loaderInstance.setClearReferencesRmiTargets(false);
			tomcatLoader.setLoaderInstance(loaderInstance);
			context.setParentClassLoader(highestRankedContext.getClassLoader());
			context.setLoader(tomcatLoader);
			File workDir = (File) highestRankedContext.getAttribute(ServletContext.TEMPDIR);
			if (workDir == null) {
				workDir = configuration.server().getTemporaryDirectory();
			}
			context.setWorkDir(workDir.getAbsolutePath());

			// copy contexts parameters - from all contexts
			for (String p : context.findParameters()) {
				context.removeParameter(p);
			}
			this.osgiContextModels.get(contextPath).forEach(ocm -> ocm.getContextParams()
					.forEach((k, v) -> {
						if (context.findParameter(k) == null) {
							context.addParameter(k, v);
						}
					}));

			context.setOsgiServletContext(null);
			ServletContext realContext = context.getServletContext();

			highestRankedContext.clearAttributesFromPreviousCycle();
			clearDynamicRegistrations(contextPath, highestRanked);

			DynamicRegistrations registrations = this.dynamicRegistrations.get(contextPath);
			// allow dynamic registration, which will be restricted by RegisteringContainerInitializer
			OsgiDynamicServletContext dynamicContext = new OsgiDynamicServletContext(highestRankedContext, registrations);
			context.setOsgiServletContext(dynamicContext);

			Collection<SCIWrapper> initializers = new TreeSet<>(this.initializers.get(contextPath));
			// Initially I thought we should take only these SCIs, which are associated with highest ranked OCM,
			// but it turned out that just as we take servlets registered to different OsgiContextModels, but
			// the same ServletContextModel, we have to do the same with SCIs.
			// otherwise, by default (with HttpService scenario), SCIs from the OsgiContextModel related to
			// pax-web-extender-whiteboard would be taken (probably 0), simply because this bundle is usually
			// the first that grabs an instance of bundle-scoped HttpService
			// so please do not uncomment and keep for educational purposes!
//			initializers.removeIf(w -> !w.getModel().getContextModels().contains(highestRanked));

			// and finally add the registering initializer which will also mark the OsgiServletContext as no longer
			// accepting registration of additional ServletContextListeners
			initializers.add(new RegisteringContainerInitializer(highestRankedContext, registrations));
			context.setServletContainerInitializers(initializers);

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

			osgiServletContexts.forEach((ocm, osc) -> {
				if (ocm.getContextPath().equals(contextPath)) {
					osc.setContainerServletContext(realContext);
				}
			});

			// resource servlets need a reference to highest ranked Servlet Context
			// because org.apache.catalina.resources attribute has to be available
			// even if resource servlet is using custom context
			for (Container child : context.findChildren()) {
				if (child instanceof PaxWebStandardWrapper) {
					ServletModel servletModel = ((PaxWebStandardWrapper) child).getServletModel();
					if (servletModel != null && servletModel.isResourceServlet()) {
						((PaxWebStandardWrapper) child).setHighestRankedContext(highestRankedContext);
					}
				}
			}

			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(highestRankedContext.getClassLoader());
				context.start();
			} finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}
			// swap dynamic to normal context
			dynamicContext.rememberAttributesFromSCIs();
			context.setOsgiServletContext(highestRankedContext);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private boolean pendingTransaction(String contextPath) {
		return transactions.contains(contextPath);
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

	private void configureFilterMappings(FilterModel model, PaxWebStandardContext context) {
		if (model.getDynamicServletNames().size() > 0 || model.getDynamicUrlPatterns().size() > 0) {
			model.getDynamicServletNames().forEach(dm -> {
				if (!dm.isAfter()) {
					context.addFilterMapBefore(new PaxWebFilterMap(model, dm));
				}
			});
			model.getDynamicUrlPatterns().forEach(dm -> {
				if (!dm.isAfter()) {
					context.addFilterMapBefore(new PaxWebFilterMap(model, dm));
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

}
