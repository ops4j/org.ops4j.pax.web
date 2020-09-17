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
public abstract class AbstractWhiteboardDSRestartIntegrationTest extends AbstractContainerTestBase {

//	private Bundle installWarBundle;
//
//	@Inject
//	private BundleContext ctx;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		initServletListener();
//		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard-ds/" + VersionUtil.getProjectVersion();
//		installWarBundle = installAndStartBundle(bundlePath);
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
//
//	@Test
//	public void testWhiteBoardSimpleServlet() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
//						resp -> resp.contains("Hello from SimpleServlet"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
//	}
//
//	@Test
//	public void testWhiteBoardServletWithContext() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from ServletWithContext'",
//						resp -> resp.contains("Hello from ServletWithContext"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
//	}
//
//	@Test
//	public void testWhiteBoardFiltered() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Request changed by SimpleFilter'",
//						resp -> resp.contains("Request changed by SimpleFilter"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
//	}
//
//	@Test
//	public void testWhiteBoardRootRestart() throws Exception {
//		// find Whiteboard-bundle
//		final Bundle whiteBoardBundle = Arrays.stream(ctx.getBundles()).filter(bundle ->
//				"org.ops4j.pax.web.pax-web-extender-whiteboard".equalsIgnoreCase(bundle.getSymbolicName()))
//				.findFirst().orElseThrow(() -> new AssertionError("no Whiteboard bundle found"));
//
//		// stop Whiteboard bundle
//		whiteBoardBundle.stop();
//
//		new WaitCondition2("Check if Whiteboard bundle gets stopped",
//				() -> whiteBoardBundle.getState() == Bundle.RESOLVED)
//				.waitForCondition(10000, 500, () -> fail("Whiteboard bundle did not stop in time"));
//
//		// start Whiteboard bundle again
//		initServletListener();
//		whiteBoardBundle.start();
//
//		new WaitCondition2("Check if Whiteboard bundle gets activated",
//				() -> whiteBoardBundle.getState() == Bundle.ACTIVE)
//				.waitForCondition(10000, 500, () -> fail("Whiteboard bundle did not start in time"));
//		// also wait till the servlet is registered
//		waitForServletListener();
//		Thread.sleep(1500);
//
//		// Test
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
//						resp -> resp.contains("Hello from SimpleServlet"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
//
//
//		// Test
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from ServletWithContext'",
//						resp -> resp.contains("Hello from ServletWithContext"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
//	}
//
//	@Test
//	public void testWhiteBoardSampleBundleRestart() throws Exception {
//		// Test
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from ServletWithContext'",
//						resp -> resp.contains("Hello from ServletWithContext"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
//
//		// find Whiteboard-bundle
//		final Bundle whiteBoardSampleBundle = Arrays.stream(ctx.getBundles()).filter(bundle -> "org.ops4j.pax.web.samples.whiteboard-ds".equalsIgnoreCase(bundle.getSymbolicName()))
//				.findFirst().orElseThrow(() -> new AssertionError("no Whiteboard Sample bundle found"));
//
//		// stop Whiteboard bundle
//		whiteBoardSampleBundle.stop();
//
//		new WaitCondition2("Check if Whiteboard Sample bundle gets stopped",
//				() -> whiteBoardSampleBundle.getState() == Bundle.RESOLVED)
//				.waitForCondition(10000, 500, () -> fail("Whiteboard Sample bundle did not stop in time"));
//
//		// start Whiteboard bundle again
//		whiteBoardSampleBundle.start();
//
//		new WaitCondition2("Check if Whiteboard Sample bundle gets activated",
//				() -> whiteBoardSampleBundle.getState() == Bundle.ACTIVE)
//				.waitForCondition(10000, 500, () -> fail("Whiteboard Sample bundle did not start in time"));
//
//		// Test
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
//						resp -> resp.contains("Hello from SimpleServlet"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
//
//		// Test
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from ServletWithContext'",
//						resp -> resp.contains("Hello from ServletWithContext"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
//	}
}
