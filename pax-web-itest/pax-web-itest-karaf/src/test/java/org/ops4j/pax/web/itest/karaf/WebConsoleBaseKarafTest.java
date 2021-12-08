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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public abstract class WebConsoleBaseKarafTest extends AbstractKarafTestBase {

	protected Option[] webConsoleOptions() {
		return new Option[] {
				mavenBundle("commons-fileupload", "commons-fileupload").version(System.getProperty("commons-fileupload.version")),
				mavenBundle("commons-io", "commons-io").version(System.getProperty("commons-io.version")),
				mavenBundle("org.apache.felix", "org.apache.felix.webconsole").version(System.getProperty("felix-webconsole.version")).noStart()
		};
	}

	@Before
	public void setUp() throws Exception {
		bundle("org.apache.felix.configadmin").stop(Bundle.STOP_TRANSIENT);
		final Bundle wc = bundle("org.apache.felix.webconsole");
		if (wc.getState() != Bundle.ACTIVE) {
			configureAndWaitForServletWithMapping("/system/console/res/*", wc::start);
		}
	}

	@After
	public void cleanUp() throws BundleException {
		bundle("org.apache.felix.configadmin").start();
	}

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-web-war")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-web-whiteboard")));
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

}
