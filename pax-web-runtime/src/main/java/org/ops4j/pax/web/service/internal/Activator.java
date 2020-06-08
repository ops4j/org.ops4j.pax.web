/*
 * Copyright 2007 Niclas Hedhman.
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2011 Achim Nierbeck.
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
package org.ops4j.pax.web.service.internal;

import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.pax.web.annotations.PaxWebConfiguration;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.util.SupportUtils;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.web.service.PaxWebConstants.HTTPSERVICE_REGISTRATION_NAMES;

/**
 * <p>Main entry point to Pax-Web.</p>
 * <p>This activator performs these actions:<ul>
 *     <li>servlet event dispatcher</li>
 *     <li>registration of {@link ServletListener}-{@link EventAdmin} bridge</li>
 *     <li>registration of {@link ServletListener}-{@link LogService} bridge</li>
 *     <li>registration of {@link org.osgi.service.cm.ManagedService} to monitor
 *     {@code org.ops4j.pax.web} PID changes</li>
 * </ul></p>
 * <p></p>
 */
public class Activator implements BundleActivator, PaxWebManagedService.ConfigurationUpdater {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private BundleContext bundleContext;

	// "current" objects mean that they're not bound to the lifecycle of pax-web-runtime bundle, but
	// to instance of configuration (from Configuration Admin) and to ServerControllerFactory service
	// registered by one of server bundles (pax-web-jetty, pax-web-undertow, pax-web-tomcat)

	/** Current Configuration Admin configuration (PID = {@code org.ops4j.pax.web}) */
	private Dictionary<String, ?> configuration;

	/** Current {@link ServerControllerFactory} tracked from OSGi registry */
	private ServerControllerFactory serverControllerFactory;

	/** Current {@link ServerController} created using {@link #serverControllerFactory} */
	private ServerController serverController;

	/**
	 * {@link ServletEventDispatcher} bound to lifecycle of this pax-web-runtime bundle, not to configuration
	 * or {@link ServerControllerFactory}.
	 */
	private ServletEventDispatcher servletEventDispatcher;

//	/** Processor for instructions in {@code org.ops4j.pax.web.context} factory PID */
//	private HttpContextProcessing httpContextProcessing;

	/**
	 * Registration for current {@link org.osgi.framework.ServiceFactory} for {@link HttpService} and
	 * {@link WebContainer}
	 */
	private ServiceRegistration<?> httpServiceFactoryReg;

	/** Registration of {@link org.osgi.service.cm.ManagedService} for {@code org.ops4j.pax.web} PID. */
	private ServiceRegistration<?> managedServiceReg;

	/** Registration of default {@link ServletContextHelper} */
	private ServiceRegistration<ServletContextHelper> servletContextHelperReg;

//	/**
//	 * Registration of MSF for {@code org.ops4j.pax.web.context} factory PID for current
//	 * {@link ServerControllerFactory}. When {@link ServerControllerFactory} changes, this MSF is re-registered.
//	 */
//	private ServiceRegistration<ManagedServiceFactory> managedServiceFactoryReg;

	/** Tracker for {@link ServerControllerFactory} that may come from one of server bundles (e.g., pax-web-jetty) */
	private ServiceTracker<ServerControllerFactory, ServerControllerFactory> serverControllerFactoryTracker;

	/** Tracker for optional EventAdmin */
	private ServiceTracker<EventAdmin, EventAdmin> eventServiceTracker;

	/** Tracker for optional LogService, but because Slf4J comes from pax-logging anyway, this service is usually available */
	private ServiceTracker<LogService, LogService> logServiceTracker;

	private final AtomicBoolean initialConfigSet = new AtomicBoolean(false);

	/**
	 * Single thread pool to process all configuration changes, {@link ServerControllerFactory} (re)registrations
	 * and (since Pax Web 8) also actual registrations of web elements.
	 */
	private ScheduledExecutorService runtimeExecutor;
	private long registrationThreadId;

	@Override
	public void start(final BundleContext context) throws Exception {
		LOG.debug("Starting Pax Web");

		runtimeExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("paxweb-config"));
		registrationThreadId = ServerModel.getThreadIdFromSingleThreadPool(runtimeExecutor);

		bundleContext = context;

		servletEventDispatcher = new ServletEventDispatcher(context);

//		if (SupportUtils.isEventAdminAvailable()) {
//			// Do use the filters this way the eventadmin packages can be resolved optional!
//			Filter filterEvent = context.createFilter("(objectClass=org.osgi.service.event.EventAdmin)");
//			EventAdminHandler adminHandler = new EventAdminHandler(context);
//			eventServiceTracker = new ServiceTracker<>(context, filterEvent, adminHandler);
//			eventServiceTracker.open();
//
//			context.registerService(ServletListener.class, adminHandler, null);
//			LOG.info("EventAdmin support enabled, servlet events will be postet to topics.");
//		} else {
//			LOG.info("EventAdmin support is not available, no servlet events will be posted!");
//		}

//		if (SupportUtils.isLogServiceAvailable()) {
//			// Do use the filters this way the logservice packages can be resolved optional!
//			Filter filterLog = context.createFilter("(objectClass=org.osgi.service.log.LogService)");
//			LogServiceHandler logServiceHandler = new LogServiceHandler(context);
//			logServiceTracker = new ServiceTracker<>(context, filterLog, logServiceHandler);
//			logServiceTracker.open();
//
//			context.registerService(ServletListener.class, logServiceHandler, null);
//			LOG.info("LogService support enabled, log events will be created.");
//		} else {
//			LOG.info("LogService support is not available, no log events will be created!");
//		}

//		// special handling for JSP Compiler
//		if (SupportUtils.isJSPAvailable()) {
//			System.setProperty("org.apache.jasper.compiler.disablejsr199", Boolean.TRUE.toString());
//		}

		if (SupportUtils.isConfigurationAdminAvailable()) {
			// ManagedService for org.ops4j.pax.web PID monitoring, so configuration won't happen yet
			// (for example in FelixStartLevel thread), but only after Configuration Admin notifies us
			registerManagedService(context);
		} else {
			// no org.osgi.service.cm.ConfigurationAdmin available at all, so we can configure immediately
			updateConfiguration(null);
		}

		LOG.info("Pax Web started");
	}

	@Override
	public void stop(final BundleContext context) {
		LOG.debug("Stopping Pax Web...");

		if (serverControllerFactoryTracker != null) {
			serverControllerFactoryTracker.close();
		}
		if (servletContextHelperReg != null) {
			servletContextHelperReg.unregister();
			servletContextHelperReg = null;
		}
//		if (logServiceTracker != null) {
//			logServiceTracker.close();
//		}
		if (managedServiceReg != null) {
			managedServiceReg.unregister();
			managedServiceReg = null;
		}
//		if (eventServiceTracker != null) {
//			eventServiceTracker.close();
//		}
		if (servletEventDispatcher != null) {
			servletEventDispatcher.destroy();
		}
//		if (httpContextProcessing != null) {
//			httpContextProcessing.destroy();
//		}
		// Wait up to 20 seconds, otherwhise
		try {
			runtimeExecutor.shutdown();
			LOG.debug("...entering 20 seconds grace period...");
			runtimeExecutor.awaitTermination(20, TimeUnit.SECONDS);
			runtimeExecutor.shutdownNow();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			// Ignore, we are done anyways...
		}

		LOG.info("Pax Web stopped");
	}

	/**
	 * Registers a managed service to listen on configuration updates. Used only if
	 * {@link org.osgi.service.cm.ConfigurationAdmin} is available.
	 * @param context bundle context to use for registration
	 */
	private void registerManagedService(final BundleContext context) {
		final Dictionary<String, String> props = new Hashtable<>();
		props.put(Constants.SERVICE_PID, PaxWebConstants.PID);
		managedServiceReg = context.registerService("org.osgi.service.cm.ManagedService",
				new PaxWebManagedService(this), props);
	}

//	/**
//	 * Registers a managed service factory to create {@link org.osgi.service.http.HttpContext} <em>processors</em>
//	 * - these will possibly register additional web items (like login configurations or filters) for shared or
//	 * per-bundle http services.
//	 *
//	 * @param context
//	 */
//	private void createManagedServiceFactory(BundleContext context) {
//		// sanity check
//		if (managedServiceFactoryReg != null) {
//			managedServiceFactoryReg.unregister();
//			managedServiceFactoryReg = null;
//		}
//		final Dictionary<String, String> props = new Hashtable<>();
//		props.put(Constants.SERVICE_PID, HttpContextProcessing.PID);
//		httpContextProcessing = new HttpContextProcessing(bundleContext, serverController);
//		managedServiceFactoryReg = context.registerService(ManagedServiceFactory.class, httpContextProcessing, props);
//	}

	// two methods update the configuration of entire Pax Web runtime:
	//  - updateConfiguration(Dictionary) - schedules reconfiguration because configuration properties changed
	//  - updateServerControllerFactory(ServerControllerFactory) - schedules reconfiguration because ServerControllerFactory changed
	// both methods schedule the reconfiguration in another thread from single thread pool

	/**
	 * Called directly or from {@link org.osgi.service.cm.ManagedService#updated(Dictionary)}. Current
	 * {@link HttpService} has to be re-registered because configuration has changed.
	 * @param configuration
	 */
	@Override
	public void updateConfiguration(final Dictionary<String, ?> configuration) {
		LOG.info("Scheduling Pax Web reconfiguration because configuration has changed");
		// change configuration using new properties (possibly from configadmin) and current ServerControllerFactory
		runtimeExecutor.submit(() -> updateController(configuration, serverControllerFactory));
	}

	/**
	 * Called by tracker of {@link ServerControllerFactory} services. Current {@link HttpService} has to be
	 * re-registered because target server has changed.
	 * @param controllerFactory
	 */
	private void updateServerControllerFactory(final ServerControllerFactory controllerFactory) {
		if (this.serverControllerFactory != null) {
			if (controllerFactory == null) {
				LOG.info("Scheduling Pax Web reconfiguration because ServerControllerFactory has been unregistered");
			} else {
				LOG.info("Scheduling Pax Web reconfiguration because ServerControllerFactory has been re-registered");
			}
		} else if (controllerFactory != null) {
			LOG.info("Scheduling Pax Web reconfiguration because ServerControllerFactory has been registered");
		}

		// change configuration using new (or null when not available) ServerControllerFactory and current configuration
		Future<?> future = runtimeExecutor.submit(() -> updateController(configuration, controllerFactory));

		// Make sure that when destroying the configuration (factory == null), we do things synchronously
		if (controllerFactory == null) {
			try {
				future.get(20, TimeUnit.SECONDS);
			} catch (Exception e) {
				LOG.info("Error when updating factory: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * <p>This method is the only place which is allowed to modify the config and factory fields and it should
	 * run only within single-threded {@link java.util.concurrent.ExecutorService}.</p>
	 *
	 * <p>Here a new {@link org.osgi.framework.ServiceFactory} for {@link HttpService} and {@link WebContainer}
	 * is registered for {@code org.ops4j.pax.web} PID.</p>
	 *
	 * <p>This method may be called in 3 cases:<ul>
	 *     <li>when {@link org.osgi.service.cm.ConfigurationAdmin} passes changed {@code org.ops4j.pax.web}
	 *     PID config</li>
	 *     <li>when {@link ServerControllerFactory} is added/removed by tracker</li>
	 *     <li>when {@link org.osgi.service.cm.ConfigurationAdmin} is not available and this activator starts
	 *     (default configuration)</li>
	 * </ul></p>
	 *
	 * @param dictionary
	 * @param controllerFactory
	 */
	@PaxWebConfiguration
	private void updateController(Dictionary<String, ?> dictionary, ServerControllerFactory controllerFactory) {
		// We want to make sure the configuration is known before starting the
		// service tracker, else the configuration could be set after the
		// service is found which would cause a restart of the service
		if (!initialConfigSet.get()) {
			LOG.debug("Initial configuration of pax-web-runtime, registration of ServerControllerFactory tracker");
			initialConfigSet.compareAndSet(false, true);
			this.configuration = dictionary;
			this.serverControllerFactory = controllerFactory; // should always be null here

			// the only place where tracker of ServerControllerFactory services is created and opened
			serverControllerFactoryTracker = new ServiceTracker<>(bundleContext, ServerControllerFactory.class, new ServerControllerFactoryCustomizer());
			serverControllerFactoryTracker.open();

			// we have configuration (possibly empty). Getting ServerControllerFactory from the tracker is the
			// next step. We can't do anything without the server controller.
			// remember that updateController() may be called quickly in the above tracker.open() method, where
			// initial reference to ServerControllerFactory is already available (that's why we use ExecutorService)
			return;
		}

		if (Utils.same(dictionary, this.configuration) && Utils.same(controllerFactory, this.serverControllerFactory)) {
			LOG.debug("No change in configuration of Pax Web Runtime.");
			return;
		}

		if (httpServiceFactoryReg != null) {
			LOG.info("Unregistering current HTTP Service factory");
			httpServiceFactoryReg.unregister();
			httpServiceFactoryReg = null;
		}
//		if (managedServiceFactoryReg != null) {
//			managedServiceFactoryReg.unregister();
//			managedServiceFactoryReg = null;
//		}
		if (serverController != null) {
			LOG.info("Stopping current server controller {}", serverController);
			try {
				serverController.stop();
			} catch (Exception e) {
				LOG.error("Problem stopping server controller: " + e.getMessage(), e);
			}
			serverController = null;
		}

		boolean hadSCF = this.serverControllerFactory != null;

		this.configuration = dictionary;
		this.serverControllerFactory = controllerFactory;

		if (serverControllerFactory == null) {
			if (hadSCF) {
				LOG.info("ServerControllerFactory is gone, HTTP Service is not available now.");
			}
			return;
		}

		// proceed with possibly non-empty (non-default) configuration and with available ServerControllerFactory
		// configuration from PID (if available) has higher priority than properties from BundleContext / MetaType
		performConfiguration();
	}

	/**
	 * Actual configuration method called only when {@link ServerControllerFactory} is added.
	 */
	@PaxWebConfiguration
	private void performConfiguration() {
		try {
			// Configure chained PropertyResolver to get properties from Config Admin, Bundle Context, Meta Type
			// information (in such order).
			// Properties as map will also be available in proper order

			Map<String, String> allProperties = new HashMap<>(System.getenv());
			allProperties.putAll(Utils.toMap(System.getProperties()));

			MetaTypePropertyResolver defaultResolver = new MetaTypePropertyResolver();
			allProperties.putAll(Utils.toMap(defaultResolver.getProperties()));

			// can't get all bundle context properties as map...
			PropertyResolver tmpResolver = new BundleContextPropertyResolver(bundleContext, defaultResolver);

			PropertyResolver resolver = this.configuration != null ? new DictionaryPropertyResolver(this.configuration, tmpResolver) : tmpResolver;
			allProperties.putAll(Utils.toMap(this.configuration));

			// full configuration with all required properties. That's all that is needed down the stream
			final Configuration configuration = ConfigurationBuilder.getConfiguration(resolver, allProperties);

			// global, single representation of web server state. It's used
			//  - in all bundle-scoped instances of HttpServiceEnabled
			//  - also to reflect Whiteboard registrations (through pax-web-extender-whiteboard)
			final ServerModel serverModel = new ServerModel(runtimeExecutor, registrationThreadId);

			// create a controller object to operate on any supported web server
			serverController = serverControllerFactory.createServerController(configuration);

			// first step is to configure the server without actually starting it
			LOG.info("Configuring server controller {}", serverController.getClass().getName());
			serverController.configure();

			LOG.info("Starting server controller {}", serverController.getClass().getName());
			serverController.start();

			// create default ServletContextModel in ServerModel and propagate it to the controller.
			// it's important to do it before registering ServiceFactory for HttpService/WebContainer
			serverModel.createDefaultServletContextModel(serverController);

			// this is where org.osgi.service.http.HttpService bundle-scoped service is registered in OSGi
			// this is the most fundamental operation related to Http Service specification
			Dictionary<String, Object> props = determineServiceProperties(this.configuration, configuration);
			ServiceFactory<StoppableHttpService> factory = new StoppableHttpServiceFactory() {
				@Override
				StoppableHttpService createService(Bundle bundle) {
					HttpServiceEnabled enabledService =
							new HttpServiceEnabled(bundle, serverController, serverModel, servletEventDispatcher, configuration);

					return new HttpServiceProxy(bundle, enabledService);
				}
			};
			LOG.info("Registering HttpService factory");
			httpServiceFactoryReg = bundleContext.registerService(HTTPSERVICE_REGISTRATION_NAMES, factory, props);

			// ManagedServiceFactory for org.ops4j.pax.web.context factory PID
			// we need registered WebContainer for this MSF to work
//			createManagedServiceFactory(bundleContext);
		} catch (Throwable t) {
			// TODO: ignore those exceptions if the bundle is being stopped
			LOG.error("Unable to start Pax Web server: {}", t.getMessage(), t);
		}
	}

	/**
	 * Pass properties used to configure {@link HttpService} to service registration.
	 *
	 * @param managedConfig
	 * @param configuration
	 * @param httpPort
	 * @param httpSecurePort
	 * @return
	 *
	 * @since 0.6.0, PAXWEB-127
	 */
	private Dictionary<String, Object> determineServiceProperties(final Dictionary<String, ?> managedConfig, final Configuration configuration) {

		final Dictionary<String, Object> toPropagate = new Hashtable<>();
		// first store all configuration properties as received via managed service
		if (managedConfig != null && !managedConfig.isEmpty()) {
			final Enumeration<String> enumeration = managedConfig.keys();
			while (enumeration.hasMoreElements()) {
				String key = enumeration.nextElement();
				toPropagate.put(key, managedConfig.get(key));
			}
		}

		// then add/replace configuration properties
//		setProperty(toPropagate, PROPERTY_HTTP_ENABLED, configuration.isHttpEnabled());
//		setProperty(toPropagate, PROPERTY_HTTP_PORT, configuration.getHttpPort());
//		setProperty(toPropagate, PROPERTY_HTTP_CONNECTOR_NAME,
//				configuration.getHttpConnectorName());
//		setProperty(toPropagate, PROPERTY_HTTP_SECURE_ENABLED,
//				configuration.isHttpSecureEnabled());
//		setProperty(toPropagate, PROPERTY_HTTP_SECURE_PORT,
//				configuration.getHttpSecurePort());
//		setProperty(toPropagate, PROPERTY_HTTP_SECURE_CONNECTOR_NAME,
//				configuration.getHttpSecureConnectorName());
//		setProperty(toPropagate, PROPERTY_HTTP_USE_NIO, configuration.useNIO());
//		setProperty(toPropagate, PROPERTY_SSL_CLIENT_AUTH_NEEDED,
//				configuration.isClientAuthNeeded());
//		setProperty(toPropagate, PROPERTY_SSL_CLIENT_AUTH_WANTED,
//				configuration.isClientAuthWanted());
//		setProperty(toPropagate, PROPERTY_SSL_KEYSTORE, configuration.getSslKeystore());
//		setProperty(toPropagate, PROPERTY_SSL_KEYSTORE_TYPE,
//				configuration.getSslKeystoreType());
//		setProperty(toPropagate, PROPERTY_SSL_PASSWORD, configuration.getSslPassword());
//		setProperty(toPropagate, PROPERTY_SSL_KEYPASSWORD,
//				configuration.getSslKeyPassword());
//		setProperty(toPropagate, PROPERTY_CIPHERSUITE_INCLUDED, configuration.getCiphersuiteIncluded());
//		setProperty(toPropagate, PROPERTY_CIPHERSUITE_EXCLUDED, configuration.getCiphersuiteExcluded());
//		setProperty(toPropagate, PROPERTY_SSL_RENEGOTIATION_ALLOWED, configuration.isSslRenegotiationAllowed());
//		setProperty(toPropagate, PROPERTY_TEMP_DIR,
//				configuration.getTemporaryDirectory());
//		setProperty(toPropagate, PROPERTY_SESSION_TIMEOUT,
//				configuration.getSessionTimeout());
//		setProperty(toPropagate, PROPERTY_SESSION_URL, configuration.getSessionUrl());
//		setProperty(toPropagate, PROPERTY_SESSION_COOKIE,
//				configuration.getSessionCookie());
//		setProperty(toPropagate, PROPERTY_SESSION_DOMAIN,
//				configuration.getSessionDomain());
//		setProperty(toPropagate, PROPERTY_SESSION_PATH,
//				configuration.getSessionPath());
//		setProperty(toPropagate, PROPERTY_SESSION_COOKIE_SECURE,
//				configuration.getSessionCookieSecure());
//		setProperty(toPropagate, PROPERTY_WORKER_NAME, configuration.getWorkerName());
//		setProperty(toPropagate, PROPERTY_LISTENING_ADDRESSES,
//				configuration.getListeningAddresses());
//		setProperty(toPropagate, PROPERTY_DEFAULT_AUTHMETHOD,
//				configuration.getDefaultAuthMethod());
//		setProperty(toPropagate, PROPERTY_DEFAULT_REALMNAME,
//				configuration.getDefaultRealmName());
//		setProperty(toPropagate, PROPERTY_SHOW_STACKS,
//				configuration.isShowStacks());
//
//		// then replace ports
//		setProperty(toPropagate, PROPERTY_HTTP_PORT, httpPort);
//		setProperty(toPropagate, PROPERTY_HTTP_SECURE_PORT, httpSecurePort);
//
//		// then add/replace configuration properties for external jetty.xml file
//		setProperty(toPropagate, PROPERTY_SERVER_CONFIGURATION_FILE,
//				configuration.getConfigurationDir());
//
//		setProperty(toPropagate, PROPERTY_SERVER_CONFIGURATION_URL,
//				configuration.getConfigurationURL());
//
//		// Request Log - e.g NCSA log
//		setProperty(toPropagate, PROPERTY_LOG_NCSA_FORMAT,
//				configuration.getLogNCSAFormat());
//		setProperty(toPropagate, PROPERTY_LOG_NCSA_RETAINDAYS,
//				configuration.getLogNCSARetainDays());
//		setProperty(toPropagate, PROPERTY_LOG_NCSA_APPEND,
//				configuration.isLogNCSAAppend());
//		setProperty(toPropagate, PROPERTY_LOG_NCSA_EXTENDED,
//				configuration.isLogNCSAExtended());
//		setProperty(toPropagate, PROPERTY_LOG_NCSA_DISPATCH,
//				configuration.isLogNCSADispatch());
//		setProperty(toPropagate, PROPERTY_LOG_NCSA_DISPATCH,
//				configuration.isLogNCSADispatch());
//		setProperty(toPropagate, PROPERTY_LOG_NCSA_LOGTIMEZONE,
//				configuration.getLogNCSATimeZone());
//		setProperty(toPropagate, PROPERTY_CRL_PATH,
//                            configuration.getCrlPath());
//		setProperty(toPropagate, PROPERTY_ENABLE_CRLDP,
//                            configuration.isEnableCRLDP());
//		setProperty(toPropagate, PROPERTY_VALIDATE_CERTS,
//                            configuration.isValidateCerts());
//		setProperty(toPropagate, PROPERTY_VALIDATE_PEER_CERTS,
//                            configuration.isValidatePeerCerts());
//		setProperty(toPropagate, PROPERTY_ENABLE_OCSP,
//                            configuration.isEnableOCSP());
//		setProperty(toPropagate, PROPERTY_OCSP_RESPONDER_URL,
//                            configuration.getOcspResponderURL());
//		setProperty(toPropagate, PROPERTY_ENC_ENABLED,
//                            configuration.isEncEnabled());
//		setProperty(toPropagate, PROPERTY_ENC_MASTERPASSWORD,
//                            configuration.getEncMasterPassword());
//		setProperty(toPropagate, PROPERTY_ENC_ALGORITHM,
//                            configuration.getEncAlgorithm());
//		setProperty(toPropagate, PROPERTY_ENC_PREFIX,
//                            configuration.getEncPrefix());
//		setProperty(toPropagate, PROPERTY_ENC_SUFFIX,
//                            configuration.getEncSuffix());
//
//		if (SupportUtils.isJSPAvailable()) {
//			setProperty(toPropagate, PROPERTY_JSP_CHECK_INTERVAL,
//					configuration.getJspCheckInterval());
//			setProperty(toPropagate, PROPERTY_JSP_DEBUG_INFO,
//					configuration.getJspClassDebugInfo());
//			setProperty(toPropagate, PROPERTY_JSP_DEVELOPMENT,
//					configuration.getJspDevelopment());
//			setProperty(toPropagate, PROPERTY_JSP_ENABLE_POOLING,
//					configuration.getJspEnablePooling());
//			setProperty(toPropagate, PROPERTY_JSP_IE_CLASS_ID,
//					configuration.getJspIeClassId());
//			setProperty(toPropagate, PROPERTY_JSP_JAVA_ENCODING,
//					configuration.getJspJavaEncoding());
//			setProperty(toPropagate, PROPERTY_JSP_KEEP_GENERATED,
//					configuration.getJspKeepgenerated());
//			setProperty(toPropagate, PROPERTY_JSP_LOG_VERBOSITY_LEVEL,
//					configuration.getJspLogVerbosityLevel());
//			setProperty(toPropagate, PROPERTY_JSP_MAPPED_FILE,
//					configuration.getJspMappedfile());
//			setProperty(toPropagate, PROPERTY_JSP_SCRATCH_DIR,
//					configuration.getJspScratchDir());
//			setProperty(toPropagate, PROPERTY_JSP_TAGPOOL_MAX_SIZE,
//					configuration.getJspTagpoolMaxSize());
//			setProperty(toPropagate, PROPERTY_JSP_PRECOMPILATION,
//					configuration.getJspPrecompilation());
//		}

		return toPropagate;
	}

	private void setProperty(final Hashtable<String, Object> properties,
							 final String name, final Object value) {
		if (value != null) {
			if (value instanceof File) {
				properties.put(name, ((File) value).getAbsolutePath());
			} else if (value instanceof Object[]) {
				properties.put(name, join(",", (Object[]) value));
			} else {
				properties.put(name, value.toString());
			}
		} else {
			properties.remove(name);
		}
	}

	private static String join(String token, Object[] array) {
		if (array == null) {
			return null;
		}
		if (array.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();

		for (int x = 0; x < (array.length - 1); x++) {
			if (array[x] != null) {
				sb.append(array[x].toString());
			} else {
				sb.append("null");
			}
			sb.append(token);
		}
		sb.append(array[array.length - 1]);

		return (sb.toString());
	}

	/**
	 * {@link ServiceTrackerCustomizer} to monitor {@link ServerControllerFactory} services.
	 */
	private class ServerControllerFactoryCustomizer implements ServiceTrackerCustomizer<ServerControllerFactory, ServerControllerFactory> {

		@Override
		public ServerControllerFactory addingService(ServiceReference<ServerControllerFactory> reference) {
			final ServerControllerFactory controllerFactory = bundleContext.getService(reference);
			updateServerControllerFactory(controllerFactory);
			return controllerFactory;
		}

		@Override
		public void modifiedService(ServiceReference<ServerControllerFactory> reference, ServerControllerFactory service) {
			// no need to process service properties change
		}

		@Override
		public void removedService(ServiceReference<ServerControllerFactory> reference, ServerControllerFactory service) {
			if (bundleContext != null) {
				bundleContext.ungetService(reference);
			}
			updateServerControllerFactory(null);
		}
	}

}
