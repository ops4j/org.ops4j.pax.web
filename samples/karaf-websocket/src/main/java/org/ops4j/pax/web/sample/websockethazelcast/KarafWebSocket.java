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
package org.ops4j.pax.web.sample.websockethazelcast;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * @author Jimmy Pannier
 */
@WebSocket
public class KarafWebSocket {

	@OnWebSocketConnect
	public void onOpen(Session session) {
		KarafWebSocketActivator.registerConnection(session);
	}

	@OnWebSocketClose
	public void onClose(Session session, int statusCode, String reason) {
		KarafWebSocketActivator.unregisterConnection(session);
	}

	@OnWebSocketMessage
	public void onText(Session session, String message) {
		KarafWebSocketActivator.onMessage(message, session);
	}
}
