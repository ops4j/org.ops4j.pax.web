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
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractJerseyCustomContextIntegrationTest extends AbstractContainerTestBase {
//	private Bundle installWarBundle;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		String bundlePath = "mvn:org.ops4j.pax.web.samples/web-jersey/" + VersionUtil.getProjectVersion();
//		initServletListener();
//		installWarBundle = installAndStartBundle(bundlePath);
//		waitForServer("http://127.0.0.1:8181/");
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
//	public void testRoot() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'New session created'", resp -> resp.contains("New session created"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/");
//		// test image-serving
//		HttpTestClientFactory.createDefaultTestClient()
//				.doGETandExecuteTest("http://127.0.0.1:8181/images/success.png");
//	}

}
