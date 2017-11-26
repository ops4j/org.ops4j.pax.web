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
package org.ops4j.pax.web.itest.common;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.SimpleFilter;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractCrossServiceIntegrationTest extends ITestBase {

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		waitForWebListener();
	}

	@Test
	public void testMultipleServiceCombination() throws Exception {
		ServiceReference<HttpService> reference = bundleContext.getServiceReference(HttpService.class);
		HttpService httpService = bundleContext.getService(reference);

		HttpContext defaultHttpContext = httpService.createDefaultHttpContext();

		Dictionary<String, Object> contextProps = new Hashtable<>();
		contextProps.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "crosservice");

		bundleContext.registerService(HttpContext.class.getName(), defaultHttpContext, contextProps);

		//registering without an explicit context might be the issue. 
		httpService.registerServlet("/crosservice", new TestServlet(), null, defaultHttpContext);

		// Register a servlet filter via whiteboard
		Dictionary<String, Object> filterProps = new Hashtable<>();
		filterProps.put("filter-name", "Sample Filter");
		filterProps.put(ExtenderConstants.PROPERTY_URL_PATTERNS, "/crosservice/*");
		filterProps.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "crosservice");
		ServiceRegistration<?> registerService = bundleContext.registerService(Filter.class.getName(), new SimpleFilter(), filterProps);


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
		ServiceReference<HttpService> reference = bundleContext.getServiceReference(HttpService.class);
		HttpService httpService = bundleContext.getService(reference);

		//registering without an explicit context might be the issue. 
		httpService.registerServlet("/crosservice", new TestServlet(), null, null);

		// Register a servlet filter via whiteboard
		Dictionary<String, Object> filterProps = new Hashtable<>();
//        filterProps.put("filter-name", "Sample Filter");
		filterProps.put(ExtenderConstants.PROPERTY_URL_PATTERNS, "/crosservice/*");
		ServiceRegistration<?> registerService = bundleContext.registerService(Filter.class.getName(), new SimpleFilter(), filterProps);

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
		ServiceReference<HttpService> reference = bundleContext.getServiceReference(HttpService.class);
		HttpService httpService = bundleContext.getService(reference);

		ServiceReference<WebContainer> wcReference = bundleContext.getServiceReference(WebContainer.class);
		WebContainer wcService = bundleContext.getService(wcReference);


		//registering without an explicit context might be the issue. 
		httpService.registerServlet("/crossservice", new TestServlet(), null, null);

		// Register a servlet filter via webcontainer
		SimpleFilter filter = new SimpleFilter();
		wcService.registerFilter(filter, new String[]{"/crossservice/*"}, null, null, null);

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
