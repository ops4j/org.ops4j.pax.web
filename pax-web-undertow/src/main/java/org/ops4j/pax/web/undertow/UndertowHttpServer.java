/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.web.undertow;

import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_ENABLED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_PORT;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_SECURE_ENABLED;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_HTTP_SECURE_PORT;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.JBossLoggingAccessLogReceiver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.ops4j.pax.web.undertow.ssl.SslContextFactory;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = UndertowHttpServer.class, configurationPid = "org.ops4j.pax.web.undertow")
public class UndertowHttpServer {

    private static Logger log = LoggerFactory.getLogger(UndertowHttpServer.class);

    private Undertow server;

    private PathHandler pathHandler;
    
    private HttpHandler rootHandler;
    
    private Map<String, PathHandler> virtualHostMap = new HashMap<>();

    @Activate
    public void activate(ComponentContext cc) throws IOException {
        Builder builder = Undertow.builder();
        pathHandler = Handlers.path();

        PropertyResolver resolver = buildPropertyResolver(cc);

        boolean httpEnabled = Boolean.parseBoolean(resolver.get(PROPERTY_HTTP_ENABLED));
        boolean httpsEnabled = Boolean.parseBoolean(resolver.get(PROPERTY_HTTP_SECURE_ENABLED));
        boolean accessLogEnabled = Boolean.parseBoolean(resolver.get("org.ops4j.pax.web.undertow.accesslog.enabled"));
        Integer httpPort = null;
        Integer httpsPort = null;

        if (httpEnabled) {

            // get HTTP port
            String httpPortNumber = resolver.get(PROPERTY_HTTP_PORT);
            httpPort = Integer.valueOf(httpPortNumber);

            builder.addHttpListener(httpPort, "0.0.0.0");
        }

        if (httpsEnabled) {

            // get HTTPS port
            String httpsPortNumber = resolver.get(PROPERTY_HTTP_SECURE_PORT);
            httpsPort = Integer.valueOf(httpsPortNumber);

            SslContextFactory sslContextFactory = new SslContextFactory(resolver);
            SSLContext sslContext = sslContextFactory.createSslContext();
            builder.addHttpsListener(httpsPort, "0.0.0.0", sslContext);
        }
        
        rootHandler = pathHandler;
        configureVirtualsHosts(resolver);
        
        if (accessLogEnabled) {
            String format = resolver.get("org.ops4j.pax.undertow.accesslog.format");
            rootHandler = new AccessLogHandler(pathHandler, new JBossLoggingAccessLogReceiver(),
                format, AccessLogHandler.class.getClassLoader());
        }
                
        server = builder.setHandler(rootHandler).build();
        log.info("starting Undertow server");
        server.start();
    }

    private void configureVirtualsHosts(PropertyResolver resolver) {
        String hostsConfig = resolver.get("org.ops4j.pax.web.hosts");
        if (hostsConfig == null) {
            return;
        }
        String[] hosts = hostsConfig.trim().split(",\\s*");
        for (String host : hosts) {
            configureVirtualHost(host, resolver);
        }
    }

    private void configureVirtualHost(String host, PropertyResolver resolver) {
        String aliasesConfig = resolver.get(String.format("org.ops4j.pax.web.host.%s.aliases", host));
        if (aliasesConfig == null) {
            return;
        }
        String[] aliases = aliasesConfig.trim().split(",\\s*");
        PathHandler hostPathHandler = Handlers.path();
        virtualHostMap.put(host, hostPathHandler);
        rootHandler = Handlers.virtualHost(pathHandler, hostPathHandler, aliases);
    }

    private PropertyResolver buildPropertyResolver(ComponentContext cc) throws IOException {
        Properties defaultProperties = new Properties();
        defaultProperties.load(UndertowHttpServer.class.getResourceAsStream("/default.properties"));

        PropertyResolver defaultResolver = new PropertiesPropertyResolver(defaultProperties);
        PropertyResolver fallbackResolver = new PropertiesPropertyResolver(System.getProperties(),
            defaultResolver);
        PropertyResolver resolver = new DictionaryPropertyResolver(cc.getProperties(),
            fallbackResolver);
        return resolver;
    }

    @Deactivate
    public void deactivate(ComponentContext cc) {
        server.stop();
    }

    public Undertow getServer() {
        return server;
    }

    public PathHandler getPathHandler() {
        return pathHandler;
    }
    
    public  PathHandler findPathHandlerForHost(String virtualHostId) {
        if (virtualHostId == null) {
            return pathHandler;
        }
        return virtualHostMap.get(virtualHostId);
    }
}
