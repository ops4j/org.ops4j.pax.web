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

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;

/**
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
public abstract class AbstractWarWelcomeFilesIntegrationTest extends AbstractContainerTestBase {

//	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarWelcomeFilesIntegrationTest.class);
//
//	private Bundle installWarBundle;
//
//	@Configuration
//	public static Option[] configure() {
//		return configureUndertow();
//	}
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		LOG.info("Setting up test");
//
//		initWebListener();
//
//		String bundlePath = WEB_BUNDLE
//				+ "mvn:org.ops4j.pax.web.samples/war-introspection/"
//				+ VersionUtil.getProjectVersion() + "/war?"
//				+ WEB_CONTEXT_PATH + "=/war-bundle&"
//				+ "Bundle-SymbolicName=war1";
//		installWarBundle = bundleContext.installBundle(bundlePath);
//		installWarBundle.start();
//
//		waitForWebListener();
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//		if (installWarBundle != null) {
//			installWarBundle.stop();
//			installWarBundle.uninstall();
//		}
//	}
//
//	@Test
//	public void testWelcomeFiles() throws Exception {
//		// redirect to http://127.0.0.1:8181/war-bundle/ and then 403
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(403)
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle");
//		// no redirect, just 403
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(403)
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/");
//		// OSGi problem with org.osgi.framework.Bundle#getEntry() not detecting directories...
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(200)
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static");
//		// no redirect, just 403
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(403)
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/");
//		// correct welcome-file usage
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Should return start.txt content",
//						r -> r.contains("static/misc/start.txt"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/misc/");
//		// OSGi problem with org.osgi.framework.Bundle#getEntry() not detecting directories...
//		// but we explicitly try welcome-files
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Should return start.txt content",
//						r -> r.contains("static/misc/start.txt"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/misc");
//		// correct welcome-file usage - even with directory name equal to context name
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Should return start.txt content",
//						r -> r.contains("war-bundle/start.txt"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/war-bundle/");
//		// OSGi problem with org.osgi.framework.Bundle#getEntry() not detecting directories...
//		// but we explicitly try welcome-files
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Should return start.txt content",
//						r -> r.contains("war-bundle/start.txt"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/war-bundle");
//	}

}
