/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.container.httpservice;

import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;
import org.osgi.framework.Bundle;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWebContainerSecuredIntegrationTest extends AbstractControlledTestBase {

	private Bundle installWarBundle;

//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		initWebListener();
//		final String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
//				+ VersionUtil.getProjectVersion();
//		installWarBundle = installAndStartBundle(bundlePath);
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
//	@Test
//	public void testWebContextPath() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("https://127.0.0.1:8443/helloworld/wc");
//	}
}
