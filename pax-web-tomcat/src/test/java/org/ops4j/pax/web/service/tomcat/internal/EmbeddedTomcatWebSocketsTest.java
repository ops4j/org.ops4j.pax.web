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
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
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

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.websocket.Constants;
import org.apache.tomcat.websocket.server.WsSci;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class EmbeddedTomcatWebSocketsTest {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedTomcatWebSocketsTest.class);

	@BeforeClass
	public static void initClass() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@AfterClass
	public static void cleanupClass() {
		SLF4JBridgeHandler.uninstall();
	}

	@Test
	public void webSockets() throws Exception {
		// even if in theory Service could be running without server, it is required by different components, e.g.,
		// connectors that want to access utility thread pool
		Server server = new StandardServer();
		File catalinaBase = new File("target/tomcat-websockets");
		FileUtils.deleteDirectory(catalinaBase);
		catalinaBase.mkdirs();
		server.setCatalinaBase(catalinaBase);

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(0);
		connector.setProperty("address", "127.0.0.1");
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Context context = new StandardContext();
		context.setName("");
		context.setPath("");
		context.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				context.setConfigured(true);
			}
		});
		host.addChild(context);

		Set<Class<?>> classes = new HashSet<>();
		classes.add(MyServerApplicationConfig.class);
		classes.add(MyAnnotatedEndpoint.class);
		context.addServletContainerInitializer(new WsSci(), classes);

		// any servlet is needed in order for org.apache.tomcat.websocket.server.WsFilter to work
		// otherwise simple 404 is returned for ws:// requests
		Wrapper wrapper = new StandardWrapper();
		wrapper.setServlet(new DefaultServlet());
		wrapper.setName("default");
		context.addChild(wrapper);
		context.addServletMappingDecoded("/", wrapper.getName(), false);

		server.start();

		LOG.info("Local port after start: {}", connector.getLocalPort());

		// javax.websocket.Endpoint - A logical websocket endpoint to handle endpoint's lifecycle endpoints
		// There is one instance per application per VM of the Endpoint class to represent the logical endpoint
		// per connected peer (unless special configuration is specified by
		// javax.websocket.server.ServerEndpointConfig.Configurator.getEndpointInstance()) implementation
		//
		// javax.websocket.Session - models the sequence of interactions between an endpoint and each of its peers
		// the interaction begins with onOpen() method called on an Endpoint
		//
		// javax.websocket.MessageHandler - is registered on a Session object to handle messages between open and
		// close events. There may be only one MessageHandler per type of message (text, binary, pong)
		// The API forces implementations to get the MessageHandler’s type parameter in runtime, so it's
		// not possible to pass lambdas
		//
		// javax.websocket.RemoteEndpoint - models a peer of a Session for an Endpoint
		//
		// javax.websocket.WebSocketContainer - websocket implementation used to deploy endpoints. In server
		// deployments, there's one instance per application per VM. In client deployments, an instance is
		// obtained using ServiceLocator and javax.websocket.ContainerProvider.class service
		// (in Tomcat it's org.apache.tomcat.websocket.WsContainerProvider)

		// coniguration (chapter 3) includes:
		// - uri mapping
		// - subprotocols
		// - extensions
		// - encoders/decoders

		ServiceLoader<ContainerProvider> providers = ServiceLoader.load(ContainerProvider.class);
		ContainerProvider provider = providers.iterator().next();
		assertNotNull(provider);

		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		assertNotNull(container);

		container.setAsyncSendTimeout(3600000L);
		ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
				.configurator(new ClientEndpointConfig.Configurator() {
					@Override
					public void beforeRequest(Map<String, List<String>> headers) {
						super.beforeRequest(headers);
					}
				})
				.build();
		config.getUserProperties().put(Constants.IO_TIMEOUT_MS_PROPERTY, "3600000");
		container.connectToServer(new MyClientEndpoint("c1"), config,
				URI.create("ws://localhost:" + connector.getLocalPort() + "/endpoint1"));

		ServerContainer sc = (ServerContainer) context.getServletContext().getAttribute(ServerContainer.class.getName());
		assertThat(sc.getClass().getName(), equalTo("org.apache.tomcat.websocket.server.WsServerContainer"));
		sc.addEndpoint(MyAnnotatedEndpoint.class);

		container.connectToServer(new MyClientEndpoint("c2"), config,
				URI.create("ws://localhost:" + connector.getLocalPort() + "/endpoint2"));

		Thread.sleep(200);

		server.stop();
		server.destroy();
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
