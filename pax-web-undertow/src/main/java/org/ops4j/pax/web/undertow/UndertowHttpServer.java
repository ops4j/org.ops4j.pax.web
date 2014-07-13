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

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;

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
    public void activate(ComponentContext cc) {
        pathHandler = Handlers.path();

        // get HTTP port
        String httpPortNumber = (String) cc.getProperties().get("org.osgi.service.http.port");
        if (httpPortNumber == null) {
            httpPortNumber = cc.getBundleContext().getProperty("org.osgi.service.http.port");
        }
        if (httpPortNumber == null) {
            httpPortNumber = "8181";
        }

        // start server listening on port
        server = Undertow.builder().addHttpListener(Integer.valueOf(httpPortNumber), "0.0.0.0")
            .setHandler(pathHandler).build();
        server.start();
        log.info("started HTTP server at port {}", httpPortNumber);
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
