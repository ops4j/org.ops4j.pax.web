/*
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2007 Damian Golda.
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

import org.ops4j.pax.web.service.whiteboard.ResourceMapping;

/**
 * Tracks {@link ResourceMapping}s.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ResourceMappingTracker /*extends AbstractElementTracker<ResourceMapping, ResourceMappingWebElement>*/ {

	/**
	 * Constructor.
	 *
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
//	private ResourceMappingTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
//		super(extenderContext, bundleContext);
//	}
//
//	public static ServiceTracker<ResourceMapping, ResourceMappingWebElement> createTracker(
//			final ExtenderContext extenderContext, final BundleContext bundleContext) {
//		return new ResourceMappingTracker(extenderContext, bundleContext).create(ResourceMapping.class);
//	}
//
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
