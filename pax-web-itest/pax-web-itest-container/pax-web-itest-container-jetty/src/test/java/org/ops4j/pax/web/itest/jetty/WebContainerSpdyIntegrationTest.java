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

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
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
public class WebContainerSpdyIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WebContainerSpdyIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return OptionUtils.combine(
		        configureSpdyJetty(),
		        systemProperty("org.osgi.service.http.secure.enabled").value(
						"true"),
				systemProperty("org.ops4j.pax.web.ssl.keystore").value(
						WebContainerSpdyIntegrationTest.class.getClassLoader().getResource("keystore").getFile()),
				systemProperty("org.ops4j.pax.web.ssl.password").value(
						"password"),
				systemProperty("org.ops4j.pax.web.ssl.keypassword").value(
						"password"),
				systemProperty("org.ops4j.pax.web.ssl.clientauthneeded").value(
						"required"));
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		final String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (final Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE
					&& b.getState() != Bundle.RESOLVED) {
			    if (!b.getSymbolicName().contains("alpn"))
			        fail("Bundle should be active: " + b);
			}

			final Dictionary<String, String> headers = b.getHeaders();
			final String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

	}

	@Test
	public void testWebContextPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("https://127.0.0.1:8443/helloworld/wc");
	}
}
