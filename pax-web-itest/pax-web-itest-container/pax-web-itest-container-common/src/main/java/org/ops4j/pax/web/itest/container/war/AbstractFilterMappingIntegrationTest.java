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

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractFilterMappingIntegrationTest extends AbstractContainerTestBase {

	@Test
	public void testFilterWar() throws Exception {
		Bundle wab = configureAndWaitForDeploymentUnlessInstalled("war-filters", () -> {
			installAndStartBundle(sampleWarURI("war-filters"));
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain filter output in correct order",
						resp -> resp.contains("f2f3f1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-filters/index.route1");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain filter output in correct order",
						resp -> resp.contains("f3f1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-filters/index.route2");

		wab.uninstall();
	}

}
