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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy;

import java.util.Arrays;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.AbstractContextTracker;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.context.DefaultHttpContext;
import org.ops4j.pax.web.service.spi.context.DefaultMultiBundleWebContainerContext;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.ops4j.pax.web.service.http.HttpContext;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * <p>Tracks {@link HttpContextMapping} - legacy Pax Web whiteboard service to allow users creating
 * {@link org.ops4j.pax.web.service.http.HttpContext} contexts from Http Service specification in a Whiteboard way.</p>
 *
 * <p>Note that CMPN R7 specification provides only one way to register a <em>context</em> - using
 * {@link org.osgi.service.servlet.context.ServletContextHelper}.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 06, 2008
 */
public class HttpContextMappingTracker extends AbstractContextTracker<HttpContextMapping> {

	private HttpContextMappingTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<HttpContextMapping, OsgiContextModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new HttpContextMappingTracker(whiteboardExtenderContext, bundleContext).create(HttpContextMapping.class);
	}

	@Override
	protected void configureContextModel(ServiceReference<HttpContextMapping> serviceReference,
			final OsgiContextModel model) {

		HttpContextMapping service = null;
		try {
			// dereference here to get some information and unget later.
			// this reference will NEVER be dereferenced again, because we use the "mapping" as the information
			// on how to obtain actual "context"
			LOG.debug("Dereferencing service {} to obtain HttpContextMapping instance", serviceReference);
			service = model.getOwnerBundle().getBundleContext().getService(serviceReference);

			model.setShared(service.isShared());

			// 1. context name
			String name = setupName(model, service);

			// 2. context path
			// NOTE: Pax Web 7 was stripping leading "/" and was mixing concepts of "name" and "path"
			String contextPath = setupContextPath(model, service);

			// 3. context params
			model.getContextParams().clear();
			if (service.getInitParameters() != null) {
				model.getContextParams().putAll(service.getInitParameters());
			}

			// 4. don't pass service registration properties...
			// ... create ones instead (service.id and service.rank are already there)
			setupArtificialServiceRegistrationProperties(model, service, false);
			// property to allow Whiteboard elements to be registered for HttpService-related context
			model.getContextRegistrationProperties().put(PaxWebConstants.HTTP_SERVICE_CONTEXT_PROPERTY, name);
			// and additionally a whiteboard context path
			model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);

			// 5. virtual hosts
			model.getVirtualHosts().clear();
			model.getConnectors().clear();
			if (service.getVirtualHosts() != null) {
				model.getVirtualHosts().addAll(Arrays.asList(service.getVirtualHosts()));
			}
			if (service.getConnectors() != null) {
				model.getConnectors().addAll(Arrays.asList(service.getConnectors()));
			}

			// 6. source of the context
			// HttpContextMapping is the legaciest of the Whiteboard methods - it's Pax Web specific
			// registration of the "mapping" that creates legacy HttpContext
			// but if user has registered the service as singleton and getHttpContext() returns the same
			// instance all the time, we can treat such context as if it was passed via
			// httpService.registerServlet(..., context).
			String scope = Utils.getStringProperty(serviceReference, Constants.SERVICE_SCOPE);
			if (!Constants.SCOPE_SINGLETON.equals(scope)) {
				LOG.warn("HttpContextMapping service was not registered as \"singleton\". However it's only used" +
						" to obtain a HttpContext instance. Consider registering non-singleton HttpContext service instead");
			}

			HttpContext httpContext = service.getHttpContext(serviceReference.getBundle());
			if (httpContext == null) {
				DefaultHttpContext ctx = new DefaultHttpContext(model.getOwnerBundle(), name);
				httpContext = !service.isShared() ? ctx : new DefaultMultiBundleWebContainerContext(ctx);
			}

			if (httpContext instanceof WebContainerContext) {
				// assume that if user wants this context to be "shared", it's actually an instance
				// of org.ops4j.pax.web.service.MultiBundleWebContainerContext and "shared" service
				// registration property is not relevant
				model.setHttpContext((WebContainerContext) httpContext);
				boolean actuallyShared = ((WebContainerContext) httpContext).isShared();
				if (model.isShared() && !actuallyShared) {
					LOG.warn("contextMapping is registered as shared, but actual HttpContext is not shared. Switching to non-shared.");
				} else if (!model.isShared() && actuallyShared) {
					LOG.warn("contextMapping is registered as non-shared, but actual HttpContext is marked as shared. Switching to shared.");
				}
				model.setShared(actuallyShared);
			} else {
				if (model.isShared()) {
					LOG.warn("contextMapping is registered as shared, but actual HttpContext is not an instance"
							+ " of WebContainerContext with \"shared\" property");
				}
				model.setHttpContext(new WebContainerContextWrapper(serviceReference.getBundle(), httpContext, name));
				model.setShared(false);
			}

			// we have a "context" (HttpContext) which:
			//  - is a singleton (whether or not HttpContextMapping service was a singleton)
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
		} finally {
			if (service != null) {
				// the service was obtained only to extract the data out of it, so we have to unget()
				bundleContext.ungetService(serviceReference);
			}
		}
	}

}
