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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WaitCondition2;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import javax.inject.Inject;
import java.util.Arrays;

import static org.junit.Assert.fail;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardRestartIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Inject
	private BundleContext ctx;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard/" + VersionUtil.getProjectVersion();
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
	public void testWhiteBoardRoot() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to the Welcome page'",
						resp -> resp.contains("Welcome to the Welcome page"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

	@Test
	public void testWhiteBoardForbidden() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.doGETandExecuteTest("http://127.0.0.1:8181/forbidden");
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter was there before'",
						resp -> resp.contains("Filter was there before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/filtered");
	}

	@Test
	public void testWhiteBoardRootRestart() throws Exception {
		// find Whiteboard-bundle
		final Bundle whiteBoardBundle = Arrays.stream(ctx.getBundles()).filter(bundle ->
				"org.ops4j.pax.web.pax-web-extender-whiteboard".equalsIgnoreCase(bundle.getSymbolicName()))
				.findFirst().orElseThrow(() -> new AssertionError("no Whiteboard bundle found"));

		// stop Whiteboard bundle
		whiteBoardBundle.stop();

		new WaitCondition2("Check if Whiteboard bundle gets stopped",
				() -> whiteBoardBundle.getState() == Bundle.RESOLVED)
				.waitForCondition(10000, 500, () -> fail("Whiteboard bundle did not stop in time"));

		// start Whiteboard bundle again
		whiteBoardBundle.start();

		new WaitCondition2("Check if Whiteboard bundle gets activated",
				() -> whiteBoardBundle.getState() == Bundle.ACTIVE)
				.waitForCondition(10000, 500, () -> fail("Whiteboard bundle did not start in time"));

		// Test
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");
	}
}
