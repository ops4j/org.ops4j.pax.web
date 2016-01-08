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
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
public class JettyAnnotationWebappIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(JettyAnnotationWebappIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configuration() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
				.artifactId("jetty-auth-config-fragment")
				.version(VersionUtil.getProjectVersion()).noStart());
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		final String bundlePath = WEB_BUNDLE
				+ "mvn:org.mortbay.jetty/test-annotation-webapp/8.0.0.M2/war?"
				+ WEB_CONTEXT_PATH + "=/test-annotation-webapp";
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

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		List<Bundle> failedBundles = new ArrayList<Bundle>();
		for (final Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE
					&& b.getState() != Bundle.RESOLVED) {
				System.err.println("Failed - Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
				failedBundles.add(b);
				continue;
			}

			final Dictionary<String,String> headers = b.getHeaders();
			final String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Active - Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Active - Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

		if (!failedBundles.isEmpty()) {
			String failedBundlesString = "";
			for (Bundle bundle : failedBundles) {
				failedBundlesString.concat(" ").concat(bundle.toString());
			}
			fail("Bundles should be active: " + failedBundlesString);
		}
	}

	@Test
	public void testLoginPage() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/test-annotation-webapp/login.html",
				"<H1> Enter your username and password to login </H1>");

	}

	@Test
	@Ignore
	public void testLoginPageDoLogin() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/test-annotation-webapp/login.html",
				"<H1> Enter your username and password to login </H1>");

		// testClient.testWebPath("http://127.0.0.1:8181/test-annotation-webapp/j_security_check",
		// "role", 200, true);

	}
}
