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

	private HttpContextTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<HttpContext, OsgiContextModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new HttpContextTracker(whiteboardExtenderContext, bundleContext).create(HttpContext.class);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void configureContextModel(ServiceReference<HttpContext> serviceReference,
			OsgiContextModel model) {

		// false by default, unless there's special registration (checked later)
		model.setShared(false);

		// 1. context name - checked using only legacy property name
		String name = Utils.getStringProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID);
		if (name == null || "".equals(name.trim())) {
			name = HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
		}
		model.setName(name);

		// 2. context path - only legacy property name
		String contextPath = Utils.getStringProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH);
		if (contextPath == null || "".equals(contextPath.trim())) {
			contextPath = PaxWebConstants.DEFAULT_CONTEXT_PATH;
		}
		model.setContextPath(contextPath);

		// 3. context params - with legacy prefix (and warning) and if none found - with Whiteboard prefix
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
		if (initParams.size() == 0) {
			// try with Whiteboard prefix
			for (String key : serviceReference.getPropertyKeys()) {
				if (key.startsWith(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX)) {
					String value = Utils.getStringProperty(serviceReference, key);
					if (value != null) {
						initParams.put(key.substring(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX.length()), value);
					}
				}
			}
		}
		model.getContextParams().clear();
		model.getContextParams().putAll(initParams);

		// 4. pass all service registration properties...
		model.getContextRegistrationProperties().putAll(Utils.toMap(serviceReference));
		// ... but in case there was no context path or name property, let's set them now
		model.getContextRegistrationProperties().put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, name);
		model.getContextRegistrationProperties().put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, contextPath);
		// property to allow Whiteboard elements to be registered for HttpService-related context
		model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY, name);
		// and additionally a whiteboard context path
		model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);

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
			LOG.debug("Dereferencing singleton service {} to obtain HttpContext instance", serviceReference);

			// even here, obtain the instance using the registering bundle context
			HttpContext context = model.getOwnerBundle().getBundleContext().getService(serviceReference);

			if (context instanceof WebContainerContext) {
				// assume that if user wants this context to be "shared", it's actually an instance
				// of org.ops4j.pax.web.service.MultiBundleWebContainerContext and "shared" service
				// registration property is not relevant
				model.setHttpContext((WebContainerContext) context);
				model.setShared(((WebContainerContext)context).isShared());

				// name check
				String actualName = ((WebContainerContext) context).getContextId();
				if (!name.equals(actualName)) {
					LOG.warn("The registered context has name \"{}\", but {} service property was \"{}\"."
									+ " Switching to \"{}\".",
							actualName, PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, name, actualName);
					model.setName(actualName);
				}
			} else {
				Boolean shared = Utils.getBooleanProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_SHARED);
				if (shared != null && shared) {
					LOG.warn("{} property is true, but the service is not an instance of "
							+ "WebContainerContext with \"shared\" property. Switching to non-shared.", context);
				}
				model.setHttpContext(new WebContainerContextWrapper(serviceReference.getBundle(), context, name));
				model.setShared(false);
			}

			// we have a "context" (HttpContext) which:
			//  - is a singleton
			//  - has a name
			//  - has associated bundle (the one registering HttpContext)
			// it means that such context is suitable to be used directly as a parameter to httpService.register()
			// (pax-web-extender-whiteboard will pass such context to pax-web-runtime to store it as HttpService-
			// related context - potentially overriding existing context created within the scope of HttpService)
			// and it can be referenced during Whiteboard registrations of web elements with:
			//    osgi.http.whiteboard.context.select = "(osgi.http.whiteboard.context.httpservice=<name>)"
			// but not with:
			//    osgi.http.whiteboard.context.select = "(osgi.http.whiteboard.context.name=<name>)"

			// this property is not present, but we explicitly show that it CAN'T be present
			model.getContextRegistrationProperties().remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);
		} else {
			model.setContextReference(serviceReference);

			// we have a "context" (HttpContext) which:
			//  - is NOT a singleton
			//  - has a name
			//  - has associated bundle (the one registering HttpContext)
			// it means that such context is NOT suitable to be used directly as a parameter to httpService.register()
			// (pax-web-extender-whiteboard will not pass this context to pax-web-runtime)
			// but it can be referenced during Whiteboard registrations of web elements with:
			//    osgi.http.whiteboard.context.select = "(osgi.http.whiteboard.context.httpservice=<name>)"
			// but not with:
			//    osgi.http.whiteboard.context.select = "(osgi.http.whiteboard.context.name=<name>)"

			// we have to believe the flag
			Boolean shared = Utils.getBooleanProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_SHARED);
			if (shared != null) {
				model.setShared(shared);
			}
		}
	}

	@Override
	protected void cleanupContextModel(ServiceReference<HttpContext> serviceReference, OsgiContextModel unpublished) {
		String scope = Utils.getStringProperty(serviceReference, Constants.SERVICE_SCOPE);
		if (Constants.SCOPE_SINGLETON.equals(scope)) {
			// that was the only case when we called getService(), so we have to unget - even if it's only
			// for reference counting
			unpublished.getOwnerBundle().getBundleContext().ungetService(serviceReference);
		}
	}

}
