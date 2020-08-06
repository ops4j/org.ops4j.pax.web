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

import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks {@link HttpContext} services which were never designed to be tracked in Whiteboard specification.
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.1.0, August 21, 2007
 */
public class HttpContextTracker extends AbstractContextTracker<HttpContext> {

	private HttpContextTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	public static ServiceTracker<HttpContext, OsgiContextModel> createTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new HttpContextTracker(extenderContext, bundleContext).create(HttpContext.class);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected OsgiContextModel configureContextModel(ServiceReference<HttpContext> serviceReference,
			OsgiContextModel model) {

		// false by default, unless there's special registration (checked later)
		model.setShared(false);

		// 1. context name - checked using only legacy property name
		String name = Utils.getStringProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID);
		model.setName(name);

		// 2. context path - only legacy property name
		String contextPath = Utils.getStringProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH);
		if (contextPath == null || "".equals(contextPath.trim())) {
			contextPath = PaxWebConstants.DEFAULT_CONTEXT_PATH;
		}
		model.setContextPath(contextPath);

		// 3. context params - only with legacy prefix
		Map<String, String> initParams = new LinkedHashMap<>();
		String legacyInitPrefix = Utils.getStringProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_INIT_PREFIX);
		if (legacyInitPrefix == null) {
			legacyInitPrefix = PaxWebConstants.DEFAULT_INIT_PREFIX_PROP;
		}
		for (String key : serviceReference.getPropertyKeys()) {
			if (key.startsWith(legacyInitPrefix)) {
				String value = Utils.getStringProperty(serviceReference, key);
				if (value != null) {
					initParams.put(key.substring(legacyInitPrefix.length()), value);
				}
			}
		}
		model.getContextParams().clear();
		model.getContextParams().putAll(initParams);

		// 4. pass all service registration properties...
		model.getContextRegistrationProperties().putAll(Utils.toMap(serviceReference));
		// ... but in case there was no osgi.http.whiteboard.context.path property, let's set it now
		model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);
		if (name != null) {
			// property to allow Whiteboard elements to be registered for HttpService-related context
			model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY, name);
			// we could allow old HttpContext to be target for new Whiteboard web elements
			// but we won't do it - user has to specify osgi.http.whiteboard.context.name directly.
			// do not uncomment
//			model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, name);
		}

		// 5. TODO: virtual hosts
//			service.getVirtualHosts();

		// 6. source of the context
		// with ServletContextHelper registration (according to Whiteboard Service spec), the service should be
		// a service factory, so passing reference is needed
		// in legacy Pax Web Whiteboard, there's a convention to register singleton HttpContext and if it's the case,
		// we can really treat such HttpContext as equivalent to one passed to httpService.registerServlet(..., context)
		// OsgiContextModel with direct reference to WebContainerContext can be passed to WebContainer
		// to register bundle-scoped (or shared) WebContainerContext->OsgiContextModel mapping
		String scope = Utils.getStringProperty(serviceReference, Constants.SERVICE_SCOPE);
		if (Constants.SCOPE_SINGLETON.equals(scope)) {
			LOG.debug("Dereferencing singleton service {}", serviceReference);
			HttpContext context = bundleContext.getService(serviceReference);
			if (context instanceof WebContainerContext) {
				// assume that if user wants this context to be "shared", it's actually an instance
				// of org.ops4j.pax.web.service.MultiBundleWebContainerContext and "shared" service
				// registration property is not relevant
				model.setHttpContext((WebContainerContext) context);

				model.setShared(((WebContainerContext)context).isShared());
			} else {
				Boolean shared = Utils.getBooleanProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_SHARED);
				if (shared != null && shared) {
					LOG.warn("{} property is true, but the service is not an instance of "
							+ "WebContainerContext with \"shared\" property. Switching to non-shared.", context);
				}
				model.setHttpContext(new WebContainerContextWrapper(serviceReference.getBundle(), context, name));
			}

			// this means that such OsgiContextModel will be passed directly to HttpService, replacing
			// HttpService-specific, bundle-scoped (or shared) context
			// also this means that we should target such context using ONLY
			// "osgi.http.whiteboard.context.httpservice" property only
			model.getContextRegistrationProperties().remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);
		} else {
			model.setContextReference(serviceReference);

			// we have to believe the flag
			Boolean shared = Utils.getBooleanProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_SHARED);
			if (shared != null) {
				model.setShared(shared);
			}
		}

		return model;
	}

}
