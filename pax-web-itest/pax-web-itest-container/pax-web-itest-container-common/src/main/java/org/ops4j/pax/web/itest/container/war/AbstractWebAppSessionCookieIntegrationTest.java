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

package org.ops4j.pax.web.itest.container.war;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.CookieState;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Grzegorz Grzybek
 */
public class AbstractWebAppSessionCookieIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWebAppSessionCookieIntegrationTest.class);

	@Inject
	private ConfigurationAdmin caService;

	private Bundle wab = null;

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("war-introspection-bundle", () -> {
			installAndStartBundle(sampleWarURI("war-introspection-bundle"));
		});
	}

	@Test
	public void testSessions() throws Exception {
		CookieState cookieJar = new CookieState();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_ACCEPTABLE)
				.withResponseAssertion("Response must contain 'Please log in first'",
						resp -> resp.contains("Please log in first"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/sessions/visit");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome Grzegorz'",
						resp -> resp.contains("Welcome Grzegorz"))
				.useCookieState(cookieJar)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/sessions/login?user=Grzegorz");

		for (int n = 0; n < 3; n++) {
			int finalN = n;
			HttpTestClientFactory.createDefaultTestClient()
					.withResponseAssertion("Response must contain visit number",
							resp -> resp.contains("That's your " + (finalN + 1) + " visit after logging in, Grzegorz"))
					.useCookieState(cookieJar)
					.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/sessions/visit");
		}

		assertThat(cookieJar.getCookieStore().getCookies().get(0).getName(), equalTo("JSID"));
	}

}
