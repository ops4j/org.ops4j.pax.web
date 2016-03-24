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
 package org.ops4j.pax.web.itest.karaf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarManualIntegrationTest extends KarafBaseTest {

	private static final Logger LOG = LoggerFactory.getLogger(WarManualIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public Option[] config() {
		return jettyConfig();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");
		
		initWebListener();
		
		String bundlePath = "webbundle:mvn:org.ops4j.pax.web/pax-web-manual/"
				+ VersionUtil.getProjectVersion() + "/war?Web-ContextPath=/pax-web-manual";
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
	public void testManual() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text from Pax-Web-Manuel served by Karaf!",
						resp -> resp.contains("<title>Pax Web</title>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/pax-web-manual");
	}

}

