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
 package org.ops4j.pax.web.itest.jetty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarSpringIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WarSpringIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}


	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");
		
		initWebListener();
		
		String bundlePath = "mvn:org.ops4j.pax.web.samples/war-spring/"
				+ VersionUtil.getProjectVersion() + "/war";
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
	public void testWC() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h2>Spring MVC - Hello World</h2>'",
						resp -> resp.contains("<h2>Spring MVC - Hello World</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-spring");

//		testClient.testWebPath("http://127.0.0.1:8181/war-spring", "<h2>Spring MVC - Hello World</h2>");
			
	}

	@Test
	public void testCallController() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h2>Spring MVC - Hello World</h2>'",
						resp -> resp.contains("<h2>Spring MVC - Hello World</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-spring");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Done! Spring MVC works like a charm!'",
						resp -> resp.contains("Done! Spring MVC works like a charm!"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-spring/helloWorld.do");

//		testClient.testWebPath("http://127.0.0.1:8181/war-spring", "<h2>Spring MVC - Hello World</h2>");
//		testClient.testWebPath("http://127.0.0.1:8181/war-spring/helloWorld.do", "Done! Spring MVC works like a charm!");
	}
	
}