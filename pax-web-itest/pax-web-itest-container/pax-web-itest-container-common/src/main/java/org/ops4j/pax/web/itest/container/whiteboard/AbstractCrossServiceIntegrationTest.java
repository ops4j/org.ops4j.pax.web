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

import java.util.Dictionary;
import java.util.Hashtable;
import jakarta.servlet.Filter;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.SimpleFilter;
import org.ops4j.pax.web.itest.utils.web.TestServlet;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.ServiceRegistration;
import org.ops4j.pax.web.service.http.HttpContext;
import org.ops4j.pax.web.service.http.HttpService;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractCrossServiceIntegrationTest extends AbstractContainerTestBase {

	@Test
	@SuppressWarnings("deprecation")
	public void testMultipleServiceCombination() throws Exception {
		WebContainer httpService = getWebContainer(context);

		HttpContext defaultHttpContext = httpService.createDefaultHttpContext("crosservice");

		Dictionary<String, Object> contextProps = new Hashtable<>();
		contextProps.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "crosservice");

		context.registerService(HttpContext.class.getName(), defaultHttpContext, contextProps);

		//registering without an explicit context might be the issue.
		httpService.registerServlet("/crosservice", new TestServlet(), null, defaultHttpContext);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Crossservice response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.withResponseAssertion("Crossservice response should not contain 'FILTER-INIT: true'",
						resp -> !resp.contains("FILTER-INIT: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/crosservice");

		// Register a servlet filter via whiteboard
		Dictionary<String, Object> filterProps = new Hashtable<>();
		filterProps.put("filter-name", "Sample Filter");
		filterProps.put(PaxWebConstants.SERVICE_PROPERTY_URL_PATTERNS, "/crosservice/*");
		filterProps.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "crosservice");
		ServiceRegistration<?> registerService = context.registerService(Filter.class, new SimpleFilter(), filterProps);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Crossservice response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.withResponseAssertion("Crossservice response must contain 'FILTER-INIT: true'",
						resp -> resp.contains("FILTER-INIT: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/crosservice");

		registerService.unregister();
		httpService.unregister("/crosservice");
	}

	@Test
	public void testMultipleServiceCombinationWithDefaultHttpContext() throws Exception {
		HttpService httpService = getHttpService(context);

		// registering without an explicit context might be the issue.
		httpService.registerServlet("/crosservice", new TestServlet(), null, null);

		// Register a servlet filter via whiteboard
		Dictionary<String, Object> filterProps = new Hashtable<>();
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/crosservice/*");
		// "140.5 Registering Servlet Filters": Servlet filters are only applied to servlet requests if they are bound
		// to the same Servlet Context Helper and the same Http Whiteboard implementation.
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, PaxWebConstants.HTTP_SERVICE_CONTEXT_FILTER);
		ServiceRegistration<?> registerService = context.registerService(Filter.class.getName(), new SimpleFilter(), filterProps);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Crossservice response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.withResponseAssertion("Crossservice response must contain 'FILTER-INIT: true'",
						resp -> resp.contains("FILTER-INIT: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/crosservice");

		registerService.unregister();
		httpService.unregister("/crosservice");
	}

	@Test
	public void testMultipleServiceCombinationWithWebContainer() throws Exception {
		HttpService httpService = getHttpService(context);

		WebContainer wcService = getWebContainer(context);

		//registering without an explicit context might be the issue.
		httpService.registerServlet("/crossservice", new TestServlet(), null, null);

		// Register a servlet filter via webcontainer
		SimpleFilter filter = new SimpleFilter();
		wcService.registerFilter(filter, new String[] { "/crossservice/*" }, null, null, null);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Crossservice response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.withResponseAssertion("Crossservice response must contain 'FILTER-INIT: true'",
						resp -> resp.contains("FILTER-INIT: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/crossservice");

		wcService.unregisterFilter(filter);
		httpService.unregister("/crossservice");
	}

}
