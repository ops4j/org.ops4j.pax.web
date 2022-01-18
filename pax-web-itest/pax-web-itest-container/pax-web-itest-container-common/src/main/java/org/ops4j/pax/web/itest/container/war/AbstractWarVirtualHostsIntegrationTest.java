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

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConstants;

/**
 * Tests {@link PaxWebConstants#HEADER_CONNECTORS} and {@link PaxWebConstants#HEADER_VIRTUAL_HOSTS} MANIFEST headers.
 *
 * @author Gareth Collins
 */
public class AbstractWarVirtualHostsIntegrationTest extends AbstractContainerTestBase {

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war", () -> {
			installAndStartWebBundle("org.ops4j.pax.web.samples", "war", System.getProperty("pax-web.version"),
					"org.ops4j.pax.web.samples.war", "/test",
					// we can't use localhost@default, so Pax Web handles localhost/default (<vhost>/<connector>)
					uri -> uri + "&" + PaxWebConstants.HEADER_VIRTUAL_HOSTS + "=localhost/default"
							+ "&" + PaxWebConstants.HEADER_CONNECTORS + "=custom");
		});
	}

	// 1. it should work for matching virtual host without a need to match a connector
	@Test
	public void testWeb() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8181/test/wc/example");
	}

	// 2. it should not work when neither virtual host nor connector matches
	@Test
	public void testWebIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test/wc/example");
	}

	// 3. even if virtual host doesn't match, connector matches
	@Test
	public void testWebJettyIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8282/test/wc/example");
	}

	// 4. it should work if both virtual host and connector (name) match
	@Test
	public void testWebJetty() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8282/test/wc/example");
	}

}
