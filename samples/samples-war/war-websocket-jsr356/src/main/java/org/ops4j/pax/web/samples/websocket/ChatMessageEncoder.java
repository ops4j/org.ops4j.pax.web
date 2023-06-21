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
import java.io.StringWriter;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class ChatMessageEncoder implements Encoder.Text<ChatMessage> {

	@Override
	public void init(final EndpointConfig config) {
	}

	@Override
	public void destroy() {
	}

	@Override
	public String encode(final ChatMessage chatMessage) throws EncodeException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			StringWriter w = new StringWriter();
			JsonGenerator generator = mapper.createGenerator(w);
			ObjectNode tree = new ObjectNode(mapper.getNodeFactory());
			tree.set("message", new TextNode(chatMessage.getMessage()));
			tree.set("sender", new TextNode(chatMessage.getSender()));
			tree.set("received", new TextNode(chatMessage.getReceived().toString()));
			generator.writeTree(tree);
			return w.toString();
		} catch (IOException e) {
			e.printStackTrace();
			throw new EncodeException(chatMessage, e.getMessage(), e);
		}
	}

}
