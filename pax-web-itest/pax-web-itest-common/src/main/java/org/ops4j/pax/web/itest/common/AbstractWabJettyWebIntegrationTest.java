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
package org.ops4j.pax.web.itest.common;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Beryozkin
 */
public abstract class AbstractWabJettyWebIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWabJettyWebIntegrationTest.class);

	private Bundle installWarBundle;

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = "mvn:org.ops4j.pax.web.samples/wab-jetty-web/"
				+ VersionUtil.getProjectVersion() + "/jar";

		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}


	@Test
	public void testDispatchJsp() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'It works'",
						resp -> resp.contains("It works"))
				.doGETandExecuteTest("http://127.0.0.1:8181/wab-jetty-web/index.html");
	}


}
