/* Copyright 2007 Niclas Hedhman.
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2011 Achim Nierbeck.
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
package org.ops4j.pax.web.service.internal;

import static org.ops4j.pax.web.service.WebContainerConstants.PID;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_ENABLED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_PORT;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_SECURE_ENABLED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_SECURE_PORT;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_USE_NIO;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LISTENING_ADDRESSES;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_APPEND;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_EXTENDED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_FORMAT;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_LOGTIMEZONE;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_RETAINDAYS;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SERVER_CONFIGURATION_FILE;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SESSION_COOKIE;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SESSION_TIMEOUT;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SESSION_URL;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SSL_CLIENT_AUTH_NEEDED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SSL_CLIENT_AUTH_WANTED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SSL_KEYPASSWORD;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SSL_KEYSTORE;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SSL_KEYSTORE_TYPE;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SSL_PASSWORD;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_TEMP_DIR;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_WORKER_NAME;

import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.util.JspSupportUtils;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private final Lock m_lock;
	private ServerController m_serverController;
	private boolean m_serverControllerDefaultConfigured;
	private ServerModel m_serverModel;
	private ServiceRegistration m_httpServiceFactoryReg;
	private Dictionary<String, Object> m_httpServiceFactoryProps;

	private BundleContext bundleContext;

	private ScheduledExecutorService executors;

	private ServletEventDispatcher servletEventDispatcher;

	private ServiceTracker eventServiceTracker;

	private ServiceTracker logServiceTracker;

	public Activator() {
		m_lock = new ReentrantLock();
	}

	public void start(final BundleContext bundleContext) throws Exception {
		LOG.debug("Starting Pax Web");
		this.bundleContext = bundleContext;
		m_serverModel = new ServerModel();
		
		executors = Executors.newScheduledThreadPool(3, new ThreadFactory() {

			private final AtomicInteger count = new AtomicInteger();
			
			public Thread newThread(Runnable r) {
				final Thread t = Executors.defaultThreadFactory().newThread(r);
		        t.setName("WebListenerExecutor" + ": " + count.incrementAndGet());
		        t.setDaemon(true);
		        return t;
			}
		});
		
		servletEventDispatcher = new ServletEventDispatcher(bundleContext, executors);
        
		//Do use the filters this way the eventadmin packages can be resolved optional!
		Filter filterEvent = bundleContext.createFilter("(objectClass=org.osgi.service.event.EventAdmin)");
		eventServiceTracker = new ServiceTracker(bundleContext, filterEvent, new EventServiceCustomizer());
		eventServiceTracker.open();
		
		Filter filterLog = bundleContext.createFilter("(objectClass=org.osgi.service.log.LogService)");
		logServiceTracker = new ServiceTracker(bundleContext, filterLog, new LogServiceCustomizer());
		logServiceTracker.open();
		
		// For dynamics the DynamicsServiceTrackerCustomizer is used
		ServiceTracker st = new ServiceTracker(bundleContext,
				ServerControllerFactory.class.getName(),
				new DynamicsServiceTrackerCustomizer());
		st.open();
		
		LOG.info("Pax Web started");
	}

	public void stop(final BundleContext bundleContext) throws Exception {
		LOG.debug("Stopping Pax Web");
		if (m_serverController != null) {
			m_serverController.stop();
			m_serverController = null;
		}
		m_serverControllerDefaultConfigured = false;
		m_serverModel = null;
		LOG.info("Pax Web stopped");
	}
	
	private void createHttpServiceFactory(final BundleContext bundleContext) {
		LOG.debug("createHttpServiceFactory");
		m_httpServiceFactoryReg = bundleContext.registerService(new String[] {
				HttpService.class.getName(), WebContainer.class.getName() },
				new HttpServiceFactoryImpl() {
					HttpService createService(final Bundle bundle) {
						LOG.debug("creating service for Bundle {}", bundle.getSymbolicName());
						return new HttpServiceProxy(new HttpServiceStarted(
								bundle, m_serverController, m_serverModel, servletEventDispatcher));
					}
				}, m_httpServiceFactoryProps);
	}

	/**
	 * Registers a managed service to listen on configuration updates.
	 * 
	 * @param bundleContext
	 *            bundle context to use for registration
	 */
	private void createManagedService(final BundleContext bundleContext) {
		final ManagedService managedService = new ManagedService() {
			
			/**
			 * Sets the resolver on sever controller.
			 * 
			 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
			 */
			public void updated(final Dictionary config)
					throws ConfigurationException {
				try {
					m_lock.lock();
					boolean aboutToDefaultConfigure;
					final PropertyResolver resolver;
					if (config == null) {
						if (m_serverControllerDefaultConfigured) {
							// do not re-configure defaults
							return;
						}
						resolver = new BundleContextPropertyResolver(
								bundleContext, new DefaultPropertyResolver());
						aboutToDefaultConfigure = true;
					} else {
						resolver = new DictionaryPropertyResolver(config,
								new BundleContextPropertyResolver(
										bundleContext,
										new DefaultPropertyResolver()));
						aboutToDefaultConfigure = false;
					}
					final ConfigurationImpl configuration = new ConfigurationImpl(
							resolver);
					m_serverController.configure(configuration);
					determineServiceProperties(config, configuration,
							m_serverController.getHttpPort(),
							m_serverController.getHttpSecurePort());
					if (m_httpServiceFactoryReg != null) {
						m_httpServiceFactoryReg
								.setProperties(m_httpServiceFactoryProps);
					}
					m_serverControllerDefaultConfigured = aboutToDefaultConfigure;
				} finally {
					m_lock.unlock();
				}
			}

		};
		final Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(Constants.SERVICE_PID, PID);
		bundleContext.registerService(ManagedService.class.getName(),
				managedService, props);
		try {
			m_lock.lock();
			if (!m_serverController.isConfigured()) {
				try {
					managedService.updated(null);
				} catch (ConfigurationException ignore) {
					// this should never happen
					LOG.error(
							"Internal error. Cannot set initial configuration resolver.",
							ignore);
				}
			}
		} finally {
			m_lock.unlock();
		}
	}

	private void determineServiceProperties(final Dictionary managedConfig,
			final Configuration config, final Integer httpPort,
			final Integer httpSecurePort) {

		final Hashtable<String, Object> toPropagate = new Hashtable<String, Object>();
		// first store all configuration properties as received via managed
		// service
		if (managedConfig != null && !managedConfig.isEmpty()) {
			final Enumeration enumeration = managedConfig.keys();
			while (enumeration.hasMoreElements()) {
				String key = (String) enumeration.nextElement();
				toPropagate.put(key, managedConfig.get(key));
			}
		}
		
		// then add/replace configuration properties
		setProperty(toPropagate, PROPERTY_HTTP_ENABLED, config.isHttpEnabled());
		setProperty(toPropagate, PROPERTY_HTTP_PORT, config.getHttpPort());
		setProperty(toPropagate, PROPERTY_HTTP_SECURE_ENABLED,
				config.isHttpEnabled());
		setProperty(toPropagate, PROPERTY_HTTP_SECURE_PORT,
				config.getHttpSecurePort());
		setProperty(toPropagate, PROPERTY_HTTP_USE_NIO, config.useNIO());
		setProperty(toPropagate, PROPERTY_SSL_CLIENT_AUTH_NEEDED,
				config.isClientAuthNeeded());
		setProperty(toPropagate, PROPERTY_SSL_CLIENT_AUTH_WANTED,
				config.isClientAuthWanted());
		setProperty(toPropagate, PROPERTY_SSL_KEYSTORE, config.getSslKeystore());
		setProperty(toPropagate, PROPERTY_SSL_KEYSTORE_TYPE,
				config.getSslKeystoreType());
		setProperty(toPropagate, PROPERTY_SSL_PASSWORD, config.getSslPassword());
		setProperty(toPropagate, PROPERTY_SSL_KEYPASSWORD,
				config.getSslKeyPassword());
		setProperty(toPropagate, PROPERTY_TEMP_DIR,
				config.getTemporaryDirectory());
		setProperty(toPropagate, PROPERTY_SESSION_TIMEOUT,
				config.getSessionTimeout());
		setProperty(toPropagate, PROPERTY_SESSION_URL, config.getSessionUrl());
		setProperty(toPropagate, PROPERTY_SESSION_COOKIE,
				config.getSessionCookie());
		setProperty(toPropagate, PROPERTY_WORKER_NAME, config.getWorkerName());
		setProperty(toPropagate, PROPERTY_LISTENING_ADDRESSES,
				config.getListeningAddresses());

		// then replace ports
		setProperty(toPropagate, PROPERTY_HTTP_PORT, httpPort);
		setProperty(toPropagate, PROPERTY_HTTP_SECURE_PORT, httpSecurePort);

		// then add/replace configuration properties for external jetty.xml file
		setProperty(toPropagate, PROPERTY_SERVER_CONFIGURATION_FILE,
				config.getConfigurationDir());

        // Request Log - e.g NCSA log
        setProperty(toPropagate, PROPERTY_LOG_NCSA_FORMAT, config.getLogNCSAFormat());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_RETAINDAYS, config.getLogNCSARetainDays());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_APPEND, config.isLogNCSAAppend());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_EXTENDED, config.isLogNCSAExtended());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_LOGTIMEZONE, config.getLogNCSATimeZone());

		if (JspSupportUtils.jspSupportAvailable()) {
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_CHECK_INTERVAL,
					config.getJspCheckInterval());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_DEBUG_INFO,
					config.getJspClassDebugInfo());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_DEVELOPMENT,
					config.getJspDevelopment());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_ENABLE_POOLING,
					config.getJspEnablePooling());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_IE_CLASS_ID,
					config.getJspIeClassId());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_JAVA_ENCODING,
					config.getJspJavaEncoding());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_KEEP_GENERATED,
					config.getJspKeepgenerated());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_LOG_VERBOSITY_LEVEL,
					config.getJspLogVerbosityLevel());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_MAPPED_FILE,
					config.getJspMappedfile());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_SCRATCH_DIR,
					config.getJspScratchDir());
			setProperty(
					toPropagate,
					org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_TAGPOOL_MAX_SIZE,
					config.getJspTagpoolMaxSize());
		}

		m_httpServiceFactoryProps = toPropagate;
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
		StringBuffer sb = new StringBuffer();

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

	private class DynamicsServiceTrackerCustomizer implements
			ServiceTrackerCustomizer {

		public Object addingService(ServiceReference reference) {
			LOG.debug("Adding service for ServiceReference: {}",reference);
			ServerControllerFactory factory = (ServerControllerFactory) bundleContext
					.getService(reference);
			m_serverController = factory.createServerController(m_serverModel);
			m_serverControllerDefaultConfigured = false;
			createManagedService(bundleContext);
			createHttpServiceFactory(bundleContext);
			return factory;
		}

		public void modifiedService(ServiceReference reference, Object service) {
			// stoping server
			if (m_serverController != null)
				m_serverController.stop();
			m_serverController = null;
			m_serverControllerDefaultConfigured = false;

			if (m_httpServiceFactoryReg != null)
				m_httpServiceFactoryReg.unregister();
			m_httpServiceFactoryReg = null;

			ServerControllerFactory factory = (ServerControllerFactory) bundleContext
					.getService(reference);
			m_serverController = factory.createServerController(m_serverModel);

			createManagedService(bundleContext);
			createHttpServiceFactory(bundleContext);

		}

		public void removedService(ServiceReference reference, Object service) {
			// stoping server
			if (m_serverController != null)
				m_serverController.stop();
			m_serverController = null;
			m_serverControllerDefaultConfigured = false;

			if (m_httpServiceFactoryReg != null)
				m_httpServiceFactoryReg.unregister();
			m_httpServiceFactoryReg = null;
			if (bundleContext != null)
				bundleContext.ungetService(reference);
		}

	}
	
    private class LogServiceCustomizer implements ServiceTrackerCustomizer {

    	public Object addingService(ServiceReference reference) {
    		Object logService = bundleContext.getService(reference);
    		if (logService instanceof LogService)
    			servletEventDispatcher.setLogService(logService);
    		return logService;
    	}
    	
		public void modifiedService(ServiceReference reference, Object service) {
		}

		public void removedService(ServiceReference reference, Object service) {
			if(servletEventDispatcher != null)
				servletEventDispatcher.setLogService(null);
			bundleContext.ungetService(reference);
		}

	}
    
    private class EventServiceCustomizer implements ServiceTrackerCustomizer {

		public Object addingService(ServiceReference reference) {
			Object eventService = bundleContext.getService(reference);
			if (eventService instanceof EventAdmin)
				servletEventDispatcher.setEventAdminService(eventService);
			return eventService;
		}

		public void modifiedService(ServiceReference reference, Object service) {
		}

		public void removedService(ServiceReference reference, Object service) {
			if (servletEventDispatcher != null)
				servletEventDispatcher.setEventAdminService(null);
			bundleContext.ungetService(reference);
		}
    	
    }
}
