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

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.elements.WebSocketModel;
import org.ops4j.pax.web.service.spi.model.events.WebSocketEventData;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class WebSocketTracker extends AbstractElementTracker<Object, Object, WebSocketEventData, WebSocketModel> {

	private WebSocketTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	@SuppressWarnings("deprecation")
	public static ServiceTracker<Object, WebSocketModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {

		String filter = "(|(" + PaxWebConstants.SERVICE_PROPERTY_WEBSOCKET + "=true)(" +
				PaxWebConstants.SERVICE_PROPERTY_WEBSOCKET_LEGACY + "=true))";

		return new WebSocketTracker(whiteboardExtenderContext, bundleContext).create(filter);
	}

	@Override
	protected WebSocketModel createElementModel(ServiceReference<Object> serviceReference, Integer rank, Long serviceId) {
		WebSocketModel model = new WebSocketModel();
		model.setRegisteringBundle(serviceReference.getBundle());
		model.setElementReference(serviceReference);
		model.setServiceRank(rank);
		model.setServiceId(serviceId);
		return model;
	}

}
