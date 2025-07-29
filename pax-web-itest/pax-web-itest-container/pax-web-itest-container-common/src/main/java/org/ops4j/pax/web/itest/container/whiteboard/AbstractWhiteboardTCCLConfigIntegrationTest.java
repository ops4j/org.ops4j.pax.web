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
package org.ops4j.pax.web.itest.container.whiteboard;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import javax.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

public abstract class AbstractWhiteboardTCCLConfigIntegrationTest extends AbstractContainerTestBase {

	@Inject
	private ConfigurationAdmin caService;

	@Before
	public void setUp() throws Exception {
		org.osgi.service.cm.Configuration config = caService.getConfiguration(PaxWebConstants.PID, null);

		Dictionary<String, Object> current = config.getProperties();
		if (current == null || !"8182".equals(current.get(PaxWebConfig.PID_CFG_HTTP_PORT))) {
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(PaxWebConfig.PID_CFG_LISTENING_ADDRESSES, "127.0.0.1");
			props.put(PaxWebConfig.PID_CFG_HTTP_PORT, "8182");
			props.put(PaxWebConfig.PID_CFG_TCCL_TYPE, getTCCLType());

			configureAndWaitForListener(8182, () -> {
				config.update(props);
			});
		}

		configureAndWaitForNamedServlet("tccl-servlet", () -> {
			Dictionary<String, String> properties = new Hashtable<>();
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "tccl-servlet");
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/tccl");
			ServiceRegistration<Servlet> registerService = context
					.registerService(Servlet.class, new TCCLServlet(), properties);
		});
		configureAndWaitForFilterWithMapping("/*", () -> {
			Dictionary<String, String> properties = new Hashtable<>();
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "tccl-filter");
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
			ServiceRegistration<Filter> registerFilter = context
					.registerService(Filter.class, new TCCLFilter(), properties);
		});
	}

	protected String getTCCLType() {
		// that's the default value
		return "servlet";
	}

	@Test
	public void testTCCL() throws Exception {
		if ("whiteboard".equals(getTCCLType())) {
			HttpTestClientFactory.createDefaultTestClient()
					.withResponseAssertion("Response must contain 'true|true'",
							resp -> resp.contains("true|true"))
					.doGETandExecuteTest("http://127.0.0.1:8182/tccl");
		} else {
			HttpTestClientFactory.createDefaultTestClient()
					.withResponseAssertion("Response must contain 'false|false'",
							resp -> resp.contains("false|false"))
					.doGETandExecuteTest("http://127.0.0.1:8182/tccl");
		}
	}

	private static class TCCLServlet extends HttpServlet {
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			resp.getWriter().print(req.getServletContext().getClassLoader()
					.equals(Thread.currentThread().getContextClassLoader()));
		}
	}

	private static class TCCLFilter implements jakarta.servlet.Filter {
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			response.getWriter().print(request.getServletContext().getClassLoader()
					.equals(Thread.currentThread().getContextClassLoader()) + "|");
			chain.doFilter(request, response);
		}
	}

}
