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

package org.ops4j.pax.web.itest.jetty.war;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;

import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * This is Jetty-only test for PAXWEB-1084 (https://github.com/ops4j/org.ops4j.pax.web/issues/1375)
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
public class WarExtendedIntegrationTest extends AbstractContainerTestBase {

	private Bundle installWar1Bundle;

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		Option[] jspOptions = combine(serverOptions, paxWebJsp());
		Option[] wsOptions = combine(jspOptions, jettyWebSockets());
		return combine(wsOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
		installWar1Bundle = configureAndWaitForDeploymentUnlessInstalled("war", () -> {
			installAndStartWebBundle("war", "/war1");
		});

		configureAndWaitForDeploymentUnlessInstalled("helloworld-servlet3", () -> {
			installAndStartWebBundle("helloworld-servlet3", "/war2");
		});
	}

	@Test
	public void testWars() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war1/wc");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war2/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war3");

		// after stopping one of two wars, without PAXWEB-1084 fix, Jetty's qtp is stopped as well
		installWar1Bundle.stop();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war1/wc");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war2/hello");
	}

}
