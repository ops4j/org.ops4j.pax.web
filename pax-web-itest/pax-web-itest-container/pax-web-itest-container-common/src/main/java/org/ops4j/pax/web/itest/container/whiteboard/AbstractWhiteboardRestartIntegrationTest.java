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

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.WaitCondition;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.spi.model.events.ElementEvent;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardRestartIntegrationTest extends AbstractContainerTestBase {

	private Bundle bundle;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/",
				() -> bundle = installAndStartBundle(sampleURI("whiteboard")));
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle != null) {
			bundle.stop();
			bundle.uninstall();
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
		final Bundle whiteBoardBundle = Arrays.stream(context.getBundles()).filter(bundle ->
				"org.ops4j.pax.web.pax-web-extender-whiteboard".equalsIgnoreCase(bundle.getSymbolicName()))
				.findFirst().orElseThrow(() -> new AssertionError("no Whiteboard bundle found"));

		// stop Whiteboard bundle
		whiteBoardBundle.stop();

		new WaitCondition("Check if Whiteboard bundle gets stopped") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return whiteBoardBundle.getState() == Bundle.RESOLVED;
			}
		}.waitForCondition();

		// start Whiteboard bundle again
		configureAndWait(() -> {
			try {
				whiteBoardBundle.start();
			} catch (BundleException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}, events -> {
			return events.stream().anyMatch(e -> e.getType() == ElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("root-servlet"));
		});

		// Test
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");
	}

}
