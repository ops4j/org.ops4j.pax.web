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


import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_CHECK_INTERVAL;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_DEBUG_INFO;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_DEVELOPMENT;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_ENABLE_POOLING;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_IE_CLASS_ID;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_JAVA_ENCODING;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_KEEP_GENERATED;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_LOG_VERBOSITY_LEVEL;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_MAPPED_FILE;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_PRECOMPILATION;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_SCRATCH_DIR;
import static org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_TAGPOOL_MAX_SIZE;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_CONNECTOR_NAME;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_ENABLED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_PORT;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_SECURE_CONNECTOR_NAME;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_SECURE_ENABLED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_SECURE_PORT;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_USE_NIO;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LISTENING_ADDRESSES;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_APPEND;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_DISPATCH;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_EXTENDED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_FORMAT;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_LOGTIMEZONE;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_LOG_NCSA_RETAINDAYS;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SERVER_CONFIGURATION_FILE;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SERVER_CONFIGURATION_URL;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.util.SupportUtils;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.ServletListener;
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
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private ServerController m_serverController;
    private ServiceRegistration m_httpServiceFactoryReg;

    private BundleContext bundleContext;

    private ServletEventDispatcher servletEventDispatcher;

    private ServiceTracker eventServiceTracker;

    private ServiceTracker logServiceTracker;

    private ServiceTracker dynamicsServiceTracker;

    private final ExecutorService configExecutor = new ThreadPoolExecutor(0, 1, 20, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private Dictionary config;

    private ServerControllerFactory factory;

    private boolean initialConfigSet = false;

    public Activator() {
    }

    public void start(final BundleContext bundleContext) throws Exception {
        LOG.debug("Starting Pax Web");
        this.bundleContext = bundleContext;
        servletEventDispatcher = new ServletEventDispatcher(bundleContext);
        if (SupportUtils.isEventAdminAvailable()) {
            //Do use the filters this way the eventadmin packages can be resolved optional!
            Filter filterEvent = bundleContext.createFilter("(objectClass=org.osgi.service.event.EventAdmin)");
            EventAdminHandler adminHandler = new EventAdminHandler(bundleContext);
            eventServiceTracker = new ServiceTracker(bundleContext, filterEvent, adminHandler);
            eventServiceTracker.open();
            bundleContext.registerService(ServletListener.class.getName(), adminHandler, null);
            LOG.info("EventAdmin support enabled, servlet events will be postet to topics.");
        } else {
            LOG.info("EventAdmin support is not available, no servlet events will be postet!");
        }
        if (SupportUtils.isLogServiceAvailable()) {
            //Do use the filters this way the logservice packages can be resolved optional!
            Filter filterLog = bundleContext.createFilter("(objectClass=org.osgi.service.log.LogService)");
            LogServiceHandler logServiceHandler = new LogServiceHandler(bundleContext);
            logServiceTracker = new ServiceTracker(bundleContext, filterLog, logServiceHandler);
            logServiceTracker.open();
            bundleContext.registerService(ServletListener.class.getName(), logServiceHandler, null);
            LOG.info("LogService support enabled, log events will be created.");
        } else {
            LOG.info("LogService support is not available, no log events will be created!");
        }

        if (SupportUtils.isManagedServiceAvailable()) {
            createManagedService(bundleContext);
        } else {
            scheduleUpdateConfig(null);
        }

        LOG.info("Pax Web started");
    }

    public void stop(final BundleContext bundleContext) {
        LOG.debug("Stopping Pax Web");
        configExecutor.shutdownNow();
        if (dynamicsServiceTracker != null) {
            dynamicsServiceTracker.close();
        }
        if (logServiceTracker != null) {
            logServiceTracker.close();
        }
        if (eventServiceTracker != null) {
            eventServiceTracker.close();
        }
        if (servletEventDispatcher != null) {
            servletEventDispatcher.destroy();
        }
        //Wait up to 20 seconds, otherwhise 
        try {
            configExecutor.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //Ignore, we are done anyways...
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
                scheduleUpdateConfig(config);
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

    private void scheduleUpdateConfig(final Dictionary config) {
        configExecutor.submit(new Runnable() {
            public void run() {
                updateController(config, factory);
            }
        });
    }

    private void scheduleUpdateFactory(final ServerControllerFactory factory) {
        configExecutor.submit(new Runnable() {
            public void run() {
                updateController(config, factory);
            }
        });
    }

    /**
     * This method is the only place which is allowed to modify the config and factory fields.
     * @param config
     * @param factory
     */
    protected void updateController(Dictionary config, ServerControllerFactory factory) {
        // We want to make sure the configuration is known before starting the
        // service tracker, else the configuration could be set after the
        // service is found which would cause a restart of the service
        if (!initialConfigSet) {
            initialConfigSet = true;
            this.config = config;
            this.factory = factory;
            dynamicsServiceTracker = new ServiceTracker(bundleContext,
                    ServerControllerFactory.class.getName(),
                    new DynamicsServiceTrackerCustomizer());
            dynamicsServiceTracker.open();
            return;
        }
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
            if (!m_serverController.isStarted()) {
                while (!m_serverController.isConfigured()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LOG.warn("caught interruptexception while waiting for configuration", e);
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                m_serverController.start();
            }
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

        setProperty(toPropagate, PROPERTY_SERVER_CONFIGURATION_URL, config.getConfigurationURL());

        // Request Log - e.g NCSA log
        setProperty(toPropagate, PROPERTY_LOG_NCSA_FORMAT, config.getLogNCSAFormat());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_RETAINDAYS, config.getLogNCSARetainDays());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_APPEND, config.isLogNCSAAppend());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_EXTENDED, config.isLogNCSAExtended());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_DISPATCH, config.isLogNCSADispatch());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_DISPATCH, config.isLogNCSADispatch());
        setProperty(toPropagate, PROPERTY_LOG_NCSA_LOGTIMEZONE, config.getLogNCSATimeZone());

        if (SupportUtils.isJSPAvailable()) {
            setProperty(toPropagate, PROPERTY_JSP_CHECK_INTERVAL, config.getJspCheckInterval());
            setProperty(toPropagate, PROPERTY_JSP_DEBUG_INFO, config.getJspClassDebugInfo());
            setProperty(toPropagate, PROPERTY_JSP_DEVELOPMENT, config.getJspDevelopment());
            setProperty(toPropagate, PROPERTY_JSP_ENABLE_POOLING, config.getJspEnablePooling());
            setProperty(toPropagate, PROPERTY_JSP_IE_CLASS_ID, config.getJspIeClassId());
            setProperty(toPropagate, PROPERTY_JSP_JAVA_ENCODING,  config.getJspJavaEncoding());
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

    private class DynamicsServiceTrackerCustomizer implements ServiceTrackerCustomizer {

        public Object addingService(ServiceReference reference) {
            final ServerControllerFactory factory = (ServerControllerFactory) bundleContext.getService(reference);
            scheduleUpdateFactory(factory);
            return factory;
        }

        public void modifiedService(ServiceReference reference, Object service) {
        }

        public void removedService(ServiceReference reference, Object service) {
            if (bundleContext != null) {
                bundleContext.ungetService(reference);
            }
            scheduleUpdateFactory(null);
        }
    }


}
