/*
 * Copyright 2023 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.container.whiteboard;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;

public abstract class AbstractWhiteboardSecurityIntegrationTest extends AbstractContainerTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractWhiteboardSecurityIntegrationTest.class);

	private Bundle bundle;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/error/*",
				() -> bundle = installAndStartBundle(sampleURI("whiteboard-security")));
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle != null) {
			bundle.stop();
			bundle.uninstall();
		}
	}

	@Test
	public void testSecureAccess() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to Anonymous Servlet'",
						resp -> resp.contains("Welcome to Anonymous Servlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/app/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to Anonymous Servlet'",
						resp -> resp.contains("Welcome to Anonymous Servlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/pax-web-security/app/x");

		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "default")
				.withResponseAssertion("Response must contain 'Welcome to Protected Servlet (admins and viewers only)'",
						resp -> resp.contains("Welcome to Protected Servlet (admins and viewers only)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/secure/x");
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "default")
				.withResponseAssertion("Response must contain 'Welcome to Protected Servlet (admins and viewers only)'",
						resp -> resp.contains("Welcome to Protected Servlet (admins and viewers only)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/pax-web-security/secure/x");

		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("viewer", "viewer", "default")
				.withResponseAssertion("Response must contain 'Welcome to Protected Servlet (admins and viewers only)'",
						resp -> resp.contains("Welcome to Protected Servlet (admins and viewers only)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/secure/x");
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("viewer", "viewer", "default")
				.withResponseAssertion("Response must contain 'Welcome to Protected Servlet (admins and viewers only)'",
						resp -> resp.contains("Welcome to Protected Servlet (admins and viewers only)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/pax-web-security/secure/x");

		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "default")
				.withResponseAssertion("Response must contain 'Welcome to Secure Servlet (admins only)'",
						resp -> resp.contains("Welcome to Secure Servlet (admins only)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/very-secure/x");
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "default")
				.withResponseAssertion("Response must contain 'Welcome to Secure Servlet (admins only)'",
						resp -> resp.contains("Welcome to Secure Servlet (admins only)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/pax-web-security/very-secure/x");

		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("viewer", "viewer", "default")
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/very-secure/x");
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("viewer", "viewer", "default")
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/pax-web-security/very-secure/x");

		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("viewer", "viewer2", "default")
				.withReturnCode(HttpServletResponse.SC_UNAUTHORIZED)
				.doGETandExecuteTest("http://127.0.0.1:8181/very-secure/x");
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("viewer", "viewer2", "default")
				.withReturnCode(HttpServletResponse.SC_UNAUTHORIZED)
				.doGETandExecuteTest("http://127.0.0.1:8181/pax-web-security/very-secure/x");
	}

}
