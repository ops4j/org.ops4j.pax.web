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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WebConsoleIntegrationTest extends ITestBase {
	private static final Logger LOG = LoggerFactory
			.getLogger(WarIntegrationTest.class);

	@Configuration
	public static Option[] configure() {

		return OptionUtils
				.combine(
						configureJetty(),
						workingDirectory("target/paxexam/"),
						systemProperty(
								"org.ops4j.pax.logging.DefaultServiceLog.level")
								.value("TRACE"),
						systemProperty("org.osgi.service.http.hostname").value(
								"127.0.0.1"),
						systemProperty("org.osgi.service.http.port").value(
								"8181"),
						systemProperty("java.protocol.handler.pkgs").value(
								"org.ops4j.pax.url"),
						systemProperty(
								"org.ops4j.pax.url.war.importPaxLoggingPackages")
								.value("true"),
						systemProperty("org.ops4j.pax.web.log.ncsa.enabled")
								.value("true"),
						mavenBundle()
								.groupId("org.apache.felix")
								.artifactId("org.apache.felix.bundlerepository")
								.version("1.6.2"),
						mavenBundle().groupId("org.apache.felix")
								.artifactId("org.apache.felix.configadmin")
								.version("1.2.8"),
						mavenBundle().groupId("org.apache.felix")
								.artifactId("org.apache.felix.shell")
								.version("1.4.2"),
						mavenBundle().groupId("org.apache.felix")
								.artifactId("org.apache.felix.shell.tui")
								.version("1.4.1"),
						mavenBundle().groupId("org.apache.felix")
								.artifactId("org.apache.felix.webconsole")
								.version("3.1.8"),

						// HTTP Client needed for UnitTesting
						mavenBundle("commons-codec", "commons-codec").version(
								asInProject())// ,
				// wrappedBundle(mavenBundle("org.apache.httpcomponents",
				// "httpclient", "4.1")),
				// wrappedBundle(mavenBundle("org.apache.httpcomponents",
				// "httpcore", "4.1"))
				);
	}

	@Before
	public void setUp() throws Exception {
		initServletListener(null);
		waitForServer("http://127.0.0.1:8181/");
		waitForServletListener();
	}


	@Test
	public void testBundlesPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.doGETandExecuteTest("http://localhost:8181/system/console/bundles");

		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "OSGi Management Console")
				.withResponseAssertion("Response must contain 'Apache Felix Web Console<br/>Bundles'",
						resp -> resp.contains("Apache Felix Web Console<br/>Bundles"))
				.doGETandExecuteTest("http://localhost:8181/system/console/bundles");


//		testClient.testWebPath("http://localhost:8181/system/console/bundles", "", 401,
//				false);
//
//		testClient.testWebPath("http://localhost:8181/system/console/bundles",
//				"Apache Felix Web Console<br/>Bundles", 200, true);
	}

}