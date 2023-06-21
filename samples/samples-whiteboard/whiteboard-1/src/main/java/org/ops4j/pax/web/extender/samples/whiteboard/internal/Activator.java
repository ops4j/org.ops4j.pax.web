/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import jakarta.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private ServiceRegistration<ServletContextHelper> contextReg;
	private ServiceRegistration<Servlet> servletReg;

	public void start(final BundleContext bundleContext) throws Exception {
		Dictionary<String, Object> props;

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "custom");
		contextReg = bundleContext.registerService(ServletContextHelper.class, new ServletContextHelper() { }, props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] { "/whiteboard/*" });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "whiteboard-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(osgi.http.whiteboard.context.name=custom)");
		servletReg = bundleContext.registerService(Servlet.class, new WhiteboardServlet(), props);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (servletReg != null) {
			servletReg.unregister();
			servletReg = null;
		}
		if (contextReg != null) {
			contextReg.unregister();
			contextReg = null;
		}
	}

}
