/*
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2007 Damian Golda.
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

import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardContext;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.whiteboard.ResourceMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks {@link ResourceMapping}s.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ResourceMappingTracker extends AbstractMappingTracker<ResourceMapping, Servlet, ServletEventData, ServletModel> {

	private ResourceMappingTracker(final WhiteboardContext whiteboardContext, final BundleContext bundleContext) {
		super(whiteboardContext, bundleContext);
	}

	public static ServiceTracker<ResourceMapping, ServletModel> createTracker(final WhiteboardContext whiteboardContext,
			final BundleContext bundleContext) {
		return new ResourceMappingTracker(whiteboardContext, bundleContext).create(ResourceMapping.class);
	}

	@Override
	protected ServletModel doCreateElementModel(Bundle bundle, ResourceMapping service, Integer rank, Long serviceId) {
		// pass everything to a handy builder - there's no servlet/servletSupplier/servletReference/servletClass
		// provided, which will trigger a call to ServerController.createResourceServlet()
		ServletModel.Builder builder = new ServletModel.Builder()
				.withServiceRankAndId(rank, serviceId)
				.withRegisteringBundle(bundle)
				.withAlias(service.getAlias())
				.withUrlPatterns(service.getUrlPatterns())
				.withRawPath(service.getPath()) // could be file: or a chroot inside a bundle - we'll check and validate later
				.withLoadOnStartup(1)
				.withAsyncSupported(true)
				.resourceServlet(true);

		return builder.build();
	}

//	/**
//	 * @see AbstractElementTracker#createWebElement(ServiceReference, Object)
//	 */
//	@Override
//	ResourceMappingWebElement createWebElement(final ServiceReference<ResourceMapping> serviceReference,
//			final ResourceMapping published) {
//		return new ResourceMappingWebElement(serviceReference, published);
//	}
//	@Override
//	public void register(final WebContainer webContainer,
//						 final HttpContext httpContext) throws Exception {
//		webContainer.registerResources(
//				resourceMapping.getAlias(),
//				resourceMapping.getPath(),
//				httpContext);
//	}
//
//	@Override
//	public void unregister(final WebContainer webContainer,
//						   final HttpContext httpContext) {
//		webContainer.unregister(resourceMapping.getAlias());
//	}

}
