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
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.ServerController;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;

/**
 * @author Guillaume Nodet
 */
public class ServerControllerImpl implements ServerController, IdentityManager {

    private enum State {
        Unconfigured,
        Stopped,
        Started
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServerControllerImpl.class);

    private Configuration configuration;
    private final Set<ServerListener> listeners = new CopyOnWriteArraySet<>();
    private State state = State.Unconfigured;

    private IdentityManager identityManager;
    private final PathHandler path = Handlers.path();
    private Undertow server;

    private final ConcurrentMap<HttpContext, Context> contextMap = new ConcurrentHashMap<>();

    public ServerControllerImpl() {
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
            state = State.Stopped;
            notifyListeners(ServerEvent.CONFIGURED);
            break;
        case Started:
            doStop();
            doStart();
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

    void doStart() {
        Undertow.Builder builder = Undertow.builder();

        HttpHandler rootHandler = path;

        // PAXWEB-193 suggested we should open this up for external
        // configuration
        URL undertowResource = configuration.getConfigurationURL();
        if (undertowResource == null) {
            undertowResource = getClass().getResource("/undertow.properties");
        }
        if (undertowResource != null) {
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

                /*
                 * String listeners = config.get("listeners"); if (listeners != null) { String[] names =
                 * listeners.split("(, )+"); for (String name : names) { String type = config.get("listeners."
                 * + name + ".type"); String address = config.get("listeners." + name + ".address"); String
                 * port = config.get("listeners." + name + ".port"); if ("http".equals(type)) {
                 * builder.addHttpListener(Integer.parseInt(port), address); } } }
                 */

            } catch (Exception e) {
                LOG.error("Exception while starting Undertow", e);
                throw new RuntimeException("Exception while starting Undertow", e);
            }
        }

        if (configuration.isLogNCSAFormatEnabled()) {

            String logNCSADirectory = configuration.getLogNCSADirectory();
            String logNCSAFormat = configuration.getLogNCSAFormat();

            Bundle bundle = FrameworkUtil.getBundle(ServerControllerImpl.class);
            ClassLoader loader = bundle.adapt(BundleWiring.class).getClassLoader();
            XnioWorker worker = UndertowUtil.createWorker(loader);

            // String logNameSuffix = logNCSAFormat.substring(logNCSAFormat.lastIndexOf("."));
            // String logBaseName = logNCSAFormat.substring(0, logNCSAFormat.lastIndexOf("."));

            AccessLogReceiver logReceiver = DefaultAccessLogReceiver.builder().setLogWriteExecutor(worker)
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

            rootHandler = new AccessLogHandler(path, logReceiver, format,
                                               AccessLogHandler.class.getClassLoader());
        }

        for (String address : configuration.getListeningAddresses()) {
            if (configuration.isHttpEnabled()) {
                LOG.info("Starting undertow http listener on " + address + ":" + configuration.getHttpPort());
                builder.addHttpListener(configuration.getHttpPort(), address);
            }
            if (configuration.isHttpSecureEnabled()) {
                try {
                    URL keyStorePath = loadResource(configuration.getSslKeystore());
                    KeyStore keyStore = getKeyStore(keyStorePath,
                                                    configuration.getSslKeystoreType() != null
                                                        ? configuration.getSslKeystoreType() : "JKS",
                                                    configuration.getSslKeyPassword());

                    String _keyManagerFactoryAlgorithm = Security
                        .getProperty("ssl.KeyManagerFactory.algorithm") == null
                            ? KeyManagerFactory.getDefaultAlgorithm()
                            : Security.getProperty("ssl.KeyManagerFactory.algorithm");
                    String _keyManagerPassword = configuration.getSslPassword();
                    String _keyStorePassword = configuration.getSslKeyPassword();
                    KeyManagerFactory keyManagerFactory = KeyManagerFactory
                        .getInstance(_keyManagerFactoryAlgorithm);
                    keyManagerFactory.init(keyStore,
                                           _keyManagerPassword == null
                                               ? (_keyStorePassword == null
                                                   ? null : _keyStorePassword.toCharArray())
                                               : _keyManagerPassword.toCharArray());
                    KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

                    TrustManager[] trustManagers = null;
                    String _secureRandomAlgorithm = null;
                    SecureRandom random = (_secureRandomAlgorithm == null)
                        ? null : SecureRandom.getInstance(_secureRandomAlgorithm);
                    if (configuration.getTrustStore() != null) {
                        URL trustStorePath = loadResource(configuration.getTrustStore());
                        KeyStore trustStore = getKeyStore(trustStorePath,
                                                          configuration.getTrustStoreType() != null
                                                              ? configuration.getTrustStoreType() : "JKS",
                                                          configuration.getTrustStorePassword());

                        String _trustManagerFactoryAlgorithm = Security
                            .getProperty("ssl.TrustManagerFactory.algorithm") == null
                                ? TrustManagerFactory.getDefaultAlgorithm()
                                : Security.getProperty("ssl.TrustManagerFactory.algorithm");

                        
                        Collection<? extends CRL> crls = configuration.getCrlPath() == null
                            ? null : loadCRL(configuration.getCrlPath());
                        String certAlias = configuration.getSslKeyAlias();
                        if (configuration.isValidateCerts() && keyStore != null) {
                            if (certAlias == null) {
                                List<String> aliases = Collections.list(keyStore.aliases());
                                certAlias = aliases.size() == 1 ? aliases.get(0) : null;
                            }

                            Certificate cert = certAlias == null ? null : keyStore.getCertificate(certAlias);
                            if (cert == null) {
                                throw new Exception("No certificate found in the keystore"
                                                    + (certAlias == null ? "" : " for alias " + certAlias));
                            }

                            CertificateValidator validator = new CertificateValidator(trustStore, crls);
                            validator.setEnableCRLDP(configuration.isEnableCRLDP());
                            validator.setEnableOCSP(configuration.isEnableOCSP());
                            validator.setOcspResponderURL(configuration.getOcspResponderURL());
                            validator.validate(keyStore, cert);
                        }

                        if (trustStore != null) {
                            // Revocation checking is only supported for PKIX algorithm
                            if (configuration.isValidatePeerCerts()
                                && _trustManagerFactoryAlgorithm.equalsIgnoreCase("PKIX")) {
                                PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore,
                                                                                           new X509CertSelector());

                                // Make sure revocation checking is enabled
                                pbParams.setRevocationEnabled(true);

                                if (crls != null && !crls.isEmpty()) {
                                    pbParams.addCertStore(CertStore
                                        .getInstance("Collection", new CollectionCertStoreParameters(crls)));
                                }

                                if (configuration.isEnableCRLDP()) {
                                    // Enable Certificate Revocation List Distribution Points (CRLDP) support
                                    System.setProperty("com.sun.security.enableCRLDP", "true");
                                }

                                if (configuration.isEnableOCSP()) {
                                    // Enable On-Line Certificate Status Protocol (OCSP) support
                                    Security.setProperty("ocsp.enable", "true");

                                    if (configuration.getOcspResponderURL() != null) {
                                        // Override location of OCSP Responder
                                        Security.setProperty("ocsp.responderURL",
                                                             configuration.getOcspResponderURL());
                                    }
                                }

                                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                                    .getInstance(_trustManagerFactoryAlgorithm);
                                trustManagerFactory.init(new CertPathTrustManagerParameters(pbParams));

                                trustManagers = trustManagerFactory.getTrustManagers();
                            } else {
                                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                                    .getInstance(_trustManagerFactoryAlgorithm);
                                trustManagerFactory.init(trustStore);

                                trustManagers = trustManagerFactory.getTrustManagers();
                            }
                        }
                    }
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(keyManagers, trustManagers, random);

                    LOG.info("Starting undertow https listener on " + address + ":"
                             + configuration.getHttpSecurePort());
                    builder.addHttpsListener(configuration.getHttpSecurePort(), address, context);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to build SSL context", e);
                }
            }
        }

        builder.setHandler(rootHandler);
        server = builder.build();
        server.start();
    }

    private URL loadResource(String resource) throws MalformedURLException {
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
        try (InputStream is = storePath.openStream()) {
            keystore.load(is, storePassword.toCharArray());
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
}
