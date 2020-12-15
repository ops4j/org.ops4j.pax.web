/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy;

import java.util.EventListener;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.events.EventListenerEventData;
import org.ops4j.pax.web.service.whiteboard.ListenerMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks {@link ListenerMapping}s.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ListenerMappingTracker extends AbstractMappingTracker<ListenerMapping, EventListener, EventListenerEventData, EventListenerModel> {

	protected ListenerMappingTracker(WhiteboardExtenderContext whiteboardExtenderContext, BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<ListenerMapping, EventListenerModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new ListenerMappingTracker(whiteboardExtenderContext, bundleContext).create(ListenerMapping.class);
	}

	@Override
	protected EventListenerModel doCreateElementModel(Bundle bundle, ListenerMapping service, Integer rank, Long serviceId) {
		EventListenerModel model = new EventListenerModel(service.getListener());
		model.setRegisteringBundle(bundle);
		model.setServiceRank(rank);
		model.setServiceId(serviceId);
		return model;
	}

}
