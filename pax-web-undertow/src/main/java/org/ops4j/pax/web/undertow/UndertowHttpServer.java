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
import io.undertow.server.handlers.PathHandler;

import java.io.IOException;
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

    @Activate
    public void activate(ComponentContext cc) throws IOException {
        Builder builder = Undertow.builder();
        pathHandler = Handlers.path();

        PropertyResolver resolver = buildPropertyResolver(cc);

        boolean httpEnabled = Boolean.parseBoolean(resolver.get(PROPERTY_HTTP_ENABLED));
        boolean httpsEnabled = Boolean.parseBoolean(resolver.get(PROPERTY_HTTP_SECURE_ENABLED));
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

        server = builder.setHandler(pathHandler).build();
        log.info("starting Undertow server");
        server.start();
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
}
