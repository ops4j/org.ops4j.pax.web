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
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Beryozkin
 */
public abstract class AbstractWarDispatchIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarDispatchIntegrationTest.class);

	private Bundle wab;

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("war-dispatch-jsp", () -> {
			installAndStartWebBundle("war-dispatch-jsp", "/war-dispatch-jsp");
		});
	}

	@Test
	public void testDispatchJsp() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h2>Hello World!</h2>'",
						resp -> resp.contains("<h2>Hello World!</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-dispatch-jsp/wc/dispatch/jsp");
	}

}
