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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public abstract class WarBaseKarafTest extends AbstractKarafTestBase {

	private Bundle wab;

	@Before
	public void setup() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war",
				() -> wab = installAndStartWebBundle("war", "/war"));
	}

	@After
	public void tearDown() throws BundleException {
		if (wab != null) {
			wab.stop();
			wab.uninstall();
		}
	}

	@Test
	public void testWC() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text from served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
	}

	@Test
	public void testWCExample() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/example");

		createTestClientForKaraf()
				.doGETandExecuteTest("http://127.0.0.1:8181/war/images/logo.png");
	}

	@Test
	public void testWCSN() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/sn");
	}

	@Test
	public void testSlash() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/");
	}

	@Test
	public void testSubJSP() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h2>Hello World!</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/subjsp");
	}

	@Test
	public void testErrorJSPCall() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain text from error-page served by Karaf!",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/error.jsp");
	}

	@Test
	public void testWrongServlet() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain text from error-page served by Karaf!",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wrong/");
	}

}
