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
 * @author Grzegorz Grzybek
 */
public abstract class AbstractWebAppSessionsIntegrationTest extends AbstractContainerTestBase {

//	private static final Logger LOG = LoggerFactory.getLogger(AbstractWebAppSessionsIntegrationTest.class);
//
//	private Bundle installWarBundle;
//
//	@Inject
//	private ConfigurationAdmin caService;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException, IOException {
//		LOG.info("Setting up test");
//
//		initWebListener();
//
//		String bundlePath = "mvn:org.ops4j.pax.web.samples/war-introspection-bundle/"
//				+ VersionUtil.getProjectVersion() + "/war";
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
//	protected boolean areSessionsPersisted() {
//		return false;
//	}
//
//	@Test
//	public void testSessions() throws Exception {
//		CookieState cookieJar = new CookieState();
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(HttpServletResponse.SC_NOT_ACCEPTABLE)
//				.withResponseAssertion("Response must contain 'Please log in first'",
//						resp -> resp.contains("Please log in first"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/sessions/visit");
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Welcome Grzegorz'",
//						resp -> resp.contains("Welcome Grzegorz"))
//				.useCookieState(cookieJar)
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/sessions/login?user=Grzegorz");
//
//		for (int n = 0; n < 3; n++) {
//			int finalN = n;
//			HttpTestClientFactory.createDefaultTestClient()
//					.withResponseAssertion("Response must contain visit number",
//							resp -> resp.contains("That's your " + (finalN + 1) + " visit after logging in, Grzegorz"))
//					.useCookieState(cookieJar)
//					.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/sessions/visit");
//		}
//
//		initWebListener();
//		installWarBundle.stop();
//		installWarBundle.start();
//		waitForWebListener();
//
//		if (!areSessionsPersisted()) {
//			// session should be gone
//			HttpTestClientFactory.createDefaultTestClient()
//					.withReturnCode(HttpServletResponse.SC_NOT_ACCEPTABLE)
//					.withResponseAssertion("Response must contain 'Please log in first'",
//							resp -> resp.contains("Please log in first"))
//					.useCookieState(cookieJar)
//					.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/sessions/visit");
//		} else {
//			HttpTestClientFactory.createDefaultTestClient()
//					.withResponseAssertion("Response must contain visit number",
//							resp -> resp.contains("That's your 4 visit after logging in, Grzegorz"))
//					.useCookieState(cookieJar)
//					.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/sessions/visit");
//		}
//	}

}
