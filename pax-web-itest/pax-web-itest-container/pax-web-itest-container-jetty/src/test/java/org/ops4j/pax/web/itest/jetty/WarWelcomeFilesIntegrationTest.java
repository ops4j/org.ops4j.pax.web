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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
public class WarWelcomeFilesIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WarWelcomeFilesIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-introspection/"
				+ VersionUtil.getProjectVersion() + "/war?"
				+ WEB_CONTEXT_PATH + "=/war-bundle&"
				+ "Bundle-SymbolicName=war1";
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
	public void testWelcomeFiles() throws Exception {
		// redirect to http://127.0.0.1:8181/war-bundle/ and then 403
		testClient.testWebPath("http://127.0.0.1:8181/war-bundle", 403);
		// no redirect, just 403
		testClient.testWebPath("http://127.0.0.1:8181/war-bundle/", 403);
		// OSGi problem with org.osgi.framework.Bundle#getEntry() not detecting directories...
		testClient.testWebPath("http://127.0.0.1:8181/war-bundle/static", "", 200, false);
		// no redirect, just 403
		testClient.testWebPath("http://127.0.0.1:8181/war-bundle/static/", 403);
		// correct welcome-file usage
		testClient.testWebPath("http://127.0.0.1:8181/war-bundle/static/misc/", "static/misc/start.txt", 200, false);
		// OSGi problem with org.osgi.framework.Bundle#getEntry() not detecting directories...
		// but we explicitly try welcome-files
		testClient.testWebPath("http://127.0.0.1:8181/war-bundle/static/misc", "static/misc/start.txt", 200, false);
		// correct welcome-file usage - even with directory name equal to context name
		testClient.testWebPath("http://127.0.0.1:8181/war-bundle/war-bundle/", "war-bundle/start.txt", 200, false);
		// OSGi problem with org.osgi.framework.Bundle#getEntry() not detecting directories...
		// but we explicitly try welcome-files
		testClient.testWebPath("http://127.0.0.1:8181/war-bundle/war-bundle", "war-bundle/start.txt", 200, false);
	}

}
