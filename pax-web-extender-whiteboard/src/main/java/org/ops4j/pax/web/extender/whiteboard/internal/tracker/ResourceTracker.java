/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import jakarta.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.runtime.dto.DTOConstants;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Collection;

/**
 * Tracks OSGi services that should result in registration of a {@link jakarta.servlet.Servlet} acting as
 * <em>default (resource) servlet - though mapped to any URL pattern(s).</em>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public class ResourceTracker extends AbstractElementTracker<Object, Servlet, ServletEventData, ServletModel> {

	private ResourceTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<Object, ServletModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new ResourceTracker(whiteboardExtenderContext, bundleContext)
				.create(String.format("(%s=*)", HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN));
	}

	@Override
	protected ServletModel createElementModel(ServiceReference<Object> serviceReference, Integer rank, Long serviceId) {
		log.debug("Creating resource model from R7 whiteboard service {} (id={})", serviceReference, serviceId);

		// URL patterns
		boolean propertiesInvalid = false;
		String[] urlPatterns = Utils.getPaxWebProperty(serviceReference,
				null, HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN,
				Utils::asStringArray);
		Object v = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN);
		if (v != null && !(v instanceof String || v instanceof String[] || v instanceof Collection)) {
			propertiesInvalid = true;
		}

		// prefix
		String path = Utils.getStringProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);
		if (path == null || "".equals(path.trim())) {
			path = "/";
		}

		// pass everything to a handy builder - there's no servlet/servletSupplier/servletReference/servletClass
		// provided, which will trigger a call to ServerController.createResourceServlet()
		ServletModel.Builder builder = new ServletModel.Builder()
				.withServiceRankAndId(rank, serviceId)
				.withRegisteringBundle(serviceReference.getBundle())
				.withUrlPatterns(urlPatterns)
				.withLoadOnStartup(1)
				.withAsyncSupported(true)
				.withRawPath(path) // could be file: or a chroot inside a bundle - we'll check and validate later
				.resourceServlet(true);

		ServletModel model = builder.build();
		if (propertiesInvalid) {
			model.setDtoFailureCode(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}
		return model;
	}

}
