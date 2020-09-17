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
public abstract class AbstractWarBasicAuthSecuredIntegrationTest extends AbstractContainerTestBase {

//	private Bundle installWarBundle;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		logger.info("Setting up test");
//
//		initWebListener();
//
//		String bundlePath = WEB_BUNDLE
//				+ "mvn:org.ops4j.pax.web.samples/war-authentication/"
//				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH
//				+ "=/war-authentication";
//		installWarBundle = bundleContext.installBundle(bundlePath);
//		installWarBundle.start();
//
//		waitForWebListener();
//		waitForServer("https://127.0.0.1:8443");
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
//				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/wc");
//	}
//
//	@Test
//	public void testWebContainerExample() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(401)
//				.withResponseAssertion("Response must contain 'Unauthorized'",
//						resp -> resp.contains("Unauthorized"))
//				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/wc/example");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.authenticate("admin", "admin", "Test Realm")
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/wc/example");
//	}
//
//	@Test
//	public void testWebContainerSN() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/wc/sn");
//	}
//
//	@Test
//	public void testSlash() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/");
//	}

}
