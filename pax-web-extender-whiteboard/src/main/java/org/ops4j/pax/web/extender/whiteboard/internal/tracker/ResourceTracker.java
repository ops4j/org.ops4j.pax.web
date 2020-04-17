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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks resource (service) registrations.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ResourceTracker /*extends AbstractElementTracker<Object, ResourceWebElement>*/ {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ResourceTracker.class);

	/**
	 * Constructor.
	 *
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
//	private ResourceTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
//		super(extenderContext, bundleContext);
//	}
//
//	public static ServiceTracker<Object, ResourceWebElement> createTracker(final ExtenderContext extenderContext,
//			final BundleContext bundleContext) {
//		return new ResourceTracker(extenderContext, bundleContext).create("(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX + "=*)");
//	}
//
//	/**
//	 * @see AbstractElementTracker#createElementModel(ServiceReference, Object)
//	 */
//	@Override
//	ResourceWebElement createWebElement(final ServiceReference<Object> serviceReference, final Object published) {
//
//		String[] resourcePattern = ServicePropertiesUtils.getArrayOfStringProperty(serviceReference,
//				HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN);
//		String prefix = ServicePropertiesUtils.getStringProperty(serviceReference,
//				HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);
//
//		if (resourcePattern != null && prefix != null) {
//
//			String httpContextId = ServicePropertiesUtils.extractHttpContextId(serviceReference);
//
//			final DefaultResourceMapping mapping = new DefaultResourceMapping();
//			mapping.setHttpContextId(httpContextId);
//
//			mapping.setAlias(resourcePattern[0]); // TODO: make sure multiple
//													// patterns are supported
//			mapping.setPath(prefix);
//
//			return new ResourceWebElement(serviceReference, mapping);
//		} else {
//			return null;
//		}
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
