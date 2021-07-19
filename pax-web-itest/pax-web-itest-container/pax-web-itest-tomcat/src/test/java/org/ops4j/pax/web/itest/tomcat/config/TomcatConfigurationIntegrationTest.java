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
package org.ops4j.pax.web.itest.tomcat.config;

import java.io.File;
import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class TomcatConfigurationIntegrationTest extends AbstractContainerTestBase {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebTomcat());
		// this will install a fragment attached to pax-web-tomcat bundle, so it can find "tomcat-server.xml" resource
		// used to alter the Tomcat server
		MavenArtifactProvisionOption auth = mavenBundle("org.ops4j.pax.web.samples", "config-fragment-tomcat")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart();
		return combine(combine(serverOptions, paxWebExtenderWar()), auth);
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war", () -> {
			installAndStartWebBundle("war", "/test");
		});
	}

	@Test
	public void testWeb() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8324/test/wc/example");
		// the valve configured in the tomcat-server.xml should have written an access log
		checkAccessLog();
	}

	@Test
	public void testWebIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8324/test/wc/example");
		// the valve configured in the tomcat-server.xml should have written an access log
		checkAccessLog();
	}

	/*
	 * The tomcat-server.xml contains another connector with an alternate port. Check that this also works
	 */
	@Test
	public void testWebAlternatePort() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8325/test/wc/example");
		// the valve configured in the tomcat-server.xml should have written an access log
		checkAccessLog();
	}

	/*
	 * The default connector should bind to 0.0.0.0 try another address
	 */
	@Test
	public void testWebAlternateIp() throws Exception {
		String hostname = InetAddress.getLocalHost().getHostAddress();
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://" + hostname + ":8324/test/wc/example");
		// the valve configured in the tomcat-server.xml should have written an access log
		checkAccessLog();
	}

	private void checkAccessLog() {
		File[] files = getLogDir().listFiles((dir, name) -> name.matches("localhost_access_log.*txt"));
		assertTrue("http access log is missing", files != null && files.length == 1);
	}

	private static void purgeLogDir() {
		purgeDirectory(getLogDir());
	}

	private static File getLogDir() {
		File logDir = new File("target/tomcat/logs");
		if (logDir.exists() && !logDir.isDirectory()) {
			logDir.delete();
		}
		if (!logDir.exists()) {
			logDir.mkdirs();
		}
		return logDir;
	}

	private static void purgeDirectory(File dir) {
		for (File file : dir.listFiles()) {
			if (!file.isDirectory()) {
				file.delete();
			}
		}
	}

}
