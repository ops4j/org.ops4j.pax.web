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

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWebFragmentIntegrationTest extends AbstractContainerTestBase {

//	private static final Logger LOG = LoggerFactory.getLogger(AbstractWebFragmentIntegrationTest.class);
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		LOG.info("Setting up test");
//
//		initWebListener();
//		waitForWebListener();
//	}
//
//	@Test
//	public void testWC() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
//						resp -> resp.contains("Have bundle context in filter: true"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
//	}
//
//	@Test
//	public void testFilterInit() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
//						resp -> resp.contains("Have bundle context in filter: true"))
//				.withResponseAssertion("Response must contain 'Hello World (url pattern)' from Filter set in web.xml",
//						resp -> resp.contains("Hello World (url pattern)"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
//	}
//
//	@Test
//	public void testWebContainerExample() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/example");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/images/logo.png");
//	}
//
//	@Test
//	public void testWebContainerSN() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/sn");
//	}
//
//	/**
//	 * Note: Undertow (in current pax-web-undertow implementation) doesn't use custom (servlet) error pages
//	 * for HTTP codes returned from {@code io.undertow.server.handlers.resource.ResourceHandler}. So even
//	 * if we get HTTP 403, we won't get our custom error page.
//	 * @throws Exception
//	 */
//	@Test
//	public void testSlash() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(403)
////				.withResponseAssertion("Response must contain '<h1>Error 403 Page</h1>'",
////						resp -> resp.contains("<h1>Error 403 Page</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/");
//	}
//
//
//	@Test
//	public void testSubJSP() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h2>Hello World!</h2>'",
//						resp -> resp.contains("<h2>Hello World!</h2>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/subjsp");
//	}
//
//	@Test
//	public void testErrorJSPCall() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(404)
//				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
//						resp -> resp.contains("<h1>Error Page</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/error.jsp");
//	}
//
//	@Test
//	public void testWrongServlet() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(404)
//				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
//						resp -> resp.contains("<h1>Error Page</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wrong/");
//	}
}