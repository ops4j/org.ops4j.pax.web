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
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.WebSocketMapping;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * Registers/unregisters {@link WebSocketMapping} with {@link WebContainer}.
 *
 * @since 6.0.0
 */
public class WebSocketElement extends WebElement {

	private WebSocketMapping mapping;

	/**
	 * Constructs a new WebSocketElement
	 * @param ref the service-reference behind the registered http-whiteboard-service
	 * @param mapping WebSocketMapping containing all necessary information
	 */
	public WebSocketElement(ServiceReference<Object> ref, WebSocketMapping mapping) {
		super(ref);
		NullArgumentException.validateNotNull(mapping, "Websocket mapping");
		this.mapping = mapping;

		// validate
		// FIXME validation
	}

	@Override
	public void register(WebContainer webContainer, HttpContext httpContext) throws Exception {
//		webContainer.registerWebSocket(mapping.getWebSocket(), httpContext);
	}

	@Override
	public void unregister(WebContainer webContainer, HttpContext httpContext) {
//		webContainer.unregisterWebSocket(mapping.getWebSocket(), httpContext);
	}

	@Override
	public String getHttpContextId() {
		return mapping.getHttpContextId();
	}
}
