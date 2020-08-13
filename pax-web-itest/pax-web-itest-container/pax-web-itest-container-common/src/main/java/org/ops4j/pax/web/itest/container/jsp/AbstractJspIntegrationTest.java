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
package org.ops4j.pax.web.itest.container.jsp;

import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractJspIntegrationTest extends AbstractControlledTestBase {

//	private Bundle installWarBundle;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		/*
//		 * Tomcat will start a default root context. This will not hurt, but if we initialize the
//		 * ServletListener too early it will detect this startup and will start the test before the
//		 * Servlet configured here is registered. Therefore we wait for a second before we initialize
//		 * the ServletListener and register the configuration.
//		 */
//		Thread.sleep(1000);
//
//		initWebListener();
//		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-jsp/" + VersionUtil.getProjectVersion();
//		installWarBundle = installAndStartBundle(bundlePath);
//		// TODO this is not a war bundle. web listener is never called
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
//	public void testSimpleJsp() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://localhost:8181/helloworld/jsp/simple.jsp");
//	}
//
//	@Test
//	public void testTldJsp() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello World'",
//						resp -> resp.contains("Hello World"))
//				.doGETandExecuteTest("http://localhost:8181/helloworld/jsp/using-tld.jsp");
//	}
//
//	@Test
//	public void testPrecompiled() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://localhost:8181/helloworld/jspc/simple.jsp");
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello World'",
//						resp -> resp.contains("Hello World"))
//				.doGETandExecuteTest("http://localhost:8181/helloworld/jspc/using-tld.jsp");
//	}
}
