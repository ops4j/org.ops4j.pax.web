/*
 * Copyright 2020 OPS4J.
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
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.util.tracker.ServiceTracker;

/**
 * <p>Tracks {@link ServletContextHelperMapping} - R7 Whiteboard service but wrapped in Pax Web specific
 * <em>mapping</em> that keeps all the information within the service itself, instead of among service
 * registration properties.</p>
 *
 * @author Grzegorz Grzybek
 */
public class ServletContextHelperMappingTracker extends AbstractContextTracker<ServletContextHelperMapping> {

	private ServletContextHelperMappingTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	public static ServiceTracker<ServletContextHelperMapping, OsgiContextModel> createTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new ServletContextHelperMappingTracker(extenderContext, bundleContext).create(ServletContextHelperMapping.class);
	}

	@Override
	protected OsgiContextModel configureContextModel(ServiceReference<ServletContextHelperMapping> serviceReference,
			final OsgiContextModel model) {

		ServletContextHelperMapping service = null;
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

			// 5. TODO: virtual hosts
//			service.getVirtualHosts();

			// 6. source of the context
			// ServletContexxtHelperMapping is legacy (Pax Web specific) method for registration of
			// standard ServletContextMapping with the details specified directly and not as service registration
			// properties
			// even if the service registered is singleton and getServletContextHelper() returns the same instance
			// all the time, we still configure a supplier, because ServletContexteHelper should never be
			// treated as a context passed to httpService.registerServlet(..., context)
			model.setContextSupplier((bundleContext, ignoredName) -> {
				ServletContextHelperMapping mapping = null;
				try {
					mapping = bundleContext.getService(serviceReference);
					// get the ServletContextHelperMapping again - within proper bundle context, but again - only to
					// obtain all the information needed. The "factory" method also accepts a Bundle, so we pass it.
					String name = mapping.getContextId();
					ServletContextHelper helper = mapping.getServletContextHelper(bundleContext.getBundle());
					return new WebContainerContextWrapper(bundleContext.getBundle(), helper, name);
				} finally {
					// TOCHECK: hmm, won't the ServletContextHelper returned from the mapping go away if we unget
					//          the mapping?
					if (mapping != null) {
						bundleContext.ungetService(serviceReference);
					}
				}
			});
		} finally {
			if (service != null) {
				// the service was obtained only to extract the data out of it, so we have to unget()
				bundleContext.ungetService(serviceReference);
			}
		}

		return model;
	}

}
