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
import javax.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardRootFilterIntegrationTest extends ITestBase {

	private ServiceRegistration<Servlet> service;

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initServletListener();

		Dictionary<String, String> initParams = new Hashtable<>();
		initParams.put("alias", "/");
		service = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/"), initParams);

		waitForServletListener();
	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();

	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		Dictionary<String, String> props = new Hashtable<>();
		props.put("urlPatterns", "*");
		ServiceRegistration<Filter> filter = bundleContext.registerService(
				Filter.class, new WhiteboardFilter(), props);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter was there before'",
						resp -> resp.contains("Filter was there before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");

		filter.unregister();
	}

	@Test
	public void testWhiteBoardNotFiltered() throws Exception {
		Dictionary<String, String> initParams = new Hashtable<>();
		initParams.put("alias", "/whiteboard");
		ServiceRegistration<Servlet> whiteboard = bundleContext.registerService(
				Servlet.class, new WhiteboardServlet("/whiteboard"),
				initParams);

		Dictionary<String, String> props = new Hashtable<>();
		props.put("urlPatterns", "/*");
		ServiceRegistration<Filter> filter = bundleContext.registerService(
				Filter.class, new WhiteboardFilter(), props);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter was there before'",
						resp -> resp.contains("Filter was there before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter was there before'",
						resp -> resp.contains("Filter was there before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");

		filter.unregister();
		whiteboard.unregister();
	}

}
