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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

	private ServiceRegistration<HttpContextMapping> httpContextMappingReg1;
	private ServiceRegistration<HttpContextMapping> httpContextMappingReg2;
	private ServiceRegistration<HttpContextMapping> httpContextMappingReg3;
	private ServiceRegistration<HttpContextMapping> httpContextMappingReg4;
	private ServiceRegistration<Servlet> servletReg1;
	private ServiceRegistration<Servlet> servletReg2;
	private ServiceRegistration<Servlet> servletReg3;
	private ServiceRegistration<Servlet> servletReg4;

	public void start(final BundleContext bundleContext) throws Exception {
		Dictionary<String, String> props;

		// register the first context
		props = new Hashtable<>();
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended");
		HashMap<String, String> contextMappingParams = new HashMap<>();
		contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS,
				"localhost");
		contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_CONNECTORS,
				"jettyConn1");
		httpContextMappingReg1 = bundleContext.registerService(
				HttpContextMapping.class, new WhiteboardHttpContextMapping(
						"extended", "foo", contextMappingParams), props);

		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended2");
		contextMappingParams = new HashMap<>();
		contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_CONNECTORS,
				"default");
		httpContextMappingReg2 = bundleContext.registerService(
				HttpContextMapping.class, new WhiteboardHttpContextMapping(
						"extended2", "bar", contextMappingParams), props);

		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended3");
		contextMappingParams = new HashMap<>();
		contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS,
				"127.0.0.1");
		httpContextMappingReg3 = bundleContext.registerService(
				HttpContextMapping.class, new WhiteboardHttpContextMapping(
						"extended3", null, contextMappingParams), props);

		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended4");
		contextMappingParams = new HashMap<>();
		httpContextMappingReg4 = bundleContext.registerService(
				HttpContextMapping.class, new WhiteboardHttpContextMapping(
						"extended4", "default", contextMappingParams), props);

		props = new Hashtable<>();
		props.put(ExtenderConstants.PROPERTY_ALIAS, "/whiteboard");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended");
		servletReg1 = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/whiteboard"), props);

		props = new Hashtable<>();
		props.put(ExtenderConstants.PROPERTY_ALIAS, "/whiteboard2");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended2");
		servletReg2 = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/whiteboard2"), props);

		props = new Hashtable<>();
		props.put(ExtenderConstants.PROPERTY_ALIAS, "/whiteboard3");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended3");
		servletReg3 = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/whiteboard3"), props);

		props = new Hashtable<>();
		props.put(ExtenderConstants.PROPERTY_ALIAS, "/whiteboard4");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended4");
		servletReg4 = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/whiteboard4"), props);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (servletReg4 != null) {
			servletReg4.unregister();
			servletReg4 = null;
		}
		if (servletReg3 != null) {
			servletReg3.unregister();
			servletReg3 = null;
		}
		if (servletReg2 != null) {
			servletReg2.unregister();
			servletReg2 = null;
		}
		if (servletReg1 != null) {
			servletReg1.unregister();
			servletReg1 = null;
		}
		if (httpContextMappingReg4 != null) {
			httpContextMappingReg4.unregister();
			httpContextMappingReg4 = null;
		}
		if (httpContextMappingReg3 != null) {
			httpContextMappingReg3.unregister();
			httpContextMappingReg3 = null;
		}
		if (httpContextMappingReg2 != null) {
			httpContextMappingReg2.unregister();
			httpContextMappingReg2 = null;
		}
		if (httpContextMappingReg1 != null) {
			httpContextMappingReg1.unregister();
			httpContextMappingReg1 = null;
		}
	}
}
