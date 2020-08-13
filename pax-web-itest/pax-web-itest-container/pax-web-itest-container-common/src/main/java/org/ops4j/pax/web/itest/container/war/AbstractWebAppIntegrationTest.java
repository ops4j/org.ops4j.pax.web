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

import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;

/**
 * @author Grzegorz Grzybek
 */
public abstract class AbstractWebAppIntegrationTest extends AbstractControlledTestBase {

//	private static final Logger LOG = LoggerFactory.getLogger(AbstractWebAppIntegrationTest.class);
//
//	private Bundle installWarBundle;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
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
//	@Test
//	public void testWars() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'OK: hello'",
//						resp -> resp.contains("OK: hello"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/introspect?message=hello");
//
//		List<String> events = events(installWarBundle);
//
//		assertThat("There should be single ServletContext initialization",
//				events.stream().filter(s -> s.equals("contextInitialized: /war-bundle")).count(), equalTo(1L));
//		assertThat("The context parameter should be available",
//				events.stream().filter(s -> s.equals("paramFound: my.value")).count(), equalTo(1L));
//
//		installWarBundle.stop();
//
//		assertThat("There should be single ServletContext disposal",
//				events.stream().filter(s -> s.equals("contextDestroyed: /war-bundle")).count(), equalTo(1L));
//		assertThat("war-bundle should have 6 registration, 6 unregistration, and 1 parameter found events recorded",
//				events.size(), equalTo(13));
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/introspect?message=hello");
//
//		initWebListener();
//		installWarBundle.start();
//		waitForWebListener();
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'OK: hello'",
//						resp -> resp.contains("OK: hello"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/introspect?message=hello");
//
//		assertThat("war-bundle should have 12 registration, 6 unregistration, and 2 parameter found events recorded",
//				events.size(), equalTo(20));
//		assertThat("There should be two ServletContext initialization",
//				events.stream().filter(s -> s.equals("contextInitialized: /war-bundle")).count(), equalTo(2L));
//	}
//
//	@Test
//	public void resources() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Special directory'",
//						resp -> resp.contains("Special directory"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/war-bundle/readme.txt");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'miscellaneous resources'",
//						resp -> resp.contains("miscellaneous resources"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/misc/readme.txt");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'miscellaneous resources'",
//						resp -> resp.contains("Some directory with WAR resources."))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/war-bundle/readme.txt");
//	}
//
//	@SuppressWarnings("unchecked")
//	private List<String> events(Bundle bundle) throws Exception {
//		Class<?> introspection = bundle.adapt(BundleWiring.class).getClassLoader().loadClass("org.ops4j.pax.web.introspection.Introspection");
//		return (List<String>) introspection.getMethod("events").invoke(null);
//	}

}
