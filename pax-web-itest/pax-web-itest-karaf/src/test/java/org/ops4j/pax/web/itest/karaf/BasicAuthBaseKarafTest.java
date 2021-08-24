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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public abstract class BasicAuthBaseKarafTest extends AbstractKarafTestBase {

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-authentication", () -> {
			installAndStartWebBundle("war-authentication", "/war-authentication");
		});
	}

	@Test
	public void testWC() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("WAR-Authentication-Sample must be available!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc");
	}

	@Test
	public void testWCExample() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(401)
				.withResponseAssertion("Unauthorized Access must be blocked!",
						resp -> resp.contains("Unauthorized"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/example");

		createTestClientForKaraf()
				.authenticate("admin", "admin", "Test Realm")
				.withResponseAssertion("Authorized Access must be allowed!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/example");
	}

	@Test
	public void testWCAdditionalSample() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(401)
				.withResponseAssertion("Unauthorized Access must be blocked for '/additionalsample'!",
						resp -> resp.contains("Unauthorized"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/additionalsample");

		createTestClientForKaraf()
				.authenticate("admin", "admin", "Test Realm")
				.withResponseAssertion("Authorized Access must be allowed for '/additionalsample'!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/additionalsample");
	}

	@Test
	public void testWcSn() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("WAR-Authentication-Sample under sn-path must be available!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/sn");
	}

	@Test
	public void testSlash() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("WAR-Authentication-Sample must be available without /wc path!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/");
	}

}
