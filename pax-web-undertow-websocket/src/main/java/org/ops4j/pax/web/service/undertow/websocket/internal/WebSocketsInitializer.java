/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.undertow.websocket.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An SCI which performs similar role to {@link io.undertow.websockets.jsr.Bootstrap#handleDeployment}, however the
 * endpoints (and configs) are not taken from {@link io.undertow.websockets.jsr.WebSocketDeploymentInfo} but directly
 * from the classes passed to {@link ServletContainerInitializer#onStartup}.
 */
@HandlesTypes({ Endpoint.class, ServerApplicationConfig.class, ServerEndpoint.class })
public class WebSocketsInitializer implements ServletContainerInitializer {

    public static final Logger LOG = LoggerFactory.getLogger(WebSocketsInitializer.class);

    @Override
    @SuppressWarnings("unchecked")
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        ServerContainer wsContainer = (ServerContainer) ctx.getAttribute(ServerContainer.class.getName());

        if (wsContainer == null) {
            LOG.warn("[dev error] No javax.websocket.server.ServerContainer available in servlet context." +
                    " Skipping WebSocket registration.");
            return;
        }

        // inspired by Tomcat's org.apache.tomcat.websocket.server.WsSci and
        // Jetty's org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer

        Set<Class<?>> annotatedEndpointClasses = new HashSet<>();
        Set<Class<? extends Endpoint>> conigurableEndpoints = new HashSet<>();
        Set<ServerApplicationConfig> configs = new HashSet<>();

        for (Class<?> potentialEndpointClass : c) {
            // skip some classes without checking
            int modifiers = potentialEndpointClass.getModifiers();
            if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
                continue;
            }
            if (potentialEndpointClass.getPackage().getName().startsWith("javax.")) {
                continue;
            }
            if (Endpoint.class.isAssignableFrom(potentialEndpointClass)) {
                // a class to be processed by javax.websocket.server.ServerApplicationConfig
                conigurableEndpoints.add((Class<? extends Endpoint>) potentialEndpointClass);
            }
            if (ServerApplicationConfig.class.isAssignableFrom(potentialEndpointClass)) {
                // a class that processes javax.websocket.Endpoints
                try {
                    configs.add((ServerApplicationConfig) potentialEndpointClass.getConstructor().newInstance());
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                         InvocationTargetException e) {
                    LOG.warn("Problem instantiating potentialEndpointClass {}: {}. Skipping", potentialEndpointClass,
                            e.getMessage(), e);
                }
            }
            if (potentialEndpointClass.isAnnotationPresent(ServerEndpoint.class)) {
                annotatedEndpointClasses.add(potentialEndpointClass);
            }
        }

        Set<ServerEndpointConfig> coniguredServerEndpoints = new HashSet<>();
        Set<Class<?>> configuredAnnotatedEndpointClasses = new HashSet<>();

        if (configs.isEmpty()) {
            // no javax.websocket.server.ServerApplicationConfigs available - consider only annotated endpoints
            configuredAnnotatedEndpointClasses.addAll(annotatedEndpointClasses);
        } else {
            for (ServerApplicationConfig config : configs) {
                Set<ServerEndpointConfig> configuredEndpoints = config.getEndpointConfigs(conigurableEndpoints);
                if (configuredEndpoints != null) {
                    coniguredServerEndpoints.addAll(configuredEndpoints);
                }
                Set<Class<?>> configuredEndpointClasses = config.getAnnotatedEndpointClasses(annotatedEndpointClasses);
                if (configuredEndpointClasses != null) {
                    configuredAnnotatedEndpointClasses.addAll(configuredEndpointClasses);
                }
            }
        }

        // and finally actual deployment
        try {
            // Deploy endpoints
            for (ServerEndpointConfig config : coniguredServerEndpoints) {
                wsContainer.addEndpoint(config);
            }
            // Deploy POJOs
            for (Class<?> clazz : configuredAnnotatedEndpointClasses) {
                wsContainer.addEndpoint(clazz);
            }
        } catch (DeploymentException e) {
            throw new ServletException(e);
        }
    }

}
