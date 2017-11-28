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
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractKarafDocWarIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractKarafDocWarIntegrationTest.class);

	private Bundle installWarBundle;

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		// we have to install with webbundle:, because manual.war is a bundle that doesn't
		// import javax.servlet package, which is needed when processing using pax-web-extender-war
		String bundlePath = WEB_BUNDLE
				+ "mvn:org.apache.karaf/manual/3.0.1/war";
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
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Apache Karaf'",
						resp -> resp.contains("Apache Karaf"))
				.doGETandExecuteTest("http://127.0.0.1:8181/karaf-doc");
	}

}