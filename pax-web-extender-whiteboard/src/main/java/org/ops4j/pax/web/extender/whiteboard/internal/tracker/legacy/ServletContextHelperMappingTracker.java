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

import java.util.Arrays;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.AbstractContextTracker;
import org.ops4j.pax.web.service.spi.context.DefaultServletContextHelper;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
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

	private ServletContextHelperMappingTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<ServletContextHelperMapping, OsgiContextModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new ServletContextHelperMappingTracker(whiteboardExtenderContext, bundleContext).create(ServletContextHelperMapping.class);
	}

	@Override
	protected void configureContextModel(ServiceReference<ServletContextHelperMapping> serviceReference,
			final OsgiContextModel model) {

		ServletContextHelperMapping service = null;
		try {
			// dereference here to get some information, but unget later.
			// this reference will be dereferenced again in the (Bundle)Context of actual Whiteboard service
			service = model.getOwnerBundle().getBundleContext().getService(serviceReference);

			model.setShared(true);

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
			setupArtificialServiceRegistrationProperties(model, service, true);

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
			// ServletContextHelperMapping is legacy (Pax Web specific) method for registration of
			// standard ServletContextMapping with the details specified directly and not as service registration
			// properties
			// even if the service registered is singleton and getServletContextHelper() returns the same instance
			// all the time, we still configure a supplier, because ServletContexteHelper should never be
			// treated as a context passed to httpService.registerServlet(..., context)
			String scope = Utils.getStringProperty(serviceReference, Constants.SERVICE_SCOPE);
			if (!Constants.SCOPE_SINGLETON.equals(scope)) {
				LOG.warn("ServletContextHelperMapping service was not registered as \"singleton\". However it's only used" +
						" to obtain a ServletContextHelper instance. Consider registering non-singleton ServletContextHelper service instead");
			}

			ServletContextHelper helper = service.getServletContextHelper(serviceReference.getBundle());
			if (helper == null) {
				helper = new DefaultServletContextHelper(serviceReference.getBundle());
			}

			// not set as direct reference, but as a supplier, so it won't be treated as HttpService-related "context"
			final ServletContextHelper finalHelper = helper;
			model.setContextSupplier((bundleContext, ignoredName) ->
					new WebContainerContextWrapper(bundleContext.getBundle(), finalHelper, model.getName()));
		} finally {
			if (service != null) {
				// the service was obtained only to extract the data out of it, so we have to unget()
				bundleContext.ungetService(serviceReference);
			}
		}
	}

}
