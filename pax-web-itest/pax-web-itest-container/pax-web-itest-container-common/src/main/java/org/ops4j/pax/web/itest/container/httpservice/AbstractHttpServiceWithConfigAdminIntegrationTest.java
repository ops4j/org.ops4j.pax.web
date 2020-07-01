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
package org.ops4j.pax.web.itest.container.httpservice;

import java.util.Dictionary;
import java.util.Hashtable;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractHttpServiceWithConfigAdminIntegrationTest extends AbstractControlledTestBase {

	private Bundle hsBundle;

	@Inject
	private ConfigurationAdmin caService;

	@Before
	public void setUp() throws Exception {
		org.osgi.service.cm.Configuration config = caService.getConfiguration(PaxWebConstants.PID, null);

		Dictionary<String, Object> props = new Hashtable<>();

		props.put(PaxWebConfig.PID_CFG_LISTENING_ADDRESSES, "127.0.0.1");
		props.put(PaxWebConfig.PID_CFG_HTTP_PORT, "8182");

		configureAndWaitForServletWithMapping("/alt-images/*", () -> {
			hsBundle = installAndStartBundle(sampleURI("hs-helloworld"));
			Dictionary<String, Object> current = config.getProperties();
			if (current == null || !"8182".equals(current.get(PaxWebConfig.PID_CFG_HTTP_PORT))) {
				configureAndWaitForListener(8182, () -> {
					config.update(props);
				});
			}
		});
	}

	@After
	public void tearDown() throws BundleException {
		if (hsBundle != null) {
			hsBundle.stop();
			hsBundle.uninstall();
		}
	}

	@Test
	public void testSubPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8182/helloworld/hs");
		// test image-serving
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8182/images/logo.png");
	}

	@Test
	public void testRootPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8182/");
	}

	@Test
	public void testServletPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet Path: \"\"'",
						resp -> resp.contains("Servlet Path: \"\""))
				.withResponseAssertion("Response must contain 'Path Info: \"/lall/blubb\"'",
						resp -> resp.contains("Path Info: \"/lall/blubb\""))
				.doGETandExecuteTest("http://127.0.0.1:8182/lall/blubb");
	}

	@Test
	public void testServletDeRegistration() throws Exception {
		if (hsBundle != null) {
			hsBundle.stop();
		}
	}

	/**
	 * Tests reconfiguration to another port
	 *
	 * @throws Exception when any error occurs
	 */
	@Test
	public void testReconfiguration() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet Path: '",
						resp -> resp.contains("Servlet Path: \"\""))
				.withResponseAssertion("Response must contain 'Path Info: \"/lall/blubb\"'",
						resp -> resp.contains("Path Info: \"/lall/blubb\""))
				.doGETandExecuteTest("http://127.0.0.1:8182/lall/blubb");

		org.osgi.service.cm.Configuration config = caService.getConfiguration(PaxWebConstants.PID, null);

		Dictionary<String, Object> props = new Hashtable<>();

		props.put(PaxWebConfig.PID_CFG_LISTENING_ADDRESSES, "127.0.0.1");
		props.put(PaxWebConfig.PID_CFG_HTTP_PORT, "9191");

		configureAndWaitForServletWithMapping("/alt-images/*", () -> {
			configureAndWaitForListener(9191, () -> {
				config.update(props);
			});
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet Path: \"\"'",
						resp -> resp.contains("Servlet Path: \"\""))
				.withResponseAssertion("Response must contain 'Path Info: \"/lall/blubb\"'",
						resp -> resp.contains("Path Info: \"/lall/blubb\""))
				.doGETandExecuteTest("http://127.0.0.1:9191/lall/blubb");
	}

}
