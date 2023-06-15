/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer ;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EmbeddedJettyWebSocketsTest {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedJettyWebSocketsTest.class);

	@Test
	public void webSockets() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] {connector});

		ContextHandlerCollection chc = new ContextHandlerCollection();

		ServletContextHandler sch = new ServletContextHandler(null, "/", ServletContextHandler.NO_SESSIONS);
		sch.setAllowNullPathInfo(true);
		sch.addServlet(DefaultServlet.class, "/");

		chc.addHandler(sch);

		server.setHandler(chc);

		Set<Class<?>> classes = new HashSet<>();
		classes.add(MyServerApplicationConfig.class);
		classes.add(MyAnnotatedEndpoint.class);
		// org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer is from
		// org.eclipse.jetty.ee10.websocket/jetty-ee10-websocket-jakarta-server
		sch.addServletContainerInitializer(new JakartaWebSocketServletContainerInitializer(), classes.toArray(new Class[0]));

		server.start();

		int port = connector.getLocalPort();

		ServiceLoader<ContainerProvider> providers = ServiceLoader.load(ContainerProvider.class);
		ContainerProvider provider = providers.iterator().next();
		assertNotNull(provider);

		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		assertNotNull(container);

		ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
				.configurator(new ClientEndpointConfig.Configurator() {
					@Override
					public void beforeRequest(Map<String, List<String>> headers) {
						super.beforeRequest(headers);
					}
				})
				.build();
		container.connectToServer(new MyClientEndpoint("c1"), config,
				URI.create("ws://localhost:" + port + "/endpoint1"));

		ServerContainer sc = (ServerContainer) sch.getServletContext().getAttribute(ServerContainer.class.getName());
		assertThat(sc.getClass().getName()).isEqualTo("org.eclipse.jetty.ee10.websocket.jakarta.server.JakartaWebSocketServerContainer");
		sc.addEndpoint(MyAnnotatedEndpoint.class);

		container.connectToServer(new MyClientEndpoint("c2"), config,
				URI.create("ws://localhost:" + port + "/endpoint2"));

		Thread.sleep(200);

		server.stop();
		server.join();
	}

	public static final class MyEndpoint extends Endpoint {

		@Override
		public void onOpen(Session session, EndpointConfig config) {
			// By implementing the onOpen method, the programmatic endpoint gains access to the Session object,
			// to which the developer may add MessageHandler implementations in order to intercept incoming websocket
			// messages.
			System.out.println("[s1] Session opened: " + session + ", with config: " + config);
			session.addMessageHandler(new MessageHandler.Whole<String>() {
				@Override
				public void onMessage(String message) {
					RemoteEndpoint.Basic remote = session.getBasicRemote();
					System.out.println("[s1] Received whole message: " + message);
					try {
						remote.sendText("I got \"" + message + "\" from you!");
					} catch (IOException e) {
						LOG.warn(e.getMessage());
					}
				}
			});
		}
	}

	public static final class MyClientEndpoint extends Endpoint {

		private final String client;

		public MyClientEndpoint(String client) {
			this.client = client;
		}

		@Override
		public void onOpen(Session session, EndpointConfig config) {
			// By implementing the onOpen method, the programmatic endpoint gains access to the Session object,
			// to which the developer may add MessageHandler implementations in order to intercept incoming websocket
			// messages.
			System.out.println("[" + client + "] Session opened: " + session + ", with config: " + config);
//			session.getUserProperties().put(Constants.IO_TIMEOUT_MS_PROPERTY, "3600000");
			RemoteEndpoint.Basic remote = session.getBasicRemote();
			session.addMessageHandler(new MessageHandler.Whole<String>() {
				@Override
				public void onMessage(String message) {
					System.out.println("[" + client + "] Received whole message: " + message);
					try {
						remote.sendText("I got \"" + message + "\" from you!");
						session.close();
					} catch (IOException e) {
						LOG.warn(e.getMessage());
					}
				}
			});
			try {
				remote.sendText("Hello from client!");
			} catch (IOException e) {
				LOG.warn(e.getMessage());
			}
		}
	}

	public static final class MyServerApplicationConfig implements ServerApplicationConfig {

		@Override
		public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
			HashSet<ServerEndpointConfig> set = new HashSet<>();
			set.add(ServerEndpointConfig.Builder.create(MyEndpoint.class, "/endpoint1")
					.configurator(new ServerEndpointConfig.Configurator() {
						@Override
						public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
							return super.getEndpointInstance(endpointClass);
						}
					})
					.build());
			return set;
		}

		@Override
		public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
			return Collections.emptySet();
		}
	}

	@ServerEndpoint("/endpoint2")
	public static final class MyAnnotatedEndpoint {

		@OnOpen
		public void handle(Session session, EndpointConfig config) {
			System.out.println("[s2] Session opened: " + session + ", with config: " + config);
		}

		@OnMessage
		public String handle(String wholeMessage) {
			System.out.println("[s2] Received whole message: " + wholeMessage);
			return "I got \"" + wholeMessage + "\" from you!";
		}
	}

}
