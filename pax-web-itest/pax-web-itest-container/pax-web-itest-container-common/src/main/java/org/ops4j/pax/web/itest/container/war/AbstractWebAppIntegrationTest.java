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

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Grzegorz Grzybek
 */
public abstract class AbstractWebAppIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWebAppIntegrationTest.class);

	private Bundle wab = null;

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("war-introspection-bundle", () -> {
			installAndStartBundle(sampleWarURI("war-introspection-bundle"));
		});
	}

	@Test
	public void testWars() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'OK: hello'",
						resp -> resp.contains("OK: hello"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/introspect?message=hello");

		List<String> events = events(wab);

		assertThat("There should be single ServletContext initialization",
				events.stream().filter(s -> s.equals("contextInitialized: /war-bundle")).count(), equalTo(1L));
		assertThat("The context parameter should be available",
				events.stream().filter(s -> s.equals("paramFound: my.value")).count(), equalTo(1L));

		wab.stop();

		assertThat("There should be single ServletContext disposal",
				events.stream().filter(s -> s.equals("contextDestroyed: /war-bundle")).count(), equalTo(1L));
		assertThat("war-bundle should have 3 registration, 3 unregistration and 1 parameter found events recorded",
				events.size(), equalTo(7));

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/introspect?message=hello");

		configureAndWaitForDeploymentUnlessInstalled("war-introspection-bundle", () -> {
			wab.start();
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'OK: hello'",
						resp -> resp.contains("OK: hello"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/introspect?message=hello");

		assertThat("war-bundle should have 6 registration, 3 unregistration, and 2 parameter found events recorded",
				events.size(), equalTo(11));
		assertThat("There should be two ServletContext initialization",
				events.stream().filter(s -> s.equals("contextInitialized: /war-bundle")).count(), equalTo(2L));
	}

	@Test
	public void resources() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Special directory'",
						resp -> resp.contains("Special directory"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/war-bundle/readme.txt");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'miscellaneous resources'",
						resp -> resp.contains("miscellaneous resources"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/misc/readme.txt");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'miscellaneous resources'",
						resp -> resp.contains("Some directory with WAR resources."))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/war-bundle/readme.txt");
	}

	@SuppressWarnings("unchecked")
	private List<String> events(Bundle bundle) throws Exception {
		Class<?> introspection = bundle.adapt(BundleWiring.class).getClassLoader().loadClass("org.ops4j.pax.web.introspection.Introspection");
		return (List<String>) introspection.getMethod("events").invoke(null);
	}

}
