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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.List;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerContainer;

import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.servlet.core.ContextClassLoaderSetupAction;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.websockets.WebSocketExtension;
import io.undertow.websockets.extensions.ExtensionHandshake;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.jsr.Bootstrap;
import io.undertow.websockets.jsr.ExtensionImpl;
import io.undertow.websockets.jsr.JsrWebSocketFilter;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.undertow.PaxWebUndertowExtension;
import org.ops4j.pax.web.service.undertow.UndertowSupport;
import org.ops4j.pax.web.service.undertow.configuration.model.ServletContainer;
import org.ops4j.pax.web.service.undertow.configuration.model.UndertowConfiguration;

/**
 * Not every {@link DeploymentInfo} needs websocket related attributes. But if {@code pax-web-undertow-websocket}
 * bundle is available, we have to prepare this information when the context is started.
 */
public class WebSocketsExtension implements PaxWebUndertowExtension {

	@Override
	public void handleDeployment(DeploymentInfo deploymentInfo, UndertowConfiguration configuration,
			UndertowSupport support, OsgiContextModel osgiContextModel) {
		ServletContainer.Websockets wsConfig = null;
		if (configuration != null && configuration.getSubsystem() != null
				&& configuration.getSubsystem().getServletContainer() != null) {
			wsConfig = configuration.getSubsystem().getServletContainer().getWebsockets();
		}
		WebSocketDeploymentInfo wsInfo = new WebSocketDeploymentInfo();
		if (wsConfig != null) {
			wsInfo.setDispatchToWorkerThread(wsConfig.isDispatchToWorker());
			wsInfo.setWorker(support.getWorker(wsConfig.getWorkerName()));
			wsInfo.setBuffers(support.getBufferPool(wsConfig.getBufferPoolName()));
			if (wsConfig.isPerMessageDeflate()) {
				// see org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService#createServletConfig()
				PerMessageDeflateHandshake perMessageDeflate = new PerMessageDeflateHandshake(false, wsConfig.getDeflaterLevel());
				wsInfo.addExtension(perMessageDeflate);
			}
		} else {
			wsInfo.setDispatchToWorkerThread(true);
			wsInfo.setWorker(support.getWorker("default"));
			wsInfo.setBuffers(support.getBufferPool("default"));
		}

		final List<ThreadSetupHandler> setup = new ArrayList<>();
		setup.add(new ContextClassLoaderSetupAction(deploymentInfo.getClassLoader()));
		setup.addAll(deploymentInfo.getThreadSetupActions());

		List<Extension> extensions = new ArrayList<>();
		for (ExtensionHandshake e : wsInfo.getExtensions()) {
			extensions.add(ExtensionImpl.create(new WebSocketExtension(e.getName(), Collections.emptyList())));
		}
		ServerWebSocketContainer container = new ServerWebSocketContainer(deploymentInfo.getClassIntrospecter(),
				deploymentInfo.getClassLoader(), wsInfo.getWorker(), wsInfo.getBuffers(),
				setup, wsInfo.isDispatchToWorkerThread(), null, wsInfo.getReconnectHandler(), extensions);

		// not adding any endpoints here - they'll be added in
		// org.ops4j.pax.web.service.undertow.websocket.internal.WebSocketsInitializer.onStartup
		deploymentInfo.addServletContextAttribute(ServerContainer.class.getName(), container);

		// needed for io.undertow.websockets.jsr.JsrWebSocketFilter.init(). But we don't want this attribute
		// to be found by io.undertow.websockets.jsr.Bootstrap, because we are doing the setup.
		// so please do NOT uncomment - we'll set a bit different attribute and swap it to proper one in the
		// listener itself
//		deploymentInfo.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, wsInfo);
		deploymentInfo.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME + ".paxweb", wsInfo);

		EventListener proxy = support.proxiedServletContextListener(new WebSocketListener(), osgiContextModel);
		// adding the listener to deployment info will let it register additional listeners/filters/servlets
		// to the context
		deploymentInfo.addListener(Servlets.listener(WebSocketListener.class, new ImmediateInstanceFactory<>(proxy)));
		deploymentInfo.addDeploymentCompleteListener(new WsCleanUpServletContextListener(container));
	}

	@Override
	public void cleanDeployment(DeploymentInfo deploymentInfo) {
		deploymentInfo.getServletContextAttributes().remove(ServerContainer.class.getName());
		deploymentInfo.getServletContextAttributes().remove(WebSocketDeploymentInfo.ATTRIBUTE_NAME + ".paxweb");
		deploymentInfo.getListeners().removeIf(li -> li.getListenerClass() == WebSocketListener.class);
		deploymentInfo.getDeploymentCompleteListeners().removeIf(dcl -> dcl.getClass() == WsCleanUpServletContextListener.class);
	}

	private static final class WebSocketListener implements ServletContextListener {

		private ServerWebSocketContainer container;

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			// how to trick Undertow...
			ServletContext servletContext = sce.getServletContext();

			Object wsInfo = servletContext.getAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME + ".paxweb");
			servletContext.setAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, wsInfo);

			// the remaining part as in io.undertow.websockets.jsr.Bootstrap
			container = (ServerWebSocketContainer) servletContext.getAttribute(ServerContainer.class.getName());
			FilterRegistration.Dynamic filter = servletContext.addFilter(Bootstrap.FILTER_NAME, JsrWebSocketFilter.class);
			servletContext.addListener(JsrWebSocketFilter.LogoutListener.class);
			filter.setAsyncSupported(true);
			filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			container.close();
		}
	}

	private static class WsCleanUpServletContextListener implements ServletContextListener {
		private final ServerWebSocketContainer container;

		WsCleanUpServletContextListener(ServerWebSocketContainer container) {
			this.container = container;
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			container.validateDeployment();
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
		}
	}

}
