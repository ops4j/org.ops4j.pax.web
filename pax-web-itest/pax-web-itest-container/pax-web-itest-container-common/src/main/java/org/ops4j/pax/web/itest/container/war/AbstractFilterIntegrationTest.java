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
package org.ops4j.pax.web.itest.container.war;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractFilterIntegrationTest extends AbstractContainerTestBase {

	@Test
	public void testSimpleFilter() throws Exception {
		WebContainer service = getWebContainer(context);

		final String fullContent = "This content is Filtered by a javax.servlet.Filter";
		Filter filter = new Filter() {
			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				PrintWriter writer = response.getWriter();
				writer.write(fullContent);
				writer.flush();
			}

			@Override
			public void destroy() {
			}
		};

		Dictionary<String, String> initParams = new Hashtable<>();

		configureAndWaitForFilterWithMapping("/testFilter/*", () -> {
			HttpContext defaultHttpContext = service.createDefaultHttpContext();
			service.registerResources("/", "default", defaultHttpContext);
			service.registerFilter(filter, new String[] { "/testFilter/*", }, new String[] { "default", },
					initParams, defaultHttpContext);
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This content is Filtered by a javax.servlet.Filter'",
						resp -> resp.contains("This content is Filtered by a javax.servlet.Filter"))
				.doGETandExecuteTest("http://127.0.0.1:8181/testFilter/filter.me");

		service.unregisterFilter(filter);
	}

	@Test
	public void testFilterWar() throws Exception {
		Bundle wab = configureAndWaitForDeploymentUnlessInstalled("simple-filter", () -> {
			installAndStartBundle(sampleWarURI("simple-filter"));
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain test from previous FilterChain",
						resp -> resp.contains("Filtered"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jsp-filter/me.filter");

		wab.uninstall();
	}

}
