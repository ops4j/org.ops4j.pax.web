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
package org.ops4j.pax.web.itest.container.war.todo;

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;


/**
 * @author Marc Schlegel
 */
public abstract class AbstractWarContainerInitializerIntegrationTest extends AbstractContainerTestBase {

//	protected abstract Bundle installWarBundle(String webXml) throws Exception;
//
//	@Test
//	public void testServlet_2_5() throws Exception {
//		initWebListener();
//		Bundle bundle = installWarBundle("web-2.5.xml");
//		bundle.start();
//
//		waitForWebListener();
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'TEST OK'!", resp -> resp.contains("TEST OK"))
//				.withResponseAssertion("Response must NOT contain 'FILTER-INIT'! Since this WAR uses Servlet 2.5 no ContainerInitializer should be used", resp -> !resp.contains("FILTER-INIT"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/contextroot/servlet");
//
//		bundle.uninstall();
//	}
//
//
//	@Test
//	public void testServlet_3_0() throws Exception {
//		initWebListener();
//
//		Bundle bundle = installWarBundle("web-3.0.xml");
//		bundle.start();
//
//		waitForWebListener();
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'TEST OK'!", resp -> resp.contains("TEST OK"))
//				.withResponseAssertion("Filter is registered in Service-Locator for ContainerInitializer. Response must contain 'FILTER-INIT'", resp -> resp.contains("FILTER-INIT"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/contextroot/servlet");
//
//		bundle.uninstall();
//	}
}

