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

import javax.servlet.http.HttpServletResponse;

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
 * @author Grzegorz Grzybek
 */
public abstract class AbstractWarExtendedIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarExtendedIntegrationTest.class);

	private Bundle installWar1Bundle;
	private Bundle installWar2Bundle;

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war/"
				+ VersionUtil.getProjectVersion() + "/war?"
				+ WEB_CONTEXT_PATH + "=/war1";
		installWar1Bundle = bundleContext.installBundle(bundlePath);
		installWar1Bundle.start();

		bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/helloworld-servlet3/"
				+ VersionUtil.getProjectVersion() + "/war?"
				+ WEB_CONTEXT_PATH + "=/war2";
		installWar2Bundle = bundleContext.installBundle(bundlePath);
		installWar2Bundle.start();

		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWar1Bundle != null) {
			installWar1Bundle.stop();
			installWar1Bundle.uninstall();
		}
		if (installWar2Bundle != null) {
			installWar2Bundle.stop();
			installWar2Bundle.uninstall();
		}
	}

	@Test
	public void testWars() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war1/wc");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war2/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war3");

		// after stopping one of two wars, without PAXWEB-1084 fix, Jetty's qtp is stopped as well
		installWar1Bundle.stop();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war1/wc");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war2/hello");
	}

}
