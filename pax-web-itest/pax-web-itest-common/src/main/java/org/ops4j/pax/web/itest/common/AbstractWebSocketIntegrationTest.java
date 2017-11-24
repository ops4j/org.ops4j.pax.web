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
package org.ops4j.pax.web.itest.common;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.StringReader;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.BundleException;


/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWebSocketIntegrationTest extends ITestBase {

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		waitForWebListener();
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
	@Ignore (value = "PAXWEB-1027")
	public void testWebsocket() throws Exception {

		WebSocketClient client = new WebSocketClient();
		SimpleChatSocket socket = new SimpleChatSocket();
		URI uri = new URI("ws://0.0.0.0:8181/websocket/chat/scala");

		client.start();
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		client.connect(socket, uri, request);
		socket.awaitClose(5, TimeUnit.SECONDS);
		client.stop();

		JsonObject obj = Json.createReader(new
				StringReader(socket.getAnswer()))
				.readObject();

		assertThat(obj.getString("message"), equalTo("test"));
		assertThat(obj.getString("sender"), equalTo("me"));
	}

	@WebSocket
	public class SimpleChatSocket {

		private final CountDownLatch closeLatch = new CountDownLatch(1);
		private String answer;

		@OnWebSocketConnect
		public void onConnect(Session session) throws Exception {
			System.out.printf("Got connect: %s%n",session);
			session.getRemote()
					.sendStringByFuture("{'message':'test', 'sender':'me'}")
					.get(2, TimeUnit.SECONDS);
			session.close(StatusCode.NORMAL, "Done");
		}

		@OnWebSocketMessage
		public void onMessage(final String session, final String message){
			System.out.printf("Message received: %s", message);
			this.answer = message;
		}

		@OnWebSocketClose
		public void onClose(int statusCode, String reason){
			System.out.printf("Connection closed: %d - %s%n", statusCode,
					reason);
			this.closeLatch.countDown();
		}

		boolean awaitClose(int duration, TimeUnit unit) throws
				InterruptedException
		{
			return this.closeLatch.await(duration,unit);
		}

		String getAnswer() {
			return answer;
		}
	}
}