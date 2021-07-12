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
import org.ops4j.pax.web.samples.whiteboard.ds.AdminService;
import org.ops4j.pax.web.samples.whiteboard.ds.ManagementService;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.ops4j.pax.exam.OptionUtils.combine;

public abstract class AbstractWhiteboardDSDynamicIntegrationTest extends AbstractContainerTestBase {

	private Bundle bundle;

	@Override
	protected Option[] baseConfigure() {
		return combine(super.baseConfigure(), combine(configAdmin(), scr()));
	}

	@Before
	public void setUp() throws Exception {
		configureAndWait(() -> bundle = installAndStartBundle(sampleURI("whiteboard-ds-dynamic")), events -> {
			return events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("StableServlet")
					&& usesContexts(e.getData(), "default"));
		});
	}

	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<it's me, the servlet>'",
						resp -> resp.equals("<it's me, the servlet>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");

		configureAndWaitForFilterWithMapping("/*", () -> {
			context.registerService(AdminService.class, () -> "a1", null);
			context.registerService(ManagementService.class, () -> "m1", null);
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<a1><it's me, the servlet><m1>'",
						resp -> resp.equals("<a1><it's me, the servlet><m1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(Constants.SERVICE_RANKING, 1);
		context.registerService(AdminService.class, () -> "a2", props);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<a2><it's me, the servlet><m1>'",
						resp -> resp.equals("<a2><it's me, the servlet><m1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

}
