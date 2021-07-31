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
package org.ops4j.pax.web.extender.whiteboard.runtime;


import org.ops4j.pax.web.service.whiteboard.WebSocketMapping;

public class DefaultWebSocketMapping extends AbstractContextRelated implements WebSocketMapping {

	private Object webSocketAnnotatedEndpoint;
	private Class<?> webSocketClass;

	@Override
	public Object getWebSocketAnnotatedEndpoint() {
		return webSocketAnnotatedEndpoint;
	}

	public void setWebSocketAnnotatedEndpoint(Object webSocketAnnotatedEndpoint) {
		this.webSocketAnnotatedEndpoint = webSocketAnnotatedEndpoint;
	}

	@Override
	public Class<?> getWebSocketClass() {
		return webSocketClass;
	}

	public void setWebSocketClass(Class<?> webSocketClass) {
		this.webSocketClass = webSocketClass;
	}

	@Override
	public String toString() {
		return "DefaultWebSocketMapping{"
				+ "webSocketAnnotatedEndpoint=" + webSocketAnnotatedEndpoint
				+ ", webSocketClass=" + webSocketClass + '}';
	}

}
