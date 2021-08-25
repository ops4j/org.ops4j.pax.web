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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.utils.client.CookieState;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public abstract class JSFBaseKarafTest extends AbstractKarafTestBase {

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-jsf23-embedded", () -> {
			installAndStartBundle(sampleWarURI("war-jsf23-embedded"));
		});
	}

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-web-war")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-web-whiteboard")));
	}

	@Test
	public void testSlash() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain 'Hello from JSF 2.3 example running on Pax Web 8'",
						resp -> resp.contains("Hello from JSF 2.3 example running on Pax Web 8"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf23-embedded/");
	}

	@Test
	public void testJSF() throws Exception {
		CookieState cookieState = new CookieState();

		LOG.debug("Testing JSF workflow!");
		String response = HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response must contain 'Hello from JSF 2.3 example running on Pax Web 8'",
						resp -> resp.contains("Hello from JSF 2.3 example running on Pax Web 8"))
				.withResponseAssertion("Response must contain JSF-ViewState-ID",
						resp -> {
							LOG.debug("Found JSF starting page: {}", resp);

							Pattern patternViewState = Pattern.compile("id=\"j_id_.*:javax.faces.ViewState:\\w\"");
							Matcher viewStateMatcher = patternViewState.matcher(resp);
							if (!viewStateMatcher.find()) {
								return false;
							}
							String viewStateID = resp.substring(viewStateMatcher.start() + 4, viewStateMatcher.end() - 1);

							String substring = resp.substring(viewStateMatcher.end() + 8);
							int indexOf = substring.indexOf("\"");
							String viewStateValue = substring.substring(0, indexOf);

							LOG.debug("Found ViewState-ID '{}' with value '{}'", viewStateID, viewStateValue);

							return true;
						})
				.withResponseAssertion("Response must contain JSF-Input-ID",
						resp -> {
							Pattern pattern = Pattern.compile("(input id=\"mainForm:j_id_\\w*)");
							Matcher matcher = pattern.matcher(resp);
							if (!matcher.find()) {
								return false;
							}

							String inputID = resp.substring(matcher.start(), matcher.end());
							inputID = inputID.substring(inputID.indexOf('"') + 1);
							LOG.debug("Found ID: {}", inputID);

							return true;
						})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf23-embedded");

		Pattern patternViewState = Pattern.compile("id=\"j_id_.*:javax.faces.ViewState:\\w\"");
		Matcher viewStateMatcher = patternViewState.matcher(response);
		if (!viewStateMatcher.find()) {
			fail("Didn't find required ViewState ID!");
		}
		String viewStateID = response.substring(viewStateMatcher.start() + 4, viewStateMatcher.end() - 1);

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

		HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response from POST must contain 'Hello world!'",
						resp -> resp.contains("Hello world!"))
				.doPOST("http://127.0.0.1:8181/war-jsf23-embedded/start.xhtml")
				.addParameter("mainForm:what", "world")
				.addParameter(viewStateID, viewStateValue)
				.addParameter(inputID, "say")
				.addParameter("javax.faces.ViewState", viewStateValue)
				.addParameter("mainForm_SUBMIT", "1")
				.executeTest();
	}

}
