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

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWarBasicAuthIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarBasicAuthIntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-authentication", () -> {
			installAndStartWebBundle("war-authentication", "/war-authentication");
		});
	}

	@Test
	public void testWC() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc");
	}

	@Test
	public void testWCExample() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseAssertion("Response must contain 'Unauthorized'",
						resp -> resp.contains("Unauthorized"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/example");

		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "Test Realm")
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/example");
	}

	@Test
	public void testWCAdditionalSample() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseAssertion("Response must contain 'Unauthorized'",
						resp -> resp.contains("Unauthorized"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/additionalsample");

		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "Test Realm")
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/additionalsample");
	}

	@Test
	public void testWcSn() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/wc/sn");
	}

	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-authentication/");
	}

}
