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
package org.ops4j.pax.web.itest.container.war.todo;

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractServlet3WarIntegrationTest extends AbstractContainerTestBase {

//	private static final Logger LOG = LoggerFactory.getLogger(AbstractServlet3WarIntegrationTest.class);
//
//	private Bundle installWarBundle;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		LOG.info("Setting up test");
//
//		initWebListener();
//
//		String bundlePath = WEB_BUNDLE
//				+ "mvn:org.ops4j.pax.web.samples/helloworld-servlet3/"
//				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH + "=/war3";
//		installWarBundle = bundleContext.installBundle(bundlePath);
//		installWarBundle.start();
//
//		waitForWebListener();
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//		if (installWarBundle != null) {
//			installWarBundle.stop();
//			installWarBundle.uninstall();
//		}
//	}
//
//
//	@Test
//	public void testWC() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war3/hello");
//	}
//
//	@Test
//	public void testFilterInit() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
//						resp -> resp.contains("Have bundle context in filter: true"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war3/hello/filter");
//	}
//
//	@Test
//	public void testDuplicateDefinitionServlet() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Duplicate Servlet</h1>'",
//						resp -> resp.contains("<h1>Duplicate Servlet</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war3/duplicate");
//	}
//
//	@Test
//	public void testMimeImage() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseHeaderAssertion("Header 'Content-Type' must for image must be 'image/png'",
//						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
//								&& header.getValue().equals("image/png")))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war3/images/logo.png");
//	}
//
//	@Test
//	public void testMimeStyle() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseHeaderAssertion("Header 'Content-Type' must for image must be 'text/css'",
//						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
//								&& header.getValue().equals("text/css")))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war3/css/content.css");
//	}
//
//	@Test
//	public void testWrongServlet() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(404)
//				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
//						resp -> resp.contains("<h1>Error Page</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war3/wrong/");
//	}
}
