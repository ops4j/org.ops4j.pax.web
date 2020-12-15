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

import java.util.LinkedHashMap;
import java.util.Map;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks services of {@link ServletContextHelper} class - <strong>the</strong> context for R7 Whiteboard
 * Service specification.
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.2.0, August 21, 2007
 */
public class ServletContextHelperTracker extends AbstractContextTracker<ServletContextHelper> {

	private ServletContextHelperTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<ServletContextHelper, OsgiContextModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new ServletContextHelperTracker(whiteboardExtenderContext, bundleContext).create(ServletContextHelper.class);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected OsgiContextModel configureContextModel(ServiceReference<ServletContextHelper> serviceReference,
			OsgiContextModel model) {

		// always shared
		model.setShared(true);

		// 1. context name
		String name = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
				Utils::asString);
		if (name == null || "".equals(name.trim())) {
			name = HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
		}
		model.setName(name);

		// 2. context path
		// NOTE: Pax Web 7 was stripping leading "/" and was mixing concepts of "name" and "path"
		String contextPath = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
				Utils::asString);
		if (contextPath == null || "".equals(contextPath.trim())) {
			contextPath = PaxWebConstants.DEFAULT_CONTEXT_PATH;
		}
		model.setContextPath(contextPath);

		// 3. context params
		// NOTE: Pax Web 7 was passing ALL service registration properties as init parameters
		Map<String, String> initParams = new LinkedHashMap<>();
		for (String key : serviceReference.getPropertyKeys()) {
			if (key.startsWith(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX)) {
				String value = Utils.getStringProperty(serviceReference, key);
				if (value != null) {
					initParams.put(key.substring(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX.length()), value);
				}
			}
		}
		model.getContextParams().clear();
		model.getContextParams().putAll(initParams);

		// 4. pass all service registration properties...
		model.getContextRegistrationProperties().putAll(Utils.toMap(serviceReference));
		// ... but in case there was no osgi.http.whiteboard.context.path property, let's set it now
		model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, name);
		model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);

		// 5. TODO: virtual hosts
//			service.getVirtualHosts();

		// 6. source of the context
		// ServletContextHelper will be obtained from service reference on each call using correct bundle - always
		// this is the standard Whiteboard Service specification scenario
		model.setContextReference(serviceReference);

		return model;
	}

}
