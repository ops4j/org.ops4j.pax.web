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

/**
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
public class WebAppIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WebAppIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureUndertow();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = "mvn:org.ops4j.pax.web.samples/war-introspection-bundle/"
				+ VersionUtil.getProjectVersion() + "/war";
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
	public void testWars() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'OK: hello'",
						resp -> resp.contains("OK: hello"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/introspect?message=hello");

		List<String> events = events(installWarBundle);

		assertThat("There should be single ServletContext initialization",
				events.stream().filter(s -> s.equals("contextInitialized: /war-bundle")).count(), equalTo(1L));

		installWarBundle.stop();

		assertThat("There should be single ServletContext disposal",
				events.stream().filter(s -> s.equals("contextDestroyed: /war-bundle")).count(), equalTo(1L));
		assertThat("war-bundle should have 5 registration and 5 unregistration events recorded",
				events.size(), equalTo(10));

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/introspect?message=hello");

		initWebListener();
		installWarBundle.start();
		waitForWebListener();

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'OK: hello'",
						resp -> resp.contains("OK: hello"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/introspect?message=hello");

		assertThat("war-bundle should have 10 registration and 5 unregistration events recorded",
				events.size(), equalTo(15));
		assertThat("There should be two ServletContext initialization",
				events.stream().filter(s -> s.equals("contextInitialized: /war-bundle")).count(), equalTo(2L));
	}

	@SuppressWarnings("unchecked")
	private List<String> events(Bundle bundle) throws Exception {
		Class<?> introspection = bundle.adapt(BundleWiring.class).getClassLoader().loadClass("org.ops4j.pax.web.introspection.Introspection");
		return (List<String>) introspection.getMethod("events").invoke(null);
	}

}
