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
package org.ops4j.pax.web.itest.container.war.todo.jsf;

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWarJSFWiredIntegrationTest extends AbstractContainerTestBase {

//	@Configuration
//	public Option[] configure() {
//		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
//		Option[] jspOptions = combine(serverOptions, paxWebJsp());
//		// only the dependencies, because myfaces jars and commons-* jars are packaged within the WAB
//		Option[] jsfOptions = combine(jspOptions, myfacesDependencies());
//		return combine(jsfOptions, paxWebExtenderWar());
//	}
//
//	private Bundle wab;
//
//	@Before
//	public void setUp() throws Exception {
//		wab = configureAndWaitForDeploymentUnlessInstalled("war-jsf23-embedded", () -> {
//			installAndStartBundle(sampleWarURI("war-jsf23-embedded"));
//		});
//	}
//
//	@Test
//	public void testSlash() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Please enter your name'",
//						resp -> resp.contains("Please enter your name"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf23-embedded/");
//	}
//
//	@Test
//	public void testJSF() throws Exception {
//		// needed to wait for fully initializing the container
//		Thread.sleep(1000);
//
//		CookieState cookieState = new CookieState();
//
//		LOG.debug("Testing JSF workflow!");
//		String response = HttpTestClientFactory.createDefaultTestClient()
//				.useCookieState(cookieState)
//				.withResponseAssertion("Response must contain 'Please enter your name'",
//						resp -> resp.contains("Please enter your name"))
//				.withResponseAssertion("Response must contain JSF-ViewState-ID",
//						resp -> {
//							LOG.debug("Found JSF starting page: {}", resp);
//
//							Pattern patternViewState = Pattern.compile("id=\"j_id_.*:javax.faces.ViewState:\\w\"");
//							Matcher viewStateMatcher = patternViewState.matcher(resp);
//							if (!viewStateMatcher.find()) {
//								return false;
//							}
//							String viewStateID = resp.substring(viewStateMatcher.start() + 4, viewStateMatcher.end() - 1);
//
//							String substring = resp.substring(viewStateMatcher.end() + 8);
//							int indexOf = substring.indexOf("\"");
//							String viewStateValue = substring.substring(0, indexOf);
//
//							LOG.debug("Found ViewState-ID '{}' with value '{}'", viewStateID, viewStateValue);
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
//							LOG.debug("Found ID: {}", inputID);
//
//							return true;
//						})
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf23-embedded");
//
//		Pattern patternViewState = Pattern.compile("id=\"j_id_.*:javax.faces.ViewState:\\w\"");
//		Matcher viewStateMatcher = patternViewState.matcher(response);
//		if (!viewStateMatcher.find()) {
//			fail("Didn't find required ViewState ID!");
//		}
//		String viewStateID = response.substring(viewStateMatcher.start() + 4, viewStateMatcher.end() - 1);
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
//				.doPOST("http://127.0.0.1:8181/war-jsf23-embedded/faces/helloWorld.jsp")
//				.addParameter("mainForm:name", "Dummy-User")
//				.addParameter(viewStateID, viewStateValue)
//				.addParameter(inputID, "Press me")
//				.addParameter("javax.faces.ViewState", viewStateValue)
//				.addParameter("mainForm_SUBMIT", "1")
//				.executeTest();
//	}

}
