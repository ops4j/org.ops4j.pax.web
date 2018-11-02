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

import java.util.List;
import javax.servlet.http.HttpServletResponse;

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
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
public class WarExtendedIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WarExtendedIntegrationTest.class);

	private Bundle installWar1Bundle;
	private Bundle installWar2Bundle;

	@Configuration
	public static Option[] configure() {
		return configureUndertow();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-introspection/"
				+ VersionUtil.getProjectVersion() + "/war?"
				+ WEB_CONTEXT_PATH + "=/war1&"
				+ "Bundle-SymbolicName=war1";
		installWar1Bundle = bundleContext.installBundle(bundlePath);
		installWar1Bundle.start();

		waitForWebListener();

		initWebListener();

		bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-introspection/"
				+ VersionUtil.getProjectVersion() + "/war?"
				+ WEB_CONTEXT_PATH + "=/war2&"
				+ "Bundle-SymbolicName=war2";
		installWar2Bundle = bundleContext.installBundle(bundlePath);
		installWar2Bundle.start();

		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWar1Bundle != null) {
			installWar1Bundle.stop();
			installWar1Bundle.uninstall();
		}
		if (installWar2Bundle != null) {
			installWar2Bundle.stop();
			installWar2Bundle.uninstall();
		}
	}

	@Test
	public void testWars() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'OK: hello'",
						resp -> resp.contains("OK: hello"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war1/introspect?message=hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'OK: hello'",
						resp -> resp.contains("OK: hello"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war2/introspect?message=hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war3");

		List<String> events1 = events(installWar1Bundle);
		List<String> events2 = events(installWar2Bundle);

		assertThat("There should be single ServletContext initialization",
				events1.stream().filter(s -> s.equals("contextInitialized: /war1")).count(), equalTo(1L));
		assertThat("There should be single ServletContext initialization",
				events2.stream().filter(s -> s.equals("contextInitialized: /war2")).count(), equalTo(1L));

		installWar1Bundle.stop();

		assertThat("There should be single ServletContext disposal",
				events1.stream().filter(s -> s.equals("contextDestroyed: /war1")).count(), equalTo(1L));
		assertThat("war1 should have 6 registration, 6 unregistration, and 1 parameter found events recorded",
				events1.size(), equalTo(13));
		assertTrue("There should be no ServletContext disposal",
				events2.stream().noneMatch(s -> s.equals("contextDestroyed: /war2")));
		assertThat("war2 should have only 6 registration and 1 parameter found events recorded",
				events2.size(), equalTo(7));

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war1/introspect?message=hello");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'OK: hello'",
						resp -> resp.contains("OK: hello"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war2/introspect?message=hello");
	}

	@SuppressWarnings("unchecked")
	private List<String> events(Bundle bundle) throws Exception {
		Class<?> introspection = bundle.adapt(BundleWiring.class).getClassLoader().loadClass("org.ops4j.pax.web.introspection.Introspection");
		return (List<String>) introspection.getMethod("events").invoke(null);
	}

}
