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
package org.ops4j.pax.web.itest.container.jsf;

import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWarJSFIntegrationTest extends AbstractControlledTestBase {

//	private Bundle installWarBundle;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		Bundle[] bundles = bundleContext.getBundles();
//		for (Bundle bundle : bundles) {
//			if ("org.apache.myfaces.core.api".equalsIgnoreCase(bundle
//					.getSymbolicName())
//					|| "org.apache.myfaces.core.impl".equalsIgnoreCase(bundle
//					.getSymbolicName())) {
//				bundle.stop();
//				bundle.start();
//			}
//		}
//
//		logger.info("Setting up test");
//
//		// Give the web container a second to recover after bundle restarts
//		Thread.sleep(1000);
//
//		initWebListener();
//
//		String bundlePath = "mvn:org.ops4j.pax.web.samples/war-jsf/"
//				+ VersionUtil.getProjectVersion() + "/war";
//		installWarBundle = bundleContext.installBundle(bundlePath);
//		installWarBundle.start();
//
//		waitForWebListener();
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//		if (installWarBundle != null) {
//			installWarBundle.stop();
//			installWarBundle.uninstall();
//		}
//	}
//
//
//	@Test
//	public void testSlash() throws Exception {
//		// needed to wait for fully initializing the container
//		Thread.sleep(1000);
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Please enter your name'",
//						resp -> resp.contains("Please enter your name"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-sample/");
//
//	}
//
//	@Test
//	public void testJSF() throws Exception {
//		// needed to wait for fully initializing the container
//		Thread.sleep(1000);
//
//		CookieState cookieState = new CookieState();
//
//		logger.debug("Testing JSF workflow!");
//		String response = HttpTestClientFactory.createDefaultTestClient()
//				.useCookieState(cookieState)
//				.withResponseAssertion("Response must contain 'Please enter your name'",
//						resp -> resp.contains("Please enter your name"))
//				.withResponseAssertion("Response must contain JSF-ViewState-ID",
//						resp -> {
//							logger.debug("Found JSF starting page: {}", resp);
//
//							Pattern patternViewState = Pattern
//									.compile("id=\\\"j_id_.*:javax.faces.ViewState:\\w\\\"");
//							Matcher viewStateMatcher = patternViewState.matcher(resp);
//							if (!viewStateMatcher.find()) {
//								return false;
//							}
//							String viewStateID = resp.substring(viewStateMatcher.start() + 4,
//									viewStateMatcher.end() - 1);
//
//							String substring = resp.substring(viewStateMatcher.end() + 8);
//							int indexOf = substring.indexOf("\"");
//							String viewStateValue = substring.substring(0, indexOf);
//
//							logger.debug("Found ViewState-ID '{}' with value '{}'", viewStateID, viewStateValue);
//
//							return true;
//						})
//				.withResponseAssertion("Response must contain JSF-Input-ID",
//						resp -> {
//							Pattern pattern = Pattern.compile("(input id=\"mainForm:j_id_\\w*)");
//							Matcher matcher = pattern.matcher(resp);
//							if (!matcher.find()) {
//								return false;
//							}
//
//							String inputID = resp.substring(matcher.start(), matcher.end());
//							inputID = inputID.substring(inputID.indexOf('"') + 1);
//							logger.debug("Found ID: {}", inputID);
//
//							return true;
//						})
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-sample");
//
//		Pattern patternViewState = Pattern
//				.compile("id=\\\"j_id_.*:javax.faces.ViewState:\\w\\\"");
//		Matcher viewStateMatcher = patternViewState.matcher(response);
//		if (!viewStateMatcher.find()) {
//			fail("Didn't find required ViewState ID!");
//		}
//		String viewStateID = response.substring(viewStateMatcher.start() + 4,
//				viewStateMatcher.end() - 1);
//
//		String substring = response.substring(viewStateMatcher.end() + 8);
//		int indexOf = substring.indexOf("\"");
//		String viewStateValue = substring.substring(0, indexOf);
//
//		Pattern pattern = Pattern.compile("(input id=\"mainForm:j_id_\\w*)");
//		Matcher matcher = pattern.matcher(response);
//
//		if (!matcher.find()) {
//			fail("Didn't find required input id!");
//		}
//		String inputID = response.substring(matcher.start(), matcher.end());
//		inputID = inputID.substring(inputID.indexOf('"') + 1);
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.useCookieState(cookieState)
//				.withResponseAssertion("Response from POST must contain 'Hello Dummy-User. We hope you enjoy Apache MyFaces'",
//						resp -> resp.contains("Hello Dummy-User. We hope you enjoy Apache MyFaces"))
//				.doPOST("http://127.0.0.1:8181/war-jsf-sample/faces/helloWorld.jsp")
//				.addParameter("mainForm:name", "Dummy-User")
//				.addParameter(viewStateID, viewStateValue)
//				.addParameter(inputID, "Press me")
//				.addParameter("javax.faces.ViewState", viewStateValue)
//				.addParameter("mainForm_SUBMIT", "1")
//				.executeTest();
//	}
}
