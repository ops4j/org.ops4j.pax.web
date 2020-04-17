/*
 * Copyright 2008 Alin Dreghiciu.
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

import org.ops4j.pax.web.service.whiteboard.JspMapping;

/**
 * Tracks {@link JspMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, March 14, 2008
 */
public class JspMappingTracker /*extends AbstractElementTracker<JspMapping, JspWebElement>*/ {

	/**
	 * Constructor.
	 *
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
//	private JspMappingTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
//		super(extenderContext, bundleContext);
//	}
//
//	public static ServiceTracker<JspMapping, JspWebElement> createTracker(final ExtenderContext extenderContext,
//			final BundleContext bundleContext) {
//		return new JspMappingTracker(extenderContext, bundleContext).create(JspMapping.class);
//	}
//
//	/**
//	 * @see AbstractElementTracker#createWebElement(ServiceReference, Object)
//	 */
//	@Override
//	JspWebElement createWebElement(final ServiceReference<JspMapping> serviceReference, final JspMapping published) {
//		return new JspWebElement(serviceReference, published);
//	}

//	@Override
//	public void register(final WebContainer webContainer, final HttpContext httpContext) throws Exception {
////		webContainer.registerJsps(
////					jspMapping.getUrlPatterns(),
////					DictionaryUtils.adapt(jspMapping.getInitParams()),
////					httpContext);
//	}
//
//	@Override
//	public void unregister(final WebContainer webContainer, final HttpContext httpContext) {
////			webContainer.unregisterJsps(
////					jspMapping.getUrlPatterns(), httpContext);
//	}

}