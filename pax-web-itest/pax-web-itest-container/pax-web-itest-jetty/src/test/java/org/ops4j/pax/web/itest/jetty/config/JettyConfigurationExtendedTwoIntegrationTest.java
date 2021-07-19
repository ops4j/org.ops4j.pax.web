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
package org.ops4j.pax.web.itest.jetty.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.TestServlet;

import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * Tests default virtual host and connector configuration for web apps Based on
 * JettyConfigurationIntegrationTest.java
 *
 * @author Gareth Collins
 */
@RunWith(PaxExam.class)
@Ignore("Waiting for virtual host/connector handling")
public class JettyConfigurationExtendedTwoIntegrationTest extends AbstractContainerTestBase {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		// this will install a fragment attached to pax-web-jetty bundle, so it can find "jetty.xml" resource
		// used to alter the Jetty server
		MavenArtifactProvisionOption config = mavenBundle("org.ops4j.pax.web.samples", "config-fragment-jetty")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart();
		Option[] authOptions = combine(serverOptions,
				config,
				systemProperty("org.ops4j.pax.web.default.virtualhosts").value("127.0.0.1"),
				systemProperty("org.ops4j.pax.web.default.connectors").value("default")
		);
		return combine(authOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war", () -> {
			installAndStartWebBundle("war", "/test");
		});
		configureAndWaitForServletWithMapping("", () -> {
			getHttpService(context).registerServlet("/test2", new TestServlet(), null, null);
		});
	}

	@After
	public void tearDown() {
		getHttpService(context).unregister("/test2");
	}

	// It should work if the port == 8181 (it appears that if connector is set virtual host is ignored)
	@Test
	public void testWeb() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8181/test/wc/example");
	}

	@Test
	public void testWebIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test/wc/example");
	}

	@Test
	public void testWebJettyIPWrongPort() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8282/test/wc/example");
	}

	@Test
	public void testWebJettyWrongPort() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/test/wc/example");
	}

	@Test
	public void testHttpService() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://localhost:8181/test2");
	}

	@Test
	public void testHttpServiceIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2");
	}

	@Test
	public void testHttpServiceJettyIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8282/test2");
	}

	@Test
	public void testHttpServiceJetty() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/test2");
	}

}
