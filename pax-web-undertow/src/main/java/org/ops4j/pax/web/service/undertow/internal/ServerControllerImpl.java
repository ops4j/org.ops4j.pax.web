/*
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
package org.ops4j.pax.web.service.undertow.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParserFactory;

import io.undertow.UndertowOptions;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.server.handlers.ProxyPeerAddressHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.SessionPersistenceManager;
import io.undertow.servlet.util.InMemorySessionPersistence;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.ConfigurationSource;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerEx;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.ResourceModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.pax.web.service.spi.model.WelcomeFileModel;
import org.ops4j.pax.web.service.undertow.internal.configuration.ResolvingContentHandler;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.IoSubsystem;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.SecurityRealm;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.Server;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.ServletContainer.PersistentSessionsConfig;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.UndertowConfiguration;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.UndertowSubsystem;
import org.ops4j.pax.web.service.undertow.internal.security.JaasIdentityManager;
import org.ops4j.pax.web.service.undertow.internal.security.PropertiesIdentityManager;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.SslClientAuthMode;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PeerNameResolvingHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;

/**
 * @author Guillaume Nodet
 */
public class ServerControllerImpl implements ServerController, ServerControllerEx, IdentityManager {

    private enum State {
        Unconfigured,
        Stopped,
        Started
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServerControllerImpl.class);

    private final BundleContext bundleContext;
    private JAXBContext jaxb = null;

    private Configuration configuration;

    private final Set<ServerListener> listeners = new CopyOnWriteArraySet<>();
    private State state = State.Unconfigured;
    private IdentityManager identityManager;
    private SessionPersistenceManager sessionPersistenceManager;
    private int defaultSessionTimeoutInMinutes;

    // Standard URI -> HttpHandler map - may be wrapped by access log, filters, etc. later
    private final ContextAwarePathHandler path = new ContextAwarePathHandler(Handlers.path());
    // all Contexts add own mapping here
    private Undertow server;
    private final ConcurrentMap<HttpContext, Context> contextMap = new ConcurrentHashMap<>();

    private XnioWorker xnioWorker;

    public ServerControllerImpl(BundleContext context) {
        this.bundleContext = context;
    }

    @Override
    public synchronized void start() {
        LOG.debug("Starting server [{}]", this);
        assertState(State.Stopped);
        doStart();
        state = State.Started;
        notifyListeners(ServerEvent.STARTED);
    }

    @Override
    public synchronized void stop() {
        LOG.debug("Stopping server [{}]", this);
        assertNotState(State.Unconfigured);
        if (state == State.Started) {
            doStop();
            state = State.Stopped;
        }
        notifyListeners(ServerEvent.STOPPED);
    }

    @Override
    public synchronized void configure(final Configuration config) {
        LOG.debug("Configuring server [{}] -> [{}] ", this, config);
        if (config == null) {
            throw new IllegalArgumentException("configuration == null");
        }
        configuration = config;
        switch (state) {
        case Unconfigured:
            doConfigure();
            state = State.Stopped;
            notifyListeners(ServerEvent.CONFIGURED);
            break;
        case Started:
            // reconfigure first
            doConfigure();
            state = State.Stopped;
            notifyListeners(ServerEvent.CONFIGURED);
            // and restart
            doStop();
            state = State.Stopped;
            notifyListeners(ServerEvent.STOPPED);
            doStart();
            state = State.Started;
            notifyListeners(ServerEvent.STARTED);
            break;
        }
    }

    @Override
    public void addListener(ServerListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        listeners.add(listener);
    }

    @Override
    public void removeListener(ServerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public synchronized boolean isStarted() {
        return state == State.Started;
    }

    @Override
    public synchronized boolean isConfigured() {
        return state != State.Unconfigured;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public synchronized Integer getHttpPort() {
        Configuration config = configuration;
        if (config == null) {
            throw new IllegalStateException("Not configured");
        }
        return config.getHttpPort();
    }

    @Override
    public synchronized Integer getHttpSecurePort() {
        Configuration config = configuration;
        if (config == null) {
            throw new IllegalStateException("Not configured");
        }
        return config.getHttpSecurePort();
    }

    void notifyListeners(ServerEvent event) {
        for (ServerListener listener : listeners) {
            listener.stateChanged(event);
        }
    }

    /**
     * Here's where Undertow is being rebuild at {@link Undertow} level (not {@link ServletContainer} level).
     * This is were <em>global</em> objects are configured (listeners, global filters, ...)
     */
    private void doConfigure() {
        Undertow.Builder builder = Undertow.builder();

        // if no configuration method change root handler, simple path->HttpHandler will be used
        // where each HttpHandler is created in separate org.ops4j.pax.web.service.undertow.internal.Context
        HttpHandler rootHandler = path;

        URL undertowResource = detectUndertowConfiguration();
        ConfigSource source = ConfigSource.kind(undertowResource);

        switch (source) {
            case XML:
                LOG.info("Using \"" + undertowResource + "\" to configure Undertow");
                rootHandler = configureUndertow(configuration, builder, rootHandler, undertowResource);
                break;
            case PROPERTIES:
                LOG.info("Using \"" + undertowResource + "\" to read additional configuration for Undertow");
                configureIdentityManager(undertowResource);
                // do not break - go to standard PID configuration
            case PID:
                LOG.info("Using \"org.ops4j.pax.url.web\" PID to configure Undertow");
                rootHandler = configureUndertow(configuration, builder, rootHandler);
                break;
        }

        for (Context context : contextMap.values()) {
            try {
                context.setSessionPersistenceManager(sessionPersistenceManager);
                context.setDefaultSessionTimeoutInMinutes(defaultSessionTimeoutInMinutes);
                context.start();
            } catch (Exception e) {
                LOG.error("Could not start the servlet context for context path [" + context.getContextModel().getContextName() + "]", e);
            }
        }

        builder.setHandler(rootHandler);
        server = builder.build();
    }

    /**
     * This method requires previously configured {@link Undertow.Builder} instance used to create {@link Undertow}
     * instance. The remaining task is to start the server.
     */
    void doStart() {
        server.start();
    }

    /**
     * Loads additional properties and configure {@link ServerControllerImpl#identityManager}
     * @param undertowResource
     */
    private void configureIdentityManager(URL undertowResource) {
        try {
            Properties props = new Properties();
            try (InputStream is = undertowResource.openStream()) {
                props.load(is);
            }
            Map<String, String> config = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                config.put(entry.getKey().toString(), entry.getValue().toString());
            }
            identityManager = (IdentityManager)createConfigurationObject(config, "identityManager");

//            String listeners = config.get("listeners");
//            if (listeners != null) {
//                String[] names = listeners.split("(, )+");
//                for (String name : names) {
//                    String type = config.get("listeners." + name + ".type");
//                    String address = config.get("listeners." + name + ".address");
//                    String port = config.get("listeners." + name + ".port");
//                    if ("http".equals(type)) {
//                        builder.addHttpListener(Integer.parseInt(port), address);
//                    }
//                }
//            }
        } catch (Exception e) {
            LOG.error("Exception while starting Undertow", e);
            throw new RuntimeException("Exception while starting Undertow", e);
        }
    }

    /**
     * Configuration using <code>org.ops4j.pax.web</code> PID - only listeners and NCSA logging
     * @param configuration
     * @param builder
     * @param rootHandler current root handler
     * @return
     */
    private HttpHandler configureUndertow(Configuration configuration, Undertow.Builder builder, HttpHandler rootHandler) {
        if (configuration.isLogNCSAFormatEnabled()) {
            String logNCSADirectory = configuration.getLogNCSADirectory();
            String logNCSAFormat = configuration.getLogNCSAFormat();

            Bundle bundle = FrameworkUtil.getBundle(ServerControllerImpl.class);
            ClassLoader loader = bundle.adapt(BundleWiring.class).getClassLoader();
            xnioWorker = UndertowUtil.createWorker(loader);

            // String logNameSuffix = logNCSAFormat.substring(logNCSAFormat.lastIndexOf("."));
            // String logBaseName = logNCSAFormat.substring(0, logNCSAFormat.lastIndexOf("."));

            AccessLogReceiver logReceiver = DefaultAccessLogReceiver.builder().setLogWriteExecutor(xnioWorker)
                    .setOutputDirectory(new File(logNCSADirectory).toPath()).setLogBaseName("request.")
                    .setLogNameSuffix("log").setRotate(true).build();

            String format;
            if (configuration.isLogNCSAExtended()) {
                format = "combined";
            } else {
                format = "common";
            }

            // String format = "%a - - [%t] \"%m %U %H\" %s ";
            // TODO: still need to find out how to add cookie etc.

            rootHandler = new AccessLogHandler(rootHandler, logReceiver, format,
                    AccessLogHandler.class.getClassLoader());
        }

        for (String address : configuration.getListeningAddresses()) {
            if (configuration.isHttpEnabled()) {
                LOG.info("Starting undertow http listener on " + address + ":" + configuration.getHttpPort());
                builder.addHttpListener(configuration.getHttpPort(), address);
            }
            if (configuration.isHttpSecureEnabled()) {
                LOG.info("Starting undertow https listener on " + address + ":" + configuration.getHttpSecurePort());
                // TODO: could this be shared across interface:port bindings?
                SSLContext context = buildSSLContext();
                builder.addHttpsListener(configuration.getHttpSecurePort(), address, context);
            }
        }

        if (configuration.checkForwardedHeaders()) {
            rootHandler = new ProxyPeerAddressHandler(rootHandler);
        }

        return rootHandler;
    }

    /**
     * Configuration using <code>undertow.xml</code> conforming to Undertow/Wildfly XML Schemas
     * @param configuration
     * @param builder
     * @param rootHandler current root handler
     * @param undertowResource URI for XML configuration
     * @return
     */
    private HttpHandler configureUndertow(Configuration configuration, Undertow.Builder builder, HttpHandler rootHandler, URL undertowResource) {
        try {
            if (jaxb == null) {
                // we don't want static references here
                jaxb = JAXBContext.newInstance("org.ops4j.pax.web.service.undertow.internal.configuration.model",
                        UndertowConfiguration.class.getClassLoader());
            }
            Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            UnmarshallerHandler unmarshallerHandler = unmarshaller.getUnmarshallerHandler();

            Dictionary<String, Object> properties = new Hashtable<>();

            if (configuration instanceof ConfigurationSource) {
                Dictionary<String, Object> externalConfig = ((ConfigurationSource) configuration).getConfiguration();
                if (externalConfig != null) {
                    for (Enumeration<String> e = externalConfig.keys(); e.hasMoreElements(); ) {
                        String key = e.nextElement();
                        properties.put(key, externalConfig.get(key));
                    }
                }
            }
            if (properties.get(WebContainerConstants.PROPERTY_HTTP_PORT) == null && configuration.getHttpPort() != null) {
                properties.put(WebContainerConstants.PROPERTY_HTTP_PORT, Integer.toString(configuration.getHttpPort()));
            }
            if (properties.get(WebContainerConstants.PROPERTY_HTTP_SECURE_PORT) == null && configuration.getHttpSecurePort() != null) {
                properties.put(WebContainerConstants.PROPERTY_HTTP_SECURE_PORT, Integer.toString(configuration.getHttpSecurePort()));
            }

            // BundleContextPropertyResolver gives access to e.g., ${karaf.base}
            final PropertyResolver resolver = new DictionaryPropertyResolver(properties,
                    new BundleContextPropertyResolver(bundleContext));

            // indirect unmarslaling with property resolution *inside XML attribute values*
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            XMLReader xmlReader = spf.newSAXParser().getXMLReader();

            // tricky PropertyResolver -> Properties bridge
            xmlReader.setContentHandler(new ResolvingContentHandler(new Properties() {
                @Override
                public String getProperty(String key) {
                    return resolver.get(key);
                }

                @Override
                public String getProperty(String key, String defaultValue) {
                    String value = resolver.get(key);
                    return value == null ? defaultValue : value;
                }
            }, unmarshallerHandler));
            try (InputStream stream = undertowResource.openStream()) {
                xmlReader.parse(new InputSource(stream));
            }

            UndertowConfiguration cfg = (UndertowConfiguration) unmarshallerHandler.getResult();
            if (cfg == null
                    || cfg.getSocketBindings().size() == 0
                    || cfg.getInterfaces().size() == 0
                    || cfg.getSubsystem() == null
                    || cfg.getSubsystem().getServer() == null) {
                throw new IllegalArgumentException("Problem configuring Undertow server using \"" + undertowResource + "\": invalid XML");
            }
            cfg.init();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Undertow XML configuration: {}", cfg);
            }

            // ok, we have everything unmarshalled from XML to config object
            // we can configure all/some aspects of Undertow now

            IoSubsystem io = cfg.getIoSubsystem();
            if (io != null) {
                Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());

                // PAXWEB-1255 only "default" worker and buffer pool are supported for now
                IoSubsystem.Worker worker = cfg.worker("default");
                IoSubsystem.BufferPool bufferPool = cfg.bufferPool("default");

                if (worker != null) {
                    OptionMap.Builder b = OptionMap.builder();
                    b.set(Options.CONNECTION_HIGH_WATER, 1000000);
                    b.set(Options.CONNECTION_LOW_WATER, 1000000);
                    b.set(Options.TCP_NODELAY, true);
                    b.set(Options.CORK, true);
                    b.set(Options.WORKER_IO_THREADS, worker.getIoThreads());
                    b.set(Options.WORKER_TASK_MAX_THREADS, worker.getTaskMaxThreads());
                    b.set(Options.WORKER_TASK_CORE_THREADS, worker.getTaskCoreThreads());
                    XnioWorker w = xnio.createWorker(b.getMap());
                    builder.setWorker(w);
                }

                if (bufferPool != null) {
                    if (bufferPool.getDirectBuffers() != null) {
                        builder.setDirectBuffers(bufferPool.getDirectBuffers());
                    }
                    if (bufferPool.getBufferSize() != null) {
                        builder.setBufferSize(bufferPool.getBufferSize());
                    }
                }
            }

            Server.HttpListener http = cfg.getSubsystem().getServer().getHttpListener();
            Server.HttpsListener https = cfg.getSubsystem().getServer().getHttpsListener();
            if (http == null && https == null) {
                throw new IllegalArgumentException("No listener configuration available in \"" + undertowResource + "\". Please configure http and/or https listeners.");
            }

            // PAXWEB-1233
            boolean forwardHeaders = false;

            // PAXWEB-1232
            boolean recordRequestStartTime = false;
            
            // PAXWEB-1236
            boolean peerHostLookup = false;
 
            // http listener
            if (http != null) {
                UndertowConfiguration.BindingInfo binding = cfg.bindingInfo(http.getSocketBindingName());
                for (String address : binding.getAddresses()) {
                    LOG.info("Starting undertow http listener on " + address + ":" + binding.getPort());
                    Undertow.ListenerBuilder lb = new Undertow.ListenerBuilder();
                    lb.setHost(address).setPort(binding.getPort());
                    lb.setType(Undertow.ListenerType.HTTP);
                    if (http.getMaxConnections() > 0) {
                        lb.setOverrideSocketOptions(OptionMap.builder()
                                .set(Options.CONNECTION_HIGH_WATER, http.getMaxConnections())
                                .set(Options.CONNECTION_LOW_WATER, http.getMaxConnections())
                                .getMap());
                    }
                    builder.addListener(lb);
                    if (http.isRecordRequestStartTime()) {
                        recordRequestStartTime = true;
                    }
                    if ("true".equalsIgnoreCase(http.getProxyAddressForwarding())) {
                        forwardHeaders = true;
                    }
                    if ("true".equalsIgnoreCase(http.getPeerHostLookup())) {
                        peerHostLookup = true;
                    }
                }
            }

            // https listener
            if (https != null) {
                UndertowConfiguration.BindingInfo binding = cfg.bindingInfo(https.getSocketBindingName());
                SecurityRealm realm = cfg.securityRealm(https.getSecurityRealm());
                if (realm == null) {
                    throw new IllegalArgumentException("No security realm with name \"" + https.getSecurityRealm() + "\" available for \"" + https.getName() + "\" https listener.");
                }
                for (String address : binding.getAddresses()) {
                    LOG.info("Starting undertow https listener on " + address + ":" + binding.getPort());
                    // TODO: could this be shared across interface:port bindings?
                    SSLContext sslContext = buildSSLContext(realm);

                    Undertow.ListenerBuilder lb = new Undertow.ListenerBuilder();
                    lb.setHost(address).setPort(binding.getPort());
                    lb.setType(Undertow.ListenerType.HTTPS);
                    lb.setSslContext(sslContext);
                    if (https.getMaxConnections() > 0) {
                        lb.setOverrideSocketOptions(OptionMap.builder()
                                .set(Options.CONNECTION_HIGH_WATER, https.getMaxConnections())
                                .set(Options.CONNECTION_LOW_WATER, https.getMaxConnections())
                                .getMap());
                    }
                    builder.addListener(lb);
                    if (https.isRecordRequestStartTime()) {
                        recordRequestStartTime = true;
                    }
                    if ("true".equalsIgnoreCase(https.getProxyAddressForwarding())) {
                        forwardHeaders = true;
                    }
                    if ("true".equalsIgnoreCase(https.getPeerHostLookup())) {
                        peerHostLookup = true;
                    }

                    // options - see io.undertow.protocols.ssl.UndertowAcceptingSslChannel()
                    // one of NOT_REQUESTED, REQUESTED, REQUIRED
                    builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, SslClientAuthMode.valueOf(https.getVerifyClient()));

                    SecurityRealm.Engine engine = realm.getIdentities().getSsl().getEngine();
                    if (engine != null) {
                        // could be taken from these as well:
                        //  - https.getEnabledProtocols();
                        //  - https.getEnabledCipherSuites();
                        if (engine.getEnabledProtocols().size() > 0) {
                            builder.setSocketOption(Options.SSL_ENABLED_PROTOCOLS, Sequence.of(engine.getEnabledProtocols()));
                        }
                        if (engine.getEnabledCipherSuites().size() > 0) {
                            builder.setSocketOption(Options.SSL_ENABLED_CIPHER_SUITES, Sequence.of(engine.getEnabledCipherSuites()));
                        }
                    }
                }
            }

            builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, recordRequestStartTime);
            if (configuration.getConnectorIdleTimeout() != null) {
                builder.setServerOption(UndertowOptions.IDLE_TIMEOUT, configuration.getConnectorIdleTimeout());
            }

            // identity manager - looked up in "default" security realm
            SecurityRealm defaultRealm = cfg.securityRealm("default");
            if (defaultRealm != null) {
                SecurityRealm.JaasAuth jaasAuth = defaultRealm.getAuthentication().getJaas();
                SecurityRealm.PropertiesAuth propertiesAuth = defaultRealm.getAuthentication().getProperties();
                if (jaasAuth != null) {
                    String userPrincipalClassName = defaultRealm.getUserPrincipalClassName();
                    if (userPrincipalClassName == null || "".equals(userPrincipalClassName.trim())) {
                        userPrincipalClassName = "java.security.Principal";
                    }
                    Set<String> rolePrincipalClassNames = new LinkedHashSet<>(defaultRealm.getRolePrincipalClassNames());
                    identityManager = new JaasIdentityManager(jaasAuth.getName(),
                            userPrincipalClassName, rolePrincipalClassNames);
                } else if (propertiesAuth != null) {
                    File userBase = new File(propertiesAuth.getPath());
                    if (!userBase.isFile()) {
                        throw new IllegalArgumentException(userBase.getCanonicalPath() + " is not accessible. Can't load users/groups information.");
                    }
                    Properties userProperties = new Properties();
                    Map<String, String> map = new HashMap<>();
                    try (FileInputStream stream = new FileInputStream(userBase)) {
                        userProperties.load(stream);
                        for (String user : userProperties.stringPropertyNames()) {
                            map.put(user, userProperties.getProperty(user));
                        }
                    }
                    identityManager = new PropertiesIdentityManager(map);
                }
            }

            // /undertow/subsystem/server/host/location - file handlers for static context paths.
            if (cfg.getSubsystem().getServer().getHost() != null) {
                for (Server.Host.Location location : cfg.getSubsystem().getServer().getHost().getLocation()) {
                    String context = location.getName();
                    String handlerRef = location.getHandler();
                    UndertowSubsystem.FileHandler fileHandler = cfg.handler(handlerRef);
                    if (fileHandler == null) {
                        throw new IllegalArgumentException("No handler with name \"" + location.getHandler() + "\" available for " + location.getName() + " location.");
                    }
                    File base = new File(fileHandler.getPath());
                    if (!base.isDirectory()) {
                        throw new IllegalArgumentException(base.getCanonicalPath() + " is not accessible. Can't configure handler for " + location.getName() + " location.");
                    }
                    // fileHandler.path is simply filesystem directory
                    ResourceHandler rh = new ResourceHandler(new FileResourceManager(base, 4096));
                    if (cfg.getSubsystem().getServletContainer() != null) {
                        rh.setWelcomeFiles();
                        for (org.ops4j.pax.web.service.undertow.internal.configuration.model.ServletContainer.WelcomeFile wf : cfg.getSubsystem().getServletContainer().getWelcomeFiles()) {
                            rh.addWelcomeFiles(wf.getName());
                        }
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
            if (cfg.getSubsystem().getServer().getHost() != null) {
                for (Server.Host.FilterRef fr : cfg.getSubsystem().getServer().getHost().getFilterRef()) {
                    UndertowSubsystem.AbstractFilter filter = cfg.filter(fr.getName());
                    if (filter == null) {
                        throw new IllegalArgumentException("No filter with name \"" + fr.getName() + "\" available.");
                    }
                    rootHandler = filter.configure(rootHandler, fr.getPredicate());
                }
            }

            // access log
            if (cfg.getSubsystem().getServer().getHost() != null
                    && cfg.getSubsystem().getServer().getHost().getAccessLog() != null) {
                Server.Host.AccessLog accessLog = cfg.getSubsystem().getServer().getHost().getAccessLog();

                Bundle bundle = FrameworkUtil.getBundle(ServerControllerImpl.class);
                ClassLoader loader = bundle.adapt(BundleWiring.class).getClassLoader();
                xnioWorker = UndertowUtil.createWorker(loader);

                AccessLogReceiver logReceiver = DefaultAccessLogReceiver.builder()
                        .setLogWriteExecutor(xnioWorker)
                        .setOutputDirectory(new File(accessLog.getDirectory()).toPath())
                        .setLogBaseName(accessLog.getPrefix())
                        .setLogNameSuffix(accessLog.getSuffix())
                        .setRotate(Boolean.parseBoolean(accessLog.getRotate()))
                        .build();

                rootHandler = new AccessLogHandler(rootHandler, logReceiver, accessLog.getPattern(),
                        AccessLogHandler.class.getClassLoader());
            }

            // session configuration and persistence
            this.defaultSessionTimeoutInMinutes = 30;
            try {
                if (cfg.getSubsystem().getServletContainer() != null) {
                    String defaultSessionTimeout = cfg.getSubsystem().getServletContainer().getDefaultSessionTimeout();
                    if (defaultSessionTimeout != null && !"".equals(defaultSessionTimeout)) {
                        this.defaultSessionTimeoutInMinutes = Integer.parseInt(defaultSessionTimeout);
                    }
                }
            } catch (NumberFormatException ignored) {
            }

            PersistentSessionsConfig persistentSessions = cfg.getSubsystem().getServletContainer() == null ? null
                    : cfg.getSubsystem().getServletContainer().getPersistentSessions();
            if (persistentSessions == null) {
                // no sessions, but let's use InMemorySessionPersistence
                LOG.info("Using in-memory session persistence");
                sessionPersistenceManager = new InMemorySessionPersistence();
            } else {
                if (persistentSessions.getPath() != null && !"".equals(persistentSessions.getPath().trim())) {
                    // file persistence manager
                    File sessionsDir = new File(persistentSessions.getPath());
                    sessionsDir.mkdirs();
                    LOG.info("Using file session persistence. Location: " + sessionsDir.getCanonicalPath());
                    sessionPersistenceManager = new FileSessionPersistence(sessionsDir);
                } else {
                    // in memory persistence manager
                    LOG.info("No path configured for persistent-sessions. Using in-memory session persistence.");
                    sessionPersistenceManager = new InMemorySessionPersistence();
                }
            }

            if (forwardHeaders) {
                rootHandler = new ProxyPeerAddressHandler(rootHandler);
            }

            if (peerHostLookup) {
                rootHandler = new PeerNameResolvingHandler(rootHandler);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Problem configuring Undertow server using \"" + undertowResource + "\": " + e.getMessage(), e);
        }

        return rootHandler;
    }

    /**
     * <p>Scans for file used to configure Undertow:<ul>
     *     <li>Always consider <code>org.ops4j.pax.web</code> PID</li>
     *     <li>If this PID contains <code>org.ops4j.pax.web.config.url</code> or <code>org.ops4j.pax.web.config.file</code>
     *     check such resource</li>
     *     <li>If such resource exists and has <code>properties</code> extension <strong>or</strong>
     *     is a directory and there's <code>undertow.properties</code> inside, load some properties from there
     *     (identity manager configuration) and merge with other properties from <code>org.ops4j.pax.web</code> PID</li>
     *     <li>If such resource exists and has <code>xml</code> extension <strong>or</strong>
     *     is a directory and there's <code>undertow.xml</code> inside, use this configuration and
     *     <strong>do not</strong> consider other properties from the PID <strong>except</strong>
     *     for property placeholder resolution inside XML file.</li>
     * </ul>
     * </p>
     * @return
     */
    private URL detectUndertowConfiguration() {
        URL undertowResource = configuration.getConfigurationURL();
        // even if it's "dir" it may point to "file"
        // (same as in o.o.p.w.s.jetty.internal.JettyFactoryImpl.getHttpConfiguration())
        File serverConfigDir = configuration.getConfigurationDir();

        try {
            if (undertowResource == null && serverConfigDir != null) {
                // org.ops4j.pax.web.config.file
                if (serverConfigDir.isFile() && serverConfigDir.canRead()) {
                    undertowResource = serverConfigDir.toURI().toURL();
                } else if (serverConfigDir.isDirectory()) {
                    for (String name : new String[] { "undertow.xml", "undertow.properties" }) {
                        File configuration = new File(serverConfigDir, name);
                        if (configuration.isFile() && configuration.canRead()) {
                            undertowResource = configuration.toURI().toURL();
                            break;
                        }
                    }
                }
            }
        } catch (MalformedURLException ignored) {
        }

        if (undertowResource == null) {
            undertowResource = getClass().getResource("/undertow.xml");
        }
        if (undertowResource == null) {
            undertowResource = getClass().getResource("/undertow.properties");
        }

        return undertowResource;
    }

    /**
     * Build {@link SSLContext} using arguments only
     * @param keystorePath
     * @param keystoreType
     * @param keystorePassword
     * @param keystoreCertAlias
     * @param truststorePath
     * @param truststoreType
     * @param truststorePassword
     * @param validateCerts
     * @param crlPath
     * @param secureRandomAlgorithm
     * @param validatePeerCerts
     * @param enableCRLDP
     * @param enableOCSP
     * @param ocspResponderURL
     * @return
     */
    private SSLContext buildSSLContext(String keystorePath, String keystoreType, String keystorePassword, String keystoreKeyPassword, String keystoreCertAlias,
                                       String truststorePath, String truststoreType, String truststorePassword,
                                       boolean validateCerts, String crlPath,
                                       String secureRandomAlgorithm,
                                       boolean validatePeerCerts, boolean enableCRLDP,
                                       boolean enableOCSP, String ocspResponderURL) {
        try {
            URL keyStoreURL = loadResource(keystorePath);
            KeyStore keyStore = getKeyStore(keyStoreURL,
                    keystoreType != null ? keystoreType : "JKS",
                    keystorePassword);

            if (keystoreCertAlias != null) {
                // just as in org.jboss.as.domain.management.security.FileKeystore#load(), we have to
                // create temporary, single key entry keystore
                KeyStore newKeystore = KeyStore.getInstance(keystoreType != null ? keystoreType : "JKS");
                newKeystore.load(null);

                if (keyStore.containsAlias(keystoreCertAlias)) {
                    KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(keystoreKeyPassword == null ? null : keystoreKeyPassword.toCharArray());
                    if (keyStore.isKeyEntry(keystoreCertAlias)) {
                        KeyStore.Entry entry = keyStore.getEntry(keystoreCertAlias, password);
                        newKeystore.setEntry(keystoreCertAlias, entry, password);
                        keyStore = newKeystore;
                    } else {
                        throw new IllegalArgumentException("Entry keystoreCertAlias=\"" + keystoreCertAlias + "\" is not private key entry in keystore " + keystorePath);
                    }
                } else {
                    throw new IllegalArgumentException("Entry keystoreCertAlias=\"" + keystoreCertAlias + "\" not found in keystore " + keystorePath);
                }
            }

            // key managers
            String _keyManagerFactoryAlgorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm") == null
                    ? KeyManagerFactory.getDefaultAlgorithm()
                    : Security.getProperty("ssl.KeyManagerFactory.algorithm");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(_keyManagerFactoryAlgorithm);
            keyManagerFactory.init(keyStore, keystoreKeyPassword == null ? null : keystoreKeyPassword.toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            // trust managers - possibly with OCSP
            TrustManager[] trustManagers = null;
            SecureRandom random = (secureRandomAlgorithm == null) ? null : SecureRandom.getInstance(secureRandomAlgorithm);
            if (truststorePath != null) {
                URL trustStoreURL = loadResource(truststorePath);
                KeyStore trustStore = getKeyStore(trustStoreURL,
                        truststoreType != null ? truststoreType : "JKS",
                        truststorePassword);

                String _trustManagerFactoryAlgorithm = Security.getProperty("ssl.TrustManagerFactory.algorithm") == null
                        ? TrustManagerFactory.getDefaultAlgorithm()
                        : Security.getProperty("ssl.TrustManagerFactory.algorithm");

                Collection<? extends CRL> crls = crlPath == null ? null : loadCRL(crlPath);

                if (validateCerts && keyStore != null) {
                    if (keystoreCertAlias == null) {
                        List<String> aliases = Collections.list(keyStore.aliases());
                        keystoreCertAlias = aliases.size() == 1 ? aliases.get(0) : null;
                    }

                    Certificate cert = keystoreCertAlias == null ? null : keyStore.getCertificate(keystoreCertAlias);
                    if (cert == null) {
                        throw new IllegalArgumentException("No certificate found in the keystore" + (keystoreCertAlias == null ? "" : " for alias \"" + keystoreCertAlias + "\""));
                    }

                    CertificateValidator validator = new CertificateValidator(trustStore, crls);
                    validator.setEnableCRLDP(enableCRLDP);
                    validator.setEnableOCSP(enableOCSP);
                    validator.setOcspResponderURL(ocspResponderURL);
                    validator.validate(keyStore, cert);
                }

                // Revocation checking is only supported for PKIX algorithm
                // see org.eclipse.jetty.util.ssl.SslContextFactory.getTrustManagers()
                if (validatePeerCerts && _trustManagerFactoryAlgorithm.equalsIgnoreCase("PKIX")) {
                    PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());

                    // Make sure revocation checking is enabled
                    pbParams.setRevocationEnabled(true);

                    if (crls != null && !crls.isEmpty()) {
                        pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
                    }

                    if (enableCRLDP) {
                        // Enable Certificate Revocation List Distribution Points (CRLDP) support
                        System.setProperty("com.sun.security.enableCRLDP", "true");
                    }

                    if (enableOCSP) {
                        // Enable On-Line Certificate Status Protocol (OCSP) support
                        Security.setProperty("ocsp.enable", "true");

                        if (ocspResponderURL != null) {
                            // Override location of OCSP Responder
                            Security.setProperty("ocsp.responderURL", ocspResponderURL);
                        }
                    }

                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(_trustManagerFactoryAlgorithm);
                    trustManagerFactory.init(new CertPathTrustManagerParameters(pbParams));

                    trustManagers = trustManagerFactory.getTrustManagers();
                } else {
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(_trustManagerFactoryAlgorithm);
                    trustManagerFactory.init(trustStore);

                    trustManagers = trustManagerFactory.getTrustManagers();
                }
            }

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers, trustManagers, random);

            return context;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to build SSL context", e);
        }
    }

    /**
     * Build {@link SSLContext} from <code>org.ops4j.pax.web</code> PID configuration
     * @return
     */
    private SSLContext buildSSLContext() {
        String keyANDkeystorePassword = configuration.getSslKeyPassword() == null ? configuration.getSslPassword() : configuration.getSslKeyPassword();
        return buildSSLContext(configuration.getSslKeystore(), configuration.getSslKeystoreType(),
                keyANDkeystorePassword, keyANDkeystorePassword,
                configuration.getSslKeyAlias(),
                configuration.getTrustStore(), configuration.getTrustStoreType(),
                configuration.getTrustStorePassword(),
                configuration.isValidateCerts(), configuration.getCrlPath(),
                null,
                configuration.isValidatePeerCerts(), configuration.isEnableCRLDP(), configuration.isEnableOCSP(),
                configuration.getOcspResponderURL());
    }

    /**
     * Build {@link SSLContext} from XML configuration
     * @param realm
     * @return
     */
    private SSLContext buildSSLContext(SecurityRealm realm) {
        if (realm.getAuthentication() == null || realm.getAuthentication().getTruststore() == null) {
            throw new IllegalArgumentException("No truststore configuration in security realm \"" + realm.getName() + "\".");
        }
        if (realm.getIdentities() == null || realm.getIdentities().getSsl() == null
                || realm.getIdentities().getSsl().getKeystore() == null) {
            throw new IllegalArgumentException("No keystore configuration in security realm \"" + realm.getName() + "\".");
        }

        SecurityRealm.Keystore keystore = realm.getIdentities().getSsl().getKeystore();
        SecurityRealm.Truststore truststore = realm.getAuthentication().getTruststore();

        String keystorePassword = keystore.getPassword();
        String keyPassword = keystore.getKeyPassword();

        // we'll reuse PID configuration for OCSP/CRL stuff
        return buildSSLContext(keystore.getPath(), keystore.getProvider(),
                keystorePassword, keyPassword,
                keystore.getAlias(),
                truststore.getPath(), truststore.getProvider(),
                truststore.getPassword(),
                configuration.isValidateCerts(), configuration.getCrlPath(),
                null, // "SHA1PRNG", "NativePRNGNonBlocking", ...
                configuration.isValidatePeerCerts(), configuration.isEnableCRLDP(), configuration.isEnableOCSP(),
                configuration.getOcspResponderURL());
    }

    private URL loadResource(String resource) throws MalformedURLException {
        if (resource == null || "".equals(resource.trim())) {
            return null;
        }
        URL url;
        try {
            url = new URL(resource);
        } catch (MalformedURLException e) {
            if (!resource.startsWith("ftp:") && !resource.startsWith("file:")
                && !resource.startsWith("jar:")) {
                try {
                    File file = new File(resource).getCanonicalFile();
                    url = file.toURI().toURL();
                } catch (Exception e2) {
                    throw e;
                }
            } else {
                throw e;
            }
        }
        return url;
    }

    private KeyStore getKeyStore(URL storePath, String storeType, String storePassword) throws Exception {
        KeyStore keystore = KeyStore.getInstance(storeType);
        if (storePath != null) {
            try (InputStream is = storePath.openStream()) {
                keystore.load(is, storePassword.toCharArray());
            }
        } else {
            keystore.load(null, storePassword.toCharArray());
        }
        return keystore;
    }

    private Object createConfigurationObject(Map<String, String> config, String name) throws Exception {
        String clazzName = config.get(name);
        if (clazzName != null) {
            Class<?> clazz = getClass().getClassLoader().loadClass(clazzName);
            Constructor<?> cns = clazz.getDeclaredConstructor(Map.class);
            Map<String, String> subCfg = new HashMap<>();
            for (Map.Entry<String, String> entry : config.entrySet()) {
                if (entry.getKey().startsWith(name + ".")) {
                    subCfg.put(entry.getKey().substring(name.length() + 1), entry.getValue());
                }
            }
            return cns.newInstance(subCfg);
        }
        return null;
    }

    void doStop() {
        if (xnioWorker != null) {
            xnioWorker.shutdown();
        }
        server.stop();
    }

    @Override
    public synchronized LifeCycle getContext(ContextModel model) {
        assertNotState(State.Unconfigured);
        return findOrCreateContext(model);
    }

    @Override
    public synchronized void removeContext(HttpContext httpContext) {
        assertNotState(State.Unconfigured);
        final Context context = contextMap.remove(httpContext);
        if (context == null) {
            throw new IllegalStateException("Cannot remove the context because it does not exist: "
                                            + httpContext);
        }
        context.destroy();
    }

    private void assertState(State state) {
        if (this.state != state) {
            throw new IllegalStateException("State is " + this.state + " but should be " + state);
        }
    }

    private void assertNotState(State state) {
        if (this.state == state) {
            throw new IllegalStateException("State should not be " + this.state);
        }
    }

    private Context findContext(final ContextModel contextModel) {
        NullArgumentException.validateNotNull(contextModel, "contextModel");
        HttpContext httpContext = contextModel.getHttpContext();
        return contextMap.get(httpContext);
    }

    private Context findOrCreateContext(final ContextModel contextModel) {
        NullArgumentException.validateNotNull(contextModel, "contextModel");
        synchronized (contextMap) {
            if (contextMap.containsKey(contextModel.getHttpContext())) {
                return contextMap.get(contextModel.getHttpContext());
            }
            Context newCtx = new Context(this, path, contextModel);
            newCtx.setConfiguration(configuration);
            newCtx.setDefaultSessionTimeoutInMinutes(defaultSessionTimeoutInMinutes);
            newCtx.setSessionPersistenceManager(sessionPersistenceManager);
            contextMap.put(contextModel.getHttpContext(), newCtx);
            final Servlet servlet = createResourceServlet(contextModel, "/", "default");
            final ResourceModel model = new ResourceModel(contextModel, servlet, "/", "default");
            try {
                newCtx.addServlet(model);
            } catch (ServletException e) {
                LOG.warn(e.getMessage(), e);
            }
            return newCtx;
        }
    }

    @Override
    public synchronized void addServlet(ServletModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.addServlet(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add servlet", e);
        }
    }

    @Override
    public void removeServlet(ServletModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findContext(model.getContextModel());
            if (context != null) {
                context.removeServlet(model);
            }
        } catch (ServletException e) {
            throw new RuntimeException("Unable to remove servlet", e);
        }
    }

    @Override
    public void addEventListener(EventListenerModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.addEventListener(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add event listener", e);
        }
    }

    @Override
    public void removeEventListener(EventListenerModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.removeEventListener(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add event listener", e);
        }
    }

    @Override
    public void addFilter(FilterModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.addFilter(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add filter", e);
        }
    }

    @Override
    public void removeFilter(FilterModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.removeFilter(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to remove filter", e);
        }
    }

    @Override
    public void addErrorPage(ErrorPageModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.addErrorPage(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add error page", e);
        }
    }

    @Override
    public void removeErrorPage(ErrorPageModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.removeErrorPage(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to remove error page", e);
        }
    }

    @Override
    public void addWelcomFiles(WelcomeFileModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.addWelcomeFile(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add welcome files", e);
        }
    }

    @Override
    public void removeWelcomeFiles(WelcomeFileModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.removeWelcomeFile(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add welcome files", e);
        }
    }

    @Override
    public Servlet createResourceServlet(ContextModel contextModel, String alias, String name) {
        final Context context = findOrCreateContext(contextModel);
        return new ResourceServlet(context, alias, name);
    }

    @Override
    public void addSecurityConstraintMapping(SecurityConstraintMappingModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.addSecurityConstraintMapping(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add welcome files", e);
        }
    }

    @Override
    public void removeSecurityConstraintMapping(SecurityConstraintMappingModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.removeSecurityConstraintMapping(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add welcome files", e);
        }
    }

    @Override
    public void addContainerInitializerModel(ContainerInitializerModel model) {
        assertNotState(State.Unconfigured);
        try {
            final Context context = findOrCreateContext(model.getContextModel());
            context.addContainerInitializerModel(model);
        } catch (ServletException e) {
            throw new RuntimeException("Unable to add welcome files", e);
        }
    }

    public Collection<? extends CRL> loadCRL(String crlPath) throws Exception {
        Collection<? extends CRL> crlList = null;

        if (crlPath != null) {
            InputStream in = null;
            try {
                in = loadResource(crlPath).openStream();
                crlList = CertificateFactory.getInstance("X.509").generateCRLs(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }

        return crlList;
    }

    @Override
    public Account verify(Account account) {
        if (identityManager != null) {
            return identityManager.verify(account);
        }
        throw new IllegalStateException("No identity manager configured");
    }

    @Override
    public Account verify(String id, Credential credential) {
        if (identityManager != null) {
            return identityManager.verify(id, credential);
        }
        throw new IllegalStateException("No identity manager configured");
    }

    @Override
    public Account verify(Credential credential) {
        if (identityManager != null) {
            return identityManager.verify(credential);
        }
        throw new IllegalStateException("No identity manager configured");
    }

    /**
     * Kind of configuration used
     */
    private enum ConfigSource {
        /** Configuration in undertow.xml */
        XML,
        /** Additional (merged with PID) configuration in undertow.properties */
        PROPERTIES,
        /** Configuration purely from Configadmin */
        PID;

        /**
         * Detect {@link ConfigSource} by the type of URL
         * @param undertowResource
         * @return
         */
        public static ConfigSource kind(URL undertowResource) {
            if (undertowResource == null) {
                return PID;
            }
            String path = undertowResource.getPath();
            if (path == null) {
                return PID;
            }
            String name = new File(path).getName();
            if (name.endsWith(".properties")) {
                return PROPERTIES;
            } else if (name.endsWith(".xml")) {
                return XML;
            }
            return PID;
        }
    }

}
