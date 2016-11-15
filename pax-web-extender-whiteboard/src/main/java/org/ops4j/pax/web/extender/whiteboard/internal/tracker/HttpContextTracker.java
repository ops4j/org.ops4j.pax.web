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

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtendedHttpServiceRuntime;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.HttpContextElement;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.ops4j.pax.web.service.SharedWebContainerContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link HttpContext}s.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, August 21, 2007
 */
public class HttpContextTracker extends AbstractHttpContextTracker<HttpContext> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(HttpContextTracker.class);

	/**
	 * Constructor.
	 *  @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 * @param httpServiceRuntime
	 */
	private HttpContextTracker(final ExtenderContext extenderContext,
							   final BundleContext bundleContext,
							   final ExtendedHttpServiceRuntime httpServiceRuntime) {
		super(extenderContext, bundleContext, httpServiceRuntime);
	}

	public static ServiceTracker<HttpContext, HttpContextElement> createTracker(final ExtenderContext extenderContext,
																				final BundleContext bundleContext,
																				final ExtendedHttpServiceRuntime httpServiceRuntime) {
		return new HttpContextTracker(extenderContext, bundleContext, httpServiceRuntime).create(HttpContext.class);
	}

	/**
	 * @see AbstractHttpContextTracker#createHttpContextElement(ServiceReference,
	 *      Object)
	 */
	@Override
	HttpContextElement createHttpContextElement(final ServiceReference<HttpContext> serviceReference,
												final HttpContext published) {
		Object httpContextId = serviceReference.getProperty(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID);
		Object httpContextShared = serviceReference.getProperty(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED);


		final DefaultHttpContextMapping mapping = new DefaultHttpContextMapping();
		mapping.setHttpContextId((String) httpContextId);

		Boolean sharedContext = httpContextShared != null ? Boolean.valueOf((String) httpContextShared) : false;

		if (!sharedContext && published instanceof SharedWebContainerContext) {
			sharedContext = true; // in case it's a shared HttpContext make sure
									// the flag ist set.
		} else if (sharedContext && !(published instanceof SharedWebContainerContext)) {
			sharedContext = false; // this shouldn't happen but make sure it
									// doesn't
		}

		mapping.setHttpContextShared(sharedContext);
		mapping.setHttpContext(published);
		return new HttpContextElement(serviceReference, mapping);
	}

}
