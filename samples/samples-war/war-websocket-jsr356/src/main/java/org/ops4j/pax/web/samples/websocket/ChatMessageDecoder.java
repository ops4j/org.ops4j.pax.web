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
package org.ops4j.pax.web.samples.websocket;

import java.io.IOException;
import java.util.Date;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatMessageDecoder implements Decoder.Text<ChatMessage> {

	@Override
	public void init(final EndpointConfig config) {
	}

	@Override
	public void destroy() {
	}

	@Override
	public ChatMessage decode(final String textMessage) throws DecodeException {
		try {
			JsonNode tree = new ObjectMapper().createParser(textMessage).readValueAsTree();
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setMessage(tree.get("message").asText());
			chatMessage.setSender(tree.get("sender").asText());
			chatMessage.setReceived(new Date());
			return chatMessage;
		} catch (IOException e) {
			throw new DecodeException(textMessage, e.getMessage(), e);
		}
	}

	@Override
	public boolean willDecode(final String s) {
		return true;
	}

}
