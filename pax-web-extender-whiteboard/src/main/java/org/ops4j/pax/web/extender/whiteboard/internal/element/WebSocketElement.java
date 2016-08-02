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
package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.WebSocketMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

public class WebSocketElement implements WebElement {

	private WebSocketMapping mapping;

	public WebSocketElement(WebSocketMapping mapping) {
		NullArgumentException.validateNotNull(mapping, "Websocket mapping");
		this.mapping = mapping;
	}

	@Override
	public void register(HttpService httpService, HttpContext httpContext) throws Exception {
		if (WebContainerUtils.isWebContainer(httpService)) {
			((WebContainer) httpService).registerWebSocket(mapping.getWebSocket(), httpContext);
		} else {
			throw new UnsupportedOperationException(
					"Internal error: In use HttpService is not an WebContainer (from Pax Web)");
		}
	}

	@Override
	public void unregister(HttpService httpService, HttpContext httpContext) {
		if (WebContainerUtils.isWebContainer(httpService)) {
			((WebContainer) httpService).unregisterWebSocket(mapping.getWebSocket(), httpContext);
		}
	}

	@Override
	public String getHttpContextId() {
		return mapping.getHttpContextId();
	}

}
