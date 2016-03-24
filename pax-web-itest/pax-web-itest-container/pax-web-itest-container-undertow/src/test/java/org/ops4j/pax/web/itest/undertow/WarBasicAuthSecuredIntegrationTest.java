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
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarBasicAuthSecuredIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configuration() {
		return OptionUtils.combine(
				configureUndertow(),
				mavenBundle().groupId("commons-codec").artifactId("commons-codec").versionAsInProject(),
				systemProperty("org.osgi.service.http.secure.enabled").value("true"),
				systemProperty("org.ops4j.pax.web.ssl.keystore").value(
						WarBasicAuthSecuredIntegrationTest.class.getClassLoader().getResource("keystore").getFile()),
//						"src/test/resources/keystore"),
				systemProperty("org.ops4j.pax.web.ssl.password").value("password"),
				systemProperty("org.ops4j.pax.web.ssl.keypassword").value("password"),
				systemProperty("org.ops4j.pax.web.ssl.clientauthneeded").value("required"),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("undertow-auth-config-fragment")
						.version(VersionUtil.getProjectVersion()).noStart());
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-authentication/"
				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH
				+ "=/war-authentication";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

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
	public void testWC() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/wc");

//		testClient.testWebPath("https://127.0.0.1:8443/war-authentication/wc",
//				"<h1>Hello World</h1>");
	}

	@Test
	public void testWebContainerExample() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseAssertion("Response must contain 'Unauthorized'",
						resp -> resp.contains("Unauthorized"))
				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/wc/example");
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "Test Realm")
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/wc/example");

//		testClient.testWebPath("https://127.0.0.1:8443/war-authentication/wc/example",
//				"Unauthorized", 401, false);
//		testClient.testWebPath("https://127.0.0.1:8443/war-authentication/wc/example",
//				"<h1>Hello World</h1>", 200, true);

		
	}

	@Test
	public void testWebContainerSN() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/wc/sn");

//		testClient.testWebPath("https://127.0.0.1:8443/war-authentication/wc/sn",
//				"<h1>Hello World</h1>");

	}

	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("https://127.0.0.1:8443/war-authentication/wc/sn");

//		testClient.testWebPath("https://127.0.0.1:8443/war-authentication/wc/sn",
//				"<h1>Hello World</h1>");
	}

}
