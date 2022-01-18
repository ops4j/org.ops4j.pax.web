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

import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Activator implements BundleActivator {

	private ServiceRegistration<HttpContextMapping> httpContextMappingReg1;
	private ServiceRegistration<HttpContextMapping> httpContextMappingReg2;
	private ServiceRegistration<HttpContextMapping> httpContextMappingReg3;
	private ServiceRegistration<HttpContextMapping> httpContextMappingReg4;

	private ServiceRegistration<Servlet> servletReg1;
	private ServiceRegistration<Servlet> servletReg2;
	private ServiceRegistration<Servlet> servletReg3;
	private ServiceRegistration<Servlet> servletReg4;

	public void start(final BundleContext bundleContext) {
		Dictionary<String, Object> props;

		// register the first context - only virtual hosts, but connector will be set from config
		props = new Hashtable<>();
		HashMap<String, String> contextMappingParams = new HashMap<>();
		WhiteboardHttpContextMapping extended1 = new WhiteboardHttpContextMapping("extended1", "/foo", contextMappingParams);
		// set only virtual hosts - "custom" will be added by default from "org.ops4j.pax.web.default.connectors" config property
		extended1.setVirtualHost("localhost");
		httpContextMappingReg1 = bundleContext.registerService(HttpContextMapping.class, extended1, props);

		// register the second context - only connectors, so all virtual hosts
		props = new Hashtable<>();
		contextMappingParams = new HashMap<>();
		WhiteboardHttpContextMapping extended2 = new WhiteboardHttpContextMapping("extended2", "/bar", contextMappingParams);
		extended2.setConnector("default");
		httpContextMappingReg2 = bundleContext.registerService(HttpContextMapping.class, extended2, props);

		// register the third context - only virtual hosts (but different than extended1), so custom connector
		props = new Hashtable<>();
		contextMappingParams = new HashMap<>();
		props.put(Constants.SERVICE_RANKING, 1);
		WhiteboardHttpContextMapping extended3 = new WhiteboardHttpContextMapping("extended3", null, contextMappingParams);
		extended3.setVirtualHost("127.0.0.1");
		httpContextMappingReg3 = bundleContext.registerService(HttpContextMapping.class, extended3, props);

		// register the fourth context - no connectors (so all), no virtual hosts (so all)
		props = new Hashtable<>();
		contextMappingParams = new HashMap<>();
		WhiteboardHttpContextMapping extended4 = new WhiteboardHttpContextMapping("extended4", "/baz", contextMappingParams);
		httpContextMappingReg4 = bundleContext.registerService(HttpContextMapping.class, extended4, props);

		// four servlets for four different contexts

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/whiteboard");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "extended1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(%s=extended1)", HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY));
		servletReg1 = bundleContext.registerService(Servlet.class, new WhiteboardServlet("/whiteboard"), props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/whiteboard");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "extended2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(%s=extended2)", HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY));
		servletReg2 = bundleContext.registerService(Servlet.class, new WhiteboardServlet("/whiteboard"), props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/whiteboard");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "extended3");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(%s=extended3)", HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY));
		servletReg3 = bundleContext.registerService(Servlet.class, new WhiteboardServlet("/whiteboard"), props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/whiteboard");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "extended4");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(%s=extended4)", HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY));
		servletReg4 = bundleContext.registerService(Servlet.class, new WhiteboardServlet("/whiteboard"), props);
	}

	public void stop(BundleContext bundleContext) {
		if (servletReg1 != null) {
			servletReg1.unregister();
			servletReg1 = null;
		}
		if (servletReg2 != null) {
			servletReg2.unregister();
			servletReg2 = null;
		}
		if (servletReg3 != null) {
			servletReg3.unregister();
			servletReg3 = null;
		}
		if (servletReg4 != null) {
			servletReg4.unregister();
			servletReg4 = null;
		}
		if (httpContextMappingReg1 != null) {
			httpContextMappingReg1.unregister();
			httpContextMappingReg1 = null;
		}
		if (httpContextMappingReg2 != null) {
			httpContextMappingReg2.unregister();
			httpContextMappingReg2 = null;
		}
		if (httpContextMappingReg3 != null) {
			httpContextMappingReg3.unregister();
			httpContextMappingReg3 = null;
		}
		if (httpContextMappingReg4 != null) {
			httpContextMappingReg4.unregister();
			httpContextMappingReg4 = null;
		}
	}

}
