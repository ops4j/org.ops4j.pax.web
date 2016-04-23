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
 package org.ops4j.pax.web.itest.base.support;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;

import javax.servlet.Servlet;
import java.util.Hashtable;

public class ServletBundleActivator implements BundleActivator {

	private ServiceRegistration<Servlet> servletReg;
	private ServiceRegistration<HttpContext> httpContextReg;

	@Override
	public void start(BundleContext context) throws Exception {

		ServiceReference<WebContainer> serviceReference = context
				.getServiceReference(WebContainer.class);

		while (serviceReference == null) {
			serviceReference = context.getServiceReference(WebContainer.class);
		}

		WebContainer service = (WebContainer) context
				.getService(serviceReference);

		HttpContext httpContext = service.getDefaultSharedHttpContext();

		// register a custom http context that forbids access
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED, "true");
		httpContextReg = context.registerService(HttpContext.class,
				httpContext, props);

		props = new Hashtable<String, String>();
		props.put("alias", "/sharedContext");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED, "true");
		servletReg = context.registerService(Servlet.class, new TestServlet(),
				props);

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (servletReg != null) {
			servletReg.unregister();
		}
		if (httpContextReg != null) {
			httpContextReg.unregister();
		}
	}

}
