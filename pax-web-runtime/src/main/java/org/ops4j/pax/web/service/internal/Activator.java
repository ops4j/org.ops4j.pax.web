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
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.pax.web.annotations.PaxWebConfiguration;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.security.SecurePropertyResolver;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.JspConfiguration;
import org.ops4j.pax.web.service.spi.config.LogConfiguration;
import org.ops4j.pax.web.service.spi.config.ResourceConfiguration;
import org.ops4j.pax.web.service.spi.config.SecurityConfiguration;
import org.ops4j.pax.web.service.spi.config.ServerConfiguration;
import org.ops4j.pax.web.service.spi.config.SessionConfiguration;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.events.ServerEvent;
import org.ops4j.pax.web.service.spi.model.events.ServerListener;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventListener;
import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
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
 *     <li>registration of {@link WebElementEventListener}-{@link EventAdmin} bridge</li>
 *     <li>registration of {@link WebElementEventListener}-{@link LogService} bridge</li>
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
	 * {@link WebElementEventDispatcher} bound to lifecycle of this pax-web-runtime bundle, not to configuration
	 * or {@link ServerControllerFactory}.
	 */
	private WebElementEventDispatcher webElementEventDispatcher;

//	/** Processor for instructions in {@code org.ops4j.pax.web.context} factory PID */
//	private HttpContextProcessing httpContextProcessing;

	/**
	 * Registration for current {@link org.osgi.framework.ServiceFactory} for {@link HttpService} and
	 * {@link WebContainer}
	 */
	private ServiceRegistration<?> httpServiceFactoryReg;

	/**
	 * <p>Registration for current (1:1 with {@link ServerModel}) {@link HttpServiceRuntime}.
	 * See 140.9 The Http Service Runtime Service. Even if it's defined in Whiteboard (OSGi CMPN R7 140) specification,
	 * the information presented by Pax Web 8 comes from 3 "sources":<ul>
	 *     <li>Whiteboard (pax-web-extender-whiteboard)</li>
	 *     <li>{@link HttpService} (pax-web-runtime)</li>
	 *     <li>WABs (pax-web-extender-war)</li>
	 * </ul></p>
	 */
	private ServiceRegistration<HttpServiceRuntime> httpServiceRuntimeReg;

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

	private ServiceTracker<ServerListener, ServerListener> serverListenerTracker;
	private final List<ServerListener> serverListeners = new CopyOnWriteArrayList<>();

	private ServiceTracker<?, ?> jasyptTracker;
	private AtomicBoolean jasyptTracking = new AtomicBoolean(false);

	private final AtomicBoolean initialConfigSet = new AtomicBoolean(false);

	/**
	 * Global, single instance of {@link ServerModel} recreated together with each (re)registration of
	 * {@link HttpService}.
	 */
	private ServerModel serverModel = null;

	/**
	 * Single thread pool to process all configuration changes, {@link ServerControllerFactory} (re)registrations
	 * and (since Pax Web 8) also actual registrations of web elements.
	 */
	private ScheduledExecutorService runtimeExecutor;
	private long registrationThreadId;

	@Override
	public void start(final BundleContext context) throws Exception {
		LOG.debug("Starting Pax Web Runtime");

		runtimeExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("paxweb-config"));
		registrationThreadId = ServerModel.getThreadIdFromSingleThreadPool(runtimeExecutor);

		bundleContext = context;

		serverListenerTracker = new ServiceTracker<>(bundleContext, ServerListener.class, new ServerListenerCustomizer());
		serverListenerTracker.open();

		if (Utils.isConfigurationAdminAvailable(this.getClass())) {
			// ManagedService for org.ops4j.pax.web PID monitoring, so configuration won't happen yet
			// (for example in FelixStartLevel thread), but only after Configuration Admin notifies us
			registerManagedService(context);
		} else {
			// no org.osgi.service.cm.ConfigurationAdmin available at all, so we can configure immediately
			updateConfiguration(null);
		}

		if (Utils.isEventAdminAvailable(this.getClass())) {
			// Do use the filters this way the eventadmin packages can be resolved optional!
			Filter filterEvent = context.createFilter("(objectClass=org.osgi.service.event.EventAdmin)");
			EventAdminHandler adminHandler = new EventAdminHandler(context);
			eventServiceTracker = new ServiceTracker<>(context, filterEvent, adminHandler);
			eventServiceTracker.open();

			context.registerService(WebApplicationEventListener.class, adminHandler, null);
			LOG.info("EventAdmin support enabled, WAB events will be posted to EventAdmin topics.");
		} else {
			LOG.info("EventAdmin support is not available, no WAB events will be sent.");
		}

		LOG.info("Pax Web Runtime started");
	}

	@Override
	public void stop(final BundleContext context) {
		LOG.debug("Stopping Pax Web Runtime");

		if (serverModel != null) {
			serverModel.setStopping();
		}

		if (serverControllerFactory != null && serverController != null) {
			serverControllerFactory.releaseServerController(serverController, serverController.getConfiguration());
		}

		if (jasyptTracker != null) {
			jasyptTracker.close();
			jasyptTracker = null;
		}
		if (httpServiceRuntimeReg != null) {
			LOG.info("Unregistering current HttpServiceRuntime");
			httpServiceRuntimeReg.unregister();
			httpServiceRuntimeReg = null;
		}
		if (httpServiceFactoryReg != null) {
			LOG.info("Unregistering current HttpService factory");
			httpServiceFactoryReg.unregister();
			httpServiceFactoryReg = null;
		}
		if (serverListenerTracker != null) {
			serverListenerTracker.close();
			serverListenerTracker = null;
		}
		if (serverControllerFactoryTracker != null) {
			serverControllerFactoryTracker.close();
			serverControllerFactoryTracker = null;
		}
		if (servletContextHelperReg != null) {
			servletContextHelperReg.unregister();
			servletContextHelperReg = null;
		}
		if (managedServiceReg != null) {
			managedServiceReg.unregister();
			managedServiceReg = null;
		}
		if (eventServiceTracker != null) {
			eventServiceTracker.close();
			eventServiceTracker = null;
		}
		if (webElementEventDispatcher != null) {
			webElementEventDispatcher.destroy();
			webElementEventDispatcher = null;
		}
//		if (httpContextProcessing != null) {
//			httpContextProcessing.destroy();
//		}

		// Wait up to 20 seconds, otherwhise
		try {
			runtimeExecutor.shutdown();
			LOG.debug("...entering 20 seconds grace period...");
			boolean ok = runtimeExecutor.awaitTermination(20, TimeUnit.SECONDS);
			if (!ok) {
				LOG.warn("Timeout awaiting termination, shutting down the executor.");
			}
			runtimeExecutor.shutdownNow();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			// Ignore, we are done anyways...
		}

		LOG.info("Pax Web Runtime stopped");
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
		runtimeExecutor.submit(() -> {
			String name = Thread.currentThread().getName();
			try {
				Thread.currentThread().setName(name + " (change config)");
				updateController(configuration, serverControllerFactory);
			} finally {
				Thread.currentThread().setName(name);
			}
		});
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
		Future<?> future = runtimeExecutor.submit(() -> {
			String name = Thread.currentThread().getName();
			try {
				Thread.currentThread().setName(name + " (change controller)");
				updateController(configuration, controllerFactory);
			} finally {
				Thread.currentThread().setName(name);
			}
		});

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

		if (serverModel != null) {
			serverModel.setStopping();
			serverModel = null;
		}

		this.configuration = dictionary;
		this.serverControllerFactory = controllerFactory;

		if (jasyptTracker != null) {
			jasyptTracker.close();
			jasyptTracker = null;
		}
		if (httpServiceRuntimeReg != null) {
			LOG.info("Unregistering current HttpServiceRuntime");
			httpServiceRuntimeReg.unregister();
			httpServiceRuntimeReg = null;
		}
		if (httpServiceFactoryReg != null) {
			LOG.info("Unregistering current HttpService factory");
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

			// before creating a configuration, we have to check if the encryption is enabled - and there are two
			// ways to implement the decryption
			String enabled = allProperties.get(PaxWebConfig.PID_CFG_ENC_ENABLED);
			if ("true".equalsIgnoreCase(enabled)) {
				if (!Utils.isJasyptAvailable(this.getClass())) {
					LOG.warn("Encryption is enabled, but Jasypt bundle is not available. Decryption of configuration values won't be performed.");
				} else {
					String decryptor = allProperties.get(PaxWebConfig.PID_CFG_ENC_OSGI_DECRYPTOR);
					if (decryptor != null && !"".equals(decryptor)) {
						// 1. We can obtain an OSGi service of org.jasypt.encryption.StringEncryptor
						LOG.info("Encryption is enabled and Jasypt encryptor with ID \"{}\" will be looked up in OSGi registry",
								decryptor);
						String filter = String.format("(&(%s=%s)(decryptor=%s))",
								Constants.OBJECTCLASS, "org.jasypt.encryption.StringEncryptor", decryptor);

						synchronized (JasyptCustomizer.class) {
							if (jasyptTracker != null) {
								jasyptTracker.close();
								jasyptTracker = null;
							}
							jasyptTracking.set(true);
							try {
								jasyptTracker = new ServiceTracker<>(bundleContext, bundleContext.createFilter(filter), new JasyptCustomizer());
								jasyptTracker.open();
								Object encryptor = jasyptTracker.getService();
								if (encryptor != null) {
									resolver = SecurePropertyResolver.wrap(resolver, encryptor);
								} else {
									LOG.info("Jasypt encryptor with ID \"{}\" is not found in OSGi registry." +
											" Pax Web configuration will be performed after it becomes available.", decryptor);
									return;
								}
							} finally {
								jasyptTracking.set(false);
							}
						}
					} else {
						// 2. We can configure our own org.jasypt.encryption.StringEncryptor
						LOG.info("Encryption is enabled and pax-web-runtime will configure Jasypt encryptor");
						boolean foundPassword = false;
						String env = allProperties.get(PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD_ENV);
						if (env != null && !"".equals(env)) {
							LOG.debug("Environment variable \"{}\" will be used to obtain the master password", env);
							foundPassword = true;
						}
						String sys = allProperties.get(PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD_SYS);
						if (sys != null && !"".equals(sys)) {
							LOG.debug("System property \"{}\" will be used to obtain the master password", sys);
							foundPassword = true;
						}
						String password = allProperties.get(PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD);
						if (password != null && !"".equals(password)) {
//							LOG.debug("Master password was specified in the configuration");
							foundPassword = true;
						}

						if (!foundPassword) {
							LOG.warn("No master password was provided. Decryption of configuration values won't be performed.");
						} else {
							resolver = SecurePropertyResolver.wrap(resolver);
						}
					}
				}
			}

			// full configuration with all required properties. That's all that is needed down the stream
			final Configuration configuration = ConfigurationBuilder.getConfiguration(resolver, allProperties);

			webElementEventDispatcher = new WebElementEventDispatcher(bundleContext, configuration);

			// global, single representation of web server state. It's used
			//  - in all bundle-scoped instances of HttpServiceEnabled
			//  - also to reflect Whiteboard registrations (through pax-web-extender-whiteboard)
			serverModel = new ServerModel(runtimeExecutor, registrationThreadId);

			// create a controller object to operate on any supported web server
			serverController = serverControllerFactory.createServerController(configuration);
			// immediately add current ServerListeners.
			serverListeners.forEach(listener -> serverController.addListener(listener));

			// first step is to configure the server without actually starting it
			LOG.info("Configuring server controller {}", serverController.getClass().getName());
			serverController.configure();

			LOG.info("Starting server controller {}", serverController.getClass().getName());
			serverController.start();

			// this is where org.osgi.service.http.HttpService bundle-scoped service is registered in OSGi
			// this is the most fundamental operation related to Http Service specification
			Dictionary<String, Object> props = determineServiceProperties(configuration);
			ServiceFactory<StoppableHttpService> factory = new StoppableHttpServiceFactory(serverController, serverModel,
					webElementEventDispatcher) {
				@Override
				StoppableHttpService createService(Bundle bundle, ServerController serverController, ServerModel serverModel, WebElementEventDispatcher webElementEventDispatcher) {
					HttpServiceEnabled enabledService =
							new HttpServiceEnabled(bundle, serverController, serverModel,
									webElementEventDispatcher, configuration);

					return new HttpServiceProxy(bundle, enabledService);
				}
			};

			// this registration is performed inside configuration thread. It may invoke service listeners
			// awaiting HttpService/WebContainer to start registering web elements, which call configuration
			// thread again - this time without waiting (same thread)
			// but this caused a problem in pax-web-extender-whiteboard which has it's own lock.
			// that's why pax-web-extender-whiteboard should not get the lock after its service listener is called
			LOG.info("Registering HttpService factory");
			httpServiceFactoryReg = bundleContext.registerService(HTTPSERVICE_REGISTRATION_NAMES, factory, props);

			LOG.info("Registering HttpServiceRuntime");
			// see Table 140.9 Service properties for the HttpServiceRuntime service
			props = new Hashtable<>();
			// we'll set this propery later through ServerListener
			props.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, "/");
			Long httpServiceId = (Long) httpServiceFactoryReg.getReference().getProperty(Constants.SERVICE_ID);
			props.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID, Collections.singletonList(httpServiceId));
			// SERVICE_CHANGECOUNT is 1.9 OSGi Core addition, so use literal please
//			props.put(Constants.SERVICE_CHANGECOUNT, 0L);
			props.put("service.changecount", 0L);
			httpServiceRuntimeReg = bundleContext.registerService(HttpServiceRuntime.class, serverModel, props);

			// "template" ServiceReferenceDTO for HttpServiceRuntime, however it has to be updated:
			// - when "service.changecount" increases
			// - when target runtime is started/stopped to update "osgi.http.endpoint"
			// However Figure 140.3 Runtime DTO Overview Diagram doesn mention this field in RuntimeDTO at all...
			ServiceReferenceDTO httpServiceRuntimeDTO = new ServiceReferenceDTO();
			httpServiceRuntimeDTO.id = (long) httpServiceRuntimeReg.getReference().getProperty(Constants.SERVICE_ID);
			httpServiceRuntimeDTO.bundle = bundleContext.getBundle().getBundleId();
			httpServiceRuntimeDTO.properties = new HashMap<>();
			httpServiceRuntimeDTO.properties.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, "/");
			httpServiceRuntimeDTO.properties.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID, Collections.singletonList(httpServiceId));
			httpServiceRuntimeDTO.properties.put("service.changecount", 0L);
			// initially "usingBundles" is empty and we'll be setting it on every
			httpServiceRuntimeDTO.usingBundles = new long[0];
			// we'll set the template into ServerModel, so it's available from there, when creating full RuntimeDTO
			serverModel.setHttpServiceRuntimeInformation(httpServiceRuntimeReg, httpServiceRuntimeDTO);

			// added listener is immediately called with the current state
			serverController.addListener(new AddressConfiguration());

			// ManagedServiceFactory for org.ops4j.pax.web.context factory PID
			// we need registered WebContainer for this MSF to work
//			createManagedServiceFactory(bundleContext);
		} catch (Throwable t) {
			try {
				Bundle bundle = bundleContext.getBundle();
				if (bundle.getState() == Bundle.STOPPING || bundle.getState() == Bundle.UNINSTALLED) {
					return;
				}
				LOG.error("Unable to start Pax Web server: {}", t.getMessage(), t);
			} catch (IllegalStateException ignored) {
			}
		}
	}

	/**
	 * Pass properties used to configure {@link HttpService} to service registration.
	 *
	 * @param configuration
	 * @return
	 *
	 * @since 0.6.0, PAXWEB-127
	 */
	private Dictionary<String, Object> determineServiceProperties(final Configuration configuration) {
		final Hashtable<String, Object> properties = new Hashtable<>();

		// configuration already collects the properties from env/system/context properties and also from
		// metatype config and configadmin

		ServerConfiguration sc = configuration.server();
		setProperty(properties, PaxWebConfig.PID_CFG_HTTP_ENABLED, sc.isHttpEnabled());
		setProperty(properties, PaxWebConfig.PID_CFG_HTTP_PORT, sc.getHttpPort());
		setProperty(properties, PaxWebConfig.PID_CFG_HTTP_SECURE_ENABLED, sc.isHttpSecureEnabled());
		setProperty(properties, PaxWebConfig.PID_CFG_HTTP_PORT_SECURE, sc.getHttpSecurePort());
		// only relevant for Jetty
		setProperty(properties, PaxWebConfig.PID_CFG_HTTP_CONNECTOR_NAME, sc.getHttpConnectorName());
		// only relevant for Jetty
		setProperty(properties, PaxWebConfig.PID_CFG_HTTP_SECURE_CONNECTOR_NAME, sc.getHttpSecureConnectorName());
		setProperty(properties, PaxWebConfig.PID_CFG_LISTENING_ADDRESSES, sc.getListeningAddresses());
		setProperty(properties, PaxWebConfig.PID_CFG_CONNECTOR_IDLE_TIMEOUT, sc.getConnectorIdleTimeout());
		setProperty(properties, PaxWebConfig.PID_CFG_SERVER_IDLE_TIMEOUT, sc.getServerIdleTimeout());
		setProperty(properties, PaxWebConfig.PID_CFG_SERVER_MAX_THREADS, sc.getServerMaxThreads());
		setProperty(properties, PaxWebConfig.PID_CFG_SERVER_MIN_THREADS, sc.getServerMinThreads());
		setProperty(properties, PaxWebConfig.PID_CFG_SERVER_THREAD_NAME_PREFIX, sc.getServerThreadNamePrefix());
		setProperty(properties, PaxWebConfig.PID_CFG_SHOW_STACKS, sc.isShowStacks());
		setProperty(properties, PaxWebConfig.PID_CFG_EVENT_DISPATCHER_THREAD_COUNT, sc.getEventDispatcherThreadCount());
		setProperty(properties, PaxWebConfig.PID_CFG_HTTP_CHECK_FORWARDED_HEADERS, sc.checkForwardedHeaders());
		setProperty(properties, PaxWebConfig.PID_CFG_TEMP_DIR, sc.getTemporaryDirectory());
		setProperty(properties, PaxWebConfig.PID_CFG_HTTP_CHECK_FORWARDED_HEADERS, sc.checkForwardedHeaders());
		setProperty(properties, PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES, sc.getConfigurationFiles());

		LogConfiguration lc = configuration.logging();
		setProperty(properties, PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED, lc.isLogNCSAFormatEnabled());
		setProperty(properties, PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR, lc.getLogNCSADirectory());
		setProperty(properties, PaxWebConfig.PID_CFG_LOG_NCSA_LOGFILE, lc.getLogNCSAFile());
		setProperty(properties, PaxWebConfig.PID_CFG_LOG_NCSA_APPEND, lc.isLogNCSAAppend());
		setProperty(properties, PaxWebConfig.PID_CFG_LOG_NCSA_LOGFILE_DATE_FORMAT, lc.getLogNCSAFilenameDateFormat());
		setProperty(properties, PaxWebConfig.PID_CFG_LOG_NCSA_RETAINDAYS, lc.getLogNCSARetainDays());
		setProperty(properties, PaxWebConfig.PID_CFG_LOG_NCSA_EXTENDED, lc.isLogNCSAExtended());
		setProperty(properties, PaxWebConfig.PID_CFG_LOG_NCSA_LOGTIMEZONE, lc.getLogNCSATimeZone());
		setProperty(properties, PaxWebConfig.PID_CFG_LOG_NCSA_BUFFERED, lc.getLogNCSABuffered());

		SessionConfiguration sess = configuration.session();
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_TIMEOUT, sess.getSessionTimeout());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_COOKIE_NAME, sess.getSessionCookieName());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_COOKIE_DOMAIN, sess.getSessionCookieDomain());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_COOKIE_PATH, sess.getSessionCookiePath());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_COOKIE_COMMENT, sess.getSessionCookieComment());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_COOKIE_HTTP_ONLY, sess.getSessionCookieHttpOnly());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_COOKIE_SECURE, sess.getSessionCookieSecure());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_COOKIE_MAX_AGE, sess.getSessionCookieMaxAge());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_URL, sess.getSessionUrlPathParameter());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_WORKER_NAME, sess.getSessionWorkerName());
		setProperty(properties, PaxWebConfig.PID_CFG_SESSION_STORE_DIRECTORY, sess.getSessionStoreDirectoryLocation());

		ResourceConfiguration res = configuration.resources();
//		setProperty(toPropagate, PaxWebConfig.PID_CFG_DEFAULT_SERVLET_ACCEPT_RANGES, res.acceptRanges());
//		setProperty(toPropagate, PaxWebConfig.PID_CFG_DEFAULT_SERVLET_REDIRECT_WELCOME, res.redirectWelcome());
//		setProperty(toPropagate, PaxWebConfig.PID_CFG_DEFAULT_SERVLET_DIR_LISTING, res.dirListing());
//		setProperty(toPropagate, PaxWebConfig.PID_CFG_DEFAULT_SERVLET_CACHE_MAX_ENTRIES, res.maxCacheEntries());
//		setProperty(toPropagate, PaxWebConfig.PID_CFG_DEFAULT_SERVLET_CACHE_MAX_ENTRY_SIZE, res.maxCacheEntrySize());
//		setProperty(toPropagate, PaxWebConfig.PID_CFG_DEFAULT_SERVLET_CACHE_MAX_ENTRIES, res.maxTotalCacheSize());
//		setProperty(toPropagate, PaxWebConfig.PID_CFG_DEFAULT_SERVLET_CACHE_TTL, res.maxCacheTTL());

		JspConfiguration jsp = configuration.jsp();
		setProperty(properties, PaxWebConfig.PID_CFG_JSP_SCRATCH_DIR, jsp.getGloablJspScratchDir());

		SecurityConfiguration sec = configuration.security();
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_PROVIDER, sec.getSslProvider());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_KEYSTORE, sec.getSslKeystore());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_KEYSTORE_PASSWORD, "********"/*sec.getSslKeystorePassword()*/);
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_KEY_PASSWORD, "********"/*sec.getSslKeyPassword()*/);
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_KEYSTORE_TYPE, sec.getSslKeystoreType());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_KEYSTORE_PROVIDER, sec.getSslKeystoreProvider());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_KEY_MANAGER_FACTORY_ALGORITHM, sec.getSslKeyManagerFactoryAlgorithm());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_KEY_ALIAS, sec.getSslKeyAlias());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_TRUSTSTORE, sec.getTruststore());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PASSWORD, "********"/*sec.getTruststorePassword()*/);
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_TYPE, sec.getTruststoreType());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PROVIDER, sec.getTruststoreProvider());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_TRUST_MANAGER_FACTORY_ALGORITHM, sec.getTrustManagerFactoryAlgorithm());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_CLIENT_AUTH_WANTED, sec.isClientAuthWanted());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_CLIENT_AUTH_NEEDED, sec.isClientAuthNeeded());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_PROTOCOL, sec.getSslProtocol());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_SECURE_RANDOM_ALGORITHM, sec.getSecureRandomAlgorithm());
		setProperty(properties, PaxWebConfig.PID_CFG_PROTOCOLS_INCLUDED, sec.getProtocolsIncluded());
		setProperty(properties, PaxWebConfig.PID_CFG_PROTOCOLS_EXCLUDED, sec.getProtocolsExcluded());
		setProperty(properties, PaxWebConfig.PID_CFG_CIPHERSUITES_INCLUDED, sec.getCiphersuiteIncluded());
		setProperty(properties, PaxWebConfig.PID_CFG_CIPHERSUITES_EXCLUDED, sec.getCiphersuiteExcluded());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_RENEGOTIATION_ALLOWED, sec.isSslRenegotiationAllowed());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_RENEGOTIATION_LIMIT, sec.getSslRenegotiationLimit());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_SESSION_ENABLED, sec.getSslSessionsEnabled());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_SESSION_CACHE_SIZE, sec.getSslSessionCacheSize());
		setProperty(properties, PaxWebConfig.PID_CFG_SSL_SESSION_TIMEOUT, sec.getSslSessionTimeout());
		setProperty(properties, PaxWebConfig.PID_CFG_VALIDATE_CERTS, sec.isValidateCerts());
		setProperty(properties, PaxWebConfig.PID_CFG_VALIDATE_PEER_CERTS, sec.isValidatePeerCerts());
		setProperty(properties, PaxWebConfig.PID_CFG_ENABLE_OCSP, sec.isEnableOCSP());
		setProperty(properties, PaxWebConfig.PID_CFG_ENABLE_CRLDP, sec.isEnableCRLDP());
		setProperty(properties, PaxWebConfig.PID_CFG_CRL_PATH, sec.getCrlPath());
		setProperty(properties, PaxWebConfig.PID_CFG_OCSP_RESPONDER_URL, sec.getOcspResponderURL());
		setProperty(properties, PaxWebConfig.PID_CFG_MAX_CERT_PATH_LENGTH, sec.getMaxCertPathLength());
		setProperty(properties, PaxWebConfig.PID_CFG_DIGESTAUTH_MAX_NONCE_AGE, sec.getDigestAuthMaxNonceAge());
		setProperty(properties, PaxWebConfig.PID_CFG_DIGESTAUTH_MAX_NONCE_COUNT, sec.getDigestAuthMaxNonceCount());
		setProperty(properties, PaxWebConfig.PID_CFG_FORMAUTH_REDIRECT, sec.getFormAuthRedirect());

		setProperty(properties, PaxWebConfig.PID_CFG_ENC_ENABLED, sec.isEncEnabled());
		setProperty(properties, PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD, "********"/*sec.getEncMasterPassword()*/);
		setProperty(properties, PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD_ENV, sec.getEncMasterPasswordEnvVariable());
		setProperty(properties, PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD_SYS, sec.getEncMasterPasswordSystemProperty());
		setProperty(properties, PaxWebConfig.PID_CFG_ENC_PROVIDER, sec.getEncProvider());
		setProperty(properties, PaxWebConfig.PID_CFG_ENC_ALGORITHM, sec.getEncAlgorithm());
		setProperty(properties, PaxWebConfig.PID_CFG_ENC_ITERATION_COUNT, sec.getEncIterationCount());
		setProperty(properties, PaxWebConfig.PID_CFG_ENC_PREFIX, sec.getEncPrefix());
		setProperty(properties, PaxWebConfig.PID_CFG_ENC_SUFFIX, sec.getEncSuffix());
		setProperty(properties, PaxWebConfig.PID_CFG_ENC_OSGI_DECRYPTOR, sec.getEncOSGiDecryptorId());

		return properties;
	}

	private void setProperty(final Hashtable<String, Object> properties, final String name, final Object value) {
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
				if (array[x] instanceof File) {
					sb.append(((File) array[x]).getAbsolutePath());
				} else {
					sb.append(array[x].toString());
				}
			} else {
				sb.append("null");
			}
			sb.append(token);
		}
		sb.append(array[array.length - 1]);

		return sb.toString();
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

	private class ServerListenerCustomizer implements ServiceTrackerCustomizer<ServerListener, ServerListener> {

		@Override
		public ServerListener addingService(ServiceReference<ServerListener> reference) {
			ServerListener service = bundleContext.getService(reference);
			ServerController sc = serverController;
			if (sc != null) {
				serverListeners.add(service);
				sc.addListener(service);
			}
			return service;
		}

		@Override
		public void modifiedService(ServiceReference<ServerListener> reference, ServerListener service) {
		}

		@Override
		public void removedService(ServiceReference<ServerListener> reference, ServerListener service) {
			ServerController sc = serverController;
			if (sc != null) {
				sc.removeListener(service);
				serverListeners.remove(service);
			}
			if (bundleContext != null) {
				bundleContext.ungetService(reference);
			}
		}
	}

	/**
	 * Customizer that simply reconfigures the runtime when Jasypt encryptor becomes available
	 */
	private class JasyptCustomizer implements ServiceTrackerCustomizer<Object, Object> {
		@Override
		public Object addingService(ServiceReference<Object> reference) {
			synchronized (JasyptCustomizer.class) {
				if (!jasyptTracking.get() && serverControllerFactory != null) {
					performConfiguration();
				}
			}
			return bundleContext.getService(reference);
		}

		@Override
		public void modifiedService(ServiceReference<Object> reference, Object service) {
		}

		@Override
		public void removedService(ServiceReference<Object> reference, Object service) {
			if (!jasyptTracking.get() && serverControllerFactory != null) {
				performConfiguration();
			}
			bundleContext.ungetService(reference);
		}
	}

	/**
	 * This listener alters the registration of {@link HttpServiceRuntime} properties
	 */
	private class AddressConfiguration implements ServerListener {
		@Override
		public void stateChanged(ServerEvent event) {
			if (httpServiceRuntimeReg == null || httpServiceRuntimeReg.getReference() == null) {
				return;
			}
			String[] props = httpServiceRuntimeReg.getReference().getPropertyKeys();
			Dictionary<String, Object> newProps = new Hashtable<>();
			for (String key : props) {
				newProps.put(key, httpServiceRuntimeReg.getReference().getProperty(key));
			}
			if (event.getState() == ServerEvent.State.STARTED) {
				String[] addresses = Arrays.stream(event.getAddresses())
						.map(ia -> String.format("%s://%s:%d/", ia.isSecure() ? "https" : "http",
								ia.getAddress().getAddress().getHostAddress(), ia.getAddress().getPort())).toArray(String[]::new);
				newProps.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, addresses);
				serverModel.getHttpServiceRuntimeDTO().properties.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, addresses);
			} else {
				newProps.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, "/");
				serverModel.getHttpServiceRuntimeDTO().properties.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, "/");
			}
			// update the registration properties
			httpServiceRuntimeReg.setProperties(newProps);
		}
	}

}
