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
package org.ops4j.pax.web.itest.utils.web;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.http.HttpContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Bundle2Activator implements BundleActivator {

	@Override
	@SuppressWarnings("deprecation")
	public void start(BundleContext context) throws Exception {

		ServiceReference<WebContainer> serviceReference = context.getServiceReference(WebContainer.class);

		while (serviceReference == null) {
			serviceReference = context.getServiceReference(WebContainer.class);
		}

		WebContainer service = context.getService(serviceReference);

		Collection<ServiceReference<HttpContext>> serviceReferences = context.getServiceReferences(HttpContext.class, "(httpContext.id=shared)");

		if (serviceReferences.size() > 1) {
			throw new RuntimeException("should only be one http shared context");
		}

		HttpContext httpContext = context.getService(serviceReferences.iterator().next());

		Dictionary<String, String> props;

		// register a custom http context that forbids access
		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "shared");
		props.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, Bundle2SharedServlet.ALIAS);
		service.registerServlet(new Bundle2SharedServlet(), new String[] { Bundle2SharedServlet.ALIAS }, props, httpContext);

		Dictionary<String, String> filterInit = new Hashtable<>();
		filterInit.put("pattern", ".*");
		filterInit.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "shared");

		service.registerFilter(new Bundle2SharedFilter(), new String[] { "/*" }, null, filterInit, httpContext);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
