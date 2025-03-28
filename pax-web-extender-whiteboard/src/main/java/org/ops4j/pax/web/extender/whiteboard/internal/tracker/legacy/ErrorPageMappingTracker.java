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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.events.ErrorPageEventData;
import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks {@link org.ops4j.pax.web.service.whiteboard.ErrorPageMapping}.
 *
 * @author Dmitry Sklyut
 * @since 0.7.0
 */
public class ErrorPageMappingTracker extends AbstractMappingTracker<ErrorPageMapping, ErrorPageMapping, ErrorPageEventData, ErrorPageModel> {

	protected ErrorPageMappingTracker(WhiteboardExtenderContext whiteboardExtenderContext, BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<ErrorPageMapping, ErrorPageModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new ErrorPageMappingTracker(whiteboardExtenderContext, bundleContext).create(ErrorPageMapping.class);
	}

	@Override
	protected ErrorPageModel doCreateElementModel(Bundle bundle, ErrorPageMapping service, Integer rank, Long serviceId) {
		ErrorPageModel errorPageModel = new ErrorPageModel(service.getErrors(), service.getLocation());
		errorPageModel.setName(errorPageModel.getId());
		errorPageModel.setRegisteringBundle(bundle);
		errorPageModel.setServiceRank(rank);
		errorPageModel.setServiceId(serviceId);
		return errorPageModel;
	}

}
