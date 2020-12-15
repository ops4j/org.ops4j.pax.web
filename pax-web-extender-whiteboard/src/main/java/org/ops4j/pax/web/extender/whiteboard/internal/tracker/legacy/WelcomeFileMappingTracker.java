/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.model.events.WelcomeFileEventData;
import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks {@link org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping} services.
 *
 * @author Dmitry Sklyut
 * @author Grzegorz Grzybek
 * @since 0.7.0
 */
public class WelcomeFileMappingTracker extends AbstractMappingTracker<WelcomeFileMapping, WelcomeFileMapping, WelcomeFileEventData, WelcomeFileModel> {

	protected WelcomeFileMappingTracker(WhiteboardExtenderContext whiteboardExtenderContext, BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<WelcomeFileMapping, WelcomeFileModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new WelcomeFileMappingTracker(whiteboardExtenderContext, bundleContext).create(WelcomeFileMapping.class);
	}

	@Override
	protected WelcomeFileModel doCreateElementModel(Bundle bundle, WelcomeFileMapping service, Integer rank, Long serviceId) {
		WelcomeFileModel welcomeFileModel = new WelcomeFileModel(service.getWelcomeFiles(), service.isRedirect());
		welcomeFileModel.setRegisteringBundle(bundle);
		welcomeFileModel.setServiceRank(rank);
		welcomeFileModel.setServiceId(serviceId);
		return welcomeFileModel;
	}

}
