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
package org.ops4j.pax.web.itest.container.httpservice;

import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;

public abstract class AbstractHttpServiceProcessingWithConfigAdminIntegrationTest extends AbstractControlledTestBase {

//	private Bundle installWarBundle;
//
//	@Inject
//	private ConfigurationAdmin caService;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException, IOException {
//		initServletListener();
//
//		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + VersionUtil.getProjectVersion();
//		installWarBundle = installAndStartBundle(bundlePath);
//
//		waitForServletListener();
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
//	public void testContextAltering() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(200)
//				.withResponseAssertion("Response must contain 'Hello World'",
//						resp -> resp.contains("Hello World"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
//
//		Configuration cfg = caService.createFactoryConfiguration("org.ops4j.pax.web.context", null);
//		Dictionary<String, String> props = new Hashtable<>();
//		props.put("bundle.symbolicName", "org.ops4j.pax.web.samples.helloworld-hs");
//		props.put("context.id", "default");
//		props.put("login.config.authMethod", "BASIC");
//		props.put("login.config.realmName", "my-realm");
//		props.put("security.constraint.1.url", "/helloworld/*");
//		props.put("security.constraint.1.roles", "superuser, mediumuser");
//
//		waitForReRegistration(() -> {
//			try {
//				cfg.update(props);
//			} catch (IOException ignored) {
//			}
//		});
//
//		performSecurityAssertion();
//
//		waitForReRegistration(() -> {
//			try {
//				cfg.delete();
//			} catch (IOException ignored) {
//			}
//		});
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(200)
//				.withResponseAssertion("Response must contain 'Hello World'",
//						resp -> resp.contains("Hello World"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
//	}
//
//	/**
//	 * Due to incomplete configuration, Undertow returns HTTP 401, while Jetty and Tomcat - 403.
//	 * I'll investigate later...
//	 * @throws Exception
//	 */
//	protected void performSecurityAssertion() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(401)
//				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
//						headers -> headers.anyMatch(
//								header -> header.getKey().equals("WWW-Authenticate")
//										&& header.getValue().contains("my-realm")
//						)
//				)
//				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
//	}
//
//	protected void waitForReRegistration(Runnable trigger) throws InterruptedException {
//		CountDownLatch latch1 = new CountDownLatch(2);
//		bundleContext.addServiceListener(e -> {
//			if ((e.getType() == ServiceEvent.UNREGISTERING || e.getType() == ServiceEvent.REGISTERED)
//					&& ((String[])e.getServiceReference().getProperty("objectClass"))[0].equals("javax.servlet.ServletContext")) {
//				latch1.countDown();
//			}
//		});
//		trigger.run();
//		latch1.await();
//	}

}
