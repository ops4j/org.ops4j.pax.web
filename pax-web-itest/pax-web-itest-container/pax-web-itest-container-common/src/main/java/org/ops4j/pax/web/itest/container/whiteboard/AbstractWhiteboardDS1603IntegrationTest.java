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

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.osgi.framework.Bundle;

import jakarta.servlet.http.HttpServletResponse;

import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * Test case for https://github.com/ops4j/org.ops4j.pax.web/issues/1603
 */
public abstract class AbstractWhiteboardDS1603IntegrationTest extends AbstractContainerTestBase {

	private Bundle bundle;

	@Override
	protected Option[] baseConfigure() {
		return combine(super.baseConfigure(), combine(configAdmin(), scr()));
	}

	@Before
	public void setUp() throws Exception {
		configureAndWait(() -> bundle = installAndStartBundle(sampleURI("whiteboard-ds-1603")), events -> {
			boolean match = events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof FilterEventData
					&& ((FilterEventData)e.getData()).getFilterName().equals("LiftFilter")
					&& usesContexts(e.getData(), "liftweb"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("LiftServlet")
					&& usesContexts(e.getData(), "liftweb"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).isResourceServlet()
					&& usesContexts(e.getData(), "liftweb"));
			return match;
		});
	}

	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello Whiteboard World!</h1>'",
						resp -> resp.contains("<h1>Hello Whiteboard World!</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/hello-whiteboard.html");

		bundle.stop();
		bundle.uninstall();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/hello-whiteboard.html");
	}

}
