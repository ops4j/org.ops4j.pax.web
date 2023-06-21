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

import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWarSecurityIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarSecurityIntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-security", () -> {
			installAndStartWebBundle("war-security", "/war-security");
		});
	}

	@Test
	public void test() throws Exception {
		// Servlet 1 - dynamically registered, with manually called jakarta.servlet.ServletRegistration.Dynamic.setServletSecurity()
		//  - /s1/* for role-admin and role-manager (all methods but HEAD)
		//  - /s1/* for role-admin (HEAD)
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_UNAUTHORIZED)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s1/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("admin2", "admin", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s1");
					ok &= resp.contains("user principal: admin2");
					ok &= resp.contains("remote user: admin2");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: true");
					ok &= resp.contains("is role-manager: false");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s1/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("manager", "manager", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s1");
					ok &= resp.contains("user principal: manager");
					ok &= resp.contains("remote user: manager");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: false");
					ok &= resp.contains("is role-manager: true");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s1/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.authenticate("manager", "manager", "Test Realm")
				.doHEAD("http://127.0.0.1:8181/war-security/s1/hello")
				.executeTest();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("am", "am", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s1");
					ok &= resp.contains("user principal: am");
					ok &= resp.contains("remote user: am");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: true");
					ok &= resp.contains("is role-manager: true");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s1/hello");

		// Servlet 2 - dynamically registered, with @ServletSecurity
		//  - /s2/* for role-admin and role-manager (all methods but HEAD)
		//  - /s2/* for role-admin (HEAD)
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_UNAUTHORIZED)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s2/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("admin2", "admin", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s2");
					ok &= resp.contains("user principal: admin2");
					ok &= resp.contains("remote user: admin2");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: true");
					ok &= resp.contains("is role-manager: false");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s2/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("manager", "manager", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s2");
					ok &= resp.contains("user principal: manager");
					ok &= resp.contains("remote user: manager");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: false");
					ok &= resp.contains("is role-manager: true");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s2/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.authenticate("manager", "manager", "Test Realm")
				.doHEAD("http://127.0.0.1:8181/war-security/s2/hello")
				.executeTest();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("am", "am", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s2");
					ok &= resp.contains("user principal: am");
					ok &= resp.contains("remote user: am");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: true");
					ok &= resp.contains("is role-manager: true");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s2/hello");

		// Servlet 3 - fully scanned from the WAR, with @ServletSecurity
		//  - /s3/* for role-admin and role-manager (all methods but HEAD)
		//  - /s3/* for role-admin (HEAD)
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_UNAUTHORIZED)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s3/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("admin2", "admin", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s3");
					ok &= resp.contains("user principal: admin2");
					ok &= resp.contains("remote user: admin2");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: true");
					ok &= resp.contains("is role-manager: false");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s3/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("manager", "manager", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s3");
					ok &= resp.contains("user principal: manager");
					ok &= resp.contains("remote user: manager");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: false");
					ok &= resp.contains("is role-manager: true");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s3/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.authenticate("manager", "manager", "Test Realm")
				.doHEAD("http://127.0.0.1:8181/war-security/s3/hello")
				.executeTest();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("am", "am", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s3");
					ok &= resp.contains("user principal: am");
					ok &= resp.contains("remote user: am");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: true");
					ok &= resp.contains("is role-manager: true");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s3/hello");

		// Servlet 4 - added in web.xml
		//  - /s4/secure/* for role-admin (GET)
		//  - /s4/secure/very/* for role-manager (GET)
		//  - additionally there's <security-role-ref> admin -> role-admin for this servlet
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Doesn't have to be authorized", resp -> {
					boolean ok = resp.contains("servlet: s4");
					ok &= resp.contains("user principal: null");
					ok &= resp.contains("remote user: null");
					ok &= resp.contains("auth type: null");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: false");
					ok &= resp.contains("is role-manager: false");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s4/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("admin2", "admin", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s4");
					ok &= resp.contains("user principal: admin2");
					ok &= resp.contains("remote user: admin2");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: true");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: true");
					ok &= resp.contains("is role-manager: false");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s4/secure/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				// this "admin" has role "admin" but role links don't work here
				.authenticate("admin", "admin", "Test Realm")
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s4/secure/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.authenticate("manager", "manager", "Test Realm")
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s4/secure/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("manager", "manager", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s4");
					ok &= resp.contains("user principal: manager");
					ok &= resp.contains("remote user: manager");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: false");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: false");
					ok &= resp.contains("is role-manager: true");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s4/secure/very/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.authenticate("admin", "admin", "Test Realm")
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s4/secure/very/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.authenticate("admin2", "admin", "Test Realm")
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s4/secure/very/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("am", "am", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s4");
					ok &= resp.contains("user principal: am");
					ok &= resp.contains("remote user: am");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: true");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: true");
					ok &= resp.contains("is role-manager: true");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s4/secure/very/hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.authenticate("am", "am", "Test Realm")
				.withResponseAssertion("Must be authorized", resp -> {
					boolean ok = resp.contains("servlet: s4");
					ok &= resp.contains("user principal: am");
					ok &= resp.contains("remote user: am");
					ok &= resp.contains("auth type: BASIC");
					ok &= resp.contains("is admin: true");
					ok &= resp.contains("is manager: false");
					ok &= resp.contains("is role-admin: true");
					ok &= resp.contains("is role-manager: true");
					return ok;
				})
				.doGETandExecuteTest("http://127.0.0.1:8181/war-security/s4/secure/hello");

	}

}
