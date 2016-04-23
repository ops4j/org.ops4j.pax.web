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
 package org.ops4j.pax.web.itest.jetty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

import javax.servlet.*;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardRankedFilterIntegrationTest extends ITestBase {

	private ServiceRegistration<Servlet> service;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(VersionUtil.getProjectVersion())
						.noStart());

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {

		Dictionary<String, String> initParams = new Hashtable<>();
		initParams.put("alias", "/ranked");
		service = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/ranked"), initParams);

	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();

	}

	@Test
	public void testWhiteBoardFilteredFirst() throws Exception {
		Dictionary<String, String> props = new Hashtable<>();
		props.put("urlPatterns", "/ranked/*");
		props.put(WebContainerConstants.FILTER_RANKING, "1");
		props.put(WebContainerConstants.FILTER_NAME, "rank_1");
		ServiceRegistration<Filter> filter1 = bundleContext.registerService(
		        Filter.class, new RankFilter(), props);
		
		props = new Hashtable<>();
        props.put("urlPatterns", "/ranked/*");
        props.put(WebContainerConstants.FILTER_RANKING, "2");
        props.put(WebContainerConstants.FILTER_NAME, "rank_2");
        ServiceRegistration<Filter> filter2 = bundleContext.registerService(
                Filter.class, new RankFilter(), props);

        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Filter Rank: 1'",
                        resp -> resp.contains("Filter Rank: 1"))
                .withResponseAssertion("Response must contain 'Filter Rank: 2'",
                        resp -> resp.contains("Filter Rank: 2"))
                .doGETandExecuteTest("http://127.0.0.1:8181/ranked");

//		String content = testClient.testWebPath("http://127.0.0.1:8181/ranked", 200);
//		assertTrue(content.contains("Filter Rank: 1"));
//		assertTrue(content.contains("Filter Rank: 2"));

		filter1.unregister();
		filter2.unregister();
	}

	@Test
	public void testWhiteBoardFilteredLast() throws Exception {
	    Dictionary<String, String> props = new Hashtable<>();
        props.put("urlPatterns", "/ranked/*");
        props.put(WebContainerConstants.FILTER_RANKING, "2");
        props.put(WebContainerConstants.FILTER_NAME, "rank_2");
        ServiceRegistration<Filter> filter1 = bundleContext.registerService(
                Filter.class, new RankFilter(), props);
        
        props = new Hashtable<>();
        props.put("urlPatterns", "/ranked/*");
        props.put(WebContainerConstants.FILTER_RANKING, "1");
        props.put(WebContainerConstants.FILTER_NAME, "rank_1");
        ServiceRegistration<Filter> filter2 = bundleContext.registerService(
                Filter.class, new RankFilter(), props);

        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Filter Rank: 1'",
                        resp -> resp.contains("Filter Rank: 1"))
                .withResponseAssertion("Response must contain 'Filter Rank: 2'",
                        resp -> resp.contains("Filter Rank: 2"))
                .doGETandExecuteTest("http://127.0.0.1:8181/ranked");

//        String content = testClient.testWebPath("http://127.0.0.1:8181/ranked", 200);
//        assertTrue(content.contains("Filter Rank: 1"));
//        assertTrue(content.contains("Filter Rank: 2"));

        filter1.unregister();
        filter2.unregister();
	}
	
	@Test
    public void testWhiteBoardFilteredInsertInMiddle() throws Exception {
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("urlPatterns", "/ranked/*");
        props.put(WebContainerConstants.FILTER_RANKING, "1");
        props.put(WebContainerConstants.FILTER_NAME, "rank_1");
        ServiceRegistration<Filter> filter1 = bundleContext.registerService(
                Filter.class, new RankFilter(), props);

        props = new Hashtable<String, String>();
        props.put("urlPatterns", "/ranked/*");
        props.put(WebContainerConstants.FILTER_RANKING, "3");
        props.put(WebContainerConstants.FILTER_NAME, "rank_3");
        ServiceRegistration<Filter> filter3 = bundleContext.registerService(
                Filter.class, new RankFilter(), props);
        
        props = new Hashtable<String, String>();
        props.put("urlPatterns", "/ranked/*");
        props.put(WebContainerConstants.FILTER_RANKING, "2");
        props.put(WebContainerConstants.FILTER_NAME, "rank_2");
        ServiceRegistration<Filter> filter2 = bundleContext.registerService(
                Filter.class, new RankFilter(), props);

        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Filter Rank: 1'",
                        resp -> resp.contains("Filter Rank: 1"))
                .withResponseAssertion("Response must contain 'Filter Rank: 2'",
                        resp -> resp.contains("Filter Rank: 2"))
                .withResponseAssertion("Response must contain 'Filter Rank: 3'",
                        resp -> resp.contains("Filter Rank: 3"))
                .doGETandExecuteTest("http://127.0.0.1:8181/ranked");

//        String content = testClient.testWebPath("http://127.0.0.1:8181/ranked", 200);
//        assertTrue(content.contains("Filter Rank: 1"));
//        assertTrue(content.contains("Filter Rank: 2"));
//        assertTrue(content.contains("Filter Rank: 3"));

        filter1.unregister();
        filter2.unregister();
        filter3.unregister();
    }

	public class RankFilter implements Filter {

        String rank;
        
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            rank = filterConfig.getInitParameter(WebContainerConstants.FILTER_RANKING);
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            response.getWriter().println("Filter Rank: "+rank);
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
            //nothing
        }
	}
}
