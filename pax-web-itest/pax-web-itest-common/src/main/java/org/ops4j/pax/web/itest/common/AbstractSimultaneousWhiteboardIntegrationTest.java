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
package org.ops4j.pax.web.itest.common;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractSimultaneousWhiteboardIntegrationTest extends ITestBase {

	@Before
	public void setUp() throws Exception {
		Bundle whiteBoardBundle = null;
		Bundle simultaneousTestBundle = null;

		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			String symbolicName = bundle.getSymbolicName();
			if ("org.ops4j.pax.web.extender.samples.whiteboard".equals(symbolicName)) {
				whiteBoardBundle = bundle;
			} else if ("org.ops4j.pax.web.itest.SimultaneousTest".equals(symbolicName)) {
				simultaneousTestBundle = bundle;
			}
		}

		assertNotNull(simultaneousTestBundle);
		assertNotNull(whiteBoardBundle);

		// wait for the HTTP service to be available
		getHttpService(bundleContext);

		simultaneousTestBundle.start();
		whiteBoardBundle.start();

		// It may take some time for the services to get registered
		Thread.sleep(1000);
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

}
