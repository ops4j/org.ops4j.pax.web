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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import javax.servlet.ServletContext;

import org.apache.catalina.AccessLog;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AccessLogAdapter;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.LogConfiguration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.Default404Servlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.task.BatchVisitor;
import org.ops4j.pax.web.service.spi.task.FilterModelChange;
import org.ops4j.pax.web.service.spi.task.FilterStateChange;
import org.ops4j.pax.web.service.spi.task.OpCode;
import org.ops4j.pax.web.service.spi.task.OsgiContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletModelChange;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public static Logger LOG = LoggerFactory.getLogger(TomcatServerWrapper.class);

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

	/** Single map of context path to {@link Context} for fast access */
	private final Map<String, PaxWebStandardContext> contextHandlers = new HashMap<>();

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
	 * Global {@link Configuration} passed from pax-web-runtime through
	 * {@link org.ops4j.pax.web.service.spi.ServerController}
	 */
	private final Configuration configuration;

	/** Servlet to use when no servlet is mapped - to ensure that preprocessors and filters are run correctly. */
	private final Default404Servlet default404Servlet = new Default404Servlet();

//				private final Map<HttpContext, Context> contextMap = new ConcurrentHashMap<>();
//
//				private final Map<FilterModel, FilterLifecycleListener> filterLifecycleListenerMap = new ConcurrentHashMap<>();
//
//				private final Map<ServletModel, LifecycleListener> servletLifecycleListenerMap = new ConcurrentHashMap<>();

	public TomcatServerWrapper(Configuration config, TomcatFactory tomcatFactory,
			Bundle paxWebTomcatBundle, ClassLoader classLoader) {
		this.configuration = config;
		this.tomcatFactory = tomcatFactory;
		this.paxWebTomcatBundle = paxWebTomcatBundle;
		this.classLoader = classLoader;

//		((ContainerBase) server.getHost()).setStartChildren(false);
//		TomcatURLStreamHandlerFactory.disable();
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
		createServer();

		// No external configuration should replace our "Server" object
		applyTomcatConfiguration();

		// If external configuration added some connectors, we have to ensure they match declaration from
		// PID config: org.osgi.service.http.enabled and org.osgi.service.http.secure.enabled
		verifyConnectorConfiguration();

//		if (server.getErrorHandler() == null) {
//			server.setErrorHandler(new ErrorHandler());
//		}

		// Configure NCSA RequestLogHandler if needed
		if (configuration.logging().isLogNCSAFormatEnabled()) {
			configureRequestLog();
		}

//		mbeanContainer = jettyFactory.enableJmxIfPossible(server);

//		// most important part - a handler.
//		// TODO: my initial idea was to have this hierarchy:
//		//  server:
//		//   - handler collection
//		//      - handler collection to store custom handlers with @Priority > 0
//		//      - context handler collection to store context handlers
//		//      - handler collection to store custom handlers with @Priority < 0
//		//
//		// for now, let's have it like before Pax Web 8
//		this.mainHandler = new ContextHandlerCollection();
//		server.setHandler(this.mainHandler);
	}

	/**
	 * <p>Create Tomcat server using provided {@link Configuration}.</p>
	 *
	 * @return
	 */
	private void createServer() throws Exception {
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
		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		this.server = server;
		this.service = service;
		this.engine = engine;
		this.defaultHost = host;
	}

	/**
	 * It was very easy and clean in Jetty, Tomcat also has a method, where the embedded Tomcat server can be
	 * configured using external XML files conforming to Tomcat digester configuration.
	 */
	private void applyTomcatConfiguration() {
		File[] locations = configuration.server().getConfigurationFiles();
		if (locations.length == 0) {
			LOG.info("No external Tomcat configuration file specified. Default/PID configuration will be used.");
			return;
		} else if (locations.length > 1) {
			LOG.warn("Can't specify Tomcat configuration using more than one XML file. Skipping XML configuration.");
			return;
		} else {
			LOG.info("Processing Tomcat configuration using file: {}", locations[0]);
		}

		File xmlConfig = locations[0];

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try {
//			// PAXWEB-1112 - classloader leak prevention:
//			// first find XmlConfiguration class' CL to set it as TCCL when performing static
//			// initialization of XmlConfiguration in order to not leak
//			// org.eclipse.jetty.xml.XmlConfiguration.__factoryLoader.loader
//			ClassLoader jettyXmlCl = null;
//			if (paxWebJettyBundle != null) {
//				for (Bundle b : paxWebJettyBundle.getBundleContext().getBundles()) {
//					if ("org.eclipse.jetty.xml".equals(b.getSymbolicName())) {
//						jettyXmlCl = b.adapt(BundleWiring.class).getClassLoader();
//						break;
//					}
//				}
//			}
//
//			// TCCL to perform static initialization of XmlConfiguration with proper TCCL
//			Thread.currentThread().setContextClassLoader(jettyXmlCl);
//			XmlConfiguration configuration
//					= new XmlConfiguration(Resource.newResource(getClass().getResource("/jetty-empty.xml")));
//
//			// When parsing, TCCL will be set to CL of pax-web-jetty bundle
//			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
//
//			XmlConfiguration previous = null;
//			Map<String, Object> objects = new LinkedHashMap<>();
//
//			// the only existing objects. Names as found in JETTY_HOME/etc/jetty*.xml files
//			objects.put("Server", server);
//			objects.put("threadPool", server.getThreadPool());
//
//			for (File location : locations) {
//				LOG.debug("Parsing {}", location);
//				configuration = new XmlConfiguration(Resource.newResource(location));
//
//				// add objects created in previous file, so they're available when parsing next one
//				configuration.getIdMap().putAll(previous == null ? objects : previous.getIdMap());
//				// configuration will be available for Jetty when using <Property />
//				configuration.getProperties().putAll(this.configuration.all());
//
//				configuration.configure();
//
//				// collect all created objects
//				objects.putAll(configuration.getIdMap());
//
//				// collect all created HttpConfigurations
//				configuration.getIdMap().forEach((id, v) -> {
//					if (HttpConfiguration.class.isAssignableFrom(v.getClass())) {
//						httpConfigs.put(id, (HttpConfiguration) v);
//					}
//				});
//
//				previous = configuration;
//			}
//
//			if (locations.length > 0) {
//				// the "Server" object should not be redefined
//				objects.values().forEach(bean -> {
//					if (Server.class.isAssignableFrom(bean.getClass())) {
//						if (bean != server) {
//							String msg = "Can't create instance of Jetty server in external configuration files.";
//							throw new IllegalArgumentException(msg);
//						}
//					}
//				});
//
//				// summary about found connectors
//				Connector[] connectors = server.getConnectors();
//				if (connectors != null && connectors.length > 0) {
//					for (Connector connector : connectors) {
//						String host = ((ServerConnector) connector).getHost();
//						if (host == null) {
//							host = "0.0.0.0";
//						}
//						int port = ((ServerConnector) connector).getPort();
//						LOG.info("Found configured connector \"{}\": {}:{}", connector.getName(), host, port);
//					}
//				} else {
//					LOG.debug("No connectors configured in external Jetty configuration files.");
//				}
//			}
		} finally {
			Thread.currentThread().setContextClassLoader(loader);
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

		// review connectors possibly configured from external files and check if they match configadmin configuration
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

		AccessLogValve ncsaLogger = /*lc.isLogNCSAExtended() ? new ExtendedAccessLogValve() : */new AccessLogValve();
		ncsaLogger.setPattern(lc.isLogNCSAExtended() ? "combined" : "common");

		// org.apache.catalina.valves.AccessLogValve.getLogFile
		ncsaLogger.setDirectory(new File(lc.getLogNCSADirectory()).getAbsolutePath());
		ncsaLogger.setPrefix(lc.getLogNCSAFile());
		ncsaLogger.setFileDateFormat("." + lc.getLogNCSAFilenameDateFormat());
		ncsaLogger.setSuffix(".log");

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
		server.start();
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

	// --- visitor methods for model changes

	@Override
	public void visit(ServletContextModelChange change) {
		ServletContextModel model = change.getServletContextModel();
		if (change.getKind() == OpCode.ADD) {
			LOG.info("Creating new Tomcat context for {}", model);

//							Context ctx = new HttpServiceContext(getHost(), accessControllerContext);
			PaxWebStandardContext context = new PaxWebStandardContext(default404Servlet);
			context.setName(model.getId());
			context.setPath(model.getContextPath());
			context.setWorkDir(configuration.server().getTemporaryDirectory().getAbsolutePath());
//							ctx.setWebappVersion(name);
//							ctx.setDocBase(basedir);

			// in this new context, we need "initial OSGi filter" which will:
			// - call preprocessors
			// - handle security using proper httpContext/servletContextHelper
			// - proceed with the chain that includes filters and target servlet.
			//   the filters should match target servlet's OsgiServletContext
			context.createInitialOsgiFilter();

			// same as Jetty's org.eclipse.jetty.server.handler.ContextHandler.setAllowNullPathInfo(false)
			// to enable redirect from /context-path to /context-path/
			context.setMapperContextRootRedirectEnabled(true);

			context.addLifecycleListener(new Tomcat.FixContextListener());

			//							// add mimetypes here?
			//							// MIME mappings
			//							for (int i = 0; i < DEFAULT_MIME_MAPPINGS.length; i += 2) {
			//								ctx.addMimeMapping(DEFAULT_MIME_MAPPINGS[i], DEFAULT_MIME_MAPPINGS[i + 1]);
			//							}

			// TODO: handle virtual hosts here. Context should be added to all declared virtual hosts.
			//       Remember - it's much harder in Tomcat than in Jetty and Undertow
			defaultHost.addChild(context);

//							// Add Session config
//							ctx.setSessionCookieName(configurationSessionCookie);
//							// configurationSessionCookieHttpOnly
//							ctx.setUseHttpOnly(configurationSessionCookieHttpOnly);
//							// configurationSessionTimeout
//							ctx.setSessionTimeout(configurationSessionTimeout);
//							// configurationWorkerName //TODO: missing
//
//							// new OSGi methods
//							((HttpServiceContext) ctx).setHttpContext(httpContext);
//							((HttpServiceContext) ctx).setContextAttributes(contextAttributes);
//							// TODO: what about the AccessControlContext?
//							// TODO: the virtual host section below
//							// TODO: what about the VirtualHosts?
//							// TODO: what about the tomcat-web.xml config?
//							// TODO: connectors are needed for virtual host?
//							if (containerInitializers != null) {
//								for (Map.Entry<ServletContainerInitializer, Set<Class<?>>> entry : containerInitializers
//										.entrySet()) {
//									ctx.addServletContainerInitializer(entry.getKey(),
//											entry.getValue());
//								}
//							}
//
//							// Add default JSP ContainerInitializer
//							if (isJspAvailable()) { // use JasperClassloader
//								try {
//									@SuppressWarnings("unchecked")
//									Class<ServletContainerInitializer> loadClass = (Class<ServletContainerInitializer>) getClass()
//											.getClassLoader().loadClass(
//													"org.ops4j.pax.web.jsp.JasperInitializer");
//									ctx.addServletContainerInitializer(loadClass.newInstance(),
//											null);
//								} catch (ClassNotFoundException e) {
//									LOG.error("Unable to load JasperInitializer", e);
//								} catch (InstantiationException | IllegalAccessException e) {
//									LOG.error("Unable to instantiate JasperInitializer", e);
//								}
//							}

			//		final Bundle bundle = contextModel.getBundle();
			//		final BundleContext bundleContext = BundleUtils
			//				.getBundleContext(bundle);
			//
			//		if (packageAdminTracker != null) {
			//			ServletContainerInitializerScanner scanner = new ServletContainerInitializerScanner(bundle, tomcatBundle, packageAdminTracker.getService());
			//			Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers = contextModel.getContainerInitializers();
			//			if (containerInitializers == null) {
			//				containerInitializers = new HashMap<>();
			//				contextModel.setContainerInitializers(containerInitializers);
			//			}
			//			scanner.scanBundles(containerInitializers);
			//		}
			//
			//
			//		final WebContainerContext httpContext = contextModel.getHttpContext();
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
			//		context.setDisplayName(httpContext.getContextId());
			//		// Similar to the Jetty fix for PAXWEB-725
			//		// Without this the el implementation is not found
			//        ClassLoader classLoader = contextModel.getClassLoader();
			//        List<Bundle> bundles = ((ResourceDelegatingBundleClassLoader) classLoader).getBundles();
			//        ClassLoader parentClassLoader = getClass().getClassLoader();
			//        ResourceDelegatingBundleClassLoader containerSpecificClassLoader = new ResourceDelegatingBundleClassLoader(bundles, parentClassLoader);
			//        context.setParentClassLoader(containerSpecificClassLoader);
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
			//
			//		String authMethod = contextModel.getAuthMethod();
			//		if (authMethod == null) {
			//			authMethod = "NONE";
			//		}
			//		String realmName = contextModel.getRealmName();
			//		String loginPage = contextModel.getFormLoginPage();
			//		String errorPage = contextModel.getFormErrorPage();
			//		LoginConfig loginConfig = new LoginConfig(authMethod, realmName, loginPage, errorPage);
			//		context.setLoginConfig(loginConfig);
			//		LOG.debug("loginConfig: method={} realm={}", authMethod, realmName);
			//		// Custom Service Valve for checking authentication stuff ...
			//		context.getPipeline().addValve(new ServiceValve(httpContext));
			//		if (context.getAuthenticator() == null) {
			//			// Authentication Valve according to configured authentication method
			//			context.getPipeline().addValve(getAuthenticatorValve(authMethod));
			//		}
			//		if (contextModel.getContextParams() != null) {
			//			for (Map.Entry<String, String> entry : contextModel.getContextParams().entrySet()) {
			//				context.addParameter(entry.getKey(), entry.getValue());
			//			}
			//		}
			//
			//		// TODO: how about classloader?
			//		// TODO: compare with JettyServerWrapper.addContext
			//		// TODO: what about the init parameters?
			//
			//		configureJspConfigDescriptor(context, contextModel);
			//
			//		final LifecycleState state = context.getState();
			//		if (state != LifecycleState.STARTED && state != LifecycleState.STARTING
			//				&& state != LifecycleState.STARTING_PREP) {
			//
			//			LOG.debug("Registering ServletContext as service. ");
			//			final Dictionary<String, String> properties = new Hashtable<>();
			////			properties.put(WebContainerConstants.PROPERTY_SYMBOLIC_NAME, bundle.getSymbolicName());
			//
			//			final Dictionary<String, String> headers = bundle.getHeaders();
			//			final String version = headers.get(Constants.BUNDLE_VERSION);
			//			if (version != null && version.length() > 0) {
			//				properties.put("osgi.web.version", version);
			//			}
			//
			//			String webContextPath = headers.get(WEB_CONTEXT_PATH);
			//			final String webappContext = headers.get("Webapp-Context");
			//
			//			final ServletContext servletContext = context.getServletContext();
			//
			//			// This is the default context, but shouldn't it be called default?
			//			// See PAXWEB-209
			//			if ("/".equalsIgnoreCase(context.getPath())
			//					&& (webContextPath == null || webappContext == null)) {
			//				webContextPath = context.getPath();
			//			}
			//
			//			// PAXWEB-1147
			//			SessionCookieConfig scc = servletContext.getSessionCookieConfig();
			//			if (scc != null) {
			//				if (contextModel.getSessionDomain() != null) {
			//					scc.setDomain(contextModel.getSessionDomain());
			//				}
			//				if (contextModel.getSessionCookie() != null) {
			//					scc.setName(contextModel.getSessionCookie());
			//					context.setSessionCookieName(contextModel.getSessionCookie());
			//				}
			//				if (contextModel.getSessionCookieMaxAge() != null) {
			//					scc.setMaxAge(contextModel.getSessionCookieMaxAge());
			//				}
			//				if (contextModel.getSessionCookieHttpOnly() != null) {
			//					scc.setHttpOnly(contextModel.getSessionCookieHttpOnly());
			//				}
			//				if (contextModel.getSessionCookieSecure() != null) {
			//					scc.setSecure(contextModel.getSessionCookieSecure());
			//				}
			//			}
			//
			//			// makes sure the servlet context contains a leading slash
			//			webContextPath = webContextPath != null ? webContextPath
			//					: webappContext;
			//			if (webContextPath != null && !webContextPath.startsWith("/")) {
			//				webContextPath = "/" + webContextPath;
			//			}
			//			if (webContextPath == null) {
			////				LOG.warn(WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH +
			////						" couldn't be set, it's not configured. Assuming '/'");
			//				webContextPath = "/";
			//			}
			//
			////			properties.put(WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, webContextPath);
			////			properties.put(WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME, context.getServletContext().getServletContextName());
			//
			//			servletContextService = bundleContext.registerService(
			//					ServletContext.class, servletContext, properties);
			//			LOG.debug("ServletContext registered as service. ");
			//		}
			//		contextMap.put(contextModel.getHttpContext(), context);
			//
			//		return context;

			// explicit no check for existing mapping under given physical context path
			contextHandlers.put(model.getContextPath(), context);
			osgiContextModels.put(model.getContextPath(), new TreeSet<>());
		}
	}

	@Override
	public void visit(OsgiContextModelChange change) {
		OsgiContextModel osgiModel = change.getOsgiContextModel();
		ServletContextModel servletModel = change.getServletContextModel();

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
			osgiServletContexts.put(osgiModel, new OsgiServletContext(realContext.getServletContext(), osgiModel, servletModel));
			osgiContextModels.get(contextPath).add(osgiModel);

			// there may be a change in what's the "best" (highest ranked) OsgiContextModel for given
			// ServletContextModel. This will became the "fallback" OsgiContextModel for chains without
			// target servlet (with filters only)
			OsgiContextModel highestRankedModel = osgiContextModels.get(contextPath).iterator().next();
			ServletContext highestRankedContext = osgiServletContexts.get(highestRankedModel);
			realContext.setDefaultOsgiContextModel(highestRankedModel);
			realContext.setDefaultServletContext(highestRankedContext);
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
			LOG.info("Adding servlet {}", model);

			// see implementation requirements in Jetty version of this visit() method

			Set<String> done = new HashSet<>();

			model.getContextModels().forEach(osgiContext -> {
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
				realContext.addChild(wrapper);

				// <servlet-mapping>
				String name = model.getName();
				for (String pattern : model.getUrlPatterns()) {
					realContext.addServletMappingDecoded(pattern, name, false);
				}
			});
			return;
		}

		if (change.getKind() == OpCode.DISABLE || change.getKind() == OpCode.DELETE) {
			for (ServletModel model : change.getServletModels()) {
				LOG.info("Removing servlet {}", model);

				// proper order ensures that (assuming above scenario), for /c1, ocm2 will be chosen and ocm1 skipped
				model.getContextModels().forEach(osgiContextModel -> {
					String contextPath = osgiContextModel.getContextPath();

					LOG.debug("Removing servlet {} from context {}", model.getName(), contextPath);

					// there should already be a ServletContextHandler
					Context realContext = contextHandlers.get(contextPath);

					realContext.removeChild(realContext.findChild(model.getName()));
					for (String pattern : model.getUrlPatterns()) {
						realContext.removeServletMapping(pattern);
					}
				});
			}
		}
	}

	@Override
	public void visit(FilterModelChange change) {
		// no op here - will be handled with FilterStateChange
	}

	@Override
	public void visit(FilterStateChange change) {
		// there's no separate add filter, add filter, remove filter, ... set of operations
		// everything is passed in single "change"

		Map<String, TreeSet<FilterModel>> contextFilters = change.getContextFilters();

		for (Map.Entry<String, TreeSet<FilterModel>> entry : contextFilters.entrySet()) {
			String contextPath = entry.getKey();
			Set<FilterModel> filters = entry.getValue();

			LOG.info("Changing filter configuration for context {}", contextPath);

			PaxWebStandardContext context = contextHandlers.get(contextPath);

			// see implementation requirements in Jetty version of this visit() method
			// here in Tomcat we have to remember about "initial OSGi filter"

			FilterDef[] filterDefs = context.findFilterDefs();
			FilterMap[] filterMaps = context.findFilterMaps();

			// 2020-06-02: it's not possible to simply add a filter to Tomcat and init() it without init()ing
			// existing filters
			if (true || !quickFilterChange(context, filterDefs, filters)) {
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

				PaxWebFilterDef[] newFilterDefs = new PaxWebFilterDef[filters.size() + 1];
				PaxWebFilterMap[] newFilterMaps = new PaxWebFilterMap[filters.size() + 1];

				for (FilterModel model : filters) {
					PaxWebFilterDef def = new PaxWebFilterDef(model, false);
					PaxWebFilterMap map = new PaxWebFilterMap(model, false);

					context.addFilterDef(def);
					context.addFilterMap(map);
				}
				context.filterStart();
			}
		}
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
	 * @return
	 */
	private boolean quickFilterChange(PaxWebStandardContext context, FilterDef[] existingFilterDefs, Set<FilterModel> filters) {
		int pos = 0;
		FilterModel[] newModels = filters.toArray(new FilterModel[0]);
		boolean quick = newModels.length >= existingFilterDefs.length - 1;

		// by "quick" we mean - there are no removed filters and new filters come last
		while (quick) {
			if (pos >= existingFilterDefs.length - 1) {
				break;
			}
			if (!(existingFilterDefs[pos + 1] instanceof PaxWebFilterDef
					&& ((PaxWebFilterDef)existingFilterDefs[pos + 1]).getFilterModel().equals(newModels[pos]))) {
				quick = false;
				break;
			}
			pos++;
		}

		if (quick) {
			for (int i = pos; i < newModels.length; i++) {
				context.addFilterDef(new PaxWebFilterDef(newModels[pos], false));
				context.addFilterMap(new PaxWebFilterMap(newModels[pos], false));
			}
			context.filterStart();
			return true;
		}

		return false;
	}

//	@Override
//	public void addServlet(final ServletModel model) {
//		LOG.debug("add servlet [{}]", model);
//		final Context context = findOrCreateContext(model.getContextModel());
//		final String servletName = model.getName();
//		if (model.getServlet() == null) {
//			// will do class for name and set init params
//			try {
//				final Servlet servlet = model.getServletFromName();
//
//				if (servlet != null) {
//					createServletWrapper(model, context, servletName);
//
//					if (!model.getContextModel().isWebBundle()) {
//						ServletLifecycleListener listener = new ServletLifecycleListener(context, servletName, model);
//						servletLifecycleListenerMap.put(model, listener);
//						context.addLifecycleListener(listener);
//					}
//
//				} else {
//					final Wrapper sw = context.createWrapper();
//					sw.setServletClass(model.getServletClass().getName());
//
//					addServletWrapper(sw, servletName, context, model);
//
//					if (!model.getContextModel().isWebBundle()) {
//						SerlvetClassNameLifecycleListener listener = new SerlvetClassNameLifecycleListener(context, sw, model, servletName);
//						servletLifecycleListenerMap.put(model, listener);
//						context.addLifecycleListener(listener);
//					}
//				}
//
//			} catch (InstantiationException | SecurityException | ClassNotFoundException | IllegalAccessException e) {
//				LOG.error("failed to create Servlet", e);
//			}
//
//		} else {
//			createServletWrapper(model, context, servletName);
//
//			if (!model.getContextModel().isWebBundle()) {
//				WrappedServletLifecycleListener listener = new WrappedServletLifecycleListener(context, servletName, model);
//				servletLifecycleListenerMap.put(model, listener);
//				context.addLifecycleListener(listener);
//			}
//		}
//	}
//
//	@Override
//	public void removeContext(final HttpContext httpContext) {
//		LOG.debug("remove context [{}]", httpContext);
//
//		try {
//			if (servletContextService != null) {
//				servletContextService.unregister();
//			}
//		} catch (final IllegalStateException e) {
//			LOG.info("ServletContext service already removed");
//		}
//
//		final Context context = contextMap.remove(httpContext);
//		if (context == null) {
//			throw new RemoveContextException(
//					"cannot remove the context because it does not exist: "
//							+ httpContext);
//		}
//		try {
//			final LifecycleState state = context.getState();
//			if (LifecycleState.STOPPING != state
//					&& LifecycleState.STOPPED != state
//					&& LifecycleState.STOPPING_PREP != state) {
//				context.stop();
//			}
//		} catch (LifecycleException e) {
//			throw new RemoveContextException("cannot stop the context: "
//					+ httpContext, e);
//		}
//		this.server.getHost().removeChild(context);
//		try {
//			final LifecycleState state = context.getState();
//			if (LifecycleState.DESTROYED != state
//					&& LifecycleState.DESTROYING != state) {
//				context.destroy();
//			}
//		} catch (final LifecycleException e) {
//			throw new RemoveContextException("cannot destroy the context: "
//					+ httpContext, e);
//		}
//	}
//
//	@Override
//	public void addEventListener(final EventListenerModel eventListenerModel) {
//		LOG.debug("add event listener: [{}]", eventListenerModel);
//
//		final Context context = findOrCreateContext(eventListenerModel);
//		LifecycleState state = ((HttpServiceContext) context).getState();
//		boolean restartContext = false;
//		if ((LifecycleState.STARTING.equals(state) || LifecycleState.STARTED
//				.equals(state))
//				&& !eventListenerModel.getContextModel().isWebBundle()) {
//			try {
//				restartContext = true;
//				((HttpServiceContext) context).stop();
//			} catch (LifecycleException e) {
//				LOG.warn("Can't reset the Lifecycle ... ", e);
//			}
//		}
//		context.addLifecycleListener(new LifecycleListener() {
//
//			@Override
//			public void lifecycleEvent(LifecycleEvent event) {
//				if (Lifecycle.BEFORE_START_EVENT.equalsIgnoreCase(event
//						.getType())) {
//					context.getServletContext().addListener(
//							eventListenerModel.getEventListener());
//				}
//			}
//		});
//
//		if (restartContext) {
//			try {
//				((HttpServiceContext) context).start();
//			} catch (LifecycleException e) {
//				LOG.warn("Can't reset the Lifecycle ... ", e);
//			}
//		}
//	}
//
//	@Override
//	public void removeEventListener(final EventListenerModel eventListenerModel) {
//		LOG.debug("remove event listener: [{}]", eventListenerModel);
//		NullArgumentException.validateNotNull(eventListenerModel,
//				"eventListenerModel");
//		NullArgumentException.validateNotNull(
//				eventListenerModel.getEventListener(),
//				"eventListenerModel#weventListener");
//		final Context context = findOrCreateContext(eventListenerModel);
//
//		LOG.info("removing event listener");
//		// TODO open a bug in tomcat
//		if (!removeApplicationEventListener(context,
//				eventListenerModel.getEventListener())) {
//			if (!removeApplicationLifecycleListener(context,
//					eventListenerModel.getEventListener())) {
////				throw new RemoveEventListenerException(
////						"cannot remove the event lister it is a not support class : "
////								+ eventListenerModel);
//				LOG.warn("cannot remove the event lister it is a not support class : {}", eventListenerModel);
//			}
//		}
//	}
//
//	private boolean removeApplicationLifecycleListener(final Context context,
//													   final EventListener eventListener) {
//		if (!isApplicationLifecycleListener(eventListener)) {
//			return false;
//		}
//
//		Object[] applicationLifecycleListeners = context.getApplicationLifecycleListeners();
//
//		List<EventListener> listeners = new ArrayList<>();
//		boolean found = filterEventListener(listeners, applicationLifecycleListeners, eventListener);
//
//		if (found) {
//			// notify the ServletContextListener before unregistering it
//			if (eventListener instanceof ServletContextListener) {
//				((ServletContextListener) eventListener).contextDestroyed(new ServletContextEvent(context.getServletContext()));
//			}
//			context.setApplicationLifecycleListeners(listeners.toArray());
//		}
//		return found;
//	}
//
//	private boolean isApplicationLifecycleListener(
//			final EventListener eventListener) {
//		return (eventListener instanceof HttpSessionListener || eventListener instanceof ServletContextListener);
//	}
//
//	private boolean removeApplicationEventListener(final Context context,
//												   final EventListener eventListener) {
//		if (!isApplicationEventListener(eventListener)) {
//			return false;
//		}
//		Object[] applicationEventListeners = context
//				.getApplicationEventListeners();
//
//		List<EventListener> newEventListeners = new ArrayList<>();
//		boolean found = filterEventListener(newEventListeners, applicationEventListeners, eventListener);
//
//
//		if (found) {
//			context.setApplicationEventListeners(newEventListeners
//					.toArray());
//		}
//		return found;
//	}
//
//	private boolean filterEventListener(List<EventListener> listeners, Object[] applicationEventListeners, EventListener eventListener) {
//
//		boolean found = false;
//
//		for (Object object : applicationEventListeners) {
//			EventListener listener = (EventListener) object;
//			if (listener != eventListener) {
//				listeners.add(listener);
//			} else {
//				found = true;
//			}
//		}
//
//		return found;
//
//	}
//
//
//	private boolean isApplicationEventListener(final EventListener eventListener) {
//		return (eventListener instanceof ServletContextAttributeListener
//				|| eventListener instanceof ServletRequestListener
//				|| eventListener instanceof ServletRequestAttributeListener || eventListener instanceof HttpSessionAttributeListener);
//	}
//
//	@Override
//	public void addErrorPage(final ErrorPageModel model) {
//		final Context context = findOrCreateContext(model);
//		// for Nxx codes, we have to loop
//		// Tomcat doesn't support error code range handlers, but
//		// in the end - it's just a org.apache.catalina.core.StandardContext.statusPages map of code -> error page
//		if ("4xx".equals(model.getError())) {
//			for (int c = 400; c < 500; c++) {
//				final ErrorPage errorPage = createErrorPage(model, c);
//				context.addErrorPage(errorPage);
//			}
//		} else if ("5xx".equals(model.getError())) {
//			for (int c = 500; c < 600; c++) {
//				final ErrorPage errorPage = createErrorPage(model, c);
//				context.addErrorPage(errorPage);
//			}
//		} else {
//			final ErrorPage errorPage = createErrorPage(model, null);
//			context.addErrorPage(errorPage);
//		}
//	}
//
//	private ErrorPage createErrorPage(final ErrorPageModel model, Integer overrideErrorCode) {
//		NullArgumentException.validateNotNull(model, "model");
//		NullArgumentException.validateNotNull(model.getLocation(),
//				"model#location");
//		NullArgumentException.validateNotNull(model.getError(), "model#error");
//		final ErrorPage errorPage = new ErrorPage();
//		errorPage.setLocation(model.getLocation());
//		Integer errorCode;
//		if (overrideErrorCode == null) {
//			errorCode = parseErrorCode(model.getError());
//		} else {
//			errorCode = overrideErrorCode;
//		}
//		if (errorCode != null) {
//			errorPage.setErrorCode(errorCode);
//		} else {
//			if (!ErrorPageModel.ERROR_PAGE.equalsIgnoreCase(model.getError())) {
//				errorPage.setExceptionType(model.getError());
//			}
//		}
//		return errorPage;
//	}
//
//	private Integer parseErrorCode(final String errorCode) {
//		try {
//			return Integer.parseInt(errorCode);
//		} catch (final NumberFormatException e) {
//			return null;
//		}
//	}
//
//	@Override
//	public void removeErrorPage(final ErrorPageModel model) {
//		final Context context = findContext(model);
//		if (context == null) {
//			throw new RemoveErrorPageException(
//					"cannot retrieve the associated context: " + model);
//		}
//		// for Nxx codes, we have to loop
//		// Tomcat doesn't support error code range handlers, but
//		// in the end - it's just a org.apache.catalina.core.StandardContext.statusPages map of code -> error page
//		if ("4xx".equals(model.getError())) {
//			for (int c = 400; c < 500; c++) {
//				final ErrorPage errorPage = createErrorPage(model, c);
//				context.removeErrorPage(errorPage);
//			}
//		} else if ("5xx".equals(model.getError())) {
//			for (int c = 500; c < 600; c++) {
//				final ErrorPage errorPage = createErrorPage(model, c);
//				context.removeErrorPage(errorPage);
//			}
//		} else {
//			final ErrorPage errorPage = createErrorPage(model, null);
//			context.removeErrorPage(errorPage);
//		}
//	}
//
//	@Override
//	public Servlet createResourceServlet(final OsgiContextModel contextModel,
//										 final String alias, final String name) {
//		LOG.debug("createResourceServlet( contextModel: {}, alias: {}, name: {})");
//		final Context context = findOrCreateContext(contextModel);
//		return new TomcatResourceServlet(contextModel.getHttpContext(),
//				contextModel.getContextName(), alias, name, context);
//	}
//
//	@Override
//	public void addSecurityConstraintMapping(final SecurityConstraintMappingModel secMapModel) {
//		LOG.debug("add security contstraint mapping [{}]", secMapModel);
//		final Context context = findOrCreateContext(secMapModel
//				.getContextModel());
//
//		String mappingMethod = secMapModel.getMapping();
//		String constraintName = secMapModel.getConstraintName();
//		String url = secMapModel.getUrl();
//		String dataConstraint = secMapModel.getDataConstraint();
//		List<String> roles = secMapModel.getRoles();
//		boolean authentication = secMapModel.isAuthentication();
//
//		SecurityConstraint[] constraints = context.findConstraints();
//		SecurityConstraint secConstraint = new SecurityConstraint();
//		boolean foundExisting = false;
//
//		for (SecurityConstraint securityConstraint : constraints) {
//			if (securityConstraint.getDisplayName().equalsIgnoreCase(
//					constraintName)) {
//				secConstraint = securityConstraint;
//				foundExisting = true;
//				continue;
//			}
//		}
//
//		if (!foundExisting) {
//			secConstraint.setDisplayName(secMapModel.getConstraintName());
//			secConstraint.setAuthConstraint(authentication);
//			for (String authRole : roles) {
//				secConstraint.addAuthRole(authRole);
//			}
//			secConstraint.setUserConstraint(dataConstraint);
//			context.addConstraint(secConstraint);
//		}
//
//		SecurityCollection collection = new SecurityCollection();
//		collection.addMethod(mappingMethod);
//		collection.addPattern(url);
//
//		secConstraint.addCollection(collection);
//
//	}
//
//	@Override
//	public void removeSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel) {
//		if (secMapModel == null) {
//			return;
//		}
//
//		LOG.debug("remove security contstraint mapping [{}]", secMapModel);
//		final Context context = findOrCreateContext(secMapModel.getContextModel());
//
//		SecurityConstraint toRemove = null;
//		for (SecurityConstraint sc: context.findConstraints()) {
//			if (sc.getDisplayName().equalsIgnoreCase(secMapModel.getConstraintName())) {
//				toRemove = sc;
//				break;
//			}
//		}
//
//		if (toRemove != null) {
//			context.removeConstraint(toRemove);
//		}
//	}
//
//	@Override
//	public LifeCycle getContext(final OsgiContextModel model) {
//		final Context context = findOrCreateContext(model);
//		return new LifeCycle() {
//			@Override
//			public void start() throws Exception {
//				ContainerBase host = (ContainerBase) TomcatServerWrapper.this.server
//						.getHost();
//				host.setStartChildren(true);
//
//				if (!context.getState().isAvailable()) {
//					LOG.info("server is available, in state {}",
//							context.getState());
//					context.start();
//				}
//			}
//
//			@Override
//			public void stop() throws Exception {
//				context.stop();
//			}
//		};
//	}
//
//	private Context findOrCreateContext(final ElementModel elementModel) {
//		NullArgumentException.validateNotNull(elementModel, "model");
//		return findOrCreateContext(elementModel.getContextModel());
//	}
//
//	private Context findOrCreateContext(final OsgiContextModel contextModel) {
//		HttpContext httpContext = contextModel.getHttpContext();
//		Context context = contextMap.get(httpContext);
//
//		if (context == null) {
//			context = server.findContext(contextModel);
//		}
//		if (context == null) {
//			context = createContext(contextModel);
//		}
//		return context;
//	}
//
//	private Valve getAuthenticatorValve(String authMethod) {
//		String authUpper = authMethod.toUpperCase(Locale.ROOT);
//		// this is the content of org/apache/catalina/startup/Authenticators.properties
//		switch (authUpper) {
//		case "BASIC":
//			return new BasicAuthenticator();
//		case "CLIENT-CERT":
//			return new SSLAuthenticator();
//		case "DIGEST":
//			return new DigestAuthenticator();
//		case "FORM":
//			return new FormAuthenticator();
//		case "SPNEGO":
//			return new SpnegoAuthenticator();
//		case "NONE":
//			return new NonLoginAuthenticator();
//		default:
//			return getAuthenticator(authUpper);
//		}
//	}
//
//	private Valve getAuthenticator(String method) {
//		ServiceLoader<AuthenticatorService> sl = ServiceLoader.load(AuthenticatorService.class, getClass().getClassLoader());
//		for (AuthenticatorService svc : sl) {
//			try {
//				Valve auth = svc.getAuthenticatorService(method, Valve.class);
//				if (auth != null) {
//					return auth;
//				}
//			} catch (Throwable t) {
//				LOG.debug("Unable to load AuthenticatorService for: " + method, t);
//			}
//		}
//		return null;
//	}
//
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
//	private Digester createContextDigester() {
//		Digester digester = new Digester();
//		digester.setValidating(false);
//		digester.setRulesValidation(true);
//		HashMap<Class<?>, List<String>> fakeAttributes = new HashMap<>();
//		ArrayList<String> attrs = new ArrayList<>();
//		attrs.add("className");
//		fakeAttributes.put(Object.class, attrs);
//		digester.setFakeAttributes(fakeAttributes);
//		RuleSet contextRuleSet = new ContextRuleSet("", false);
//		digester.addRuleSet(contextRuleSet);
//		RuleSet namingRuleSet = new NamingRuleSet("Context/");
//		digester.addRuleSet(namingRuleSet);
//		return digester;
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
//
//	private void configureJspConfigDescriptor(Context context, OsgiContextModel model) {
//
//		Boolean elIgnored = model.getJspElIgnored();
//		Boolean isXml = model.getJspIsXml();
//		Boolean scriptingInvalid = model.getJspScriptingInvalid();
//
//
//		Collection<JspPropertyGroupDescriptor> jspPropertyGroupDescriptors = null;
//		Collection<TaglibDescriptor> taglibs = null;
//
//		if (elIgnored != null || isXml != null || scriptingInvalid != null
//				|| model.getJspIncludeCodes() != null
//				|| model.getJspUrlPatterns() != null
//				|| model.getJspIncludePreludes() != null) {
//			JspPropertyGroup jspPropertyGroup = new JspPropertyGroup();
//			JspPropertyGroupDescriptorImpl jspPropertyGroupDescriptor = new JspPropertyGroupDescriptorImpl(jspPropertyGroup);
//			if (jspPropertyGroupDescriptors == null) {
//				jspPropertyGroupDescriptors = new ArrayList<>();
//			}
//			jspPropertyGroupDescriptors.add(jspPropertyGroupDescriptor);
//
//			if (model.getJspIncludeCodes() != null) {
//				for (String includeCoda : model.getJspIncludeCodes()) {
//					jspPropertyGroup.addIncludeCoda(includeCoda);
//				}
//			}
//
//			if (model.getJspUrlPatterns() != null) {
//				for (String urlPattern : model.getJspUrlPatterns()) {
//					jspPropertyGroup.addUrlPattern(urlPattern);
//				}
//			}
//
//			if (model.getJspIncludePreludes() != null) {
//				for (String prelude : model.getJspIncludePreludes()) {
//					jspPropertyGroup.addIncludePrelude(prelude);
//				}
//			}
//
//			if (elIgnored != null) {
//				jspPropertyGroup.setElIgnored(elIgnored.toString());
//			}
//			if (isXml != null) {
//				jspPropertyGroup.setIsXml(isXml.toString());
//			}
//			if (scriptingInvalid != null) {
//				jspPropertyGroup.setScriptingInvalid(scriptingInvalid.toString());
//			}
//
//		}
//
//
//		if (model.getTagLibLocation() != null || model.getTagLibUri() != null) {
//			TaglibDescriptorImpl tagLibDescriptor = new TaglibDescriptorImpl(model.getTagLibLocation(), model.getTagLibUri());
//			if (taglibs == null) {
//				taglibs = new ArrayList<>();
//			}
//			taglibs.add(tagLibDescriptor);
//		}
//
//		if (jspPropertyGroupDescriptors != null || taglibs != null) {
//			JspConfigDescriptor jspConfig = new JspConfigDescriptorImpl(jspPropertyGroupDescriptors, taglibs);
//			((Context) context.getServletContext()).setJspConfigDescriptor(jspConfig);
//		}
//	}
//
//	private Context findContext(final OsgiContextModel contextModel) {
//		return server.findContext(contextModel);
//	}
//
//	private Context findContext(final ElementModel elementModel) {
//		return findContext(elementModel.getContextModel());
//	}
//
//	/**
//	 * Returns a list of servlet context attributes out of configured properties
//	 * and attribues containing the bundle context associated with the bundle
//	 * that created the model (web element).
//	 *
//	 * @param bundleContext bundle context to be set as attribute
//	 * @return context attributes map
//	 */
//	private Map<String, Object> getContextAttributes(
//			final BundleContext bundleContext) {
//		final Map<String, Object> attributes = new HashMap<>();
//		if (contextAttributes != null) {
//			attributes.putAll(contextAttributes);
//		}
////		attributes.put(WebContainerConstants.BUNDLE_CONTEXT_ATTRIBUTE,
////				bundleContext);
//		attributes
//				.put("org.springframework.osgi.web.org.osgi.framework.BundleContext",
//						bundleContext);
//		return attributes;
//	}
//
//	@Override
//	public void addWelcomeFiles(WelcomeFileModel model) {
//		final Context context = findOrCreateContext(model.getContextModel());
//
//		for (String welcomeFile : model.getWelcomeFiles()) {
//			context.addWelcomeFile(welcomeFile);
//		}
//	}
//
//	@Override
//	public void removeWelcomeFiles(WelcomeFileModel model) {
//		final Context context = findOrCreateContext(model.getContextModel());
//
//		LOG.info("removing welcome files");
//		for (String welcomeFile : model.getWelcomeFiles()) {
//			context.removeWelcomeFile(welcomeFile);
//		}
//	}

//	private final class WrappedServletLifecycleListener implements LifecycleListener {
//		private final Context context;
//		private final String servletName;
//		private final ServletModel model;
//
//		private WrappedServletLifecycleListener(Context context, String servletName, ServletModel model) {
//			this.context = context;
//			this.servletName = servletName;
//			this.model = model;
//		}
//
//		@Override
//		public void lifecycleEvent(LifecycleEvent event) {
//			if (Lifecycle.BEFORE_START_EVENT.equalsIgnoreCase(event
//					.getType())) {
//				Map<String, ? extends ServletRegistration> servletRegistrations = context
//						.getServletContext()
//						.getServletRegistrations();
//				if (!servletRegistrations.containsKey(servletName)) {
//					LOG.debug("need to re-register the servlet ...");
//					createServletWrapper(model, context,
//							servletName);
//				}
//			}
//		}
//	}
//
//	private final class SerlvetClassNameLifecycleListener implements LifecycleListener {
//		private final Context context;
//		private final Wrapper sw;
//		private final ServletModel model;
//		private final String servletName;
//
//		private SerlvetClassNameLifecycleListener(Context context, Wrapper sw, ServletModel model, String servletName) {
//			this.context = context;
//			this.sw = sw;
//			this.model = model;
//			this.servletName = servletName;
//		}
//
//		@Override
//		public void lifecycleEvent(LifecycleEvent event) {
//			if (Lifecycle.AFTER_START_EVENT
//					.equalsIgnoreCase(event.getType())) {
//				Map<String, ? extends ServletRegistration> servletRegistrations = context
//						.getServletContext()
//						.getServletRegistrations();
//				//CHECKSTYLE:OFF
//				if (!servletRegistrations
//						.containsKey(servletName)) {
//					LOG.debug("need to re-register the servlet ...");
//					sw.setServletClass(model
//							.getServletClass().getName());
//
//					addServletWrapper(sw, servletName,
//							context, model);
//				}
//				//CHECKSTYLE:ON
//			}
//		}
//	}
//
//	private final class ServletLifecycleListener implements LifecycleListener {
//		private final Context context;
//		private final String servletName;
//		private final ServletModel model;
//
//		private ServletLifecycleListener(Context context, String servletName, ServletModel model) {
//			this.context = context;
//			this.servletName = servletName;
//			this.model = model;
//		}
//
//		@Override
//		public void lifecycleEvent(LifecycleEvent event) {
//			if (Lifecycle.AFTER_START_EVENT
//					.equalsIgnoreCase(event.getType())) {
//				Map<String, ? extends ServletRegistration> servletRegistrations = context
//						.getServletContext()
//						.getServletRegistrations();
//				//CHECKSTYLE:OFF
//				if (!servletRegistrations
//						.containsKey(servletName)) {
//					LOG.debug("need to re-register the servlet ...");
//					createServletWrapper(model, context,
//							servletName);
//				}
//				//CHECKSTYLE:ON
//			}
//		}
//	}
//
//	private final class FilterLifecycleListener implements LifecycleListener {
//		private final FilterModel filterModel;
//		private final Context context;
//
//		private FilterLifecycleListener(FilterModel filterModel, Context context) {
//			this.filterModel = filterModel;
//			this.context = context;
//		}
//
//		@Override
//		public void lifecycleEvent(LifecycleEvent event) {
//			if (Lifecycle.BEFORE_START_EVENT.equalsIgnoreCase(event
//					.getType())) {
//				FilterRegistration.Dynamic filterRegistration = null;
//				if (filterModel.getFilter() != null) {
//					filterRegistration = context
//							.getServletContext().addFilter(
//									filterModel.getName(),
//									filterModel.getFilter());
//
//				} else if (filterModel.getFilterClass() != null) {
//					filterRegistration = context
//							.getServletContext().addFilter(
//									filterModel.getName(),
//									filterModel.getFilterClass());
//				}
//
//				if (filterRegistration == null) {
//					filterRegistration = (Dynamic) context
//							.getServletContext().getFilterRegistration(
//									filterModel.getName());
//					if (filterRegistration == null) {
//						LOG.error("Can't register Filter due to unknown reason!");
//						return;
//					}
//				}
//
//				filterRegistration.setAsyncSupported(filterModel.isAsyncSupported());
//
//				if (filterModel.getServletNames() != null
//						&& filterModel.getServletNames().length > 0) {
//					filterRegistration.addMappingForServletNames(
//							getDispatcherTypes(filterModel), /*
//															 * TODO get
//        													 * asynch
//        													 * supported?
//        													 */false,
//							filterModel.getServletNames());
//				} else if (filterModel.getUrlPatterns() != null
//						&& filterModel.getUrlPatterns().length > 0) {
//					filterRegistration.addMappingForUrlPatterns(
//							getDispatcherTypes(filterModel), /*
//        													 * TODO get
//        													 * asynch
//        													 * supported?
//        													 */false,
//							filterModel.getUrlPatterns());
//				} else {
//					throw new AddFilterException(
//							"cannot add filter to the context; at least a not empty list of servlet names or URL patterns in exclusive mode must be provided: "
//									+ filterModel);
//				}
//				filterRegistration.setInitParameters(filterModel
//						.getInitParams());
//			}
//		}
//	}
}
