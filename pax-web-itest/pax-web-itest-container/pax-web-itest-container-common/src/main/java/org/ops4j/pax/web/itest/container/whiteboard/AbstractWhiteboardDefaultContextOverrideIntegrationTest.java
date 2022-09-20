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
package org.ops4j.pax.web.itest.container.whiteboard;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public abstract class AbstractWhiteboardDefaultContextOverrideIntegrationTest extends AbstractContainerTestBase {

	private Bundle bundle2;
	private Bundle bundle3;

	@Before
	public void setUp() throws Exception {
		bundle("org.ops4j.pax.web.pax-web-runtime").stop(Bundle.STOP_TRANSIENT);
		configureAndWaitForServletWithMapping("/wb/*", () -> {
			// registers org.osgi.service.http.context.ServletContextHelper
			bundle2 = installAndStartBundle(sampleURI("whiteboard-2"));
			// registers javax.servlet.Servlet and picks up the above related OsgiContextModel, which is not
			// known to the runtime yet
			bundle3 = installAndStartBundle(sampleURI("whiteboard-3"));

			bundle("org.ops4j.pax.web.pax-web-runtime").start();
		});
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle2 != null) {
			bundle2.stop();
			bundle2.uninstall();
		}
		if (bundle3 != null) {
			bundle3.stop();
			bundle3.uninstall();
		}
	}

	@Test
	public void testWhiteboardDynamics() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Overriden default / context' and 'whiteboard2'",
						resp -> resp.contains("Overriden default / context") && resp.contains("whiteboard2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/wb/test");
	}

}
