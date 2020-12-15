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

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;


/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWebSocketIntegrationTest extends AbstractContainerTestBase {

//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		initWebListener();
//		waitForWebListener();
//	}
//
//	@Test
//	public void testWebsocketWebapp() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Chatroom'",
//						resp -> resp.contains("Chatroom"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/websocket/index.html");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.doGETandExecuteTest("http://127.0.0.1:8181/websocket/resource/js/jquery-1.10.2.min.js");
//	}
//
//	@Test
//	public void testWebsocket() throws Exception {
//        WebSocketContainer cc = getWebSocketContainer();
//		URI uri = new URI("ws://127.0.0.1:8181/websocket/chat/scala");
//		SimpleChatSocket socket = new SimpleChatSocket();
//        Session session = cc.connectToServer(socket, uri);
//
//		socket.awaitClose(5, TimeUnit.SECONDS);
//        session.close();
//
//		JsonObject obj = Json.createReader(new
//				StringReader(socket.getAnswer()))
//				.readObject();
//
//		assertThat(obj.getString("message"), equalTo("test"));
//		assertThat(obj.getString("sender"), equalTo("me"));
//	}
//
//	abstract protected WebSocketContainer getWebSocketContainer();
//
//	@ClientEndpoint
//	public class SimpleChatSocket {
//
//		private final CountDownLatch closeLatch = new CountDownLatch(1);
//		private String answer;
//		private Session session;
//
//		@OnOpen
//		public void onConnect(Session session) throws Exception {
//			System.out.printf("Got connect: %s%n",session);
//			session.getBasicRemote()
//					.sendText("{\"message\":\"test\", \"sender\":\"me\"}");
//			this.session = session;
//		}
//
//		@OnMessage
//		public void onMessage(final String message) {
//			System.out.printf("Message received: %s", message);
//			this.answer = message;
//			// close the connection once a message is received
//			try {
//				session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Done"));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//		@OnClose
//		public void onClose(CloseReason reason) {
//			System.out.printf("Connection closed: %d - %s%n", reason.getCloseCode().getCode(),
//					reason.getReasonPhrase());
//			this.closeLatch.countDown();
//		}
//
//		boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
//			return this.closeLatch.await(duration,unit);
//		}
//
//		String getAnswer() {
//			return answer;
//		}
//	}
}