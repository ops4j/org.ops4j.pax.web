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
package org.ops4j.pax.web.websocket.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModelAware;
import org.ops4j.pax.web.service.spi.model.elements.WebSocketModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ServletContainerInitializer} that can use existing
 * {@link org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel} and
 * {@link org.ops4j.pax.web.service.spi.model.elements.WebSocketModel} to register WebSocket endpoints in previously
 * configured {@link javax.websocket.server.ServerContainer}.
 */
public class PaxWebWebSocketsServletContainerInitializer implements ServletContainerInitializer,
		ContainerInitializerModelAware {

	public static final Logger LOG = LoggerFactory.getLogger(PaxWebWebSocketsServletContainerInitializer.class);

	private ContainerInitializerModel model;

	@Override
	public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
		ServerContainer wsContainer = (ServerContainer) ctx.getAttribute(ServerContainer.class.getName());

		if (wsContainer == null) {
			LOG.warn("No javax.websocket.server.ServerContainer available in servlet context." +
					" Skipping WebSocket registration.");
			return;
		}
		if (model == null) {
			LOG.warn("[dev error] No ContainerInitializerModel available with WebSocket endpoints to register.");
			return;
		}

		// just as we do it manually in org.ops4j.pax.web.service.undertow.websocket.internal.WebSocketsInitializer
		// because Undertow doesn't have own SCI for WebSocket registration, here we have to do very similar job
		// but not using the classes based on @HandlesTypes. We will use the classes and actual endpoints from the
		// associated WebSocketModels

		List<Class<?>> annotatedEndpointClasses = new ArrayList<>();
		Map<WebSocketModel, Object> annotatedEndpointInstances = new LinkedHashMap<>();

		for (WebSocketModel wsm : model.getRelatedWebSocketModels()) {
			// check the class at the end, because it's set for all sources
			if (wsm.getWebSocketEndpoint() != null) {
				annotatedEndpointInstances.put(wsm, wsm.getWebSocketEndpoint());
			} else if (wsm.getElementSupplier() != null) {
				annotatedEndpointInstances.put(wsm, wsm.getElementSupplier().get());
			} else if (wsm.getElementReference() != null) {
				// Not using ServiceObjects even if this service may be prototype-scoped. 1) Whiteboard
				// specification doesn't say anything about WebSockets, 2) WebSockets are managed via SCIs, which
				// (without special tricks/proxies) have only onStartup() methods.
				// So we rely on implicit ungetService() called when the bundle is stopped
				Object endpoint = wsm.getRegisteringBundle().getBundleContext().getService(wsm.getElementReference());
				annotatedEndpointInstances.put(wsm, endpoint);
			} else {
				// assume it's the class
				annotatedEndpointClasses.add(wsm.getWebSocketEndpointClass());
			}
		}

		// easier annotated endpoints
		if (!annotatedEndpointClasses.isEmpty()) {
			for (Class<?> clazz : annotatedEndpointClasses) {
				try {
					wsContainer.addEndpoint(clazz);
				} catch (DeploymentException e) {
					LOG.error("Problem deploying annontated Web Socket endpoint {}: {}", clazz, e.getMessage(), e);
				}
			}
		}

		if (!annotatedEndpointInstances.isEmpty()) {
			// we have to prepare ONE javax.websocket.server.ServerEndpointConfig object for each
			// WebSocket endpoint instance
			for (Map.Entry<WebSocketModel, Object> e : annotatedEndpointInstances.entrySet()) {
				WebSocketModel wsm = e.getKey();
				Object instance = e.getValue();

				ServerEndpointConfig config = new DynamicEndpointConfig(wsm, instance);

				try {
					wsContainer.addEndpoint(config);
				} catch (DeploymentException ex) {
					LOG.error("Problem deploying Web Socket endpoint {}: {}", model, ex.getMessage(), ex);
				}
			}
		}
	}

	@Override
	public void setContainerInitializerModel(ContainerInitializerModel model) {
		this.model = model;
	}

	@Override
	public ContainerInitializerModel getContainerInitializerModel() {
		return model;
	}

	private static class DynamicEndpointConfig implements ServerEndpointConfig {
		private final WebSocketModel wsm;
		private final Object instance;

		// this map has to be kept here. For example, Tomcat keeps here an important
		// org.apache.tomcat.websocket.pojo.Constants.POJO_METHOD_MAPPING_KEY mapping
		private final Map<String, Object> userProperties = new HashMap<>();

		DynamicEndpointConfig(WebSocketModel wsm, Object instance) {
			this.wsm = wsm;
			this.instance = instance;
		}

		@Override
		public Class<?> getEndpointClass() {
			return wsm.getWebSocketEndpointClassResolved();
		}

		@Override
		public String getPath() {
			return wsm.getMappedPath();
		}

		@Override
		public List<String> getSubprotocols() {
			return Arrays.asList(wsm.getSubprotocols());
		}

		@Override
		public List<Extension> getExtensions() {
			return Collections.emptyList();
		}

		@Override
		public Configurator getConfigurator() {
			// we can't use the one configured in annotation - we have to replace it with our own
			// so we actually return proper instance
			return new Configurator() {
				@Override
				public <T> T getEndpointInstance(Class<T> endpointClass) {
					return endpointClass.cast(instance);
				}
			};
		}

		@Override
		public List<Class<? extends Encoder>> getEncoders() {
			return Arrays.asList(wsm.getEncoderClasses());
		}

		@Override
		public List<Class<? extends Decoder>> getDecoders() {
			return Arrays.asList(wsm.getDecoderClasses());
		}

		@Override
		public Map<String, Object> getUserProperties() {
			return userProperties;
		}
	}

}
