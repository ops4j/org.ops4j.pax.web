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
 package org.ops4j.pax.web.itest.jetty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class AuthenticationIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return combine(configureJetty(),
				mavenBundle().groupId("commons-codec").artifactId("commons-codec").versionAsInProject()
				);
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		String bundlePath = "mvn:org.ops4j.pax.web.samples/authentication/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForWebListener();
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
