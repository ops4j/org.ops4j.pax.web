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
package org.ops4j.pax.web.itest.utils.web;

import java.util.ArrayList;
import java.util.List;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint(value = "/simple")
public class SimpleWebSocket {

	public static final Logger LOG = LoggerFactory.getLogger(SimpleWebSocket.class);

	List<String> messages = new ArrayList<>();

	@OnOpen
	public void onWebSocketConnect(Session sess) {
		System.out.println("Socket Connected: " + sess);
	}

	@OnMessage
	public String onWebSocketText(String message) {
		System.out.println("Received TEXT message: " + message);
		messages.add(message);
		return "I got \"" + message + "\"";
	}

	@OnClose
	public void onWebSocketClose(CloseReason reason) {
		System.out.println("Socket Closed: " + reason);
	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace(System.err);
	}

	public List<String> getMessages() {
		return messages;
	}

}
