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
package org.ops4j.pax.web.service.whiteboard;

/**
 * <p><em>WebSocket mapping</em> contains all the information required to register a WebSocket. Because the
 * WebSockets (JSR-356) don't have to implement any specific interface or extend any specific class, we have much
 * more flexibility and responsibility here.</p>
 *
 * <p>On purpose, we don't allow registration of other objects that usually can be passed (by means of
 * {@link jakarta.servlet.annotation.HandlesTypes} annotation on a {@link jakarta.servlet.ServletContainerInitializer}
 * related to WebSockets) by users. Only annontated classes or actual instances are handled and we don't support:<ul>
 *     <li>{@code javax.websocket.server.ServerApplicationConfig}</li>
 *     <li>{@code javax.websocket.Endpoint}</li>
 * </ul></p>
 */
public interface WebSocketMapping extends ContextRelated {

	/**
	 * Returns a {@link Class} of the WebSocket endpoint that should be annotated with
	 * {@code @javax.websocket.server.ServerEndpoint} annotation. If both the
	 * object ({@link #getWebSocketAnnotatedEndpoint()}) and the class is specified, the class takes precedence.
	 * @return
	 */
	Class<?> getWebSocketClass();

	/**
	 * Returns an instance of a class annotated with {@code @javax.websocket.server.ServerEndpoint}. If both the
	 * object and the class ({@link #getWebSocketClass()}) is specified, the class takes precedence.
	 * @return
	 */
	Object getWebSocketAnnotatedEndpoint();

}
