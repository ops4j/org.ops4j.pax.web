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

package org.ops4j.pax.web.itest.undertow.war;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleWiring;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * This is Undertow-only test for PAXWEB-1092 (https://github.com/ops4j/org.ops4j.pax.web/issues/1384)
 *
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
public class WarExtendedIntegrationTest extends AbstractContainerTestBase {

	private Bundle installWar1Bundle;
	private Bundle installWar2Bundle;

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebUndertow());
		Option[] jspOptions = combine(serverOptions, paxWebJsp());
		return combine(jspOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeployment(() -> {
			installWar1Bundle = installAndStartWebBundle("org.ops4j.pax.web.samples", "war-introspection",
					System.getProperty("pax-web.version"), "war1", "/war1", null);
		});
		configureAndWaitForDeployment(() -> {
			installWar2Bundle = installAndStartWebBundle("org.ops4j.pax.web.samples", "war-introspection",
					System.getProperty("pax-web.version"), "war2", "/war2", null);
		});
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

		// Pax Web 7 was initializing all the servlets. We're initializing only the ones with load-on-startup
		// or the ones being invoked

		// events1 = {java.util.LinkedList@6386}  size = 7
		// 0 = {@6394} "contextInitialized: /war1"
		// 1 = {@6395} "paramFound: my.value"
		// 2 = {@6396} "init: introspection-default-filter"
		// 3 = {@6397} "init: introspection-default-servlet /war1"
		// 4 = {@6398} "destroy: introspection-default-servlet /war1"
		// 5 = {@6399} "destroy: introspection-default-filter"
		// 6 = {@6400} "contextDestroyed: /war1"
		//
		// events2 = {java.util.LinkedList@6387}  size = 4
		// 0 = {@6402} "contextInitialized: /war2"
		// 1 = {@6403} "paramFound: my.value"
		// 2 = {@6404} "init: introspection-default-filter"
		// 3 = {@6405} "init: introspection-default-servlet /war2"

		assertThat("There should be single ServletContext disposal",
				events1.stream().filter(s -> s.equals("contextDestroyed: /war1")).count(), equalTo(1L));
		assertThat("war1 should have 2 registration, 2 unregistration, and 1 parameter found events recorded",
				events1.size(), equalTo(7));
		assertTrue("There should be no ServletContext disposal",
				events2.stream().noneMatch(s -> s.equals("contextDestroyed: /war2")));
		assertThat("war2 should have only 2 registration and 1 parameter found events recorded",
				events2.size(), equalTo(4));

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
