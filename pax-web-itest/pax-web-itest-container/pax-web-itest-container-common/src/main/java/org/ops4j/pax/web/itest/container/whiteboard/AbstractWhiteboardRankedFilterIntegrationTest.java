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
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
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
	@SuppressWarnings("unchecked")
	public void testWhiteBoardFilteredFirst() throws Exception {
		final ServiceRegistration<Filter>[] filter1 = new ServiceRegistration[1];
		final ServiceRegistration<Filter>[] filter2 = new ServiceRegistration[1];
		configureAndWait(() -> {
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
			props.put(Constants.SERVICE_RANKING, 1);
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "1");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_1");
			filter1[0] = context.registerService(Filter.class, new RankFilter(), props);

			props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
			props.put(Constants.SERVICE_RANKING, 2);
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "2");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_2");
			filter2[0] = context.registerService(Filter.class, new RankFilter(), props);
		}, events -> events.stream()
				.filter(e -> e.getType() == WebElementEvent.State.DEPLOYED && ((FilterEventData)e.getData()).getFilterName().equals("rank_2")).count() == 1);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter Rank: 2Filter Rank: 1'",
						resp -> resp.contains("Filter Rank: 2Filter Rank: 1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/ranked");

		configureAndWait(() -> {
			filter1[0].unregister();
		}, events -> events.stream()
				.filter(e -> e.getType() == WebElementEvent.State.UNDEPLOYED && ((FilterEventData)e.getData()).getFilterName().equals("rank_1")).count() == 1);
		configureAndWait(() -> {
			filter2[0].unregister();
		}, events -> events.stream()
				.filter(e -> e.getType() == WebElementEvent.State.UNDEPLOYED && ((FilterEventData)e.getData()).getFilterName().equals("rank_2")).count() == 1);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testWhiteBoardFilteredLast() throws Exception {
		final ServiceRegistration<Filter>[] filter1 = new ServiceRegistration[1];
		final ServiceRegistration<Filter>[] filter2 = new ServiceRegistration[1];
		configureAndWait(() -> {
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
			props.put(Constants.SERVICE_RANKING, 2);
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "2");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_2");
			filter1[0] = context.registerService(Filter.class, new RankFilter(), props);

			props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
			props.put(Constants.SERVICE_RANKING, 1);
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "1");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_1");
			filter2[0] = context.registerService(Filter.class, new RankFilter(), props);
		}, events -> events.stream()
				.filter(e -> e.getType() == WebElementEvent.State.DEPLOYED && ((FilterEventData)e.getData()).getFilterName().equals("rank_1")).count() == 1);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter Rank: 2Filter Rank: 1'",
						resp -> resp.contains("Filter Rank: 2Filter Rank: 1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/ranked");

		configureAndWait(() -> {
			filter1[0].unregister();
		}, events -> events.stream()
				.filter(e -> e.getType() == WebElementEvent.State.UNDEPLOYED && ((FilterEventData)e.getData()).getFilterName().equals("rank_2")).count() == 1);
		configureAndWait(() -> {
			filter2[0].unregister();
		}, events -> events.stream()
				.filter(e -> e.getType() == WebElementEvent.State.UNDEPLOYED && ((FilterEventData)e.getData()).getFilterName().equals("rank_1")).count() == 1);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testWhiteBoardFilteredInsertInMiddle() throws Exception {
		final ServiceRegistration<Filter>[] filter1 = new ServiceRegistration[1];
		final ServiceRegistration<Filter>[] filter2 = new ServiceRegistration[1];
		final ServiceRegistration<Filter>[] filter3 = new ServiceRegistration[1];
		configureAndWait(() -> {
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
			props.put(Constants.SERVICE_RANKING, 1);
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "1");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_1");
			filter1[0] = context.registerService(Filter.class, new RankFilter(), props);

			props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
			props.put(Constants.SERVICE_RANKING, 3);
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "3");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_3");
			filter3[0] = context.registerService(Filter.class, new RankFilter(), props);

			props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/ranked/*");
			props.put(Constants.SERVICE_RANKING, 2);
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX + Constants.SERVICE_RANKING, "2");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "rank_2");
			filter2[0] = context.registerService(Filter.class, new RankFilter(), props);
		}, events -> events.stream()
				.filter(e -> e.getType() == WebElementEvent.State.DEPLOYED && ((FilterEventData)e.getData()).getFilterName().equals("rank_2")).count() == 1);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter Rank: 3Filter Rank: 2Filter Rank: 1'",
						resp -> resp.contains("Filter Rank: 3Filter Rank: 2Filter Rank: 1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/ranked");

		// undergistration is synchronous, because otherwise the context could attempt to start after unregistration
		// of first filter, while the 2nd one is being unregistered
		filter1[0].unregister();
		filter2[0].unregister();
		filter3[0].unregister();
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
			response.getWriter().print("Filter Rank: " + rank);
			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {
		}
	}

}
