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


import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.util.ConfigAdminSupportUtils;
import org.ops4j.pax.web.service.internal.util.JspSupportUtils;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.ops4j.pax.web.service.WebContainerConstants.*;
import static org.ops4j.pax.web.jsp.JspWebdefaults.*;


public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private ServerController m_serverController;
    private ServiceRegistration m_httpServiceFactoryReg;

    private BundleContext bundleContext;

    private ScheduledExecutorService executors;

    private ServletEventDispatcher servletEventDispatcher;

    private ServiceTracker eventServiceTracker;

    private ServiceTracker logServiceTracker;

    private ServiceTracker dynamicsServiceTracker;

    private ServiceRegistration managedServiceReq;

    private ExecutorService configExecutor;

    private Dictionary config;

    private ServerControllerFactory factory;

    public Activator() {
    }

    public void start(final BundleContext bundleContext) throws Exception {
        LOG.debug("Starting Pax Web");
        this.bundleContext = bundleContext;

        configExecutor = Executors.newSingleThreadExecutor();
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

        if (ConfigAdminSupportUtils.configAdminSupportAvailable()) {
            createManagedService(bundleContext);
        } else {
            scheduleUpdate(null, null);
        }

        dynamicsServiceTracker = new ServiceTracker(bundleContext,
                ServerControllerFactory.class.getName(),
                new DynamicsServiceTrackerCustomizer());
        dynamicsServiceTracker.open();

        LOG.info("Pax Web started");
    }

    public void stop(final BundleContext bundleContext) throws Exception {
        LOG.debug("Stopping Pax Web");

        if (dynamicsServiceTracker != null) {
            dynamicsServiceTracker.close();
            dynamicsServiceTracker = null;
        }

        if (logServiceTracker != null) {
            logServiceTracker.close();
            logServiceTracker = null;
        }

        if (eventServiceTracker != null) {
            eventServiceTracker.close();
            eventServiceTracker = null;
        }

        servletEventDispatcher.destroy();

        if (configExecutor != null) {
            configExecutor.shutdown();
            configExecutor.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (executors != null) {
            executors.shutdown();
            executors.awaitTermination(5, TimeUnit.SECONDS);
        }

        LOG.info("Pax Web stopped");
    }

    /**
     * Registers a managed service to listen on configuration updates.
     *
     * @param bundleContext bundle context to use for registration
     */
    private void createManagedService(final BundleContext bundleContext) {
        ManagedService service = new ManagedService() {
            public void updated(final Dictionary config) throws ConfigurationException {
                scheduleUpdate(config, factory);
            }
        };
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, org.ops4j.pax.web.service.WebContainerConstants.PID);
        bundleContext.registerService(ManagedService.class.getName(), service, props);
        // If ConfigurationAdmin service is not available, then do a default configuration.
        // In other cases, ConfigurationAdmin service will always call the ManagedService.
        if (bundleContext.getServiceReference(ConfigurationAdmin.class.getName()) == null) {
            try {
                service.updated(null);
            } catch (ConfigurationException ignore) {
                // this should never happen
                LOG.error(
                        "Internal error. Cannot set initial configuration resolver.",
                        ignore);
            }
        }
    }

    protected boolean same(Dictionary cfg1, Dictionary cfg2) {
        if (cfg1 == null) {
            return cfg2 == null;
        } else if (cfg2 == null) {
            return false;
        } else if (cfg1.size() != cfg2.size()) {
            return false;
        } else {
            boolean result = true;
            Enumeration keys = cfg1.keys();
            while (result && keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object v1 = cfg1.get(key);
                Object v2 = cfg2.get(key);
                result = same(v1, v2);
            }
            return result;
        }
    }

    protected boolean same(Object v1, Object v2) {
        if (v1 == null) {
            return v2 == null;
        } else if (v2 == null) {
            return false;
        } else {
            return v1 == v2 || v1.equals(v2);
        }
    }

    protected void scheduleUpdate(final Dictionary config, final ServerControllerFactory factory) {
        configExecutor.submit(new Runnable() {
            @Override
            public void run() {
                updateController(config, factory);
            }
        });
    }

    protected void updateController(Dictionary config, ServerControllerFactory factory) {
        if (same(config, this.config) && same(factory, this.factory)) {
            return;
        }
        if (m_httpServiceFactoryReg != null) {
            m_httpServiceFactoryReg.unregister();
            m_httpServiceFactoryReg = null;
        }
        if (m_serverController != null) {
            m_serverController.stop();
            m_serverController = null;
        }
        if (factory != null) {
            final PropertyResolver tmpResolver = new BundleContextPropertyResolver(bundleContext, new DefaultPropertyResolver());
            final PropertyResolver resolver = config != null ? new DictionaryPropertyResolver(config, tmpResolver) : tmpResolver;
            final ConfigurationImpl configuration = new ConfigurationImpl(resolver);
            final ServerModel serverModel = new ServerModel();
            m_serverController = factory.createServerController(serverModel);
            m_serverController.configure(configuration);
            Dictionary props = determineServiceProperties(config, configuration, m_serverController.getHttpPort(), m_serverController.getHttpSecurePort());
            m_httpServiceFactoryReg = bundleContext.registerService(new String[]{
                    HttpService.class.getName(), WebContainer.class.getName()},
                    new HttpServiceFactoryImpl() {
                        HttpService createService(final Bundle bundle) {
                            return new HttpServiceProxy(new HttpServiceStarted(bundle, m_serverController, serverModel, servletEventDispatcher));
                        }
                    }, props);
        }
        this.factory = factory;
        this.config = config;
    }


    private Dictionary determineServiceProperties(final Dictionary managedConfig,
                                                  final Configuration config,
                                                  final Integer httpPort,
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
        setProperty(toPropagate, PROPERTY_HTTP_CONNECTOR_NAME, config.getHttpConnectorName());
        setProperty(toPropagate, PROPERTY_HTTP_SECURE_ENABLED, config.isHttpEnabled());
        setProperty(toPropagate, PROPERTY_HTTP_SECURE_PORT, config.getHttpSecurePort());
        setProperty(toPropagate, PROPERTY_HTTP_SECURE_CONNECTOR_NAME, config.getHttpSecureConnectorName());
        setProperty(toPropagate, PROPERTY_HTTP_USE_NIO, config.useNIO());
        setProperty(toPropagate, PROPERTY_SSL_CLIENT_AUTH_NEEDED, config.isClientAuthNeeded());
        setProperty(toPropagate, PROPERTY_SSL_CLIENT_AUTH_WANTED, config.isClientAuthWanted());
        setProperty(toPropagate, PROPERTY_SSL_KEYSTORE, config.getSslKeystore());
        setProperty(toPropagate, PROPERTY_SSL_KEYSTORE_TYPE, config.getSslKeystoreType());
        setProperty(toPropagate, PROPERTY_SSL_PASSWORD, config.getSslPassword());
        setProperty(toPropagate, PROPERTY_SSL_KEYPASSWORD, config.getSslKeyPassword());
        setProperty(toPropagate, PROPERTY_TEMP_DIR, config.getTemporaryDirectory());
        setProperty(toPropagate, PROPERTY_SESSION_TIMEOUT, config.getSessionTimeout());
        setProperty(toPropagate, PROPERTY_SESSION_URL, config.getSessionUrl());
        setProperty(toPropagate, PROPERTY_SESSION_COOKIE,  config.getSessionCookie());
        setProperty(toPropagate, PROPERTY_WORKER_NAME, config.getWorkerName());
        setProperty(toPropagate, PROPERTY_LISTENING_ADDRESSES, config.getListeningAddresses());

        // then replace ports
        setProperty(toPropagate, PROPERTY_HTTP_PORT, httpPort);
        setProperty(toPropagate, PROPERTY_HTTP_SECURE_PORT, httpSecurePort);

        // then add/replace configuration properties for external jetty.xml file
        setProperty(toPropagate, PROPERTY_SERVER_CONFIGURATION_FILE, config.getConfigurationDir());

        // Request Log - e.g NCSA log
        setProperty(toPropagate, PROPERTY_LOG_NCSA_FORMAT, config.getLogNCSAFormat());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_RETAINDAYS, config.getLogNCSARetainDays());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_APPEND, config.isLogNCSAAppend());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_EXTENDED, config.isLogNCSAExtended());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_DISPATCH, config.isLogNCSADispatch());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_DISPATCH, config.isLogNCSADispatch());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_LOGTIMEZONE, config.getLogNCSATimeZone());

        if (JspSupportUtils.jspSupportAvailable()) {
            setProperty(toPropagate, PROPERTY_JSP_CHECK_INTERVAL, config.getJspCheckInterval());
            setProperty(toPropagate, PROPERTY_JSP_DEBUG_INFO, config.getJspClassDebugInfo());
            setProperty(toPropagate, PROPERTY_JSP_DEVELOPMENT, config.getJspDevelopment());
            setProperty(toPropagate, PROPERTY_JSP_ENABLE_POOLING, config.getJspEnablePooling());
            setProperty(toPropagate, PROPERTY_JSP_IE_CLASS_ID, config.getJspIeClassId());
            setProperty( toPropagate, PROPERTY_JSP_JAVA_ENCODING,  config.getJspJavaEncoding());
            setProperty(toPropagate, PROPERTY_JSP_KEEP_GENERATED, config.getJspKeepgenerated());
            setProperty(toPropagate, PROPERTY_JSP_LOG_VERBOSITY_LEVEL, config.getJspLogVerbosityLevel());
            setProperty(toPropagate, PROPERTY_JSP_MAPPED_FILE, config.getJspMappedfile());
            setProperty(toPropagate, PROPERTY_JSP_SCRATCH_DIR, config.getJspScratchDir());
            setProperty(toPropagate, PROPERTY_JSP_TAGPOOL_MAX_SIZE, config.getJspTagpoolMaxSize());
            setProperty(toPropagate, PROPERTY_JSP_PRECOMPILATION, config.getJspPrecompilation());
        }

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
            final ServerControllerFactory factory = (ServerControllerFactory) bundleContext.getService(reference);
            scheduleUpdate(config, factory);
            return factory;
        }

        public void modifiedService(ServiceReference reference, Object service) {
        }

        public void removedService(ServiceReference reference, Object service) {
            if (bundleContext != null) {
                bundleContext.ungetService(reference);
            }
            scheduleUpdate(config, null);
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
            if (servletEventDispatcher != null)
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
