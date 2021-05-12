/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.container.jsp;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainer;

/**
 * The tests contained here will test the usage of the PAX Web Jsp directly with the HttpService, without
 * the need for a full servlet container environment. This is useful when integrating PAX Web JSP into an
 * existing servlet container using an HTTP Bridge service implementation such as the Felix Http bridge
 * service implementation.
 * <p>
 * This test validates the correction for PAXWEB-497 as well as the new functionality from PAXWEB-498.
 *
 * @author Serge Huber
 */
public abstract class AbstractJspSelfRegistrationIntegrationTest extends AbstractContainerTestBase {

	/**
	 * Test the class loader parent bug described in PAXWEB-497
	 */
	@Test
	public void testJSPEngineClassLoaderParent() throws Exception {
		WebContainer wc = getWebContainer(context);

		String urlAlias = "/jsp/jspSelfRegistrationTest.jsp";
		configureAndWaitForServletWithMapping("/jsp/jspSelfRegistrationTest.jsp",
				() -> wc.registerJspServlet(urlAlias, new String[] { urlAlias }, null, null));

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181" + urlAlias);

		wc.unregisterJspServlet(urlAlias, null);
	}

}
