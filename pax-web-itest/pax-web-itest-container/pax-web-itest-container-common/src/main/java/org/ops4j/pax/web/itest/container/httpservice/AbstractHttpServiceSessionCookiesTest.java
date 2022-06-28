/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
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
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;

public abstract class AbstractHttpServiceSessionCookiesTest extends AbstractContainerTestBase {

	private Bundle hsBundle;

	@Inject
	private ConfigurationAdmin caService;

	@Before
	public void setUp() throws Exception {
		org.osgi.service.cm.Configuration config = caService.getConfiguration(PaxWebConstants.PID, null);

		Dictionary<String, Object> props = new Hashtable<>();

		props.put(PaxWebConfig.PID_CFG_LISTENING_ADDRESSES, "127.0.0.1");
		props.put(PaxWebConfig.PID_CFG_HTTP_PORT, "8182");
		props.put(PaxWebConfig.PID_CFG_SESSION_COOKIE_SAME_SITE, "lax");

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
				.withResponseHeaderAssertion("SameSite attribute should be set in JSESSIONID cookie",
						headers -> headers.anyMatch(e
								-> e.getKey().equals("Set-Cookie") && e.getValue().contains("SameSite=Lax")))
				.doGETandExecuteTest("http://127.0.0.1:8182/helloworld/hs");
	}

}
