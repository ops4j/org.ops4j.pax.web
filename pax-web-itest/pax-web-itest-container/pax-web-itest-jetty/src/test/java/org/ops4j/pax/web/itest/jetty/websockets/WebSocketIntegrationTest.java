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
package org.ops4j.pax.web.itest.jetty.websockets;

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
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebSocketIntegrationTest extends AbstractContainerTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(WebSocketIntegrationTest.class);

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		Option[] websocketOptions = combine(serverOptions, jettyWebSockets());
		return combine(websocketOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
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

	@Test
	public void testWebsocket() throws Exception {
		WebSocketContainer cc = getWebSocketContainer();
		URI uri = new URI("ws://127.0.0.1:8181/websocket/chat/scala");
		SimpleChatSocket socket = new SimpleChatSocket();
		Session session = cc.connectToServer(socket, uri);

		socket.awaitClose(5, TimeUnit.SECONDS);
		session.close();

		String anwer = socket.getAnswer();

		assertTrue(anwer.contains("test"));
		assertTrue(anwer.contains("me"));
	}

	protected WebSocketContainer getWebSocketContainer() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Bundle jettyWebSocketClientBundle = bundle("org.eclipse.jetty.websocket.javax.websocket");
			Thread.currentThread().setContextClassLoader(jettyWebSocketClientBundle.adapt(BundleWiring.class).getClassLoader());
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

		void awaitClose(int duration, TimeUnit unit) throws InterruptedException {
			this.closeLatch.await(duration, unit);
		}

		String getAnswer() {
			return answer;
		}
	}

}
