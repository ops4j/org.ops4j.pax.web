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

public class DefaultWebSocketMapping implements WebSocketMapping {

	private String httpContextId;
	private Boolean extractSharedHttpContext;
	private Object webSocket;

	@Override
	public String getHttpContextId() {
		return httpContextId;
	}

	@Override
	public void setHttpContextId(String httpContextId) {
		this.httpContextId = httpContextId;
	}

	@Override
	public Boolean getSharedContext() {
		return extractSharedHttpContext;
	}

	@Override
	public void setSharedContext(Boolean extractSharedHttpContext) {
		this.extractSharedHttpContext = extractSharedHttpContext;
	}

	@Override
	public Object getWebSocket() {
		return webSocket;
	}

	@Override
	public void setWebSocket(Object published) {
		this.webSocket = published;
	}

}
