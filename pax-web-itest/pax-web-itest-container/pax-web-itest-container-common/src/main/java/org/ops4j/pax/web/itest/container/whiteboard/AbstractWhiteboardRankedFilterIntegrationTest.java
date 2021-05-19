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
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardRankedFilterIntegrationTest extends AbstractContainerTestBase {

	private ServiceRegistration<Servlet> service;

	@Before
	public void waitBeforeTest() throws Exception {
		configureAndWaitForListener(8181);
	}

	@Before
	@SuppressWarnings("deprecation")
	public void setUp() throws Exception {
		context.installBundle(sampleURI("whiteboard"));
		configureAndWaitForServletWithMapping("/ranked/*", () -> {
			Dictionary<String, String> params = new Hashtable<>();
			params.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, "/ranked");
			service = context.registerService(Servlet.class, new WhiteboardServlet("/ranked"), params);
		});
	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();
	}

	@Test
	public void testWhiteBoardFilteredFirst() throws Exception {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
		props.put(Constants.SERVICE_RANKING, 1);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_1");
		ServiceRegistration<Filter> filter1 = context.registerService(Filter.class, new RankFilter(), props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
		props.put(Constants.SERVICE_RANKING, 2);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_2");
		ServiceRegistration<Filter> filter2 = context.registerService(Filter.class, new RankFilter(), props);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter Rank: 1'",
						resp -> resp.contains("Filter Rank: 1"))
				.withResponseAssertion("Response must contain 'Filter Rank: 2'",
						resp -> resp.contains("Filter Rank: 2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/ranked");

		filter1.unregister();
		filter2.unregister();
	}

	@Test
	public void testWhiteBoardFilteredLast() throws Exception {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
		props.put(Constants.SERVICE_RANKING, 2);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_2");
		ServiceRegistration<Filter> filter1 = context.registerService(Filter.class, new RankFilter(), props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
		props.put(Constants.SERVICE_RANKING, 1);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_1");
		ServiceRegistration<Filter> filter2 = context.registerService(Filter.class, new RankFilter(), props);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter Rank: 1'",
						resp -> resp.contains("Filter Rank: 1"))
				.withResponseAssertion("Response must contain 'Filter Rank: 2'",
						resp -> resp.contains("Filter Rank: 2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/ranked");

		filter1.unregister();
		filter2.unregister();
	}

	@Test
	public void testWhiteBoardFilteredInsertInMiddle() throws Exception {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
		props.put(Constants.SERVICE_RANKING, 1);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_1");
		ServiceRegistration<Filter> filter1 = context.registerService(Filter.class, new RankFilter(), props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
		props.put(Constants.SERVICE_RANKING, 3);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "3");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_3");
		ServiceRegistration<Filter> filter3 = context.registerService(Filter.class, new RankFilter(), props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
		props.put(Constants.SERVICE_RANKING, 2);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_2");
		ServiceRegistration<Filter> filter2 = context.registerService(Filter.class, new RankFilter(), props);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter Rank: 1'",
						resp -> resp.contains("Filter Rank: 1"))
				.withResponseAssertion("Response must contain 'Filter Rank: 2'",
						resp -> resp.contains("Filter Rank: 2"))
				.withResponseAssertion("Response must contain 'Filter Rank: 3'",
						resp -> resp.contains("Filter Rank: 3"))
				.doGETandExecuteTest("http://127.0.0.1:8181/ranked");

		filter1.unregister();
		filter2.unregister();
		filter3.unregister();
	}

	public static class RankFilter implements Filter {
		String rank;

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			rank = filterConfig.getInitParameter(Constants.SERVICE_RANKING);
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			response.getWriter().println("Filter Rank: " + rank);
			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {
		}
	}

}
