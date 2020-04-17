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

/**
 * Tracks {@link org.ops4j.pax.web.service.whiteboard.ErrorPageMapping}.
 *
 * @author Dmitry Sklyut
 * @since 0.7.0
 */
public class ErrorPageMappingTracker /*extends AbstractElementTracker<ErrorPageMapping, ErrorPageWebElement>*/ {

	/**
	 * Constructor.
	 *
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
//	private ErrorPageMappingTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
//		super(extenderContext, bundleContext);
//	}
//
//	public static ServiceTracker<ErrorPageMapping, ErrorPageWebElement> createTracker(
//			final ExtenderContext extenderContext, final BundleContext bundleContext) {
//		return new ErrorPageMappingTracker(extenderContext, bundleContext).create(ErrorPageMapping.class);
//	}
//
//	/**
//	 * @see AbstractElementTracker#createWebElement(org.osgi.framework.ServiceReference
//	 *      , Object)
//	 */
//	@Override
//	ErrorPageWebElement createWebElement(final ServiceReference<ErrorPageMapping> serviceReference,
//			final ErrorPageMapping published) {
//		return new ErrorPageWebElement(serviceReference, published);
//	}

//	@Override
//	public void register(WebContainer webContainer, HttpContext httpContext)
//			throws Exception {
////			webContainer.registerErrorPage(
////					errorPageMapping.getError(),
////					errorPageMapping.getLocation(), httpContext);
//	}
//
//	@Override
//	public void unregister(WebContainer webContainer, HttpContext httpContext) {
////			webContainer.unregisterErrorPage(
////					errorPageMapping.getError(), httpContext);
//	}
}
