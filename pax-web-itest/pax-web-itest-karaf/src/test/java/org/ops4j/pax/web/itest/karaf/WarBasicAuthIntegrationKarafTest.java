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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarBasicAuthIntegrationKarafTest extends KarafBaseTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(WarBasicAuthIntegrationKarafTest.class);

	private Bundle warBundle;

	@Configuration
	public Option[] configuration() {
		return jettyConfig();
	}
	
	@Test
	public void testWC() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc",
				"<h1>Hello World</h1>");

	}

	@Test
	public void testWCExample() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/example",
				"Unauthorized", 401, false);

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/example",
				"<h1>Hello World</h1>", 200, true);

	}

	@Test
	public void testWCAdditionalSample() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/additionalsample",
				"Unauthorized", 401, false);

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/additionalsample",
				"<h1>Hello World</h1>", 200, true);

	}
	
	@Test
	public void testWcSn() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/sn",
				"<h1>Hello World</h1>");

	}

	@Test
	public void testSlash() throws Exception {

		LOG.info("Starting test ...");
		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/",
				"<h1>Hello World</h1>");
		LOG.info("...Done");
	}
	
	@Before
	public void setUp() throws Exception {

		initWebListener();

		String warUrl = "webbundle:mvn:org.ops4j.pax.web.samples/war-authentication/"
				+ getProjectVersion() + "/war?Web-ContextPath=/war-authentication";
		warBundle = bundleContext.installBundle(warUrl);
		warBundle.start();

		waitForWebListener();

		int failCount = 0;
		while (warBundle.getState() != Bundle.ACTIVE) {
			Thread.sleep(500);
			if (failCount > 500)
				throw new RuntimeException(
						"Required war-bundles is never active");
			failCount++;
		}

	}

	@After
	public void tearDown() throws BundleException {
		if (warBundle != null) {
			warBundle.stop();
			warBundle.uninstall();
		}
	}

}
