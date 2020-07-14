/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.itest.container.httpservice;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractAuthenticationIntegrationTest extends AbstractControlledTestBase {

	private Bundle installWarBundle;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/status-with-auth/*",
				() -> installWarBundle = installAndStartBundle(sampleURI("authentication")));
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}

	@Test
	public void testStatus() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Status must be available!",
						resp -> resp.contains("org.osgi.service.http.authentication.type : null"))
				.doGET("http://127.0.0.1:8181/status")
				.executeTest();
	}

	@Test
	public void testStatusAuth() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseAssertion("Unauthorized Access must be blocked!",
						resp -> resp.contains("Unauthorized"))
				.doGET("http://127.0.0.1:8181/status-with-auth")
				.executeTest();

		// "Test Realm" is configured in "AuthHttpContext" within the authentication-sample
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "Test Realm")
				.withResponseAssertion("Authorized Access must be allowed!",
						resp -> resp.contains("org.osgi.service.http.authentication.type : BASIC"))
				.doGET("http://127.0.0.1:8181/status-with-auth")
				.executeTest();
	}

}
