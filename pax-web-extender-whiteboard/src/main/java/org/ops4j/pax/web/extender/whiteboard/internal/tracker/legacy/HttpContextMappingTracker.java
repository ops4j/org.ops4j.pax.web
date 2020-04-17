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

import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.AbstractContextTracker;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * <p>Tracks {@link HttpContextMapping} - legacy Pax Web whiteboard service to allow users creating
 * {@link org.osgi.service.http.HttpContext} contexts from Http Service specification in a Whiteboard way.</p>
 *
 * <p>Note that CMPN R7 specification provides only one way to register a <em>context</em> - using
 * {@link org.osgi.service.http.context.ServletContextHelper}.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 06, 2008
 */
public class HttpContextMappingTracker extends AbstractContextTracker<HttpContextMapping> {

	private HttpContextMappingTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	public static ServiceTracker<HttpContextMapping, OsgiContextModel> createTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new HttpContextMappingTracker(extenderContext, bundleContext).create(HttpContextMapping.class);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected OsgiContextModel configureContextModel(ServiceReference<HttpContextMapping> serviceReference,
			final OsgiContextModel model) {

		HttpContextMapping service = null;
		try {
			// dereference here to get some information, but unget later.
			// this reference will be dereferenced again in the (Bundle)Context of actual Whiteboard service
			// TODO: check the get/unget lifecycle
			service = dereference(serviceReference);

			// 1. context name
			model.setName(service.getContextId());

			// 2. context path
			// NOTE: Pax Web 7 was stripping leading "/" and was mixing concepts of "name" and "path"
			String contextPath = service.getContextPath();
			if (contextPath == null || "".equals(contextPath.trim())) {
				contextPath = PaxWebConstants.DEFAULT_CONTEXT_PATH;
			}
			model.setContextPath(contextPath);

			// 3. context params
			model.getContextParams().clear();
			model.getContextParams().putAll(service.getInitParameters());

			// 4. don't pass service registration properties...
			// ... create ones instead (service.id and service.rank are already there)
			setupArtificialServiceRegistrationProperties(model, service);
			if (service.getContextId() != null) {
				// legacy ID
				model.getContextRegistrationProperties().put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, service.getContextId());
				// property to allow Whiteboard elements to be registered for HttpService-related context
				model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY, service.getContextId());
				// allow old HttpContext to be target for new Whiteboard web elements
				model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, service.getContextId());
			}

			// 5. TODO: virtual hosts
//			service.getVirtualHosts();

			// 6. source of the context
			// HttpContextMapping is the legaciest of the Whiteboard methods - it's Pax Web specific
			// registration of the "mapping" that creates legacy HttpContext
			// but if user has registered the service as singleton and getHttpContext() returns the same
			// instance all the time, we can treat such context as if it was passed via
			// httpService.registerServlet(..., context).
			String scope = Utils.getStringProperty(serviceReference, Constants.SERVICE_SCOPE);
			if (Constants.SCOPE_SINGLETON.equals(scope)) {
				// for singletons, we don't care about unget()
				HttpContextMapping contextMapping = bundleContext.getService(serviceReference);
				HttpContext h1 = contextMapping.getHttpContext(serviceReference.getBundle());
				HttpContext h2 = contextMapping.getHttpContext(serviceReference.getBundle());
				if (h1 == h2) {
					// it truly is singleton
					if (h1 instanceof WebContainerContext) {
						// assume that if user wants this context to be "shared", it's actually an instance
						// of org.ops4j.pax.web.service.MultiBundleWebContainerContext and "shared" service
						// registration property is not relevant
						model.setHttpContext((WebContainerContext) h1);
					} else {
						if (contextMapping.isShared()) {
							LOG.warn("contextMapping is registered as shared, but actual HttpContext is not an instance"
									+ " of WebContainerContext with \"shared\" property");
						}
						model.setHttpContext(new WebContainerContextWrapper(serviceReference.getBundle(), h1, service.getContextId()));
					}
				}
			}
			if (model.getHttpContext() == null) {
				// supplier for later derefenrencing
				model.setContextSupplier((bundleContext, ignoredName) -> {
					HttpContextMapping mapping = null;
					try {
						mapping = bundleContext.getService(serviceReference);
						// get the HttpContextMapping again - within proper bundle context, but again - only to
						// obtain all the information needed. The "factory" method also accepts a Bundle, so we pass it.
						String name = mapping.getContextId();
						HttpContext context = mapping.getHttpContext(bundleContext.getBundle());
						return new WebContainerContextWrapper(bundleContext.getBundle(), context, name);
					} finally {
						if (mapping != null) {
							bundleContext.ungetService(serviceReference);
						}
					}
				});
			}
		} finally {
			if (service != null) {
				// the service was obtained only to extract the data out of it, so we have to unget()
				// TOCHECK: should we really unget here?
				bundleContext.ungetService(serviceReference);
			}
		}

		return model;
	}

}
