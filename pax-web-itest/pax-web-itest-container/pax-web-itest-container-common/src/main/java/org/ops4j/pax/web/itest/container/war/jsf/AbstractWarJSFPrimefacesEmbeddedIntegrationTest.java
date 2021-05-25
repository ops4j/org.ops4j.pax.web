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
package org.ops4j.pax.web.itest.container.war.jsf;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.CookieState;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWarJSFPrimefacesEmbeddedIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarJSFPrimefacesEmbeddedIntegrationTest.class);

	private Bundle wab;

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("war-primefaces-embedded", () -> {
			installAndStartBundle(sampleWarURI("war-primefaces-embedded"));
		});
	}

	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-primefaces-embedded/");
	}

	@Test
	public void testJSF() throws Exception {
		// Session must be kept during test-requests
		CookieState cookieState = new CookieState();

		final String response = HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-primefaces-embedded/");

		String viewState = extractJsfViewState(response);

		HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response must contain 'Hello Dummy-User. We hope you enjoy Apache MyFaces'",
						resp -> resp.contains("Hello Dummy-User. We hope you enjoy Apache MyFaces"))
				.doPOST("http://127.0.0.1:8181/war-primefaces-embedded/helloWorld.xhtml")
				.addParameter("mainForm:name", "Dummy-User")
				.addParameter("mainForm:j_id_b", "Press me")
				.addParameter("javax.faces.ViewState", viewState)
				.addParameter("mainForm_SUBMIT", "1")
				.executeTest();
	}

	@Test
	public void testPrimefacesTagRendering() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.withResponseAssertion("The Primefaces-tag <p:panelGrid> was not rendered correctly.",
						resp -> !resp.matches("(?s).*<p:panelGrid.*>.*</p:panelGrid>.*"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-primefaces-embedded/");
	}

	/**
	 * <p>
	 *     JSF uses a hidden input-field which carries around a JSF internal View-State. The View-State is necessary
	 *     when form submits are tested with a POST-request.
	 * </p>
	 * <p>
	 *     When testing a POST against JSF, a prior GET has to be made!
	 *     This method extracts the View-State from prior GET.
	 * </p>
	 * @param response the response from a initial GET-request
	 * @return found View-State
	 * @throws IllegalStateException when no View-State was found
	 */
	protected String extractJsfViewState(String response) {
		String intermediate = response.substring(response.indexOf("name=\"javax.faces.ViewState\""));
		int indexOf = intermediate.indexOf("value=\"");
		String substring = intermediate.substring(indexOf + 7);
		indexOf = substring.indexOf("\"");
		String viewstate = substring.substring(0, indexOf);
		if (viewstate.trim().length() == 0) {
			throw new IllegalStateException("No JSF-View-State was found in response!");
		}
		return viewstate;
	}

}
