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
package org.ops4j.pax.web.itest.undertow;

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

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class AuthenticationIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureUndertow();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		String bundlePath = "mvn:org.ops4j.pax.web.samples/authentication/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
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
				.withResponseAssertion("Response must contain 'org.osgi.service.http.authentication.type : null'",
						resp -> resp.contains("org.osgi.service.http.authentication.type : null"))
				.doGETandExecuteTest("http://127.0.0.1:8181/status");
	}

	@Test
	public void testStatusAuth() throws Exception {

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseAssertion("Response must contain 'Unauthorized'",
						resp -> resp.contains("Unauthorized"))
				.doGETandExecuteTest("http://127.0.0.1:8181/status-with-auth");

		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "Test Realm")
				.withResponseAssertion("Response must contain 'org.osgi.service.http.authentication.type : BASIC'",
						resp -> resp.contains("org.osgi.service.http.authentication.type : BASIC"))
				.doGETandExecuteTest("http://127.0.0.1:8181/status-with-auth");
	}

}
