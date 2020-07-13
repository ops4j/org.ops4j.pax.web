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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParserFactory;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.OpenListener;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.ConfidentialPortManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.core.ContextClassLoaderSetupAction;
import io.undertow.servlet.core.ManagedFilter;
import io.undertow.servlet.core.ManagedFilters;
import io.undertow.servlet.core.ManagedListener;
import io.undertow.servlet.core.ManagedServlet;
import io.undertow.servlet.handlers.ServletHandler;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.LogConfiguration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.servlet.Default404Servlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedServlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.task.BatchVisitor;
import org.ops4j.pax.web.service.spi.task.EventListenerModelChange;
import org.ops4j.pax.web.service.spi.task.FilterModelChange;
import org.ops4j.pax.web.service.spi.task.FilterStateChange;
import org.ops4j.pax.web.service.spi.task.OpCode;
import org.ops4j.pax.web.service.spi.task.OsgiContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletModelChange;
import org.ops4j.pax.web.service.spi.task.WelcomeFileModelChange;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.undertow.internal.configuration.ResolvingContentHandler;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.IoSubsystem;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.SecurityRealm;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.Server;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.SocketBinding;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.UndertowConfiguration;
import org.ops4j.pax.web.service.undertow.internal.web.UndertowResourceServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xnio.IoUtils;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;

/**
 * Wrapper of actual {@link Undertow} server that can translate generic model changes into Undertow configuration
 * and deployments.
 */
class UndertowServerWrapper implements BatchVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(UndertowServerWrapper.class);

	/** An <em>entry</em> to OSGi runtime to lookup other bundles if needed (to get their ClassLoader) */
	private final Bundle paxWebUndertowBundle;
	/** Outside of OSGi, let's use passed ClassLoader */
	private final ClassLoader classLoader;

	/**
	 * <p>In Tomcat and Jetty we keep here a single reference to actual Tomcat/Jetty embedded server. With Undertow
	 * everything is a bit more flexible. Before Pax Web 8 we just had an instance of {@link Undertow} server, but
	 * after checking that Wildfly doesn't do it (it has similar {@code org.wildfly.extension.undertow.Server} for this
	 * purpose), we'll keep the Undertow internals as pieces - most important is a list of "accepting channels" that
	 * all can invoke single "root handler".</p>
	 *
	 * <p>The "root handler" is common to all connectors/listeners, but each listener may wrap the common "root
	 * handler" in listener-specific wrappers that can be configured using {@code undertow.xml}.</p>
	 *
	 * <p>In terms of Undertow, the "connector" is represented by {@link OpenListener} which is called by
	 * {@link AcceptingChannel} glued together using
	 * {@link org.xnio.ChannelListeners#openListenerAdapter(org.xnio.ChannelListener)}.</p>
	 *
	 * <p>These "listeners" are configured using {@code <server>/<http(s)-listener>} elements in
	 * {@code undertow.xml}.</p>
	 */
	private final Map<String, UndertowFactory.AcceptingChannelWithAddress> listeners = new HashMap<>();

	private final Map<String, XnioWorker> workers = new HashMap<>();
	private final Map<String, ByteBufferPool> bufferPools = new HashMap<>();

	/** Mapping from non-secure ports to secure ports - collected when reading XML listener definitions */
	private final Map<Integer, Integer> securePortMapping = new HashMap<>();

	/**
	 * Root {@link HttpHandler} for Undertow - it may be anything that eventually delegates to
	 * {@link io.undertow.server.handlers.PathHandler}. Single "root handler" is shared by all the listeners, which
	 * may (but don't have to) wrap it in listener-specific handler wrappers.
	 */
	private HttpHandler rootHandler;

	/**
	 * Top-level, or wrapped (inside different {@link #rootHandler}) {@link PathHandler} that contains 1:1
	 * mapping to actual <em>contexts</em>.
	 */
	private PathHandler pathHandler;

	private final UndertowFactory undertowFactory;

	/**
	 * Single <em>container</em> for all Undertow contexts. It can directly map context path to
	 * {@link DeploymentManager} instances.
	 */
	private final ServletContainer servletContainer = Servlets.newContainer();

	// A word of explanation on strange Undertow model...
	//  - io.undertow.servlet.api.ServletContainer - a single "container" to manage deployments through
	//    "deployment managers". This "servlet container" is not used at all by Undertow at runtime! It's kind of
	//    external context path -> context mapping. The actual HttpHandlers created for each "deployment"
	//    should be manually added to running server anyway
	//  - io.undertow.servlet.api.DeploymentInfo - a POJO representing everything related to a servlet context
	//    (i.e., actual web application)
	//  - io.undertow.servlet.api.DeploymentManager - object created by "servlet container" to represent
	//    "deployment info". After adding "deployment info" to "servlet container", it is turned into "deployment
	//    manager" that controls the lifecycle of a "deployment"
	//  - io.undertow.servlet.api.Deployment is something that's managed by "deployment manager" and it's created after
	//    telling "deployment manager" to "deploy()" itself
	//
	// Now the problem is that while "deployment" (io.undertow.servlet.api.Deployment) can be used to
	// io.undertow.servlet.api.Deployment.getServlets() to get io.undertow.servlet.core.ManagedServlets instance
	// to which we can add new servlets, this ManagedServlets class CAN'T be used to remove existing servlets...
	//
	// Because deploy() clones the original deployment, we can't have single DeploymentManager (or Deployment)
	// being used to add/remove servlets (and other web elements)
	//
	// Unfortunately, even io.undertow.servlet.api.DeploymentInfo object can't be used to fully control the
	// "servlet context". While it returns servlets and filters via direct reference to internal map, filter
	// mapping is returned as shallow copy, so we CAN'T remove existing mappings...

	/**
	 * <em>Outer handlers</em> for all the contexts - responsible for setting proper request wrappers. This can
	 * be done in cleaner way in Jetty and Tomcat.
	 */
	private final Map<String, PaxWebOuterHandlerWrapper> wrappingHandlers = new HashMap<>();

	/**
	 * Handlers that call {@link org.osgi.service.http.HttpContext#handleSecurity} and/or
	 * {@link org.osgi.service.http.context.ServletContextHelper#handleSecurity}.
	 */
	private final Map<String, PaxWebSecurityHandler> securityHandlers = new HashMap<>();

	// TODO: the three below fields are the same in Jetty, Tomcat and Undertow

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

	/** JAXB context used to unmarshall Undertow XML configuration */
	private JAXBContext jaxb = null;

	/** Servlet to use when no servlet is mapped - to ensure that preprocessors and filters are run correctly. */
	private final Default404Servlet default404Servlet = new Default404Servlet();

	// configuration read from undertow.xml
	private UndertowConfiguration undertowConfiguration;

	UndertowServerWrapper(Configuration config, UndertowFactory undertowFactory,
			Bundle paxWebUndertowBundle, ClassLoader classLoader) {
		this.configuration = config;
		this.undertowFactory = undertowFactory;
		this.paxWebUndertowBundle = paxWebUndertowBundle;
		this.classLoader = classLoader;
	}

	// --- lifecycle and configuration methods

	/**
	 * Here's where Undertow is being rebuild at {@link Undertow} level (not {@link ServletContainer} level).
	 * This is were <em>global</em> objects are configured (listeners, global filters, ...)
	 */
	public void configure() throws Exception {
		LOG.info("Creating Undertow server instance using configuration properties.");

		// initially rootHandler == pathHandler without any particular path registered
		pathHandler = Handlers.path();
		rootHandler = pathHandler;

		// apply single (if exists) external undertow.xml file by reading it according to Wildfly XSDs,
		// but using Pax Web specific handlers
		// before Pax Web 8 thre was also etc/undertow.properties with identity manager properties,
		// but it's no longer supported
		try {
			applyUndertowConfiguration();
		} catch (Exception e) {
			throw new IllegalArgumentException("Problem configuring Undertow server: " + e.getMessage(), e);
		}

		// Configure NCSA RequestLogHandler if needed
		if (configuration.logging().isLogNCSAFormatEnabled()) {
			configureRequestLog();
		}

		// If external configuration added some connectors, we have to ensure they match declaration from
		// PID config: org.osgi.service.http.enabled and org.osgi.service.http.secure.enabled
		verifyListenerConfiguration();

//					for (Context context : contextMap.values()) {
//						try {
//							context.setSessionPersistenceManager(sessionPersistenceManager);
//							context.setDefaultSessionTimeoutInMinutes(defaultSessionTimeoutInMinutes);
//							context.start();
//						} catch (Exception e) {
//							LOG.error("Could not start the servlet context for context path [" + context.getContextModel().getContextName() + "]", e);
//						}
//					}
//
//					builder.setHandler(rootHandler);
//					server = builder.build();
	}

	/**
	 * <p>This method parses existing {@code undertow.xml} file, which is the preferred method of Undertow
	 * configuration</p>
	 */
	private void applyUndertowConfiguration() throws Exception {
		File[] locations = configuration.server().getConfigurationFiles();
		if (locations.length == 0) {
			LOG.info("No external Undertow configuration file specified. Default/PID configuration will be used.");
			return;
		} else if (locations.length > 1) {
			LOG.warn("Can't specify Undertow configuration using more than one XML file. Skipping XML configuration.");
			return;
		} else {
			LOG.info("Processing Undertow configuration using file: {}", locations[0]);
		}

		File xmlConfig = locations[0];

//			case PROPERTIES:
//				LOG.info("Using \"" + undertowResource + "\" to read additional configuration for Undertow");
//				configureIdentityManager(undertowResource);
//				// do not break - go to standard PID configuration
//			case PID:
//				LOG.info("Using \"org.ops4j.pax.url.web\" PID to configure Undertow");
//				rootHandler = configureUndertow(configuration, builder, rootHandler);
//				break;
//		}

		jaxb = JAXBContext.newInstance("org.ops4j.pax.web.service.undertow.internal.configuration.model", classLoader);
		Unmarshaller unmarshaller = jaxb.createUnmarshaller();
		UnmarshallerHandler unmarshallerHandler = unmarshaller.getUnmarshallerHandler();

		// indirect unmarshalling with property resolution *inside XML attribute values*
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		XMLReader xmlReader = spf.newSAXParser().getXMLReader();

		xmlReader.setContentHandler(new ResolvingContentHandler(configuration.all(), unmarshallerHandler));
		try (InputStream stream = new FileInputStream(xmlConfig)) {
			xmlReader.parse(new InputSource(stream));
		}

		this.undertowConfiguration = (UndertowConfiguration) unmarshallerHandler.getResult();
//		if (cfg == null
//				|| cfg.getSocketBindings().size() == 0
//				|| cfg.getInterfaces().size() == 0
//				|| cfg.getSubsystem() == null
//				|| cfg.getSubsystem().getServer() == null) {
//			throw new IllegalArgumentException("Problem configuring Undertow server using \"" + xmlConfig
//					+ "\": incomplete XML configuration");
//		}
		undertowConfiguration.init();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Undertow XML configuration parsed correctly: {}", undertowConfiguration);
		}

		// collect extra workers and byte buffer pools
		IoSubsystem io = undertowConfiguration.getIoSubsystem();
		if (io != null) {
			for (IoSubsystem.Worker worker : io.getWorkers()) {
				workers.put(worker.getName(), undertowFactory.createWorker(worker));
			}
			for (IoSubsystem.BufferPool pool : io.getBuferPools()) {
				bufferPools.put(pool.getName(), undertowFactory.createBufferPool(pool));
			}
		}

		// listeners will be checked by verifyConnectorConfiguration() later
		List<Server.HttpListener> httpListeners = null;
		List<Server.HttpsListener> httpsListeners = null;
		if (undertowConfiguration.getSubsystem() != null && undertowConfiguration.getSubsystem().getServer() != null) {
			httpListeners = undertowConfiguration.getSubsystem().getServer().getHttpListeners();
			httpsListeners = undertowConfiguration.getSubsystem().getServer().getHttpsListeners();
		}

		// http listener(s)
		if (httpListeners != null) {
			for (Server.HttpListener http : httpListeners) {
				String listenerName = http.getName();
				if (!http.isEnabled()) {
					LOG.debug("Skipping disabled Undertow http listener \"{}\"", listenerName);
					continue;
				}
				UndertowConfiguration.BindingInfo binding = undertowConfiguration.bindingInfo(http.getSocketBindingName());
				for (String address : binding.getAddresses()) {
					LOG.info("Configuring Undertow http listener for address " + address + ":" + binding.getPort());

					XnioWorker workerForListener = getWorker(http.getWorkerName());
					ByteBufferPool bufferPoolForListener = getBufferPool(http.getBufferPoolName());

					// this is specific to non-secure listener
					// see org.wildfly.extension.undertow.Server#lookupSecurePort
					if (http.getRedirectSocket() != null) {
						SocketBinding secureSocketBinding = undertowConfiguration.socketBinding(http.getRedirectSocket());
						if (secureSocketBinding != null) {
							this.securePortMapping.put(binding.getPort(), secureSocketBinding.getPort());
						}
					}

					InetSocketAddress inetAddress = new InetSocketAddress(address, binding.getPort());
					AcceptingChannel<? extends StreamConnection> listener = undertowFactory.createListener(
							configuration, http, rootHandler, null,
							workerForListener, bufferPoolForListener,
							inetAddress
					);
					listeners.put(http.getName(), new UndertowFactory.AcceptingChannelWithAddress(listener, inetAddress));
				}
			}
		}

		// https listener(s)
		if (httpsListeners != null) {
			for (Server.HttpsListener https : httpsListeners) {
				String listenerName = https.getName();
				if (!https.isEnabled()) {
					LOG.debug("Skipping disabled Undertow https listener \"{}\"", listenerName);
					continue;
				}
				UndertowConfiguration.BindingInfo binding = undertowConfiguration.bindingInfo(https.getSocketBindingName());
				for (String address : binding.getAddresses()) {
					LOG.info("Configuring Undertow https listener for address " + address + ":" + binding.getPort());

					XnioWorker workerForListener = getWorker(https.getWorkerName());
					ByteBufferPool bufferPoolForListener = getBufferPool(https.getBufferPoolName());

					if (https.getSslContext() != null) {
						LOG.warn("ssl-context reference attribute from https-listener listener is not supported"
								+ " in Pax Web. Please use security-realm reference attribute instead.");
					}
					SecurityRealm realm = undertowConfiguration.securityRealm(https.getSecurityRealm());
					if (realm == null) {
						throw new IllegalArgumentException("No security realm with name \"" + https.getSecurityRealm()
								+ "\" available for \"" + https.getName() + "\" https listener.");
					}

					InetSocketAddress inetAddress = new InetSocketAddress(address, binding.getPort());
					AcceptingChannel<? extends StreamConnection> listener = undertowFactory.createListener(
							configuration, https, rootHandler, realm,
							workerForListener, bufferPoolForListener,
							inetAddress
					);
					listeners.put(https.getName(), new UndertowFactory.AcceptingChannelWithAddress(listener, inetAddress));
				}
			}
		}

//		builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, recordRequestStartTime);
//		if (configuration.server().getConnectorIdleTimeout() != null) {
//			builder.setServerOption(UndertowOptions.IDLE_TIMEOUT, configuration.server().getConnectorIdleTimeout());
//		}
//
//		// identity manager - looked up in "default" security realm
//		SecurityRealm defaultRealm = cfg.securityRealm("default");
//		if (defaultRealm != null) {
//			SecurityRealm.JaasAuth jaasAuth = defaultRealm.getAuthentication().getJaas();
//			SecurityRealm.PropertiesAuth propertiesAuth = defaultRealm.getAuthentication().getProperties();
//			if (jaasAuth != null) {
//				String userPrincipalClassName = defaultRealm.getUserPrincipalClassName();
//				if (userPrincipalClassName == null || "".equals(userPrincipalClassName.trim())) {
//					userPrincipalClassName = "java.security.Principal";
//				}
//				Set<String> rolePrincipalClassNames = new LinkedHashSet<>(defaultRealm.getRolePrincipalClassNames());
//				identityManager = new JaasIdentityManager(jaasAuth.getName(),
//						userPrincipalClassName, rolePrincipalClassNames);
//			} else if (propertiesAuth != null) {
//				File userBase = new File(propertiesAuth.getPath());
//				if (!userBase.isFile()) {
//					throw new IllegalArgumentException(userBase.getCanonicalPath() + " is not accessible. Can't load users/groups information.");
//				}
//				Properties userProperties = new Properties();
//				Map<String, String> map = new HashMap<>();
//				try (FileInputStream stream = new FileInputStream(userBase)) {
//					userProperties.load(stream);
//					for (String user : userProperties.stringPropertyNames()) {
//						map.put(user, userProperties.getProperty(user));
//					}
//				}
//				identityManager = new PropertiesIdentityManager(map);
//			}
//		}
//
//		// /undertow/subsystem/server/host/location - file handlers for static context paths.
//		if (cfg.getSubsystem().getServer().getHost() != null) {
//			for (Server.Host.Location location : cfg.getSubsystem().getServer().getHost().getLocation()) {
//				String context = location.getName();
//				String handlerRef = location.getHandler();
//				UndertowSubsystem.FileHandler fileHandler = cfg.handler(handlerRef);
//				if (fileHandler == null) {
//					throw new IllegalArgumentException("No handler with name \"" + location.getHandler() + "\" available for " + location.getName() + " location.");
//				}
//				File base = new File(fileHandler.getPath());
//				if (!base.isDirectory()) {
//					throw new IllegalArgumentException(base.getCanonicalPath() + " is not accessible. Can't configure handler for " + location.getName() + " location.");
//				}
//				// fileHandler.path is simply filesystem directory
//				ResourceHandler rh = new ResourceHandler(new FileResourceManager(base, 4096));
//				if (cfg.getSubsystem().getServletContainer() != null) {
//					rh.setWelcomeFiles();
//					for (org.ops4j.pax.web.service.undertow.internal.configuration.model.ServletContainer.WelcomeFile wf : cfg.getSubsystem().getServletContainer().getWelcomeFiles()) {
//						rh.addWelcomeFiles(wf.getName());
//					}
//				}
//				if (rootHandler instanceof PathHandler) {
//					if (LOG.isDebugEnabled()) {
//						LOG.debug("Adding resource handler for location \"" + context + "\" and base path \"" + base.getCanonicalPath() + "\".");
//					}
//					((PathHandler) rootHandler).addPrefixPath(context, rh);
//				}
//			}
//		}
//
//		// global filters (subsystem/filters/response-header and subsystem/filters/filter)
//		if (cfg.getSubsystem().getServer().getHost() != null) {
//			for (Server.Host.FilterRef fr : cfg.getSubsystem().getServer().getHost().getFilterRef()) {
//				UndertowSubsystem.AbstractFilter filter = cfg.filter(fr.getName());
//				if (filter == null) {
//					throw new IllegalArgumentException("No filter with name \"" + fr.getName() + "\" available.");
//				}
//				rootHandler = filter.configure(rootHandler, fr.getPredicate());
//			}
//		}
//
//		// session configuration and persistence
//		this.defaultSessionTimeoutInMinutes = 30;
//		try {
//			if (cfg.getSubsystem().getServletContainer() != null) {
//				String defaultSessionTimeout = cfg.getSubsystem().getServletContainer().getDefaultSessionTimeout();
//				if (defaultSessionTimeout != null && !"".equals(defaultSessionTimeout)) {
//					this.defaultSessionTimeoutInMinutes = Integer.parseInt(defaultSessionTimeout);
//				}
//			}
//		} catch (NumberFormatException ignored) {
//		}
//
//		org.ops4j.pax.web.service.undertow.internal.configuration.model.ServletContainer.PersistentSessionsConfig persistentSessions = cfg.getSubsystem().getServletContainer() == null ? null
//				: cfg.getSubsystem().getServletContainer().getPersistentSessions();
//		if (persistentSessions == null) {
//			// no sessions, but let's use InMemorySessionPersistence
//			LOG.info("Using in-memory session persistence");
//			sessionPersistenceManager = new InMemorySessionPersistence();
//		} else {
//			if (persistentSessions.getPath() != null && !"".equals(persistentSessions.getPath().trim())) {
//				// file persistence manager
//				File sessionsDir = new File(persistentSessions.getPath());
//				sessionsDir.mkdirs();
//				LOG.info("Using file session persistence. Location: " + sessionsDir.getCanonicalPath());
//				sessionPersistenceManager = new FileSessionPersistence(sessionsDir);
//			} else {
//				// in memory persistence manager
//				LOG.info("No path configured for persistent-sessions. Using in-memory session persistence.");
//				sessionPersistenceManager = new InMemorySessionPersistence();
//			}
//		}
	}

	private XnioWorker getWorker(String workerName) {
		if (workerName != null) {
			if (!workers.containsKey(workerName)) {
				throw new IllegalArgumentException("No worker named \"" + workerName + "\" is configured");
			}
			return workers.get(workerName);
		} else {
			return undertowFactory.getDefaultWorker(configuration);
		}
	}

	private ByteBufferPool getBufferPool(String bufferPoolName) {
		if (bufferPoolName != null) {
			if (!bufferPools.containsKey(bufferPoolName)) {
				throw new IllegalArgumentException("No buffer pool named \"" + bufferPoolName + "\" is configured");
			}
			return bufferPools.get(bufferPoolName);
		} else {
			return undertowFactory.getDefaultBufferPool();
		}
	}

	/**
	 * External configuration may specify listeners but we may have to add default ones if they're missing
	 */
	private void verifyListenerConfiguration() {
		boolean httpEnabled = configuration.server().isHttpEnabled();
		Integer httpPort = configuration.server().getHttpPort();

		boolean httpsEnabled = configuration.server().isHttpSecureEnabled();
		Integer httpsPort = configuration.server().getHttpSecurePort();

		String[] addresses = configuration.server().getListeningAddresses();

		// review connectors possibly configured from jetty.xml and check if they match configadmin configuration
		for (String address : addresses) {
			verifyListener(address, httpPort, httpEnabled, false,
					() -> undertowFactory.createDefaultListener(address, rootHandler, configuration));

			verifyListener(address, httpsPort, httpsEnabled, true,
					() -> undertowFactory.createSecureListener(address, rootHandler, configuration));
		}
	}

	/**
	 * Verify if current server configuration, possibly created from external {@code undertow.xml} matches the
	 * declaration from PID ({@link org.ops4j.pax.web.service.PaxWebConfig#PID_CFG_HTTP_ENABLED} and
	 * {@link org.ops4j.pax.web.service.PaxWebConfig#PID_CFG_HTTP_SECURE_ENABLED}).
	 *
	 * @param address
	 * @param port
	 * @param enabled
	 * @param secure
	 * @param listenerProvider {@link Supplier} used if connector has to be added to match PID configuration
	 */
	private void verifyListener(String address, Integer port,
			boolean enabled, boolean secure, Supplier<UndertowFactory.AcceptingChannelWithAddress> listenerProvider) {
		UndertowFactory.AcceptingChannelWithAddress expectedListener = null;

		boolean listenerFound = false;
		UndertowFactory.AcceptingChannelWithAddress backupListener = null;

		for (Iterator<UndertowFactory.AcceptingChannelWithAddress> iterator = listeners.values().iterator(); iterator.hasNext(); ) {
			UndertowFactory.AcceptingChannelWithAddress listener = iterator.next();
			String className = listener.getAcceptingChannel().getClass().getName();
			boolean secureMatch = secure && className.equals("io.undertow.protocols.ssl.UndertowAcceptingSslChannel");
			boolean nonSecureMatch = !secure && className.equals("org.xnio.nio.QueuedNioTcpServer");
			if (secureMatch || nonSecureMatch) {
				if (match(address, port, listener.getAddress())) {
					listenerFound = true;
					if (enabled) {
						expectedListener = listener;
					} else {
						LOG.warn("Listener defined in external configuration will be removed, "
								+ "because it's not enabled: {}", listener);
						iterator.remove();
					}
				} else {
					backupListener = listener;
				}
			}
		}
		if (expectedListener == null && backupListener != null) {
			expectedListener = backupListener;
		}
		if (listenerFound) {
			if (enabled) {
				LOG.info("Using configured {} as {} listener for address: {}:{}", expectedListener,
						(secure ? "secure" : "non secure"), address, port);
			}
		} else if (enabled) {
			LOG.info("Creating {} connector for address {}:{}", (secure ? "secure" : "non secure"), address, port);
			// we have to create a listener
			UndertowFactory.AcceptingChannelWithAddress listener = listenerProvider.get();
			listeners.put(UUID.randomUUID().toString(), listener);
		}
	}

	/**
	 * Check if the passed address can be treated as one <em>matching</em> the connector
	 * declared using PID properties.
	 *
	 * @param address1
	 * @param port1
	 * @param connector
	 * @return
	 */
	private boolean match(String address1, Integer port1, InetSocketAddress connector) {
		InetSocketAddress isa1 = address1 != null ? new InetSocketAddress(address1, port1)
				: new InetSocketAddress(port1);

		return isa1.equals(connector);
	}

	/**
	 * Configure request logging (AKA <em>NCSA logging</em>) for Undertow, using configuration properties.
	 */
	public void configureRequestLog() throws IOException {
		// XML configuration
		if (undertowConfiguration != null && undertowConfiguration.getSubsystem() != null
				&& undertowConfiguration.getSubsystem().getServer() != null
				&& undertowConfiguration.getSubsystem().getServer().getHost() != null
				&& undertowConfiguration.getSubsystem().getServer().getHost().getAccessLog() != null) {
			Server.Host.AccessLog accessLog = undertowConfiguration.getSubsystem().getServer().getHost().getAccessLog();

			Bundle bundle = FrameworkUtil.getBundle(UndertowServerController.class);
			ClassLoader loader = bundle.adapt(BundleWiring.class).getClassLoader();
			XnioWorker xnioWorker = undertowFactory.createLogWorker();

			AccessLogReceiver logReceiver = DefaultAccessLogReceiver.builder()
					.setLogWriteExecutor(xnioWorker)
					.setOutputDirectory(new File(accessLog.getDirectory()).toPath())
					.setLogBaseName(accessLog.getPrefix())
					.setLogNameSuffix(accessLog.getSuffix())
					.setRotate(Boolean.parseBoolean(accessLog.getRotate()))
					.build();

			rootHandler = new AccessLogHandler(rootHandler, logReceiver, accessLog.getPattern(),
					AccessLogHandler.class.getClassLoader());
			return;
		}

		LogConfiguration lc = configuration.logging();
		if (!lc.isLogNCSAFormatEnabled()) {
			return;
		}

		if (lc.getLogNCSADirectory() == null) {
			throw new IllegalArgumentException("Log directory for NCSA logging is not specified. Please set"
					+ " org.ops4j.pax.web.log.ncsa.directory property.");
		}
		File logDir = new File(lc.getLogNCSADirectory());
		if (logDir.isFile()) {
			throw new IllegalArgumentException(logDir + " is not a valid directory to store request logs");
		}

		LOG.info("NCSARequestlogging is using directory {}", lc.getLogNCSADirectory());

// properties based log configuration:

		if (lc.isLogNCSAFormatEnabled()) {
			String logNCSADirectory = lc.getLogNCSADirectory();
			String baseName = lc.getLogNCSAFile();

			Bundle bundle = FrameworkUtil.getBundle(UndertowServerController.class);
			ClassLoader loader = bundle == null ? getClass().getClassLoader()
					: bundle.adapt(BundleWiring.class).getClassLoader();
			XnioWorker xnioWorker = undertowFactory.createLogWorker();

			// String logNameSuffix = logNCSAFormat.substring(logNCSAFormat.lastIndexOf("."));
			// String logBaseName = logNCSAFormat.substring(0, logNCSAFormat.lastIndexOf("."));

			AccessLogReceiver logReceiver = DefaultAccessLogReceiver.builder()
					.setLogWriteExecutor(xnioWorker)
					.setOutputDirectory(new File(logNCSADirectory).toPath())
					.setLogBaseName(baseName)
					.setLogNameSuffix("log")
					.setRotate(true)
					.build();

			String format;
			// see io.undertow.server.handlers.accesslog.AccessLogHandler.handleCommonNames
			if (lc.isLogNCSAExtended()) {
				format = "combined";
			} else {
				format = "common";
			}

			rootHandler = new AccessLogHandler(rootHandler, logReceiver, format,
					AccessLogHandler.class.getClassLoader());
		}
	}

	/**
	 * Simply start Undertow server
	 * @throws Exception
	 */
	public void start() throws Exception {
		LOG.info("Starting {}", this);

		this.listeners.values().forEach(l -> l.getAcceptingChannel().resumeAccepts());
	}

	/**
	 * One-time operation that stops Undertow server. We should not be able to start it again.
	 *
	 * @throws Exception
	 */
	public void stop() throws Exception {
		LOG.info("Stopping {}", this);

		this.listeners.values().forEach(l -> IoUtils.safeClose(l.getAcceptingChannel()));

		servletContainer.listDeployments().forEach(d -> {
			DeploymentManager deployment = servletContainer.getDeployment(d);
			String contextPath = deployment.getDeployment().getServletContext().getContextPath();
			try {
				deployment.stop();
			} catch (ServletException e) {
				LOG.warn("Problem stopping deployment for context " + contextPath + ": " + e.getMessage(), e);
			}
		});

		this.workers.values().forEach(XnioWorker::shutdown);
		this.workers.clear();
		this.bufferPools.values().forEach(ByteBufferPool::close);
		this.bufferPools.clear();
		undertowFactory.closeDefaultPoolAndBuffer();
	}

	/**
	 * If state allows, this methods returns currently configured/started addresses of the listeners.
	 * @param useLocalPort
	 * @return
	 */
	public InetSocketAddress[] getAddresses(boolean useLocalPort) {
		if (listeners.size() == 0) {
			return null;
		}
		final List<InetSocketAddress> result = new ArrayList<>(listeners.size());
		listeners.values().forEach(ac -> result.add(ac.getAddress()));
		return result.toArray(new InetSocketAddress[0]);
	}

	// --- visitor methods for model changes

	@Override
	public void visit(ServletContextModelChange change) {
		ServletContextModel model = change.getServletContextModel();
		String contextPath = model.getContextPath();

		if (change.getKind() == OpCode.ADD) {
			LOG.info("Creating new Undertow context for {}", model);

			// meta info about "servlet context deployment"
			DeploymentInfo deployment = Servlets.deployment();

			deployment.setDeploymentName(model.getId());
			deployment.setDisplayName(model.getId());
			deployment.setContextPath(contextPath);
			deployment.setUrlEncoding(StandardCharsets.UTF_8.name());
			deployment.setEagerFilterInit(true);

			// that's only temporary class loader. It'll be changed when OsgiServletContext is created
			deployment.setClassLoader(classLoader);

			deployment.addServlet(new PaxWebServletInfo("default", default404Servlet, true).addMapping("/"));

			// In Jetty and Tomcat we can operate on FilterChains, here we have to split the OsgiFilterChain's
			// functionality into different HandlerWrappers:
			//  - to wrap request, so proper ServletContext is returned
			//  - to call preprocessors
			//  - to call handleSecurity()/finishSecurity()

			PaxWebOuterHandlerWrapper outerWrapper = new PaxWebOuterHandlerWrapper();
			this.wrappingHandlers.put(contextPath, outerWrapper);
			deployment.addOuterHandlerChainWrapper(outerWrapper);

			// TODO: ensure preprocessors work
//			PaxWebPreprocessorsHandler preprocessorWrapper = new PaxWebPreprocessorsHandler();
//			this.preprocessorsHandlers.put(contextPath, preprocessorWrapper);
//			deployment.addOuterHandlerChainWrapper(preprocessorWrapper);

			PaxWebSecurityHandler securityWrapper = new PaxWebSecurityHandler();
			this.securityHandlers.put(contextPath, securityWrapper);
			deployment.addSecurityWrapper(securityWrapper);

//							deployment.setConfidentialPortManager(getConfidentialPortManager());
//							// d.setServletStackTraces(servletContainer.getStackTraces());
//							BundleContext bundleContext = contextModel.getBundle().getBundleContext();
//							if (bundleContext != null) {
//								deployment.addServletContextAttribute(PaxWebConstants.BUNDLE_CONTEXT_ATTRIBUTE, bundleContext);
//								deployment.addServletContextAttribute("org.springframework.osgi.web.org.osgi.framework.BundleContext", bundleContext);
//							}
//							deployment.setResourceManager(this);
//							deployment.setIdentityManager(identityManager);

			DeploymentManager manager = servletContainer.addDeployment(deployment);
			// here's where Undertow-specific instance of javax.servlet.ServletContext is created
			manager.deploy();

			try {
				HttpHandler handler = manager.start();
				// actual registration of "context" in Undertow's path handler. There are no servlets,
				// filters and anything yet
				pathHandler.addPrefixPath(contextPath, handler);
			} catch (ServletException e) {
				throw new IllegalStateException("Can't create Undertow context "
						+ contextPath + ": " + e.getMessage(), e);
			}

			osgiContextModels.put(contextPath, new TreeSet<>());
		}
	}

	@Override
	public void visit(OsgiContextModelChange change) {
		OsgiContextModel osgiModel = change.getOsgiContextModel();
		ServletContextModel servletModel = change.getServletContextModel();

		String contextPath = osgiModel.getContextPath();
		DeploymentManager deploymentManager = servletContainer.getDeploymentByPath(contextPath);

		if (deploymentManager == null) {
			throw new IllegalStateException(osgiModel + " refers to unknown ServletContext for path " + contextPath);
		}

		ServletContext realServletContext = deploymentManager.getDeployment().getServletContext();

		if (change.getKind() == OpCode.ADD) {
			LOG.info("Adding {} to deployment info of {}", osgiModel, contextPath);

			// as with Jetty and Tomcat,
			// each unique OsgiServletContext (ServletContextHelper or HttpContext) is a facade for some, sometimes
			// shared by many osgi contexts, real ServletContext
			if (osgiServletContexts.containsKey(osgiModel)) {
				throw new IllegalStateException(osgiModel + " is already registered");
			}
			osgiServletContexts.put(osgiModel, new OsgiServletContext(realServletContext, osgiModel, servletModel));
			osgiContextModels.get(contextPath).add(osgiModel);
		}

		if (change.getKind() == OpCode.DELETE) {
			LOG.info("Removing {} from {}", osgiModel, contextPath);

			// TOCHECK: are there web elements associated with removed mapping for OsgiServletContext?
			osgiServletContexts.remove(osgiModel);
			osgiContextModels.get(contextPath).remove(osgiModel);
		}

		// there may be a change in what's the "best" (highest ranked) OsgiContextModel for given
		// ServletContextModel. This will became the "fallback" OsgiContextModel for chains without
		// target servlet (with filters only)
		OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
		if (highestRankedModel != null) {
			OsgiServletContext highestRankedContext = osgiServletContexts.get(highestRankedModel);
			// default "contexts" to handle security and class/resource loading
			wrappingHandlers.get(contextPath).setDefaultServletContext(highestRankedContext);
			securityHandlers.get(contextPath).setDefaultOsgiContextModel(highestRankedModel);
		} else {
			// TOCHECK: there should be no more web elements in the context, no OSGi mechanisms, just 404 all the time
			wrappingHandlers.get(contextPath).setDefaultServletContext(null);
			securityHandlers.get(contextPath).setDefaultOsgiContextModel(null);
		}

//			// manager (lifecycle manager of the deployment),
//			DeploymentManager manager = servletContainer.getDeploymentByPath(contextPath);
//			// the managed deployment
//			Deployment deployment = manager.getDeployment();
//			// and the deployment information
//			DeploymentInfo deploymentInfo = deployment.getDeploymentInfo();
//
//			// to make the code consistent, we could set DeploymentInfo's class loader here, but in fact, it's
//			// used only in situations we override anyway (like adding servlets to existing ServletContext)
////			ClassLoader classLoader = highestRankedModel.getOwnerBundle().adapt(BundleWiring.class).getClassLoader();
////			servletContainer.getDeploymentByPath(contextPath).undeploy();
////			deploymentInfo.setClassLoader(classLoader);
////			servletContainer.getDeploymentByPath(contextPath).deploy();
	}

	@Override
	public void visit(ServletModelChange change) {
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

				// manager (lifecycle manager of the deployment),
				DeploymentManager manager = servletContainer.getDeploymentByPath(contextPath);
				// the managed deployment
				Deployment deployment = manager.getDeployment();
				// and the deployment information
				DeploymentInfo deploymentInfo = deployment.getDeploymentInfo();

				// <servlet> - always associated with one of ServletModel's OsgiContextModels
				OsgiServletContext context = osgiServletContexts.get(osgiContext);

				// new servlet info
				ServletInfo info = new PaxWebServletInfo(model, osgiContext, context);

				boolean isDefaultResourceServlet = model.isResourceServlet();
				for (String pattern : model.getUrlPatterns()) {
					isDefaultResourceServlet &= "/".equals(pattern);
				}
				info.addInitParam("pathInfoOnly", Boolean.toString(!isDefaultResourceServlet));
				info.addInitParam("resolve-against-context-root", Boolean.toString(isDefaultResourceServlet));

				// when only adding new servlet, we can simply alter existing deployment
				// because this is possible (as required by methods like javax.servlet.ServletContext.addServlet())
				// we can't go the easy way when _removing_ servlets
				deploymentInfo.addServlet(info);
				deployment.getServlets().addServlet(info);
			});
			return;
		}

		if (change.getKind() == OpCode.DISABLE || change.getKind() == OpCode.DELETE) {
			for (Map.Entry<ServletModel, Boolean> entry : change.getServletModels().entrySet()) {
				ServletModel model = entry.getKey();
				if (!entry.getValue()) {
					continue;
				}
				LOG.info("Removing servlet {}", model);

				// proper order ensures that (assuming above scenario), for /c1, ocm2 will be chosen and ocm1 skipped
				model.getContextModels().forEach(osgiContextModel -> {
					String contextPath = osgiContextModel.getContextPath();

					LOG.debug("Removing servlet {} from context {}", model.getName(), contextPath);

					// take existing deployment manager and the deployment info from its deployment
					DeploymentManager manager = servletContainer.getDeploymentByPath(contextPath);
					DeploymentInfo deploymentInfo = manager.getDeployment().getDeploymentInfo();

					// let's immediately show that given context is no longer mapped
					pathHandler.removePrefixPath(contextPath);

					try {
						// manager needs to stop the deployment and get rid of it, because we
						// can't replace a deployment info within deployment manager
						manager.stop();
						manager.undeploy();
					} catch (ServletException e) {
						throw new RuntimeException("Problem stopping the deployment of context " + contextPath
								+ ": " + e.getMessage(), e);
					}

					// but we can reuse the deployment info - this is the only object from which we can remove
					// servlets
					deploymentInfo.getServlets().remove(model.getName());
					if (model.isResourceServlet() && Arrays.asList(model.getUrlPatterns()).contains("/")) {
						// we need to replace "/" servlet
						PaxWebServletInfo defaultServletInfo = new PaxWebServletInfo("default", default404Servlet, true);
						deploymentInfo.addServlet(defaultServletInfo.addMapping("/"));
					}
					manager = servletContainer.addDeployment(deploymentInfo);
					manager.deploy();

					// we've changed the deployment for given context path - new ServletContext was created, so
					// we have to propagate the change where needed
					ServletContext newRealServletContext = manager.getDeployment().getServletContext();
					this.osgiContextModels.get(contextPath).forEach(cm
							-> this.osgiServletContexts.get(cm).setContainerServletContext(newRealServletContext));

					try {
						HttpHandler handler = manager.start();
						pathHandler.addPrefixPath(contextPath, handler);
					} catch (ServletException e) {
						throw new IllegalStateException("Can't redeploy Undertow context "
								+ contextPath + ": " + e.getMessage(), e);
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
		Map<String, TreeSet<FilterModel>> contextFilters = change.getContextFilters();

		for (Map.Entry<String, TreeSet<FilterModel>> entry : contextFilters.entrySet()) {
			String contextPath = entry.getKey();
			Set<FilterModel> filters = entry.getValue();

			LOG.info("Changing filter configuration for context {}", contextPath);

			// take existing deployment manager and the deployment info from its deployment
			DeploymentManager manager = servletContainer.getDeploymentByPath(contextPath);
			DeploymentManager.State state = manager.getState();
			DeploymentInfo deploymentInfo = manager.getDeployment().getDeploymentInfo();

			boolean quick = canQuicklyAddFilter(deploymentInfo, filters);

			if (!quick) {
				// let's immediately show that given context is no longer mapped
				pathHandler.removePrefixPath(contextPath);

				try {
					// manager needs to stop the deployment and get rid of it, because we
					// can't replace a deployment info within deployment manager
					LOG.trace("Stopping and undelopying the deployment for {}", contextPath);
					manager.stop();
					manager.undeploy();
				} catch (ServletException e) {
					throw new RuntimeException("Problem stopping the deployment of context " + contextPath
							+ ": " + e.getMessage(), e);
				}

				// remove all existing filters
				deploymentInfo = undertowFactory.clearFilters(deploymentInfo);
			}

			// this time we don't have to care about filters which are not "changed" or which should
			// be destroyed, because unlike in Jetty and Tomcat, in Undertow we simply destroy entire
			// context (redeploy it)

			List<FilterInfo> added = new LinkedList<>();

			int pos = 1;
			for (FilterModel model : filters) {
				// we need highest ranked OsgiContextModel for current context path - chosen not among all
				// associated OsgiContextModels, but among OsgiContextModels of the FilterModel
				OsgiContextModel highestRankedModel = null;
				// remember, this contextModels list is properly sorted
				for (OsgiContextModel ocm : model.getContextModels()) {
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
				OsgiServletContext context = osgiServletContexts.get(highestRankedModel);

				// filter definition
				FilterInfo info = new PaxWebFilterInfo(model, context);

				if (quick) {
					// we can operate on existing ManagedFilters object from current deployment
					// if the deployment is not yet started, it's like normal, full redeployment
					if (state == DeploymentManager.State.STARTED) {
						ManagedFilters currentFilters = manager.getDeployment().getFilters();
						ManagedFilter managedFilter = currentFilters.getManagedFilter(info.getName());
						if (managedFilter == null) {
							// add only if not already there
							currentFilters.addFilter(info);
							added.add(info);
						} else {
							FilterInfo currentFilter = managedFilter.getFilterInfo();
							if (!(currentFilter instanceof PaxWebFilterInfo
									&& ((PaxWebFilterInfo) currentFilter).getFilterModel().equals(model))) {
								// add only if no filter for given FilterModel exists
								currentFilters.addFilter(info);
								added.add(info);
							}
						}
					}
				}
				if (!quick || added.size() > 0 || state != DeploymentManager.State.STARTED) {
					deploymentInfo.addFilter(info);

					String filterName = model.getName();

					// filter mapping
					for (String type : model.getDispatcherTypes()) {
						DispatcherType dt = DispatcherType.valueOf(type);

						if (model.getRegexMapping() != null && model.getRegexMapping().length > 0) {
							// TODO: handle regexp filter mapping
							deploymentInfo.addFilterUrlMapping(filterName, "/*", dt);
						} else if (model.getUrlPatterns() != null) {
							for (String pattern : model.getUrlPatterns()) {
								deploymentInfo.addFilterUrlMapping(filterName, pattern, dt);
							}
						}
						if (model.getServletNames() != null) {
							for (String name : model.getServletNames()) {
								deploymentInfo.addFilterServletNameMapping(filterName, name, dt);
							}
						}
					}
				}
			}

			if (added.size() > 0) {
				// just start newly added filters
				for (ManagedFilter filter : manager.getDeployment().getFilters().getFilters().values()) {
					try {
						new ContextClassLoaderSetupAction(deploymentInfo.getClassLoader()).create((exchange, context) -> {
							filter.createFilter();
							return null;
						}).call(null, null);
					} catch (Exception e) {
						throw new IllegalStateException("Can't start filter " + filter + ": " + e.getMessage(), e);
					}
				}
			} else if (!quick || state != DeploymentManager.State.STARTED) {
				LOG.trace("Redeploying {}", contextPath);
				manager = servletContainer.addDeployment(deploymentInfo);
				manager.deploy();

				// we've changed the deployment for given context path - new ServletContext was created, so
				// we have to propagate the change where needed
				ServletContext newRealServletContext = manager.getDeployment().getServletContext();
				this.osgiContextModels.get(contextPath).forEach(osgiContextModel
						-> this.osgiServletContexts.get(osgiContextModel).setContainerServletContext(newRealServletContext));

				try {
					HttpHandler handler = manager.start();
					pathHandler.addPrefixPath(contextPath, handler);
				} catch (ServletException e) {
					throw new IllegalStateException("Can't redeploy Undertow context "
							+ contextPath + ": " + e.getMessage(), e);
				}
			}
		}
	}

	@Override
	public void visit(EventListenerModelChange change) {
		EventListenerModel eventListenerModel = change.getEventListenerModel();
		List<OsgiContextModel> contextModels = eventListenerModel.getContextModels();

		if (change.getKind() == OpCode.ADD) {
			contextModels.forEach((context) -> {
				DeploymentManager manager = servletContainer.getDeploymentByPath(context.getContextPath());
				DeploymentInfo deploymentInfo = manager.getDeployment().getDeploymentInfo();

				EventListener eventListener = eventListenerModel.getEventListener();
				ListenerInfo info = new ListenerInfo(eventListener.getClass(), new ImmediateInstanceFactory<>(eventListener));

				// TOCHECK: workaround Undertow inflexibility related to removal of such elements
				deploymentInfo.getListeners().add(info);
				manager.getDeployment().getApplicationListeners().addListener(new ManagedListener(info, true));
			});
		}
	}

	@Override
	public void visit(WelcomeFileModelChange change) {
		WelcomeFileModel model = change.getWelcomeFileModel();
		List<OsgiContextModel> contextModels = model.getContextModels();

		OpCode op = change.getKind();
		if (op == OpCode.ADD || op == OpCode.DELETE) {
			contextModels.forEach((context) -> {
				OsgiServletContext osgiServletContext = osgiServletContexts.get(context);

				DeploymentManager manager = servletContainer.getDeploymentByPath(osgiServletContext.getContextPath());
				Deployment deployment = manager.getDeployment();
				DeploymentInfo deploymentInfo = deployment.getDeploymentInfo();

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
						currentWelcomeFiles.removeAll(Arrays.asList(model.getWelcomeFiles()));
					}
				}

				// set welcome files at OsgiServletContext level. NOT at ServletContextHandler level
				String[] newWelcomeFiles = currentWelcomeFiles.toArray(new String[0]);
				osgiServletContext.setWelcomeFiles(newWelcomeFiles);

				LOG.info("Reconfiguration of welcome files for all resource servlet in context \"{}\"", context);

				// reconfigure welcome files in resource servlets
				for (ServletHandler sh : deployment.getServlets().getServletHandlers().values()) {
					ManagedServlet ms = sh.getManagedServlet();
					if (ms != null && ms.getServletInfo() != null &&
							ms.getServletInfo() instanceof PaxWebServletInfo) {
						PaxWebServletInfo info = (PaxWebServletInfo) ms.getServletInfo();
						if (info.getServletModel() != null && info.getServletModel().isResourceServlet()
								&& context == info.getOsgiContextModel()) {
							if (ms.isStarted()) {
								try {
									Servlet servlet = ms.getServlet().getInstance();
									if (servlet instanceof UndertowResourceServlet) {
										((UndertowResourceServlet) servlet).setWelcomeFiles(newWelcomeFiles);
									} else if (servlet instanceof OsgiInitializedServlet) {
										((UndertowResourceServlet) ((OsgiInitializedServlet) servlet).getDelegate()).setWelcomeFiles(newWelcomeFiles);
									}
								} catch (ServletException e) {
									LOG.warn("Problem reconfiguring welcome files in servlet {}", ms, e);
								}
							} else {
								// no need to set it, because servlet is not yet initialized
								LOG.debug("Welcome files will be set in {} when init() is called", ms);
							}
						}
					}
				}
			});
		}
	}

	/**
	 * Check if new set of filters contains only existing filters and possibly some new. When new filters come
	 * in different order or there are removed filters, we'll have to recreate entire context...
	 *
	 * @param deploymentInfo
	 * @param filters
	 * @return
	 */
	private boolean canQuicklyAddFilter(DeploymentInfo deploymentInfo, Set<FilterModel> filters) {
		FilterInfo[] existingFilters = deploymentInfo.getFilters().values().toArray(new FilterInfo[0]);
		FilterModel[] newFilters = filters.toArray(new FilterModel[0]);
		int pos = 0;
		boolean quick = existingFilters.length <= newFilters.length;
		while (quick) {
			if (pos >= existingFilters.length) {
				break;
			}
			if (!(existingFilters[pos] instanceof PaxWebFilterInfo
					&& ((PaxWebFilterInfo) existingFilters[pos]).getFilterModel().equals(newFilters[pos]))) {
				quick = false;
				break;
			}
			pos++;
		}

		return quick;
	}

//	/**
//	 * Loads additional properties and configure {@link UndertowServerController#identityManager}
//	 * @param undertowResource
//	 */
//	private void configureIdentityManager(URL undertowResource) {
//		try {
//			Properties props = new Properties();
//			try (InputStream is = undertowResource.openStream()) {
//				props.load(is);
//			}
//			Map<String, String> config = new LinkedHashMap<>();
//			for (Map.Entry<Object, Object> entry : props.entrySet()) {
//				config.put(entry.getKey().toString(), entry.getValue().toString());
//			}
//			identityManager = (IdentityManager)createConfigurationObject(config, "identityManager");
//
////			String listeners = config.get("listeners");
////			if (listeners != null) {
////				String[] names = listeners.split("(, )+");
////				for (String name : names) {
////					String type = config.get("listeners." + name + ".type");
////					String address = config.get("listeners." + name + ".address");
////					String port = config.get("listeners." + name + ".port");
////					if ("http".equals(type)) {
////						builder.addHttpListener(Integer.parseInt(port), address);
////					}
////				}
////			}
//		} catch (Exception e) {
//			LOG.error("Exception while starting Undertow", e);
//			throw new RuntimeException("Exception while starting Undertow", e);
//		}
//	}

//	@Override
//	public synchronized LifeCycle getContext(OsgiContextModel model) {
//		return findOrCreateContext(model);
//	}
//
//	@Override
//	public synchronized void removeContext(HttpContext httpContext) {
//		final Context context = contextMap.remove(httpContext);
//		if (context == null) {
//			throw new IllegalStateException("Cannot remove the context because it does not exist: "
//											+ httpContext);
//		}
//		context.destroy();
//	}

//	private Context findContext(final OsgiContextModel contextModel) {
//		NullArgumentException.validateNotNull(contextModel, "contextModel");
//		HttpContext httpContext = contextModel.getHttpContext();
//		return contextMap.get(httpContext);
//	}
//
//	private Context findOrCreateContext(final OsgiContextModel contextModel) {
//		NullArgumentException.validateNotNull(contextModel, "contextModel");
//		synchronized (contextMap) {
//			if (contextMap.containsKey(contextModel.getHttpContext())) {
//				return contextMap.get(contextModel.getHttpContext());
//			}
//			Context newCtx = new Context(this, path, contextModel);
//			newCtx.setConfiguration(configuration);
//			newCtx.setDefaultSessionTimeoutInMinutes(defaultSessionTimeoutInMinutes);
//			newCtx.setSessionPersistenceManager(sessionPersistenceManager);
//			contextMap.put(contextModel.getHttpContext(), newCtx);
//			final Servlet servlet = createResourceServlet(contextModel, "/", "default");
//			final ResourceModel model = new ResourceModel(contextModel, servlet, "/", "default");
//			try {
//				newCtx.addServlet(model);
//			} catch (ServletException e) {
//				LOG.warn(e.getMessage(), e);
//			}
//			return newCtx;
//		}
//	}

//	@Override
//	public void addEventListener(EventListenerModel model) {
//		assertNotState(State.Unconfigured);
//		try {
//			final Context context = findOrCreateContext(model.getContextModel());
//			context.addEventListener(model);
//		} catch (ServletException e) {
//			throw new RuntimeException("Unable to add event listener", e);
//		}
//	}
//
//	@Override
//	public void removeEventListener(EventListenerModel model) {
//		assertNotState(State.Unconfigured);
//		try {
//			final Context context = findOrCreateContext(model.getContextModel());
//			context.removeEventListener(model);
//		} catch (ServletException e) {
//			throw new RuntimeException("Unable to add event listener", e);
//		}
//	}
//
//	@Override
//	public void addErrorPage(ErrorPageModel model) {
//		assertNotState(State.Unconfigured);
//		try {
//			final Context context = findOrCreateContext(model.getContextModel());
//			context.addErrorPage(model);
//		} catch (ServletException e) {
//			throw new RuntimeException("Unable to add error page", e);
//		}
//	}
//
//	@Override
//	public void removeErrorPage(ErrorPageModel model) {
//		assertNotState(State.Unconfigured);
//		try {
//			final Context context = findOrCreateContext(model.getContextModel());
//			context.removeErrorPage(model);
//		} catch (ServletException e) {
//			throw new RuntimeException("Unable to remove error page", e);
//		}
//	}
//
//	@Override
//	public void addWelcomFiles(WelcomeFileModel model) {
//		assertNotState(State.Unconfigured);
//		try {
//			final Context context = findOrCreateContext(model.getContextModel());
//			context.addWelcomeFile(model);
//		} catch (ServletException e) {
//			throw new RuntimeException("Unable to add welcome files", e);
//		}
//	}
//
//	@Override
//	public void removeWelcomeFiles(WelcomeFileModel model) {
//		assertNotState(State.Unconfigured);
//		try {
//			final Context context = findOrCreateContext(model.getContextModel());
//			context.removeWelcomeFile(model);
//		} catch (ServletException e) {
//			throw new RuntimeException("Unable to add welcome files", e);
//		}
//	}
//
//	@Override
//	public Servlet createResourceServlet(OsgiContextModel contextModel, String alias, String name) {
//		final Context context = findOrCreateContext(contextModel);
//		return new ResourceServlet(context, alias, name);
//	}
//
//	@Override
//	public void addSecurityConstraintMapping(SecurityConstraintMappingModel model) {
//		assertNotState(State.Unconfigured);
//		try {
//			final Context context = findOrCreateContext(model.getContextModel());
//			context.addSecurityConstraintMapping(model);
//		} catch (ServletException e) {
//			throw new RuntimeException("Unable to add welcome files", e);
//		}
//	}
//
//	@Override
//	public void removeSecurityConstraintMapping(SecurityConstraintMappingModel model) {
//		assertNotState(State.Unconfigured);
//		try {
//			final Context context = findOrCreateContext(model.getContextModel());
//			context.removeSecurityConstraintMapping(model);
//		} catch (ServletException e) {
//			throw new RuntimeException("Unable to add welcome files", e);
//		}
//	}
//
//	@Override
//	public void addContainerInitializerModel(ContainerInitializerModel model) {
//		assertNotState(State.Unconfigured);
//		try {
//			final Context context = findOrCreateContext(model.getContextModel());
//			context.addContainerInitializerModel(model);
//		} catch (ServletException e) {
//			throw new RuntimeException("Unable to add welcome files", e);
//		}
//	}
//
//	@Override
//	public Account verify(Account account) {
//		if (identityManager != null) {
//			return identityManager.verify(account);
//		}
//		throw new IllegalStateException("No identity manager configured");
//	}
//
//	@Override
//	public Account verify(String id, Credential credential) {
//		if (identityManager != null) {
//			return identityManager.verify(id, credential);
//		}
//		throw new IllegalStateException("No identity manager configured");
//	}
//
//	@Override
//	public Account verify(Credential credential) {
//		if (identityManager != null) {
//			return identityManager.verify(credential);
//		}
//		throw new IllegalStateException("No identity manager configured");
//	}

	// see org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService#getConfidentialPortManager
	private class SimpleConfidentialPortManager implements ConfidentialPortManager {

		@Override
		public int getConfidentialPort(HttpServerExchange exchange) {
			int port = exchange.getConnection().getLocalAddress(InetSocketAddress.class).getPort();
			if (port < 0) {
				LOG.debug("Confidential port not defined for port {}", port);
			}
			return UndertowServerWrapper.this.securePortMapping.get(port);
		}
	}

}
