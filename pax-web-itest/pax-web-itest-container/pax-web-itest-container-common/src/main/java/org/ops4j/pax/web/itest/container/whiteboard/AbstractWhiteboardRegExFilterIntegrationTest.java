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
package org.ops4j.pax.web.itest.container.whiteboard;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardRegExFilterIntegrationTest extends AbstractContainerTestBase {

	private ServiceRegistration<Servlet> service;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/s/*", () -> {
			Dictionary<String, Object> params = new Hashtable<>();
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] { "/s/*" });
			service = context.registerService(Servlet.class, new HttpServlet() {
				@Override
				protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
					resp.getWriter().print(getServletConfig().getServletName());
				}
			}, params);
		});
	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();
	}

	@Test
	public void testWhiteBoardRegExFilter() throws Exception {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, ".*xxx.*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rf1");
		ServiceRegistration<Filter> filter = context.registerService(Filter.class, new RegExFilter(), props);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'hit'",
						resp -> resp.contains("hit"))
				.doGETandExecuteTest("http://127.0.0.1:8181/s/any/xxx/any");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'hit'",
						resp -> resp.contains("hit"))
				.doGETandExecuteTest("http://127.0.0.1:8181/s/any/xx/any?query=xxx");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must NOT contain 'hit'",
						resp -> resp.contains("s1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/s/any/xx/any");

		filter.unregister();
	}

	public static class RegExFilter implements Filter {
		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
			response.getWriter().print("hit");
			// not passing to the chain
//			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {
		}
	}

}
