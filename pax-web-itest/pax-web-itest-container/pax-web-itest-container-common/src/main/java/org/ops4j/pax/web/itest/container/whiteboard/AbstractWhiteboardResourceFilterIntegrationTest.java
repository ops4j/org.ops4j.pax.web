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

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardResourceFilterIntegrationTest extends AbstractContainerTestBase {

//	private ServiceRegistration<Servlet> service;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//
//		Dictionary<String, String> initParams = new Hashtable<>();
//		initParams.put("alias", "/test-resources");
//		service = bundleContext.registerService(Servlet.class,
//				new WhiteboardServlet("/test-resources"), initParams);
//
//		initServletListener();
//		waitForServletListener();
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//		service.unregister();
//	}
//
//	@Test
//	public void testWhiteBoardFiltered() throws Exception {
//		Dictionary<String, String> props = new Hashtable<>();
//		props.put("urlPatterns", "*");
//		SimpleFilter simpleFilter = new SimpleFilter();
//		ServiceRegistration<Filter> filter = bundleContext.registerService(
//				Filter.class, simpleFilter, props);
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
//						resp -> resp.contains("Hello Whiteboard Extender"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/test-resources");
//
//		URL resource = simpleFilter.getResource();
//		assertNotNull(resource);
//
//		filter.unregister();
//	}

}
