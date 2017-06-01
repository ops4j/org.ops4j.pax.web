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
package org.ops4j.pax.web.itest.karaf;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public class WebConsoleKarafTest extends KarafBaseTest {

	private static Logger LOG = LoggerFactory.getLogger(WebConsoleKarafTest.class);

	@Configuration
	public Option[] config() {
		return combine(
				jettyConfig(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.bundlerepository")
						.version("1.6.2"),
//				mavenBundle().groupId("org.apache.felix")
//						.artifactId("org.apache.felix.configadmin")
//						.version("1.2.8"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.shell").version("1.4.2"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.shell.tui")
						.version("1.4.1"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.webconsole")
						.version("3.1.8"));
	}

	@Test
	public void test() throws Exception {
		Thread.sleep(4000);

		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-war")));
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-whiteboard")));
	}

	@Test
	public void testBundlesPathWithoutAuthentication() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(401)
				.doGETandExecuteTest("http://localhost:8181/system/console/bundles");
	}

	@Test
	public void testBundlesPathWithAuthentication() throws Exception {
		createTestClientForKaraf()
				.authenticate("admin", "admin", "OSGi Management Console")
				.withResponseAssertion("Response must contain text served by Felix Console!",
						resp -> resp.contains("Apache Felix Web Console<br/>Bundles"))
				.doGETandExecuteTest("http://localhost:8181/system/console/bundles");
	}

	@Before
	public void setUp() throws Exception {
		initServletListener();

		if (featuresService == null) {
			throw new RuntimeException("Featuresservice is null");
		}

		waitForServletListener();
	}

}
