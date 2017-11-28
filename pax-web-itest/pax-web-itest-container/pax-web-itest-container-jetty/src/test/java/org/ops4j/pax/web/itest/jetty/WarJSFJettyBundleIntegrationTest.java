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
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.TestConfiguration;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.CookieState;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarJSFJettyBundleIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {

		return OptionUtils
				.combine(
						configureJettyBundle(),
						TestConfiguration.jsfBundlesWithDependencies()
				);
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			if ("org.apache.myfaces.core.api".equalsIgnoreCase(bundle
					.getSymbolicName())
					|| "org.apache.myfaces.core.impl".equalsIgnoreCase(bundle
					.getSymbolicName())) {
				bundle.stop();
				bundle.start();
			}
		}

		logger.info("Setting up test");

		initWebListener();

		String bundlePath = "mvn:org.ops4j.pax.web.samples/war-jsf/"
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
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-sample/");
	}

	@Test
	public void testJSF() throws Exception {
		// needed to wait for fully initializing the container
		Thread.sleep(1000);

		// Session must be kept during test-requests
		CookieState cookieState = new CookieState();

		String response = HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-sample");

		Pattern patternViewState = Pattern
				.compile("id=\\\"j_id_.*:javax.faces.ViewState:\\w\\\"");
		Matcher viewStateMatcher = patternViewState.matcher(response);
		if (!viewStateMatcher.find()) {
			fail("Didn't find required ViewState ID!");
		}
		String viewStateID = response.substring(viewStateMatcher.start() + 4,
				viewStateMatcher.end() - 1);

		String substring = response.substring(viewStateMatcher.end() + 8);
		int indexOf = substring.indexOf("\"");
		String viewStateValue = substring.substring(0, indexOf);

		Pattern pattern = Pattern.compile("(input id=\"mainForm:j_id_\\w*)");
		Matcher matcher = pattern.matcher(response);
		if (!matcher.find()) {
			fail("Didn't find required input id!");
		}

		String inputID = response.substring(matcher.start(), matcher.end());
		inputID = inputID.substring(inputID.indexOf('"') + 1);
		logger.debug("Found ID: {}", inputID);

		HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response must contain 'Hello Dummy-User. We hope you enjoy Apache MyFaces'",
						resp -> resp.contains("Hello Dummy-User. We hope you enjoy Apache MyFaces"))
				.doPOST("http://127.0.0.1:8181/war-jsf-sample/faces/helloWorld.jsp")
				.addParameter("mainForm:name", "Dummy-User")
				.addParameter(viewStateID, viewStateValue)
				.addParameter(inputID, "Press me")
				.addParameter("javax.faces.ViewState", viewStateValue)
				.addParameter("mainForm_SUBMIT", "1")
				.executeTest();
	}
}
