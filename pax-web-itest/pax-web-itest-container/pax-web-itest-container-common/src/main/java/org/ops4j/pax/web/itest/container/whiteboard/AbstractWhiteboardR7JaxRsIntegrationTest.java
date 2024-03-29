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
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

public abstract class AbstractWhiteboardR7JaxRsIntegrationTest extends AbstractContainerTestBase {

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/*", () -> {
			installAndStartBundle(sampleURI("whiteboard-ds-jaxrs"));
		});
	}

	@Test
	public void testWhiteboardJaxRsApplication() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from JAXRS'",
						resp -> resp.contains("Hello from JAXRS"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jaxrs-application");
	}

}
