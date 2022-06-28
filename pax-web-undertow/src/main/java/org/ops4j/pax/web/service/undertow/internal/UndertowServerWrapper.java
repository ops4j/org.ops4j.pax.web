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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.connector.ByteBufferPool;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.OpenListener;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.session.SessionConfig;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.ConfidentialPortManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SecurityInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ServletSecurityInfo;
import io.undertow.servlet.api.ServletSessionConfig;
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.api.SessionConfigWrapper;
import io.undertow.servlet.api.SessionPersistenceManager;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.servlet.core.ContextClassLoaderSetupAction;
import io.undertow.servlet.core.DeploymentImpl;
import io.undertow.servlet.core.InMemorySessionManagerFactory;
import io.undertow.servlet.core.ManagedFilter;
import io.undertow.servlet.core.ManagedFilters;
import io.undertow.servlet.core.ManagedListener;
import io.undertow.servlet.core.ManagedServlet;
import io.undertow.servlet.handlers.ServletHandler;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.servlet.util.InMemorySessionPersistence;
import org.ops4j.pax.web.service.AuthenticatorService;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.LogConfiguration;
import org.ops4j.pax.web.service.spi.config.SessionConfiguration;
import org.ops4j.pax.web.service.spi.model.ContextMetadataModel;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerKey;
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
import org.ops4j.pax.web.service.spi.servlet.DefaultSessionCookieConfig;
import org.ops4j.pax.web.service.spi.servlet.DynamicRegistrations;
import org.ops4j.pax.web.service.spi.servlet.OsgiDynamicServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedServlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContextClassLoader;
import org.ops4j.pax.web.service.spi.servlet.OsgiSessionAttributeListener;
import org.ops4j.pax.web.service.spi.servlet.PreprocessorFilterConfig;
import org.ops4j.pax.web.service.spi.servlet.RegisteringContainerInitializer;
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
import org.ops4j.pax.web.service.undertow.PaxWebUndertowExtension;
import org.ops4j.pax.web.service.undertow.UndertowSupport;
import org.ops4j.pax.web.service.undertow.configuration.model.Interface;
import org.ops4j.pax.web.service.undertow.configuration.model.IoSubsystem;
import org.ops4j.pax.web.service.undertow.configuration.model.SecurityRealm;
import org.ops4j.pax.web.service.undertow.configuration.model.Server;
import org.ops4j.pax.web.service.undertow.configuration.model.SocketBinding;
import org.ops4j.pax.web.service.undertow.configuration.model.UndertowConfiguration;
import org.ops4j.pax.web.service.undertow.configuration.model.UndertowSubsystem;
import org.ops4j.pax.web.service.undertow.internal.configuration.ResolvingContentHandler;
import org.ops4j.pax.web.service.undertow.internal.configuration.UnmarshallingContentHandler;
import org.ops4j.pax.web.service.undertow.internal.security.JaasIdentityManager;
import org.ops4j.pax.web.service.undertow.internal.security.PropertiesIdentityManager;
import org.ops4j.pax.web.service.undertow.internal.web.FlexibleErrorPages;
import org.ops4j.pax.web.service.undertow.internal.web.OsgiResourceManager;
import org.ops4j.pax.web.service.undertow.internal.web.OsgiServletContainerInitializerInfo;
import org.ops4j.pax.web.service.undertow.internal.web.UndertowResourceServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.whiteboard.Preprocessor;
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
class UndertowServerWrapper implements BatchVisitor, UndertowSupport {

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

	private IdentityManager identityManager;

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
	// Now the problem is that while "deployment" (io.undertow.servlet.api.Deployment) can be used to call
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
	 * <em>Outer handlers</em> for all the contexts - these are used to call {@link Preprocessor preprocessors}.
	 */
	private final Map<String, PaxWebPreprocessorsHandler> preprocessorsHandlers = new HashMap<>();

	/**
	 * Handlers that call {@link org.osgi.service.http.HttpContext#handleSecurity} and/or
	 * {@link org.osgi.service.http.context.ServletContextHelper#handleSecurity}.
	 */
	private final Map<String, PaxWebSecurityHandler> securityHandlers = new HashMap<>();

	/**
	 * When constructing <em>deployment infos</em> we have to remember them separately before calling
	 * {@link DeploymentManager#deploy()}, as it'll clone the {@link DeploymentInfo} (twice) and hide
	 * the {@link Deployment} implementation in the {@link DeploymentManager} (...). This is important
	 * to implement the "start the context only after first servlet/filter/resource has been registered".
	 */
	private final Map<String, DeploymentInfo> deploymentInfos = new HashMap<>();

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
	 * As with Tomcat and Jetty, we'll manually handle SCIs to be able to add/remove them and call them with
	 * proper {@link ServletContext} implementation.
	 */
	private final Map<String, TreeSet<OsgiServletContainerInitializerInfo>> initializers = new HashMap<>();

	/**
	 * Keep dynamic configuration and use it during startup only.
	 */
	private final Map<String, DynamicRegistrations> dynamicRegistrations = new HashMap<>();

	// Jetty and Tomcat keeps the ranked/ordered listeners inside an object of class derived from Jetty/Tomcat
	// "context" class (PaxWebServletContextHandler and PaxWebStandardContext respectively). In Undertow we
	// have only DeploymentInfo and I decided not to specialize this class. So we have to keep the listeners here

	/**
	 * This maps keeps all the listeners in order, as expected by OSGi CMPN R7 Whiteboard specification.
	 */
	private final Map<String, TreeMap<EventListenerKey, PaxWebListenerInfo>> rankedListeners = new HashMap<>();

	/**
	 * Here we'll keep the listeners without associated {@link EventListenerModel}
	 */
	private final Map<String, List<PaxWebListenerInfo>> orderedListeners = new HashMap<>();

	/**
	 * Global {@link Configuration} passed from pax-web-runtime through
	 * {@link org.ops4j.pax.web.service.spi.ServerController}
	 */
	private final Configuration configuration;

	/** Servlet to use when no servlet is mapped - to ensure that preprocessors and filters are run correctly. */
	private final Default404Servlet default404Servlet = new Default404Servlet(true);

	private SessionCookieConfig defaultSessionCookieConfig = null;
	private Integer defaultSessionTimeout = null;
	private SessionPersistenceManager globalSessionPersistenceManager;

	// configuration read from undertow.xml
	private UndertowConfiguration undertowConfiguration;

	/**
	 * Map that can be used to recal what error pages we had configured at the time when there's a need
	 * to remove some of them.
	 */
	private final Map<String, FlexibleErrorPages> errorPages = new HashMap<>();

	/**
	 * All {@link EventListenerModel} instances for {@link HttpSessionAttributeListener} listeners. They'll be
	 * reviewed in order to propagate session attribute events per {@link OsgiContextModel}.
	 */
	private final List<EventListenerModel> sessionListenerModels = new ArrayList<>();

	private final Map<String, TreeMap<OsgiContextModel, SecurityConfigurationModel>> contextSecurityConstraints = new HashMap<>();

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
		// before Pax Web 8 there was also etc/undertow.properties with identity manager properties,
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

		// default session configuration is prepared, but not set in the server instance. It can be set
		// only after first context is created. Configuration from undertow.xml has higher priority than PID.
		if (defaultSessionCookieConfig == null) {
			this.defaultSessionCookieConfig = configuration.session().getDefaultSessionCookieConfig();
		}
		if (defaultSessionTimeout == null) {
			defaultSessionTimeout = configuration.session().getSessionTimeout();
		}

		if (globalSessionPersistenceManager == null) {
			File dir = configuration.session().getSessionStoreDirectory();
			if (dir != null) {
				LOG.info("Using file session persistence. Location: " + dir.getCanonicalPath());
				globalSessionPersistenceManager = new FileSessionPersistence(dir);
			} else {
				LOG.info("Using in-memory session persistence");
				globalSessionPersistenceManager = new InMemorySessionPersistence();
			}
		}
	}

	/**
	 * <p>This method parses existing {@code undertow.xml} file, which is the preferred method of Undertow
	 * configuration</p>
	 */
	private void applyUndertowConfiguration() throws Exception {
		File[] locations = configuration.server().getConfigurationFiles();
		URL undertowResource = getClass().getResource("/undertow.xml");
		if (locations.length == 0) {
			if (undertowResource == null) {
				LOG.info("No external Undertow configuration files specified. Default/PID configuration will be used.");
			} else {
				LOG.info("Found \"undertow.xml\" resource on the classpath: {}.", undertowResource);
			}
		} else if (locations.length > 1) {
			LOG.warn("Can't specify Undertow configuration using more than one XML file. Only {} will be used.", locations[0]);
		} else {
			if (undertowResource != null) {
				LOG.info("Found additional \"undertow.xml\" resource on the classpath: {}, but {} will be used instead.",
						undertowResource, locations[0]);
			}
			LOG.info("Processing Undertow configuration from file: {}", locations[0]);
		}

		File xmlConfig = locations.length > 0 ? locations[0] : null;

		if (xmlConfig != null || undertowResource != null) {
			UnmarshallingContentHandler unmarshallerHandler = new UnmarshallingContentHandler();

			// indirect unmarshalling with property resolution *inside XML attribute values*
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			spf.setValidating(false);
			spf.setXIncludeAware(false);
			spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			XMLReader xmlReader = spf.newSAXParser().getXMLReader();

			xmlReader.setContentHandler(new ResolvingContentHandler(configuration.all(), unmarshallerHandler));
			if (xmlConfig != null) {
				LOG.debug("Parsing {}", xmlConfig);
				try (InputStream stream = new FileInputStream(xmlConfig)) {
					xmlReader.parse(new InputSource(stream));
				}
			} else {
				LOG.debug("Parsing {}", undertowResource);
				try (InputStream stream = undertowResource.openStream()) {
					xmlReader.parse(new InputSource(stream));
				}
			}

			this.undertowConfiguration = unmarshallerHandler.getConfiguration();
		}

		// the external XML could be very simple - I assume that the most trivial XML can specify ... nothing,
		// besides top level {urn:org.ops4j.pax.web:undertow:1.1}undertow element
		if (undertowConfiguration == null) {
			undertowConfiguration = new UndertowConfiguration();
		}
		// add default interfaces - they'll be verified later if they match PID configuration
		if (undertowConfiguration.getInterfaces().size() == 0) {
			Interface iface = new Interface();
			iface.setName("default");
			Interface.InetAddress address = new Interface.InetAddress();
			address.setIp("0.0.0.0");
			iface.getAddresses().add(address);
			undertowConfiguration.getInterfaces().add(iface);
		}
		if (undertowConfiguration.getSocketBindings().size() == 0 && configuration.server().isHttpEnabled()) {
			SocketBinding binding = new SocketBinding();
			binding.setName("http");
			binding.setInterfaceRef("default");
			binding.setPort(configuration.server().getHttpPort());
			undertowConfiguration.getSocketBindings().add(binding);
		}

		if (undertowConfiguration.getSubsystem() == null) {
			undertowConfiguration.setSubsystem(new UndertowSubsystem());
		}
		if (undertowConfiguration.getSubsystem().getServer() == null) {
			undertowConfiguration.getSubsystem().setServer(new Server());
		}

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
			for (IoSubsystem.BufferPool pool : io.getBufferPools()) {
				bufferPools.put(pool.getName(), undertowFactory.createBufferPool(pool));
			}
		}

		// identity manager - looked up in "default" security realm
		SecurityRealm defaultRealm = undertowConfiguration.securityRealm("default");
		if (defaultRealm != null) {
			SecurityRealm.JaasAuth jaasAuth = defaultRealm.getAuthentication().getJaas();
			SecurityRealm.PropertiesAuth propertiesAuth = defaultRealm.getAuthentication().getProperties();
			SecurityRealm.UsersAuth usersAuth = defaultRealm.getAuthentication().getUsers();
			if (jaasAuth != null) {
				LOG.info("Creating JAAS Identity Manager");
				String userPrincipalClassName = defaultRealm.getUserPrincipalClassName();
				if (userPrincipalClassName == null || "".equals(userPrincipalClassName.trim())) {
					userPrincipalClassName = "java.security.Principal";
				}
				Set<String> rolePrincipalClassNames = new LinkedHashSet<>(defaultRealm.getRolePrincipalClassNames());

				identityManager = new JaasIdentityManager(jaasAuth.getName(), userPrincipalClassName, rolePrincipalClassNames);
			} else if (propertiesAuth != null || usersAuth != null) {
				Map<String, String> users = new HashMap<>();

				if (propertiesAuth != null) {
					Properties userProperties = new Properties();
					LOG.info("Creating Properties Identity Manager using {}", propertiesAuth.getPath());
					File userBase = new File(propertiesAuth.getPath());
					if (!userBase.isFile()) {
						throw new IllegalArgumentException(userBase.getCanonicalPath() + " is not accessible. Can't load users/groups information.");
					}
					try (FileInputStream stream = new FileInputStream(userBase)) {
						userProperties.load(stream);
						for (String user : userProperties.stringPropertyNames()) {
							users.put(user, userProperties.getProperty(user));
						}
					}
				} else {
					LOG.info("Creating Properties Identity Manager using XML configuration");
					for (SecurityRealm.User user : usersAuth.getUsers()) {
						users.put(user.getUsername(), user.getPassword());
					}
				}

				identityManager = new PropertiesIdentityManager(users);
			}
		}

		// /undertow/subsystem/server/host/location - file handlers for static context paths.
		if (undertowConfiguration.getSubsystem().getServer().getHost() != null) {
			for (Server.Host.Location location : undertowConfiguration.getSubsystem().getServer().getHost().getLocations()) {
				String context = location.getName();
				String handlerRef = location.getHandler();
				UndertowSubsystem.FileHandler fileHandler = undertowConfiguration.handler(handlerRef);
				if (fileHandler == null) {
					throw new IllegalArgumentException("No handler with name \"" + location.getHandler() + "\" available for " + location.getName() + " location.");
				}
				File base = new File(fileHandler.getPath());
				if (!base.isDirectory()) {
					throw new IllegalArgumentException(base.getCanonicalPath() + " is not accessible. Can't configure handler for " + location.getName() + " location.");
				}
				// fileHandler.path is simply filesystem directory
				PathResourceManager resourceManager = new PathResourceManager(base.toPath(), (long) fileHandler.getCacheBufferSize() * fileHandler.getCacheBuffers(),
						fileHandler.getCaseSensitive(), fileHandler.getFollowSymlink(), fileHandler.getSafeSymlinkPaths().toArray(new String[0]));
				HttpHandler rh = new ResourceHandler(resourceManager);
				if (undertowConfiguration.getSubsystem().getServletContainer() != null) {
					// clear welcome files first
					((ResourceHandler) rh).setWelcomeFiles();
					for (org.ops4j.pax.web.service.undertow.configuration.model.ServletContainer.WelcomeFile wf : undertowConfiguration.getSubsystem().getServletContainer().getWelcomeFiles()) {
						((ResourceHandler) rh).addWelcomeFiles(wf.getName());
					}
				}
				// filter-refs - in reverse order
				ListIterator<Server.Host.FilterRef> it = location.getFilterRefs().listIterator(location.getFilterRefs().size());
				while (it.hasPrevious()) {
					Server.Host.FilterRef fr = it.previous();
					UndertowSubsystem.AbstractFilter filter = undertowConfiguration.filter(fr.getName());
					if (filter == null) {
						throw new IllegalArgumentException("No filter with name \"" + fr.getName() + "\" available.");
					}
					// predicate means "apply SetHeaderHandler if predicate matches, otherwise forward to passed handler"

					Predicate p = fr.getPredicate() == null ? null : Predicates.parse(fr.getPredicate(), HttpHandler.class.getClassLoader());
					rh = filter.configure(rh, p);
				}

				if (rootHandler instanceof PathHandler) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Adding resource handler for location \"" + context + "\" and base path \"" + base.getCanonicalPath() + "\".");
					}
					((PathHandler) rootHandler).addPrefixPath(context, rh);
				}
			}
		}

		// global filters (subsystem/filters/response-header and subsystem/filters/filter)
		if (undertowConfiguration.getSubsystem().getServer().getHost() != null) {
			List<Server.Host.FilterRef> filterRefs = undertowConfiguration.getSubsystem().getServer().getHost().getFilterRefs();
			ListIterator<Server.Host.FilterRef> iterator = filterRefs.listIterator(filterRefs.size());
			while (iterator.hasPrevious()) {
				Server.Host.FilterRef fr = iterator.previous();
				UndertowSubsystem.AbstractFilter filter = undertowConfiguration.filter(fr.getName());
				if (filter == null) {
					throw new IllegalArgumentException("No filter with name \"" + fr.getName() + "\" available.");
				}
				// predicate means "apply SetHeaderHandler if predicate matches, otherwise forward to passed handler"

				Predicate p = fr.getPredicate() == null ? null : Predicates.parse(fr.getPredicate(), HttpHandler.class.getClassLoader());
				rootHandler = filter.configure(rootHandler, p);
			}
		}

		// only now create listeners, because rootHandler may have been wrapped

		// listeners will be checked by verifyConnectorConfiguration() later
		List<Server.HttpListener> httpListeners = undertowConfiguration.getSubsystem().getServer().getHttpListeners();
		List<Server.HttpsListener> httpsListeners = undertowConfiguration.getSubsystem().getServer().getHttpsListeners();

		// http listener(s)
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

		// https listener(s)
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

		// session configuration
		// <session-cookie name="JSESSIONID" domain="domain" http-only="true" max-age="130" secure="true" comment="Session Cookie" />
		if (undertowConfiguration.getSubsystem().getServletContainer() != null) {
			String dst = undertowConfiguration.getSubsystem().getServletContainer().getDefaultSessionTimeout();
			if (dst != null && !"".equals(dst)) {
				try {
					this.defaultSessionTimeout = Integer.parseInt(dst);
				} catch (NumberFormatException e) {
					LOG.warn("Invalid default session timeout \"" + dst + "\". Using 30 (minutes).");
				}
			}
			org.ops4j.pax.web.service.undertow.configuration.model.ServletContainer.SessionCookie cookieConfig
					= undertowConfiguration.getSubsystem().getServletContainer().getSessionCookie();
			if (cookieConfig != null) {
				defaultSessionCookieConfig = new DefaultSessionCookieConfig();
				defaultSessionCookieConfig.setName(cookieConfig.getName());
				defaultSessionCookieConfig.setComment(cookieConfig.getComment());
				defaultSessionCookieConfig.setDomain(cookieConfig.getDomain());
//				defaultSessionCookieConfig.setPath(?);
				defaultSessionCookieConfig.setSecure(cookieConfig.isSecure());
				defaultSessionCookieConfig.setHttpOnly(cookieConfig.isHttpOnly());
				defaultSessionCookieConfig.setMaxAge(cookieConfig.getMaxAge());
			}
		}

		// session persistence
		// <persistent-sessions path="${karaf.data}/web-sessions" />
		org.ops4j.pax.web.service.undertow.configuration.model.ServletContainer.PersistentSessionsConfig persistentSessions
				= undertowConfiguration.getSubsystem().getServletContainer() == null ? null : undertowConfiguration.getSubsystem().getServletContainer().getPersistentSessions();
		if (persistentSessions != null) {
			// otherwise, PID configuration will be used
			if (persistentSessions.getPath() != null && !"".equals(persistentSessions.getPath().trim())) {
				// file persistence manager
				File sessionsDir = new File(persistentSessions.getPath());
				if (sessionsDir.isDirectory() || sessionsDir.mkdirs()) {
					LOG.info("Using file session persistence. Location: " + sessionsDir.getCanonicalPath());
					globalSessionPersistenceManager = new FileSessionPersistence(sessionsDir);
				} else {
					LOG.warn("Can't access or create {} for file session persistence.", sessionsDir);
				}
			} else {
				// in memory persistence manager
				LOG.info("No path configured for persistent-sessions. Using in-memory session persistence.");
				globalSessionPersistenceManager = new InMemorySessionPersistence();
			}
		}
	}

	@Override
	public XnioWorker getWorker(String workerName) {
		if (workerName != null) {
			if (!workers.containsKey(workerName)) {
				if ("default".equals(workerName)) {
					synchronized (workers) {
						if (!workers.containsKey(workerName)) {
							workers.put("default", undertowFactory.getDefaultWorker(configuration));
						}
					}
					return workers.get(workerName);
				}
				throw new IllegalArgumentException("No worker named \"" + workerName + "\" is configured");
			}
			return workers.get(workerName);
		} else {
			return undertowFactory.getDefaultWorker(configuration);
		}
	}

	@Override
	public ByteBufferPool getBufferPool(String bufferPoolName) {
		if (bufferPoolName != null) {
			if (!bufferPools.containsKey(bufferPoolName)) {
				if ("default".equals(bufferPoolName)) {
					synchronized (bufferPools) {
						if (!bufferPools.containsKey(bufferPoolName)) {
							bufferPools.put("default", undertowFactory.getDefaultBufferPool());
						}
					}
					return bufferPools.get(bufferPoolName);
				}
				throw new IllegalArgumentException("No buffer pool named \"" + bufferPoolName + "\" is configured");
			}
			return bufferPools.get(bufferPoolName);
		} else {
			return undertowFactory.getDefaultBufferPool();
		}
	}

	@Override
	public EventListener proxiedServletContextListener(EventListener eventListener, OsgiContextModel context) {
		ContextLinkingInvocationHandler handler = new ContextLinkingInvocationHandler(eventListener);
		ClassLoader cl = context.getClassLoader();
		if (cl == null) {
			// for test scenario
			cl = eventListener.getClass().getClassLoader();
		}
		Set<Class<?>> interfaces = new LinkedHashSet<>();
		Class<?> c = eventListener.getClass();
		while (c != Object.class) {
			interfaces.addAll(Arrays.asList(c.getInterfaces()));
			c = c.getSuperclass();
		}
		EventListener wrapper = (EventListener) Proxy.newProxyInstance(cl, interfaces.toArray(new Class[0]), handler);
		// it may be a case that this listener is dynamic
		OsgiContextModel highestRanked = securityHandlers.get(context.getContextPath()).getDefaultOsgiContextModel();
		if (highestRanked != null) {
			OsgiServletContext highestRankedContext = osgiServletContexts.get(highestRanked);
			handler.setOsgiContext(highestRankedContext);
		}

		return wrapper;
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

		// review connectors possibly configured from undertow.xml and check if they match configadmin configuration
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
			boolean nonSecureMatch = !secure && className.equals("org.xnio.nio.QueuedNioTcpServer2");
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
			if (deployment.getState() != DeploymentManager.State.UNDEPLOYED) {
				String contextPath = deployment.getDeployment().getServletContext().getContextPath();
				try {
					if (preprocessorsHandlers.containsKey(contextPath)) {
						for (PreprocessorFilterConfig fc : preprocessorsHandlers.get(contextPath).getPreprocessors()) {
							if (fc.isInitCalled()) {
								fc.destroy();
								fc.setInitCalled(false);
							}
						}
					}

					deployment.stop();
					deployment.undeploy();
				} catch (ServletException e) {
					LOG.warn("Problem stopping deployment for context " + contextPath + ": " + e.getMessage(), e);
				}
			}
		});
		deploymentInfos.clear();
		// do not clear osgiContextModels and osgiServletContexts
		// - they'll be cleared individually through HttpServiceEnabled
//		osgiServletContexts.clear();
//		osgiContextModels.clear();
		preprocessorsHandlers.clear();
		securityHandlers.clear();
		wrappingHandlers.clear();
		errorPages.clear();

		this.workers.values().forEach(XnioWorker::shutdown);
		this.workers.clear();
		this.bufferPools.values().forEach(ByteBufferPool::close);
		this.bufferPools.clear();
		undertowFactory.closeDefaultPoolAndBuffer();

		// I found this necessary, when pax-web-undertow is restarted/refreshed without affecting
		// pax-web-extender-whiteboard
		osgiServletContexts.values().forEach(OsgiServletContext::unregister);
	}

	/**
	 * If state allows, this methods returns currently configured/started addresses of the listeners.
	 * @param useLocalPort
	 * @return
	 */
	public ServerEvent.Address[] getAddresses(boolean useLocalPort) {
		if (listeners.size() == 0) {
			return new ServerEvent.Address[0];
		}
		final List<ServerEvent.Address> result = new ArrayList<>(listeners.size());
		listeners.values().forEach(ac -> result.add(new ServerEvent.Address(ac.getAddress(), ac.isSecure())));
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
				DeploymentManager manager = getDeploymentManager(contextPath);
				if (toRemove != null) {
					for (Iterator<ElementModel<?, ?>> iterator = toRemove.iterator(); iterator.hasNext(); ) {
						ElementModel<?, ?> model = iterator.next();
						if (model instanceof ServletModel) {
							removeServletModel(contextPath, ((ServletModel) model), null);
						} else if (model instanceof EventListenerModel) {
							if (manager != null) {
								DeploymentInfo deploymentInfo = manager.getDeployment().getDeploymentInfo();
								Deployment deployment = manager.getDeployment();
								stopUndertowContext(contextPath, manager, deploymentInfo, false);
								removeEventListenerModel(deploymentInfo, (EventListenerModel) model,
										((EventListenerModel) model).getResolvedListener());
							} else if (deploymentInfos.containsKey(contextPath)) {
								removeEventListenerModel(deploymentInfos.get(contextPath), (EventListenerModel) model,
										((EventListenerModel) model).getResolvedListener());
							}
						}
						iterator.remove();
					}
				}
				delayedRemovals.remove(contextPath);
				if (deploymentInfos.containsKey(contextPath)) {
					// end of transaction - start the context
					ensureServletContextStarted(contextPath);
				}
			}
		}
	}

	@Override
	public void visitServletContextModelChange(ServletContextModelChange change) {
		ServletContextModel model = change.getServletContextModel();
		String contextPath = model.getContextPath();

		if (change.getKind() == OpCode.ADD) {
			if (deploymentInfos.containsKey(contextPath)) {
				// quiet return, because of how ServletContextModels for WABs are created.
				return;
			}

			LOG.info("Creating new Undertow context for {}", model);

			// meta info about "servlet context deployment"
			DeploymentInfo deploymentInfo = Servlets.deployment();

			String deploymentName = "/".equals(model.getContextPath())
					? "ROOT" : model.getContextPath().substring(1).replaceAll("/", "_");
			deploymentInfo.setDeploymentName(deploymentName);
			deploymentInfo.setDisplayName(model.getId());
			deploymentInfo.setContextPath(contextPath);
			deploymentInfo.setUrlEncoding(StandardCharsets.UTF_8.name());
			deploymentInfo.setEagerFilterInit(true);
			if (configuration.server().isShowStacks()) {
				deploymentInfo.setServletStackTraces(ServletStackTraces.ALL);
			} else {
				deploymentInfo.setServletStackTraces(ServletStackTraces.NONE);
			}

			deploymentInfo.addServlet(new PaxWebServletInfo("default", default404Servlet, true).addMapping("/"));

			deploymentInfo.setConfidentialPortManager(new SimpleConfidentialPortManager());

			// one IDM for all the contexts - it's only an authentication repository, login configs and authorization
			// checks will be handled depending on OsgiContextModel
			deploymentInfo.setIdentityManager(identityManager);

			// In Jetty and Tomcat we can operate on FilterChains, here we have to split the OsgiFilterChain's
			// functionality into different HandlerWrappers:
			//  - to wrap request, so proper ServletContext is returned
			//  - to call preprocessors
			//  - to call handleSecurity()/finishSecurity()
			//
			// io.undertow.servlet.core.DeploymentManagerImpl.wrapHandlers() turns the last wrapper
			// into the "outermost" one

			PaxWebPreprocessorsHandler preprocessorWrapper = new PaxWebPreprocessorsHandler();
			this.preprocessorsHandlers.put(contextPath, preprocessorWrapper);
			deploymentInfo.addOuterHandlerChainWrapper(preprocessorWrapper);

			PaxWebOuterHandlerWrapper outerWrapper = new PaxWebOuterHandlerWrapper(new OsgiSessionAttributeListener(sessionListenerModels));
			this.wrappingHandlers.put(contextPath, outerWrapper);
			deploymentInfo.addOuterHandlerChainWrapper(outerWrapper);

			PaxWebSecurityHandler securityWrapper = new PaxWebSecurityHandler();
			this.securityHandlers.put(contextPath, securityWrapper);
			deploymentInfo.addSecurityWrapper(securityWrapper);

			// session configuration - based on defaultSessionConfiguration, but may be later overriden in OsgiContext
			SessionConfiguration sc = configuration.session();
			deploymentInfo.setDefaultSessionTimeout(sc.getSessionTimeout() * 60);
			ServletSessionConfig ssc = new ServletSessionConfig();
			ssc.setName(defaultSessionCookieConfig.getName());
			ssc.setDomain(defaultSessionCookieConfig.getDomain());
			// will default to context path if null
			ssc.setPath(defaultSessionCookieConfig.getPath());
			ssc.setMaxAge(defaultSessionCookieConfig.getMaxAge());
			ssc.setHttpOnly(defaultSessionCookieConfig.isHttpOnly());
			ssc.setSecure(defaultSessionCookieConfig.isSecure());
			ssc.setComment(defaultSessionCookieConfig.getComment());
			deploymentInfo.setServletSessionConfig(ssc);

			deploymentInfo.setSessionPersistenceManager(globalSessionPersistenceManager);

			// do NOT add&deploy&start the context here - only after registering first "active" web element
			// only prepare the original (cloned later) DeploymentInfo
			deploymentInfos.put(contextPath, deploymentInfo);

			// prepare mapping of sorted OsgiContextModel collections per context path
			osgiContextModels.put(contextPath, new TreeSet<>());

			// configure ordered map of initializers
			initializers.put(contextPath, new TreeSet<>());
			dynamicRegistrations.put(contextPath, new DynamicRegistrations());

			rankedListeners.put(contextPath, new TreeMap<>());
			orderedListeners.put(contextPath, new ArrayList<>());
		} else if (change.getKind() == OpCode.DELETE) {
			OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
			if (highestRankedModel != null) {
				// there are still OsgiContextModel(s) for given ServletContextModel, so maybe after uninstallation
				// of a WAB, HttpService-based web apps have to stay running?
				return;
			}

			orderedListeners.remove(contextPath);
			rankedListeners.remove(contextPath);

			dynamicRegistrations.remove(contextPath);
			initializers.remove(contextPath);
			osgiContextModels.remove(contextPath);
			deploymentInfos.remove(contextPath);
			securityHandlers.remove(contextPath);
			wrappingHandlers.remove(contextPath);

			// Note: for WAB deployments, this is the last operation of the undeployment batch and all web element
			// removals are delayed until this step.
			// This is important to ensure proper order of destruction ended with contextDestroyed() calls

			DeploymentManager manager = getDeploymentManager(contextPath);
			if (manager != null) {
				DeploymentInfo deploymentInfoToRemove = manager.getDeployment().getDeploymentInfo();
				stopUndertowContext(contextPath, manager, null, false);
				servletContainer.removeDeployment(deploymentInfoToRemove);
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

		if (change.getKind() == OpCode.ADD) {
			LOG.info("Adding {} to deployment info of {}", osgiModel, contextPath);

			// as with Jetty and Tomcat,
			// each unique OsgiServletContext (ServletContextHelper or HttpContext) is a facade for some, sometimes
			// shared by many osgi contexts, real ServletContext
			if (osgiServletContexts.containsKey(osgiModel)) {
				throw new IllegalStateException(osgiModel + " is already registered");
			}

			TreeSet<OsgiContextModel> osgiContextModels = this.osgiContextModels.get(contextPath);
			if (osgiContextModels == null) {
				visitServletContextModelChange(new ServletContextModelChange(OpCode.ADD, new ServletContextModel(contextPath)));
				osgiContextModels = this.osgiContextModels.get(contextPath);
			}

			// this (and similar Jetty and Tomcat places) should be the only place where
			// org.ops4j.pax.web.service.spi.servlet.OsgiServletContext is created and we have everything ready
			// to create proper classloader for this OsgiServletContext
			// unlike in Jetty or Tomcat, getRealServletContext() may return null for not started context
			ClassLoader classLoader = null;
			if (osgiModel.getClassLoader() != null) {
				// WAB scenario - the classloader was already prepared earlier when the WAB was processed..
				// The classloader already includes several reachable bundles
				classLoader = osgiModel.getClassLoader();
			}
			if (paxWebUndertowBundle != null) {
				// it may not be the case in Test scenario
				OsgiServletContextClassLoader loader = classLoader != null
						? (OsgiServletContextClassLoader) classLoader : new OsgiServletContextClassLoader();
				loader.addBundle(osgiModel.getOwnerBundle());
				loader.addBundle(paxWebUndertowBundle);
				loader.addBundle(Utils.getPaxWebJspBundle(paxWebUndertowBundle));
				loader.addBundle(Utils.getPaxWebUndertowWebSocketBundle(paxWebUndertowBundle));
				loader.addBundle(Utils.getUndertowWebSocketBundle(paxWebUndertowBundle));
				loader.makeImmutable();
				classLoader = loader;
			} else if (classLoader == null) {
				classLoader = this.classLoader;
			}
			OsgiServletContext osgiContext = new OsgiServletContext(getRealServletContext(contextPath), osgiModel, servletContextModel,
					defaultSessionCookieConfig, classLoader);

			// that's ideal place to set ServletContext.TEMPDIR attribute - it'll work for HttpService, Whiteboard and WAB
			File tmpLocation = new File(configuration.server().getTemporaryDirectory(), osgiModel.getTemporaryLocation());
			if (!tmpLocation.exists() && !tmpLocation.mkdirs()) {
				LOG.warn("Can't create temporary directory for {}: {}", osgiModel, tmpLocation.getAbsolutePath());
			}
			osgiModel.getInitialContextAttributes().put(ServletContext.TEMPDIR, tmpLocation);
			osgiContext.setAttribute(ServletContext.TEMPDIR, tmpLocation);

			osgiServletContexts.put(osgiModel, osgiContext);
			osgiContextModels.add(osgiModel);
		}

		boolean hasStopped = false;

		if (change.getKind() == OpCode.DELETE) {
			LOG.info("Removing {} from {}", osgiModel, contextPath);

			OsgiServletContext removedOsgiServletContext = osgiServletContexts.remove(osgiModel);
			TreeSet<OsgiContextModel> models = osgiContextModels.get(contextPath);
			if (models != null) {
				models.remove(osgiModel);
			}

			if (removedOsgiServletContext != null) {
				removedOsgiServletContext.unregister();
				removedOsgiServletContext.releaseWebContainerContext();
			}

			PaxWebOuterHandlerWrapper wrapper = wrappingHandlers.get(contextPath);
			if (wrapper != null) {
				OsgiServletContext currentHighestRankedContext = wrapper.getDefaultServletContext();
				if (currentHighestRankedContext == removedOsgiServletContext || pendingTransaction(contextPath)) {
					// we have to stop the context - it'll be started later
					DeploymentManager manager = getDeploymentManager(contextPath);
					if (manager != null) {
						stopUndertowContext(contextPath, manager, null, false);
						hasStopped = true;
					}
					if (wrappingHandlers.containsKey(contextPath)) {
						wrappingHandlers.get(contextPath).setDefaultServletContext(null);
					}
					if (securityHandlers.containsKey(contextPath)) {
						securityHandlers.get(contextPath).setDefaultOsgiContextModel(null, null);
					}
				}
			}
		}

		// there may be a change in what's the "best" (highest ranked) OsgiContextModel for given
		// ServletContextModel. This will became the "fallback" OsgiContextModel for chains without
		// target servlet (with filters only)
		OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
		if (highestRankedModel != null) {
			PaxWebSecurityHandler securityHandler = securityHandlers.get(contextPath);
			if (securityHandler != null && highestRankedModel != securityHandler.getDefaultOsgiContextModel()) {
				LOG.info("Changing default OSGi context model for context \"" + contextPath + "\"");
				OsgiServletContext highestRankedContext = osgiServletContexts.get(highestRankedModel);
				// default "contexts" to handle security and class/resource loading
				if (wrappingHandlers.containsKey(contextPath)) {
					wrappingHandlers.get(contextPath).setDefaultServletContext(highestRankedContext);
				}
				securityHandler.setDefaultOsgiContextModel(highestRankedModel, highestRankedContext.getResolvedWebContainerContext());

				// we have to ensure that non-highest ranked contexts are unregistered
				osgiServletContexts.forEach((ocm, osc) -> {
					if (ocm.getContextPath().equals(contextPath) && osc != highestRankedContext) {
						osc.unregister();
					}
				});

				if (!hasStopped) {
					// we have to stop the context - because its OsgiContextModel has changed, so we may
					// have to reconfigure e.g., virtual hosts
					DeploymentManager manager = getDeploymentManager(contextPath);
					if (manager != null) {
						stopUndertowContext(contextPath, manager, null, false);
						hasStopped = true;
					}
				}

				// and the highest ranked context should be registered as OSGi service (if it wasn't registered)
				highestRankedContext.register();
			}
			DeploymentInfo deploymentInfo = deploymentInfos.get(contextPath);
			if (deploymentInfo != null) {
				ServletInfo defaultServlet = deploymentInfo.getServlets().get("default");
				if (defaultServlet instanceof PaxWebServletInfo) {
					if (((PaxWebServletInfo) defaultServlet).is404()) {
						((PaxWebServletInfo) defaultServlet).setOsgiContextModel(highestRankedModel);
					}
				}
			}

			if (hasStopped) {
				if (pendingTransaction(contextPath)) {
					change.registerBatchCompletedAction(new ContextStartChange(OpCode.MODIFY, contextPath));
				} else {
					ensureServletContextStarted(contextPath);
				}
			}
		} else {
			if (wrappingHandlers.containsKey(contextPath)) {
				wrappingHandlers.get(contextPath).setDefaultServletContext(null);
			}
			if (securityHandlers.containsKey(contextPath)) {
				securityHandlers.get(contextPath).setDefaultOsgiContextModel(null, null);
			}
			DeploymentInfo deploymentInfo = deploymentInfos.get(contextPath);
			if (deploymentInfo != null) {
				ServletInfo defaultServlet = deploymentInfo.getServlets().get("default");
				if (defaultServlet instanceof PaxWebServletInfo) {
					if (((PaxWebServletInfo) defaultServlet).is404()) {
						((PaxWebServletInfo) defaultServlet).setOsgiContextModel(null);
					}
				}
			}

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

			OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
			if (ocm == highestRankedModel) {
				LOG.info("Configuring metadata of {}", ocm);

				// only in this case we'll configure the metadata
				DeploymentInfo info = deploymentInfos.get(contextPath);
				// don't bother with already deployed infos - we'll ensure this method is called quite early
				info.setMajorVersion(meta.getMajorVersion());
				info.setMinorVersion(meta.getMinorVersion());
				info.setDisplayName(meta.getDisplayName());
				// no place to set these
//				meta.getDistributable();
//				meta.isMetadataComplete();
//				meta.getPublicId();

				info.setDefaultRequestEncoding(meta.getRequestCharacterEncoding());
				info.setDefaultResponseEncoding(meta.getResponseCharacterEncoding());

				info.setDenyUncoveredHttpMethods(meta.isDenyUncoveredHttpMethods());
			}
		}
	}

	@Override
	public void visitMimeAndLocaleMappingChange(MimeAndLocaleMappingChange change) {
		if (change.getKind() == OpCode.ADD) {
			OsgiContextModel ocm = change.getOsgiContextModel();

			String contextPath = ocm.getContextPath();

			OsgiContextModel highestRankedModel = Utils.getHighestRankedModel(osgiContextModels.get(contextPath));
			if (ocm == highestRankedModel) {
				LOG.info("Configuring MIME and Locale Encoding mapping of {}", ocm);

				DeploymentInfo info = deploymentInfos.get(contextPath);
				change.getMimeMapping().forEach((ext, mime) -> info.addMimeMapping(new MimeMapping(ext, mime)));
				change.getLocaleEncodingMapping().forEach(info::addLocaleCharsetMapping);
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

			// see implementation requirements in Jetty version of this visit() method

			change.getContextModels().forEach(osgiContext -> {
				String contextPath = osgiContext.getContextPath();
				if (!done.add(contextPath)) {
					return;
				}

				LOG.debug("Adding servlet {} to {}", model.getName(), contextPath);

				// manager (lifecycle manager of the deployment) - null until
				// io.undertow.servlet.api.ServletContainer.addDeployment()
				DeploymentManager manager = getDeploymentManager(contextPath);
				// the managed deployment
				Deployment deployment = manager == null ? null : manager.getDeployment();
				// and the deployment information
				DeploymentInfo deploymentInfo = deployment == null ? deploymentInfos.get(contextPath)
						: deployment.getDeploymentInfo();

				// <servlet> - always associated with one of ServletModel's OsgiContextModels
				OsgiServletContext context = osgiServletContexts.get(osgiContext);

				// new servlet info
				ServletInfo info = new PaxWebServletInfo(model, osgiContext, context);

				boolean isDefaultResourceServlet = model.isResourceServlet();
				if (isDefaultResourceServlet) {
					for (String pattern : model.getUrlPatterns()) {
						isDefaultResourceServlet &= "/".equals(pattern);
					}
					info.addInitParam("pathInfoOnly", Boolean.toString(!isDefaultResourceServlet));
					info.addInitParam("resolve-against-context-root", Boolean.toString(isDefaultResourceServlet));
				}

				// Jetty and Tomcat have notion of "default"/"overrideable" servlets
				// but we have to do it ourselves in Undertow...
				for (Iterator<Map.Entry<String, ServletInfo>> it = deploymentInfo.getServlets().entrySet().iterator(); it.hasNext(); ) {
					Map.Entry<String, ServletInfo> e = it.next();
					ServletInfo pwsi = e.getValue();
					boolean override = false;
					if (pwsi instanceof PaxWebServletInfo) {
						if (((PaxWebServletInfo) pwsi).getServletModel() == null || ((PaxWebServletInfo) pwsi).getServletModel().isOverridable()) {
							for (String mapping : pwsi.getMappings()) {
								if (info.getMappings().contains(mapping)) {
									override = true;
									break;
								}
							}
						}
					}
					if (override) {
						it.remove();
						if (deployment != null) {
							// unfortunately we can't remove existing servlet, so we have to clear
							// its mapping
							ManagedServlet ms = deployment.getServlets().getManagedServlet(e.getKey());
							if (ms.getServletInfo() instanceof PaxWebServletInfo) {
								// it's unmodifiable map in ServletInfo, but we override it...
								ms.getServletInfo().getMappings().clear();
							}
						}
					}
				}

				// role mapping per-servlet
				model.getRoleLinks().forEach(info::addSecurityRoleRef);

				info.setRunAs(model.getRunAs());

				// when only adding new servlet, we can simply alter existing deployment
				// because this is possible (as required by methods like javax.servlet.ServletContext.addServlet())
				// we can't go the easy way when _removing_ servlets
				deploymentInfo.addServlet(info);
				if (deployment != null) {
					deployment.getServlets().addServlet(info);
				}

				// are there any error page declarations in the model?
				ErrorPageModel epm = model.getErrorPageModel();
				if (epm != null && epm.isValid()) {
					String location = epm.getLocation();
					FlexibleErrorPages currentState = errorPages.computeIfAbsent(contextPath, cp -> new FlexibleErrorPages());
					for (String ex : epm.getExceptionClassNames()) {
						try {
							ClassLoader cl = model.getRegisteringBundle().adapt(BundleWiring.class).getClassLoader();
							@SuppressWarnings("unchecked")
							Class<Throwable> t = (Class<Throwable>) cl.loadClass(ex);
							currentState.getExceptionMappings().put(t, location);
							deploymentInfo.addErrorPage(new ErrorPage(location, t));
						} catch (ClassNotFoundException e) {
							LOG.warn("Can't load exception class {}: {}", ex, e.getMessage(), e);
						}
					}
					for (int code : epm.getErrorCodes()) {
						currentState.getErrorCodeLocations().put(code, location);
						deploymentInfo.addErrorPage(new ErrorPage(location, code));
					}
					if (epm.isXx4()) {
						for (int c = 400; c < 500; c++) {
							currentState.getErrorCodeLocations().put(c, location);
							deploymentInfo.addErrorPage(new ErrorPage(location, c));
						}
					}
					if (epm.isXx5()) {
						for (int c = 500; c < 600; c++) {
							currentState.getErrorCodeLocations().put(c, location);
							deploymentInfo.addErrorPage(new ErrorPage(location, c));
						}
					}

					// replace the error pages in actual deployment
					if (deployment instanceof DeploymentImpl) {
						((DeploymentImpl) deployment).setErrorPages(currentState);
					}
				}

				deploymentInfos.put(contextPath, deploymentInfo);

				if (!change.isDynamic()) {
					ensureServletContextStarted(contextPath);
				} else if (model.isServletSecurityPresent()) {
					List<SecurityConstraintModel> dynamicModels = new ArrayList<>();
					model.getContextModels().forEach(ocm -> {
						for (SecurityConstraintModel sc : ocm.getSecurityConfiguration().getSecurityConstraints()) {
							if (sc.getServletModel() == model) {
								dynamicModels.add(sc);
							}
						}
					});
					// io.undertow.servlet.api.ServletInfo.setServletSecurityInfo() is copied to the DeploymentInfo
					// anyway
					ServletSecurityInfo ssi = new ServletSecurityInfo();
					for (SecurityConstraintModel constraintModel : dynamicModels) {
						SecurityConstraint constraint = new SecurityConstraint();
						if (constraintModel.isAuthRolesSet()) {
							constraint.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.AUTHENTICATE);
						}
						constraint.addRolesAllowed(constraintModel.getAuthRoles());
						if (constraintModel.getTransportGuarantee() == ServletSecurity.TransportGuarantee.NONE) {
							constraint.setTransportGuaranteeType(TransportGuaranteeType.NONE);
						} else if (constraintModel.getTransportGuarantee() == ServletSecurity.TransportGuarantee.CONFIDENTIAL) {
							constraint.setTransportGuaranteeType(TransportGuaranteeType.CONFIDENTIAL);
						}
						for (SecurityConstraintModel.WebResourceCollection col : constraintModel.getWebResourceCollections()) {
							WebResourceCollection wrc = new WebResourceCollection();
							boolean methodSet = false;
							wrc.addHttpMethods(col.getMethods());
							if (col.getMethods().size() == 0) {
								wrc.addHttpMethodOmissions(col.getOmittedMethods());
							}
							wrc.addUrlPatterns(col.getPatterns());
							constraint.addWebResourceCollection(wrc);
						}
						deploymentInfo.addSecurityConstraint(constraint);
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

					removeServletModel(contextPath, model, change);
				});
			}
		}
	}

	private void removeServletModel(String contextPath, ServletModel model, ServletModelChange change) {
		// this time we just assume that the servlet context is started

		LOG.info("Removing servlet {}", model);
		LOG.debug("Removing servlet {} from context {}", model.getName(), contextPath);

		// take existing deployment manager and the deployment info from its deployment
		DeploymentManager manager = getDeploymentManager(contextPath);
		Deployment deployment = manager == null ? null : manager.getDeployment();
		DeploymentInfo deploymentInfo = deployment == null ? deploymentInfos.get(contextPath)
				: deployment.getDeploymentInfo();

		if (change == null || !change.isDynamic()) {
			stopUndertowContext(contextPath, manager, deploymentInfo, false);
		}

		// but we can reuse the deployment info - this is the only object from which we can remove
		// servlets
		if (deploymentInfo != null) {
			deploymentInfo.getServlets().remove(model.getName());
			if (Arrays.asList(model.getUrlPatterns()).contains("/")) {
				// we need to replace "/" servlet
				PaxWebServletInfo defaultServletInfo = new PaxWebServletInfo("default", default404Servlet, true);
				deploymentInfo.addServlet(defaultServletInfo.addMapping("/"));
			}
		}

		// are there any error page declarations in the model?
		// we'll be redeploying the deployment info, so we don't have to change error pages
		// in existing (the one being undeployed) deployment
		ErrorPageModel epm = model.getErrorPageModel();
		if (epm != null && deploymentInfo != null) {
			String location = epm.getLocation();
			FlexibleErrorPages currentState = errorPages.computeIfAbsent(contextPath, cp -> new FlexibleErrorPages());
			for (String ex : epm.getExceptionClassNames()) {
				try {
					ClassLoader cl = model.getRegisteringBundle().adapt(BundleWiring.class).getClassLoader();
					@SuppressWarnings("unchecked")
					Class<Throwable> t = (Class<Throwable>) cl.loadClass(ex);
					currentState.getExceptionMappings().remove(t, location);
				} catch (ClassNotFoundException e) {
					LOG.warn("Can't load exception class {}: {}", ex, e.getMessage(), e);
				}
			}
			for (int code : epm.getErrorCodes()) {
				currentState.getErrorCodeLocations().remove(code, location);
			}
			if (epm.isXx4()) {
				for (int c = 400; c < 500; c++) {
					currentState.getErrorCodeLocations().remove(c, location);
				}
			}
			if (epm.isXx5()) {
				for (int c = 500; c < 600; c++) {
					currentState.getErrorCodeLocations().remove(c, location);
				}
			}

			// keep only remaining, not removed pages
			deploymentInfo.getErrorPages().clear();
			currentState.getErrorCodeLocations()
					.forEach((c, l) -> deploymentInfo.addErrorPage(new ErrorPage(location, c)));
			currentState.getExceptionMappings()
					.forEach((e, l) -> deploymentInfo.addErrorPage(new ErrorPage(location, e)));
		}

		if (change == null || !change.isDynamic()) {
			ensureServletContextStarted(contextPath);
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

				OsgiServletContext osgiContext = osgiServletContexts.get(highestRankedModel);
				DeploymentManager manager = getDeploymentManager(contextPath);
				DeploymentInfo deploymentInfo = manager == null ? deploymentInfos.get(contextPath)
						: manager.getDeployment().getDeploymentInfo();
				Deployment deployment = manager == null ? null : manager.getDeployment();

				// filter definition
				FilterInfo info = new PaxWebFilterInfo(model, osgiContext);

				deploymentInfo.addFilter(info);
				if (deployment != null) {
					deployment.getFilters().addFilter(info);
				}

				configureFilterMappings(model, deploymentInfo);
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

		// first preprocessors only
		for (Map.Entry<String, TreeMap<FilterModel, List<OsgiContextModel>>> entry : contextFilters.entrySet()) {
			String contextPath = entry.getKey();
			Map<FilterModel, List<OsgiContextModel>> filtersMap = entry.getValue();
			Set<FilterModel> filters = new TreeSet<>(filtersMap.keySet());

			LOG.info("Changing preprocessor configuration for context {}", contextPath);

			OsgiContextModel defaultHighestRankedModel = osgiContextModels.containsKey(contextPath)
					? osgiContextModels.get(contextPath).iterator().next() : null;

			// take existing deployment manager and the deployment info from its deployment
			DeploymentManager manager = getDeploymentManager(contextPath);
			DeploymentManager.State state = manager == null ? DeploymentManager.State.UNDEPLOYED : manager.getState();
			DeploymentInfo deploymentInfo = manager == null ? deploymentInfos.get(contextPath)
					: manager.getDeployment().getDeploymentInfo();

			if (deploymentInfo == null) {
				// it can happen when unregistering last filters (or rather setting the state with empty set of filters)
				continue;
			}

			// potentially, all existing preprocessors will be removed
			PaxWebPreprocessorsHandler preprocessorsHandler = preprocessorsHandlers.get(contextPath);
			Set<PreprocessorFilterConfig> toDestroy = new HashSet<>(preprocessorsHandler.getPreprocessors());
			// some new preprocessors may be added - we have to init() them ourselves, because they're not held
			// in PaxWebFilterHolders
			List<PreprocessorFilterConfig> toInit = new LinkedList<>();

			// clear to keep the order of all available preprocessors
			preprocessorsHandler.getPreprocessors().clear();

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
					preprocessorsHandler.getPreprocessors().add(filterConfig);
					iterator.remove();
				}
			}

			if (manager != null && manager.getState() == DeploymentManager.State.STARTED) {
				for (PreprocessorFilterConfig fc : toInit) {
					try {
						fc.getInstance().init(fc);
						fc.setInitCalled(true);
					} catch (ServletException ex) {
						LOG.warn("Problem during preprocessor initialization: {}", ex.getMessage(), ex);
					}
				}
				for (PreprocessorFilterConfig fc : toDestroy) {
					fc.destroy();
					fc.setInitCalled(false);
				}
			}
		}

		// now - only non-preprocessors
		for (Map.Entry<String, TreeMap<FilterModel, List<OsgiContextModel>>> entry : contextFilters.entrySet()) {
			String contextPath = entry.getKey();
			Map<FilterModel, List<OsgiContextModel>> filtersMap = entry.getValue();
			Set<FilterModel> filters = new TreeSet<>(filtersMap.keySet());

			LOG.info("Changing filter configuration for context {}", contextPath);

			OsgiContextModel defaultHighestRankedModel = osgiContextModels.containsKey(contextPath)
					? osgiContextModels.get(contextPath).iterator().next() : null;

			// take existing deployment manager and the deployment info from its deployment
			DeploymentManager manager = getDeploymentManager(contextPath);
			DeploymentManager.State state = manager == null ? DeploymentManager.State.UNDEPLOYED : manager.getState();
			DeploymentInfo deploymentInfo = manager == null ? deploymentInfos.get(contextPath)
					: manager.getDeployment().getDeploymentInfo();

			if (deploymentInfo == null) {
				// it can happen when unregistering last filters (or rather setting the state with empty set of filters)
				continue;
			}

			boolean quick = canQuicklyAddFilter(deploymentInfo, filters);
			quick &= filtersMap.values().stream().noneMatch(Objects::nonNull);

			if (!quick) {
				stopUndertowContext(contextPath, manager, null, true);
				if (manager != null) {
					state = manager.getState();
				}

				// remove all existing filters, but not the dynamic ones
				deploymentInfo = undertowFactory.clearFilters(deploymentInfo, false, true);
				deploymentInfos.put(contextPath, deploymentInfo);
			}

			// this time we don't have to care about filters which are not "changed" or which should
			// be destroyed, because unlike in Jetty and Tomcat, in Undertow we simply destroy entire
			// context (redeploy it)

			List<FilterInfo> added = new LinkedList<>();

			for (FilterModel model : filters) {
				if (model.isPreprocessor()) {
					continue;
				}
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

					configureFilterMappings(model, deploymentInfo);
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
				if (state == DeploymentManager.State.STARTED) {
					LOG.trace("Redeploying {}", contextPath);
				}
				if (!change.isDynamic()) {
					ensureServletContextStarted(contextPath);
				}
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

				DeploymentManager manager = getDeploymentManager(contextPath);
				DeploymentInfo deploymentInfo = manager == null ? deploymentInfos.get(context.getContextPath())
						: manager.getDeployment().getDeploymentInfo();

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
				if (manager != null && manager.getState() != DeploymentManager.State.UNDEPLOYED
						&& ServletContextListener.class.isAssignableFrom(eventListener.getClass())) {
					// we have to stop the context, so existing ServletContextListeners are called
					// with contextDestroyed() and new listener is added according to ranking rules of
					// the EventListenerModel
					stopUndertowContext(contextPath, manager, null, false);
					stopped = true;
				}

				// add the listener to real context - even ServletContextAttributeListener (but only once - even
				// if there are many OsgiServletContexts per ServlApplicationListenersetContext)
				// we have to wrap the listener, so proper OsgiServletContext is passed there
				EventListener wrapper = eventListener;
				if (eventListener instanceof ServletContextListener) {
					wrapper = proxiedServletContextListener(eventListener, context);
				}
				PaxWebListenerInfo info = new PaxWebListenerInfo(eventListener.getClass(), new ImmediateInstanceFactory<>(wrapper));
				info.setModel(eventListenerModel);

				// don't add the listener to DeploymentInfo yet - all the listeners will be added after sorting,
				// when the context is started (DeploymentInfo is deployed)
				if (eventListenerModel.isDynamic()) {
					orderedListeners.get(contextPath).add(info);
					if (manager != null && manager.getState() == DeploymentManager.State.UNDEPLOYED) {
						// it may be the case, when RegisteringContainerInitializer.onStartup() is adding the listener
						// but (unlike in Jetty and Tomcat) we have no way to squeeze a listener processing
						// after SCI invocation. So we have to add the listener to existing deployment
						deploymentInfo.addListener(info);
						manager.getDeployment().getApplicationListeners().addListener(new ManagedListener(info, eventListenerModel.isDynamic()));
					}
				} else {
					rankedListeners.get(contextPath).put(EventListenerKey.ofModel(eventListenerModel), info);
				}

				// check the class of non-wrapped listener
				if (!ServletContextListener.class.isAssignableFrom(eventListener.getClass())) {
					// otherwise it'll be added anyway when context is started, because such listener can
					// be added only for stopped context
					if (manager != null) {
						// we have to add it, because there'll be no restart
						deploymentInfo.addListener(info);
						manager.getDeployment().getApplicationListeners().addListener(new ManagedListener(info, eventListenerModel.isDynamic()));
					}
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

					DeploymentManager manager = getDeploymentManager(contextPath);
					DeploymentInfo deploymentInfo = manager == null ? deploymentInfos.get(contextPath)
							: manager.getDeployment().getDeploymentInfo();

					EventListener eventListener = eventListenerModel.resolveEventListener();
					if (eventListener instanceof ServletContextAttributeListener) {
						// remove it from per-OsgiContext list
						OsgiServletContext c = osgiServletContexts.get(context);
						c.removeServletContextAttributeListener((ServletContextAttributeListener) eventListener);
					}
					if (eventListener instanceof HttpSessionAttributeListener) {
						sessionListenerModels.remove(eventListenerModel);
					}

					if (eventListenerModel.isDynamic()) {
						if (orderedListeners.containsKey(contextPath)) {
							orderedListeners.get(contextPath).removeIf(li -> li.getModel() == eventListenerModel);
						}
					} else {
						if (rankedListeners.containsKey(contextPath)) {
							rankedListeners.get(contextPath).remove(EventListenerKey.ofModel(eventListenerModel));
						}
					}

					if (pendingTransaction(contextPath)) {
						LOG.debug("Delaying removal of event listener {}", eventListenerModel);
						return;
					}

					Deployment deployment = manager == null ? null : manager.getDeployment();
					stopUndertowContext(contextPath, manager, deploymentInfo, false);

					removeEventListenerModel(deploymentInfo, eventListenerModel, eventListener);

					ensureServletContextStarted(contextPath);
				});
			}
		}
	}

	private void removeEventListenerModel(DeploymentInfo deploymentInfo, EventListenerModel eventListenerModel, EventListener eventListener) {
		// remove the listener from real context - even ServletContextAttributeListener
		// unfortunately, one does not simply remove EventListener from existing context in Undertow

		if (deploymentInfo != null) {
			// this may be null in case of WAB where we keep event listeners so they get contextDestroyed
			// event properly
			deploymentInfo.getListeners().removeIf(li -> {
				try {
					return li.getInstanceFactory() instanceof ImmediateInstanceFactory
							&& ((ImmediateInstanceFactory<?>) li.getInstanceFactory()).createInstance().getInstance() == eventListener;
				} catch (InstantiationException ignored) {
					return false;
				}
			});
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

				if (osgiServletContext == null) {
					// may happen when cleaning things out
					return;
				}

				Deployment deployment = getDeployment(context.getContextPath());

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
				if (deployment != null) {
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
											((UndertowResourceServlet) servlet).setWelcomeFilesRedirect(model.isRedirect());
										} else if (servlet instanceof OsgiInitializedServlet) {
											((UndertowResourceServlet) ((OsgiInitializedServlet) servlet).getDelegate()).setWelcomeFiles(newWelcomeFiles);
											((UndertowResourceServlet) ((OsgiInitializedServlet) servlet).getDelegate()).setWelcomeFilesRedirect(model.isRedirect());
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

			// take existing deployment manager and the deployment info from its deployment
			Deployment deployment = getDeployment(contextPath);
			DeploymentInfo deploymentInfo = deployment == null ? deploymentInfos.get(contextPath)
					: deployment.getDeploymentInfo();

			if (deploymentInfo == null) {
				// may happen when cleaning things out
				return;
			}

			deploymentInfo.getErrorPages().clear();

			Map<Integer, String> errorCodeLocations = new HashMap<>();
			Map<Class<? extends Throwable>, String> exceptionMappings = new HashMap<>();
			FlexibleErrorPages pages = new FlexibleErrorPages(errorCodeLocations, exceptionMappings, null);
			// after adding ErrorPage(s) to "deployment info" they'll be changed into non-flexible ErrorPages
			// at deployment time, but at least we can keep it cached at the wrapper level
			errorPages.put(contextPath, pages);

			for (ErrorPageModel model : errorPageModels) {
				String location = model.getLocation();
				for (String ex : model.getExceptionClassNames()) {
					try {
						ClassLoader cl = model.getRegisteringBundle().adapt(BundleWiring.class).getClassLoader();
						@SuppressWarnings("unchecked")
						Class<Throwable> t = (Class<Throwable>) cl.loadClass(ex);
						exceptionMappings.put(t, location);
						deploymentInfo.addErrorPage(new ErrorPage(location, t));
					} catch (ClassNotFoundException e) {
						LOG.warn("Can't load exception class {}: {}", ex, e.getMessage(), e);
					}
				}
				for (int code : model.getErrorCodes()) {
					errorCodeLocations.put(code, location);
					deploymentInfo.addErrorPage(new ErrorPage(location, code));
				}
				if (model.isXx4()) {
					for (int c = 400; c < 500; c++) {
						errorCodeLocations.put(c, location);
						deploymentInfo.addErrorPage(new ErrorPage(location, c));
					}
				}
				if (model.isXx5()) {
					for (int c = 500; c < 600; c++) {
						errorCodeLocations.put(c, location);
						deploymentInfo.addErrorPage(new ErrorPage(location, c));
					}
				}
			}

			if (deployment instanceof DeploymentImpl) {
				((DeploymentImpl) deployment).setErrorPages(pages);
			}
		}
	}

	@Override
	public void visitContainerInitializerModelChange(ContainerInitializerModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			ContainerInitializerModel model = change.getContainerInitializerModel();
			if (!model.isForAnyRuntime() && !model.isForUndertow()) {
				return;
			}
			List<OsgiContextModel> contextModels = change.getContextModels();
			contextModels.forEach((context) -> {
				String path = context.getContextPath();
				DeploymentManager manager = getDeploymentManager(path);
				if (manager != null) {
					stopUndertowContext(path, manager, null, false);
				}

				// because of the quirks related to Undertow's deploymentInfo vs. deployment (and their
				// clones), we'll prepare special SCIInfo here
				DynamicRegistrations registrations = this.dynamicRegistrations.get(path);
				OsgiDynamicServletContext dynamicContext = new OsgiDynamicServletContext(osgiServletContexts.get(context), registrations);
				OsgiServletContainerInitializerInfo info = new OsgiServletContainerInitializerInfo(model, dynamicContext);
				initializers.get(path).add(info);
			});
		}

		if (change.getKind() == OpCode.DELETE) {
			List<ContainerInitializerModel> models = change.getContainerInitializerModels();
			for (ContainerInitializerModel model : models) {
				if (!model.isForAnyRuntime() && !model.isForUndertow()) {
					continue;
				}
				List<OsgiContextModel> contextModels = model.getContextModels();
				contextModels.forEach((context) -> {
					String path = context.getContextPath();
					TreeSet<OsgiServletContainerInitializerInfo> initializers = this.initializers.get(path);
					if (initializers != null) {
						// just remove the ServletContainerInitializerInfo without _cleaning_ it, because it was
						// _cleaned_ just after io.undertow.servlet.core.DeploymentManagerImpl.deploy() called
						// javax.servlet.ServletContainerInitializer.onStartup()
						initializers.removeIf(i -> i.getModel() == model);
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

				ensureServletContextStarted(contextPath);
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

					ensureServletContextStarted(contextPath);
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
		LOG.debug("Removing dynamically registered servlets/filters/listeners from context {}", contextPath);

		DeploymentManager manager = getDeploymentManager(contextPath);

		stopUndertowContext(contextPath, manager, null, false);
		DeploymentInfo deploymentInfo = deploymentInfos.get(contextPath);

		final int[] removed = { 0 };

		// servlets
		Map<ServletModel, Boolean> toRemove = new HashMap<>();
		for (ServletInfo info : deploymentInfo.getServlets().values()) {
			if (info instanceof PaxWebServletInfo) {
				ServletModel model = ((PaxWebServletInfo) info).getServletModel();
				if (model != null && model.isDynamic()) {
					toRemove.put(model, Boolean.TRUE);
					removed[0]++;
				}
			}
		}
		if (!toRemove.isEmpty()) {
			// it's safe, because dynamic servlets can target only one osgi/servlet context
			visitServletModelChange(new ServletModelChange(OpCode.DELETE, toRemove, true));
		}

		// remove all existing filters, but keep the non dynamic ones
		deploymentInfo = undertowFactory.clearFilters(deploymentInfo, true, false);
		deploymentInfos.put(contextPath, deploymentInfo);

		// listeners - it's easier, because we remember them in dynamic registrations
		DynamicRegistrations contextRegistrations = this.dynamicRegistrations.get(contextPath);
		if (contextRegistrations != null) {
			DeploymentInfo finalDeploymentInfo = deploymentInfo;
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

					finalDeploymentInfo.getListeners().removeIf(li -> {
						try {
							return li.getInstanceFactory() instanceof ImmediateInstanceFactory
									&& ((ImmediateInstanceFactory<?>) li.getInstanceFactory()).createInstance().getInstance() == listenerToRemove;
						} catch (InstantiationException ignored) {
							return false;
						}
					});
				}
			});

			// we have to clean after the extensions too...
			OsgiContextModel highestRanked = securityHandlers.get(contextPath).getDefaultOsgiContextModel();
			OsgiServletContext highestRankedContext = osgiServletContexts.get(highestRanked);
			ServiceLoader<PaxWebUndertowExtension> extensions = ServiceLoader.load(PaxWebUndertowExtension.class, highestRankedContext.getClassLoader());
			for (PaxWebUndertowExtension extension : extensions) {
				extension.cleanDeployment(deploymentInfo);
			}

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
		ensureServletContextStarted(contextPath);
	}

	@Override
	public void visitContextStopChange(ContextStopChange change) {
		String contextPath = change.getContextPath();
		DeploymentManager manager = getDeploymentManager(contextPath);

		stopUndertowContext(contextPath, manager, null, false);
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
			// even if it's not highest ranked
			ocm.getSecurityConfiguration().setLoginConfig(loginConfigModel);
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
			ocm.getSecurityConfiguration().setLoginConfig(null);
			securityRoles.forEach(ocm.getSecurityConfiguration().getSecurityRoles()::remove);
			securityConstraints.forEach(sc -> {
				ocm.getSecurityConfiguration().getSecurityConstraints()
						.removeIf(scm -> scm.getName().equals(sc.getName()));
			});
			TreeMap<OsgiContextModel, SecurityConfigurationModel> constraints = contextSecurityConstraints.get(ocm.getContextPath());
			constraints.remove(ocm);
		}
	}

	/**
	 * <p>This method is always (should be) called withing the "configuration thread" of Pax Web Runtime, because
	 * it's called in visit() methods for servlets (including resources) and filters, so we can safely access
	 * {@link org.ops4j.pax.web.service.spi.model.ServerModel}.</p>
	 * @param contextPath
	 */
	private void ensureServletContextStarted(final String contextPath) {
		DeploymentManager manager = getDeploymentManager(contextPath);

		if (manager != null || pendingTransaction(contextPath) || !deploymentInfos.containsKey(contextPath)
				|| securityHandlers.get(contextPath).getDefaultOsgiContextModel() == null) {
			return;
		}
		try {
			OsgiContextModel highestRanked = securityHandlers.get(contextPath).getDefaultOsgiContextModel();
			OsgiServletContext highestRankedContext = osgiServletContexts.get(highestRanked);
			highestRankedContext.allowServletContextListeners();

			// this one will be used for non-programmatic listeners
			OsgiDynamicServletContext highestRankedDynamicContext = new OsgiDynamicServletContext(highestRankedContext, dynamicRegistrations.get(contextPath));

			LOG.info("Starting Undertow context \"{}\" with default Osgi Context {}",
					(contextPath.equals("") ? "/" : contextPath), highestRanked);

			DynamicRegistrations registrations = this.dynamicRegistrations.get(contextPath);

			highestRankedContext.clearAttributesFromPreviousCycle();
			clearDynamicRegistrations(contextPath, highestRanked);

			// take previously created deployment (possibly with listeners and other "passive" configuration)
			DeploymentInfo deployment = deploymentInfos.get(contextPath);

			// SCIs require working ServletContext inside OsgiServletContext, but Undertow's ServletContext
			// is created only later
			deployment.getServletExtensions().removeIf(e -> e instanceof ContextLinkingServletExtension);
			deployment.addServletExtension(new ContextLinkingServletExtension(contextPath, highestRankedContext, highestRankedDynamicContext));

			// first thing - only NOW we can set ServletContext's class loader! It affects many things, including
			// the TCCL used for example by javax.el.ExpressionFactory.newInstance()
			if (highestRankedContext.getClassLoader() != null) {
				deployment.setClassLoader(highestRankedContext.getClassLoader());
			}

			// keycloak accesses resources directly inside
			// org.keycloak.adapters.undertow.KeycloakServletExtension#handleDeployment where we don't have
			// access to Osgi contexts
			deployment.setResourceManager(new OsgiResourceManager("", highestRankedContext));

			// handle Pax Web specific extensions
			ServiceLoader<PaxWebUndertowExtension> extensions = ServiceLoader.load(PaxWebUndertowExtension.class, highestRankedContext.getClassLoader());
			for (PaxWebUndertowExtension extension : extensions) {
				extension.handleDeployment(deployment, undertowConfiguration, this, highestRanked);
			}

			// when starting (or, which is possible only with Pax Web, not Undertow itself - restarting), we'll
			// clear all the SCIs in the deploymentInfo and add new ones (because some of them may have been removed)
			deployment.getServletContainerInitializers().clear();

			// add all configured initializers, but as special wrappers
			Collection<OsgiServletContainerInitializerInfo> initializers = new TreeSet<>(this.initializers.get(contextPath));
			// Initially I thought we should take only these SCIs, which are associated with highest ranked OCM,
			// but it turned out that just as we take servlets registered to different OsgiContextModels, but
			// the same ServletContextModel, we have to do the same with SCIs.
			// otherwise, by default (with HttpService scenario), SCIs from the OsgiContextModel related to
			// pax-web-extender-whiteboard would be taken (probably 0), simply because this bundle is usually
			// the first that grabs an instance of bundle-scoped HttpService
			// so please do not uncomment and keep for educational purposes!
//			initializers.removeIf(info -> !info.getModel().getContextModels().contains(highestRanked));

			for (OsgiServletContainerInitializerInfo info : initializers) {
				// with no Whiteboard support, we can have only one OsgiContextModel per ContainerInitializerModel
				// but we'll still act as if there could be many
				deployment.addServletContainerInitializers(info);
			}

			// and finally add the registering initializer which will also mark the OsgiServletContext as no longer
			// accepting registration of additional ServletContextListeners
			RegisteringContainerInitializer registeringSCI = new RegisteringContainerInitializer(highestRankedContext, registrations);
			deployment.addServletContainerInitializers(new OsgiServletContainerInitializerInfo(registeringSCI));

			deployment.setSessionIdGenerator(new PaxWebSessionIdGenerator());
			deployment.setSessionConfigWrapper(new SessionConfigWrapper() {
				@Override
				public SessionConfig wrap(final SessionConfig sessionConfig, Deployment deployment) {
					return new SessionConfig() {
						@Override
						public void setSessionId(HttpServerExchange exchange, String sessionId) {
							String prefix = PaxWebSessionIdGenerator.sessionIdPrefix.get();
							if (prefix != null) {
								// to trim leading "prefix~" from sessionId
								sessionId = sessionId.substring(prefix.length() + 1);
							}
							sessionConfig.setSessionId(exchange, sessionId);
						}

						@Override
						public void clearSession(HttpServerExchange exchange, String sessionId) {
							sessionConfig.clearSession(exchange, sessionId);
						}

						@Override
						public String findSessionId(HttpServerExchange exchange) {
							String prefix = PaxWebSessionIdGenerator.sessionIdPrefix.get();
							String id = sessionConfig.findSessionId(exchange);
							if (id != null && prefix == null) {
								// we may be restoring sessions in ServletInitialHandler...
								ServletRequestContext ctx = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
								if (ctx != null && ctx.getCurrentServlet() != null) {
									if (ctx.getCurrentServlet().getManagedServlet() != null && ctx.getCurrentServlet().getManagedServlet().getServletInfo() instanceof PaxWebServletInfo) {
										OsgiContextModel osgiContextModel;
										PaxWebServletInfo paxWebServletInfo = (PaxWebServletInfo) ctx.getCurrentServlet().getManagedServlet().getServletInfo();

										if (!paxWebServletInfo.is404()) {
											osgiContextModel = paxWebServletInfo.getServletContext().getOsgiContextModel();
										} else {
											osgiContextModel = paxWebServletInfo.getOsgiContextModel();
										}
										prefix = osgiContextModel == null ? null : osgiContextModel.getTemporaryLocation().replaceAll("/", "_");
									}
								}
							}
							if (prefix != null && id != null) {
								// to add leading "prefix~" to sessionId
								return prefix + "~" + id;
							}
							return id;
						}

						@Override
						public SessionCookieSource sessionCookieSource(HttpServerExchange exchange) {
							return sessionConfig.sessionCookieSource(exchange);
						}

						@Override
						public String rewriteUrl(String originalUrl, String sessionId) {
							return sessionConfig.rewriteUrl(originalUrl, sessionId);
						}
					};
				}
			});
			deployment.setSessionManagerFactory(new InMemorySessionManagerFactory());

			// alter session configuration
			SessionConfigurationModel session = highestRanked.getSessionConfiguration();
			if (session != null) {
				if (session.getSessionTimeout() != null) {
					deployment.setDefaultSessionTimeout(session.getSessionTimeout() * 60);
				}
				SessionCookieConfig scc = session.getSessionCookieConfig();
				ServletSessionConfig ssc = deployment.getServletSessionConfig();
				if (scc != null) {
					if (ssc == null) {
						ssc = new ServletSessionConfig();
						deployment.setServletSessionConfig(ssc);
					}
					if (scc.getName() != null) {
						ssc.setName(scc.getName());
					}
					if (scc.getDomain() != null) {
						ssc.setDomain(scc.getDomain());
					}
					if (scc.getPath() != null) {
						ssc.setPath(scc.getPath());
					}
					ssc.setMaxAge(scc.getMaxAge());
					ssc.setHttpOnly(scc.isHttpOnly());
					ssc.setSecure(scc.isSecure());
					ssc.setComment(scc.getComment());

					if (session.getTrackingModes().size() > 0) {
						ssc.setSessionTrackingModes(session.getTrackingModes());
					}
				}
			}

			// security configuration - from all relevant OsgiContextModels
			Map<OsgiContextModel, SecurityConfigurationModel> allSecConfigs = contextSecurityConstraints.get(contextPath);
			SecurityConfigurationModel securityConfig = null;
			if (allSecConfigs != null && allSecConfigs.size() > 0) {
				securityConfig = allSecConfigs.values().iterator().next();
			}
			if (securityConfig == null) {
				// no context processing available - just use highest-ranked model
				securityConfig = highestRanked.getSecurityConfiguration();
				allSecConfigs = Collections.singletonMap(highestRanked, securityConfig);
			}
			LoginConfigModel lc = securityConfig != null ? securityConfig.getLoginConfig() : null;

			if (lc == null) {
				deployment.setLoginConfig(null);
			} else {
				String authMethod = lc.getAuthMethod();
				String realmName = lc.getRealmName();
				if ("BASIC".equals(authMethod) || "DIGEST".equals(authMethod)) {
					if (realmName == null) {
						realmName = "default";
					}
				}

				ServletExtension customAuthenticator = getAuthenticator(authMethod.toUpperCase());
				if (customAuthenticator != null) {
					LOG.debug("Setting custom Undertow authenticator {}", customAuthenticator);
					deployment.getServletExtensions().add(customAuthenticator);
				}

				deployment.setLoginConfig(new LoginConfig(authMethod, realmName,
						lc.getFormLoginPage(), lc.getFormErrorPage()));
			}

			deployment.getSecurityRoles().clear();
			deployment.getSecurityConstraints().clear();

			// roles and constraints are not taken only from the highest ranked OsgiContextModel - they're
			// taken from all the OCMs for given context path - on order of OCM rank
			// it's up to user to take care of the conflicts, because simple rank-ordering will add higher-ranked
			// rules first - the container may decide to override or reject the lower ranked later.

			List<SecurityConstraintModel> allConstraints = new ArrayList<>();
			Set<String> allRoles = new LinkedHashSet<>();
			if (allSecConfigs != null) {
				allSecConfigs.values().forEach(sc -> {
					allConstraints.addAll(sc.getSecurityConstraints());
					allRoles.addAll(sc.getSecurityRoles());
				});
			}

			deployment.addSecurityRoles(allRoles);

			for (SecurityConstraintModel constraintModel : allConstraints) {
				SecurityConstraint constraint = new SecurityConstraint();
				if (constraintModel.isAuthRolesSet()) {
					constraint.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.AUTHENTICATE);
				}
				constraint.addRolesAllowed(constraintModel.getAuthRoles());
				if (constraintModel.getTransportGuarantee() == ServletSecurity.TransportGuarantee.NONE) {
					constraint.setTransportGuaranteeType(TransportGuaranteeType.NONE);
				} else if (constraintModel.getTransportGuarantee() == ServletSecurity.TransportGuarantee.CONFIDENTIAL) {
					constraint.setTransportGuaranteeType(TransportGuaranteeType.CONFIDENTIAL);
				}
				for (SecurityConstraintModel.WebResourceCollection col : constraintModel.getWebResourceCollections()) {
					WebResourceCollection wrc = new WebResourceCollection();
					boolean methodSet = false;
					wrc.addHttpMethods(col.getMethods());
					if (col.getMethods().size() == 0) {
						wrc.addHttpMethodOmissions(col.getOmittedMethods());
					}
					wrc.addUrlPatterns(col.getPatterns());
					constraint.addWebResourceCollection(wrc);
				}
				deployment.addSecurityConstraint(constraint);
			}

			// only now add the listeners in correct order

			// SCIs may have added some listeners which we've hijacked, to order them according
			// to Whiteboard/ranking rules. Now it's perfect time to add them in correct order
			for (int pos = 0; pos < orderedListeners.get(contextPath).size(); pos++) {
				PaxWebListenerInfo li = orderedListeners.get(contextPath).get(pos);
				rankedListeners.get(contextPath).put(EventListenerKey.ofPosition(pos), li);
			}

			for (ListenerInfo li : rankedListeners.get(contextPath).values()) {
				deployment.addListener(li);
			}

			// taking virtual host / connector configuration from OsgiContextModel - see
			// org.eclipse.jetty.server.handler.ContextHandler.checkVirtualHost() and similar pax-web-jetty code
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

			PaxWebOuterHandlerWrapper handlerWrapper = wrappingHandlers.get(contextPath);
			if (handlerWrapper != null) {
				handlerWrapper.setVirtualHosts(allVirtualHosts.toArray(new String[0]));
			}

			manager = servletContainer.addDeployment(deployment);

			// here's where Undertow-specific instance of javax.servlet.ServletContext is created
			manager.deploy();

			HttpHandler handler = manager.start();

			// the above start() ends with filter initialization and just after that, the state is changed
			// to State.STARTED. So we can start preprocessors here
			for (PreprocessorFilterConfig fc : preprocessorsHandlers.get(contextPath).getPreprocessors()) {
				if (!fc.isInitCalled()) {
					fc.getInstance().init(fc);
					fc.setInitCalled(true);
				}
			}

			highestRankedDynamicContext.rememberAttributesFromSCIs();

			// actual registration of "context" in Undertow's path handler.
			pathHandler.addPrefixPath(contextPath, handler);
		} catch (ServletException e) {
			throw new IllegalStateException("Can't start Undertow context "
					+ contextPath + ": " + e.getMessage(), e);
		}
	}

	private boolean pendingTransaction(String contextPath) {
		return transactions.contains(contextPath);
	}

	/**
	 * If the servlet context is already started ({@link DeploymentManager#deploy()} was called), return
	 * it from current {@link Deployment}.
	 * @param contextPath
	 * @return
	 */
	private ServletContext getRealServletContext(String contextPath) {
		Deployment deployment = getDeployment(contextPath);
		return deployment == null ? null : deployment.getServletContext();
	}

	/**
	 * Get {@link Deployment} associated with given context path - but only if related {@link DeploymentManager}
	 * is started.
	 * @param contextPath
	 * @return
	 */
	private Deployment getDeployment(String contextPath) {
		DeploymentManager deploymentManager = getDeploymentManager(contextPath);

		if (deploymentManager == null || deploymentManager.getDeployment() == null) {
			return null;
		}

		return deploymentManager.getDeployment();
	}

	/**
	 * Get {@link DeploymentManager} for given context path - if available.
	 * @param contextPath
	 * @return
	 */
	private DeploymentManager getDeploymentManager(String contextPath) {
		String path = contextPath.equals("") ? "/" : contextPath;
		DeploymentManager deploymentManager = servletContainer.getDeploymentByPath(path);

		if (deploymentManager == null || deploymentManager.getDeployment() == null) {
			return null;
		}
		if (!deploymentManager.getDeployment().getDeploymentInfo().getContextPath().equals(path)) {
			// io.undertow.servlet.api.ServletContainer.getDeploymentByPath "traverses up" the request path...
			return null;
		}

		return deploymentManager;
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
			if (!(existingFilters[pos] instanceof PaxWebFilterInfo)) {
				quick = false;
				break;
			}
			PaxWebFilterInfo fi = (PaxWebFilterInfo) existingFilters[pos];
			if (!(fi.getFilterModel() != null && fi.getFilterModel().equals(newFilters[pos]))) {
				quick = false;
				break;
			}
			pos++;
		}

		return quick;
	}

	private void configureFilterMappings(FilterModel model, DeploymentInfo deploymentInfo) {
		String filterName = model.getName();

		if (model.isDynamic()) {
			model.getDynamicServletNames().forEach(dm -> {
				if (!dm.isAfter()) {
					for (DispatcherType dt : dm.getDispatcherTypes()) {
						for (String sn : dm.getServletNames()) {
							deploymentInfo.insertFilterServletNameMapping(0, filterName, sn, dt);
						}
					}
				}
			});
			model.getDynamicUrlPatterns().forEach(dm -> {
				if (!dm.isAfter()) {
					for (DispatcherType dt : dm.getDispatcherTypes()) {
						for (String pattern : dm.getUrlPatterns()) {
							deploymentInfo.insertFilterUrlMapping(0, filterName, pattern, dt);
						}
					}
				}
			});
			model.getDynamicServletNames().forEach(dm -> {
				if (dm.isAfter()) {
					for (DispatcherType dt : dm.getDispatcherTypes()) {
						for (String sn : dm.getServletNames()) {
							deploymentInfo.addFilterServletNameMapping(filterName, sn, dt);
						}
					}
				}
			});
			model.getDynamicUrlPatterns().forEach(dm -> {
				if (dm.isAfter()) {
					for (DispatcherType dt : dm.getDispatcherTypes()) {
						for (String pattern : dm.getUrlPatterns()) {
							deploymentInfo.addFilterUrlMapping(filterName, pattern, dt);
						}
					}
				}
			});
		} else {
			// non-dynamic mapping
			for (FilterModel.Mapping mapping : model.getMappingsPerDispatcherTypes()) {
				for (DispatcherType dt : mapping.getDispatcherTypes()) {
					if (mapping.getRegexPatterns() != null && mapping.getRegexPatterns().length > 0) {
						deploymentInfo.addFilterUrlMapping(filterName, "/*", dt);
					} else if (mapping.getUrlPatterns() != null) {
						for (String pattern : mapping.getUrlPatterns()) {
							deploymentInfo.addFilterUrlMapping(filterName, pattern, dt);
						}
					}
					if (mapping.getServletNames() != null) {
						for (String name : mapping.getServletNames()) {
							deploymentInfo.addFilterServletNameMapping(filterName, name, dt);
						}
					}
				}
			}
		}
	}

	/**
	 * Undertow has a bit tricky way to stop the context...
	 *
	 * @param contextPath
	 * @param manager
	 * @param deploymentInfo new {@link DeploymentInfo} that could be used if the context should be started again
	 * @param skipPreprocessors
	 */
	private void stopUndertowContext(String contextPath, DeploymentManager manager,
			DeploymentInfo deploymentInfo, boolean skipPreprocessors) {
		// let's immediately show that given context is no longer mapped
		pathHandler.removePrefixPath(contextPath);

		try {
			// manager needs to stop the deployment and get rid of it, because we
			// can't replace a deployment info within deployment manager
			if (manager != null/* && deployment != null*/) {
				LOG.info("Stopping Undertow context \"{}\"", contextPath);

				if (!skipPreprocessors) {
					for (PreprocessorFilterConfig fc : preprocessorsHandlers.get(contextPath).getPreprocessors()) {
						if (fc.isInitCalled()) {
							fc.destroy();
							fc.setInitCalled(false);
						}
					}
				}

				manager.stop();
				manager.undeploy();
			}
			// swap the deployment info, which will later be used to start the context
			if (deploymentInfo != null) {
				deploymentInfos.put(contextPath, deploymentInfo);
			}
		} catch (ServletException e) {
			LOG.warn("Error stopping Undertow context \"{}\": {}", contextPath, e.getMessage(), e);
		}

		// finally clear dynamic listeners - they'll be added again when the context is started

		if (deploymentInfos.containsKey(contextPath)) {
			// remove the listeners without associated EventListenerModel from rankedListeners map
			rankedListeners.get(contextPath).entrySet().removeIf(e -> e.getKey().getRanklessPosition() >= 0);
			// ALL listeners added without a model (listeners added by SCIs and other listeners) will be cleared
			orderedListeners.get(contextPath).clear();
			// and clear the listeners in deployment info (fortunately Undertow allows us to do it)
			deploymentInfos.get(contextPath).getListeners().clear();
		}
	}

	// see org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService#getConfidentialPortManager
	private class SimpleConfidentialPortManager implements ConfidentialPortManager {
		@Override
		public int getConfidentialPort(HttpServerExchange exchange) {
			int port = exchange.getConnection().getLocalAddress(InetSocketAddress.class).getPort();
			if (port < 0) {
				LOG.debug("Confidential port not defined for port {}", port);
			}
			Integer mappedPort = UndertowServerWrapper.this.securePortMapping.get(port);
			if (mappedPort == null) {
				return -1;
			}
			return mappedPort;
		}
	}

	private static class ContextLinkingInvocationHandler implements InvocationHandler {
		private final EventListener eventListener;
		private ServletContext osgiContext;

		ContextLinkingInvocationHandler(EventListener eventListener) {
			this.eventListener = eventListener;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (osgiContext != null) {
				if (method.getName().equals("contextInitialized")) {
					return method.invoke(eventListener, new ServletContextEvent(osgiContext));
				} else if (method.getName().equals("contextDestroyed")) {
					return method.invoke(eventListener, new ServletContextEvent(osgiContext));
				}
			}
			return method.invoke(eventListener, args);
		}

		public void setOsgiContext(ServletContext osgiContext) {
			this.osgiContext = osgiContext;
		}
	}

	/**
	 * An internal {@link ServletExtension} that propagates "container servlet context" to all related
	 * {@link OsgiServletContext}s and that configures {@link ContextLinkingInvocationHandler}s.
	 */
	private class ContextLinkingServletExtension implements ServletExtension {
		private final String contextPath;
		private final OsgiServletContext osgiContext;
		private final OsgiDynamicServletContext osgiDynamicContext;

		ContextLinkingServletExtension(String contextPath, OsgiServletContext osgiContext,
				OsgiDynamicServletContext osgiDynamicContext) {
			this.contextPath = contextPath;
			this.osgiContext = osgiContext;
			this.osgiDynamicContext = osgiDynamicContext;
		}

		@Override
		public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
			for (ListenerInfo lInfo : deploymentInfo.getListeners()) {
				try {
					// it's an immediate instance factory, so we can call createInstance() many times
					InstanceHandle<? extends EventListener> handle = lInfo.getInstanceFactory().createInstance();
					EventListener el = handle.getInstance();
					if (Proxy.isProxyClass(el.getClass())) {
						InvocationHandler ih = Proxy.getInvocationHandler(el);
						if (ih instanceof ContextLinkingInvocationHandler) {
							if (lInfo.isProgramatic()) {
								((ContextLinkingInvocationHandler) ih).setOsgiContext(osgiContext);
							} else {
								((ContextLinkingInvocationHandler) ih).setOsgiContext(osgiDynamicContext);
							}
						}
					}
				} catch (InstantiationException ignored) {
				}
			}

			osgiServletContexts.forEach((ocm, osc) -> {
				if (ocm.getContextPath().equals(contextPath)) {
					osc.setContainerServletContext(servletContext);
				}
			});
		}
	}

	private ServletExtension getAuthenticator(String method) {
		ServiceLoader<AuthenticatorService> sl = ServiceLoader.load(AuthenticatorService.class, getClass().getClassLoader());
		for (AuthenticatorService svc : sl) {
			try {
				ServletExtension auth = svc.getAuthenticatorService(method, ServletExtension.class);
				if (auth != null) {
					return auth;
				}
			} catch (Throwable t) {
				LOG.debug("Unable to load AuthenticatorService for: " + method, t);
			}
		}
		return null;
	}

}
