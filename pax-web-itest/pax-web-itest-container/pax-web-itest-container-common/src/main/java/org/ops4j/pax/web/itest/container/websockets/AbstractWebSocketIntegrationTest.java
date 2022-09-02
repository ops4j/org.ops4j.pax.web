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
package org.ops4j.pax.web.itest.container.websockets;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWebSocketIntegrationTest extends AbstractContainerTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractWebSocketIntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		// Jetty uses org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer SCI that handles:
		//  - classes implementing javax.websocket.server.ServerApplicationConfig
		//  - classes implementing javax.websocket.Endpoint
		//  - classes annotated with @javax.websocket.server.ServerEndpoint
		// Tomcat uses org.apache.tomcat.websocket.server.WsSci SCI that handles:
		//  - classes implementing javax.websocket.server.ServerApplicationConfig
		//  - classes implementing javax.websocket.Endpoint
		//  - classes annotated with @javax.websocket.server.ServerEndpoint
		// Undertow doesn't provide any special SCI. When web sockets are configured, Wildfly calls
		// io.undertow.servlet.api.DeploymentInfo.addServletContextAttribute() with
		// "io.undertow.websockets.jsr.WebSocketDeploymentInfo" as attribute name and an instance of
		// io.undertow.websockets.jsr.WebSocketDeploymentInfo as the value. All necessary information
		// is prepared using org.wildfly.extension.undertow.deployment.UndertowJSRWebSocketDeploymentProcessor#deploy()

		configureAndWaitForDeploymentUnlessInstalled("war-websocket-jsr356", () -> {
			installAndStartBundle(sampleWarURI("war-websocket-jsr356"));
		});
	}

	@Test
	public void testWebsocketWebapp() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Chatroom'",
						resp -> resp.contains("Chatroom"))
				.doGETandExecuteTest("http://127.0.0.1:8181/websocket/index.html");

		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8181/websocket/resource/js/jquery-1.10.2.min.js");
	}

	protected abstract String getContainerSpecificWebSocketsBundleSN();

	@Test
	public void testWebsocket() throws Exception {
		WebSocketContainer cc = getWebSocketContainer();
		URI uri = new URI("ws://127.0.0.1:8181/websocket/chat/scala");
		SimpleChatSocket socket = new SimpleChatSocket();
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Bundle webSocketClientBundle = bundle(getContainerSpecificWebSocketsBundleSN());
			Thread.currentThread().setContextClassLoader(webSocketClientBundle.adapt(BundleWiring.class).getClassLoader());
			Session session = cc.connectToServer(socket, uri);

			socket.awaitClose(5, TimeUnit.SECONDS);
			session.close();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}

		String anwer = socket.getAnswer();

		assertTrue(anwer.contains("test"));
		assertTrue(anwer.contains("me"));
	}

	protected WebSocketContainer getWebSocketContainer() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Bundle webSocketClientBundle = bundle(getContainerSpecificWebSocketsBundleSN());
			Thread.currentThread().setContextClassLoader(webSocketClientBundle.adapt(BundleWiring.class).getClassLoader());
			return ContainerProvider.getWebSocketContainer();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@ClientEndpoint
	public static class SimpleChatSocket {

		private final CountDownLatch closeLatch = new CountDownLatch(1);
		private String answer;
		private Session session;

		@OnOpen
		public void onConnect(Session session) throws Exception {
			LOG.info("Got connect: {}", session);
			session.getBasicRemote().sendText("{\"message\":\"test\", \"sender\":\"me\"}");
			this.session = session;
		}

		@OnMessage
		public void onMessage(final String message) {
			LOG.info("Message received: {}", message);
			this.answer = message;
			// close the connection once a message is received
			try {
				session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Done"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@OnClose
		public void onClose(CloseReason reason) {
			LOG.info("Connection closed: {} - {}", reason.getCloseCode().getCode(), reason.getReasonPhrase());
			this.closeLatch.countDown();
		}

		public void awaitClose(int duration, TimeUnit unit) throws InterruptedException {
			this.closeLatch.await(duration, unit);
		}

		public String getAnswer() {
			return answer;
		}
	}

}
