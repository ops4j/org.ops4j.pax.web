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
import java.util.Dictionary;
import java.util.Hashtable;
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
import org.ops4j.pax.web.itest.utils.web.SimpleWebSocket;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.spi.model.events.WebSocketEventData;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWebSocketWhiteBoardIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWebSocketWhiteBoardIntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		configureAndWaitForListener(8181);
	}

	@Test
	public void testWebsocket() throws Exception {
		SimpleWebSocket simpleWebSocket = new SimpleWebSocket();

		Dictionary<String, Object> filter = new Hashtable<>();
		filter.put(PaxWebConstants.SERVICE_PROPERTY_WEBSOCKET, "true");
		configureAndWait(() -> {
			context.registerService(Object.class.getName(), simpleWebSocket, filter);
		}, events -> events.stream()
				.anyMatch(ev -> ev.getType() == WebElementEvent.State.DEPLOYED
						&& (ev.getData() instanceof WebSocketEventData)));

		WebSocketContainer cc = getWebSocketContainer();
		URI uri = new URI("ws://127.0.0.1:8181/simple");
		WebSocketClient client = new WebSocketClient();
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Bundle webSocketClientBundle = bundle(getContainerSpecificWebSocketsBundleSN());
			Thread.currentThread().setContextClassLoader(webSocketClientBundle.adapt(BundleWiring.class).getClassLoader());
			Session session = cc.connectToServer(client, uri);

			client.awaitClose(5, TimeUnit.SECONDS);
			session.close();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}

		String anwer = client.getAnswer();

		assertThat(anwer, equalTo("I got \"Hello\""));
		assertThat(simpleWebSocket.getMessages().size(), equalTo(1));
		assertThat(simpleWebSocket.getMessages().get(0), equalTo("Hello"));
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

	protected abstract String getContainerSpecificWebSocketsBundleSN();


	@ClientEndpoint
	public static class WebSocketClient {

		private final CountDownLatch closeLatch = new CountDownLatch(1);
		private String answer;
		private Session session;

		@OnOpen
		public void onConnect(Session session) throws Exception {
			LOG.info("Got connect: {}", session);
			session.getBasicRemote().sendText("Hello");
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
