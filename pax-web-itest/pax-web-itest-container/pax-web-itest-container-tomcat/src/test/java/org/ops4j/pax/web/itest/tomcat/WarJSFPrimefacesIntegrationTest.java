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
package org.ops4j.pax.web.itest.tomcat;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.TestConfiguration;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.CookieState;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarJSFPrimefacesIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WarJSFPrimefacesIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {

		return OptionUtils
				.combine(
						configureTomcat(),
						TestConfiguration.jsfBundlesWithDependencies(),
						mavenBundle().groupId("org.primefaces").artifactId("primefaces").version(asInProject())
				);
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		final Bundle[] bundles = bundleContext.getBundles();
		for (final Bundle bundle : bundles) {
			if ("org.apache.myfaces.core.api".equalsIgnoreCase(bundle
					.getSymbolicName())
					|| "org.apache.myfaces.core.impl".equalsIgnoreCase(bundle
					.getSymbolicName())) {
				bundle.stop();
				bundle.start();
			}
		}

		LOG.info("Setting up test");

		initWebListener();

		final String bundlePath = "mvn:org.ops4j.pax.web.samples/war-jsf-primefaces/"
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
	public void testSlash() throws Exception {
		// needed to wait for fully initializing the container
		Thread.sleep(1000);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8282/war-jsf-primefaces-sample/");
	}

	@Test
	@Ignore("This does work in the browser but not in the test")
	public void testJSF() throws Exception {
		// needed to wait for fully initializing the container
		Thread.sleep(1000);

		// Session must be kept during test-requests
		CookieState cookieState = new CookieState();

		final String response = HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8282/war-jsf-primefaces-sample/");

		String intermediate = response.substring(response.indexOf("name=\"javax.faces.ViewState\""));
		int indexOf = intermediate.indexOf("value=\"");
		String substring = intermediate.substring(indexOf + 7);
		indexOf = substring.indexOf("\"");
		substring = substring.substring(0, indexOf);

		HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				//.withResponseAssertion("Response must contain 'Hello Dummy-User. We hope you enjoy Apache MyFaces'",
				//		resp -> resp.contains("Hello Dummy-User. We hope you enjoy Apache MyFaces"))
				.doPOST("http://127.0.0.1:8282/war-jsf-primefaces-sample/")
				.addParameter("mainForm:name", "Dummy-User")
				.addParameter("mainForm:j_id_b", "Press+me")
				.addParameter("javax.faces.ViewState", substring)
				.addParameter("mainForm_SUBMIT", "1")
				.executeTest();
	}

	@Test
	@Ignore("what is wrong with the panelGrid?")
	public void testPrimefacesTagRendering() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.withResponseAssertion("The Primefaces-tag <p:panelGrid> was not rendered correctly.",
						resp -> !resp.matches("(?s).*<p:panelGrid.*>.*</p:panelGrid>.*"))
				.doGETandExecuteTest("http://127.0.0.1:8282/war-jsf-primefaces-sample/");
	}
}
