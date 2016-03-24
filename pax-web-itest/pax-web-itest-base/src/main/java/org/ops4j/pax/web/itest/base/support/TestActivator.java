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
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;

public class TestActivator implements BundleActivator {

	private static final Logger LOG = LoggerFactory
			.getLogger(TestActivator.class);

	private ServiceRegistration<Filter> filterReg;

	@Override
	public void start(BundleContext context) throws Exception {
		Dictionary<String, String> props;
		// register a filter
		props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_URL_PATTERNS, "/filtered/*");
		filterReg = context.registerService(Filter.class,
				new WhiteboardFilter(), props);

		LOG.info("Test activator started ... ");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (filterReg != null) {
			filterReg.unregister();
			filterReg = null;
		}
	}
	
	public class WhiteboardFilter implements Filter {

		public void init(FilterConfig filterConfig) throws ServletException {
			LOG.info("Initialized");
		}

		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
			response.getWriter().println(
					"Filter was there before. Time: " + new Date().toString());
			chain.doFilter(request, response);
			response.getWriter().println(
					"Filter was there after. Time: " + new Date().toString());
			response.getWriter().close();
		}

		public void destroy() {
			LOG.info("Destroyed");
		}
	}

}