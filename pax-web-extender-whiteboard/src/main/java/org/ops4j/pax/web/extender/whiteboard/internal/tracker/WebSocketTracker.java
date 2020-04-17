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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketTracker /*extends AbstractElementTracker<Object, WebSocketElement>*/ {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketTracker.class);

	private BundleContext bundleContext;

//	WebSocketTracker(ExtenderContext extenderContext, BundleContext bundleContext) {
//		super(extenderContext, bundleContext);
//		this.bundleContext = bundleContext;
//	}
//
//	public static ServiceTracker<Object, WebSocketElement> createTracker(final ExtenderContext extenderContext,
//			final BundleContext bundleContext) {
//		return new WebSocketTracker(extenderContext, bundleContext).create("(&(objectClass=" + Object.class.getName() + ")(" + ExtenderConstants.WEBSOCKET + "=true))");
//	}
//
//	@Override
//	WebSocketElement createWebElement(ServiceReference<Object> serviceReference, Object published) {
//
//		// FIXME Validation for WebSockets done here rather than in WebElement
//		// because we're tracking Object
//
//		if (Endpoint.class.isAssignableFrom(published.getClass())) {
//			LOG.warn(
//					"WebSockets created as instances of Endpoint isn't supported, because it requires also to register ServerApplicationConfig");
//			return null;
//		}
//
//		ServerEndpoint serverEndpoint = published.getClass().getAnnotation(ServerEndpoint.class);
//		if (serverEndpoint == null) {
//			return null;
//		}
//
//		LOG.info("found websocket endpoint!!");
//
//		WebSocketMapping mapping = new DefaultWebSocketMapping();
//		mapping.setHttpContextId(ServicePropertiesUtils.extractHttpContextId(serviceReference));
//		mapping.setWebSocket(published);
//		return new WebSocketElement(serviceReference, mapping);
//	}

//	@Override
//	public void register(WebContainer webContainer, HttpContext httpContext) throws Exception {
////		webContainer.registerWebSocket(mapping.getWebSocket(), httpContext);
//	}
//
//	@Override
//	public void unregister(WebContainer webContainer, HttpContext httpContext) {
////		webContainer.unregisterWebSocket(mapping.getWebSocket(), httpContext);
//	}

}
