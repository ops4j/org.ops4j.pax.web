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
package org.ops4j.pax.web.service.undertow.internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import javax.servlet.http.HttpServlet;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class EmbeddedUndertowWebSocketsTest {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedUndertowWebSocketsTest.class);

	@Test
	public void webSockets() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		ServletInfo servlet = Servlets.servlet("default", DefaultServlet.class, new ImmediateInstanceFactory<HttpServlet>(new DefaultServlet()));
		servlet.addMapping("/");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
//				.addServletExtension(new Bootstrap())
				.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME,
						new WebSocketDeploymentInfo()
								.addEndpoint(ServerEndpointConfig.Builder.create(MyEndpoint.class, "/endpoint1")
										.configurator(new ServerEndpointConfig.Configurator() {
											@Override
											public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
												return super.getEndpointInstance(endpointClass);
											}
										})
										.build()))
				.addServlets(servlet);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		ServiceLoader<ContainerProvider> providers = ServiceLoader.load(ContainerProvider.class);
		ContainerProvider provider = providers.iterator().next();
		assertNotNull(provider);

		WebSocketContainer wsc = ContainerProvider.getWebSocketContainer();
		assertNotNull(wsc);

		// we can add more endpoints only before io.undertow.websockets.jsr.JsrWebSocketFilter.init() is called
		ServerContainer sc = (ServerContainer) dm.getDeployment().getServletContext().getAttribute(ServerContainer.class.getName());
		assertThat(sc.getClass().getName(), equalTo("io.undertow.websockets.jsr.ServerWebSocketContainer"));
		sc.addEndpoint(MyAnnotatedEndpoint.class);

		ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
				.configurator(new ClientEndpointConfig.Configurator() {
					@Override
					public void beforeRequest(Map<String, List<String>> headers) {
						super.beforeRequest(headers);
					}
				})
				.build();
		config.getUserProperties().put(ServerWebSocketContainer.TIMEOUT, 3600); // seconds
		wsc.connectToServer(new MyClientEndpoint("c1"), config,
				URI.create("ws://localhost:" + port + "/endpoint1"));

		// this will cause java.lang.IllegalStateException: UT003017: Cannot add endpoint after deployment
//		ServerContainer sc = (ServerContainer) dm.getDeployment().getServletContext().getAttribute(ServerContainer.class.getName());
//		sc.addEndpoint(MyAnnotatedEndpoint.class);

		wsc.connectToServer(new MyClientEndpoint("c2"), config,
				URI.create("ws://localhost:" + port + "/endpoint2"));

		Thread.sleep(200);

		server.stop();
	}

	public static final class MyEndpoint extends Endpoint {

		@Override
		public void onOpen(Session session, EndpointConfig config) {
			// By implementing the onOpen method, the programmatic endpoint gains access to the Session object,
			// to which the developer may add MessageHandler implementations in order to intercept incoming websocket
			// messages.
			System.out.println("[s1] Session opened: " + session + ", with config: " + config);
			session.addMessageHandler(String.class, message -> {
				RemoteEndpoint.Basic remote = session.getBasicRemote();
				System.out.println("[s1] Received whole message: " + message);
				try {
					remote.sendText("I got \"" + message + "\" from you!");
				} catch (IOException e) {
					LOG.warn(e.getMessage());
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
			session.addMessageHandler(String.class, message -> {
				System.out.println("[" + client + "] Received whole message: " + message);
				try {
					remote.sendText("I got \"" + message + "\" from you!");
					session.close();
				} catch (IOException e) {
					LOG.warn(e.getMessage());
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
