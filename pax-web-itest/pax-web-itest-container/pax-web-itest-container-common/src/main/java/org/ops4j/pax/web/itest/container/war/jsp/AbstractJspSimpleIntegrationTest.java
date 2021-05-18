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
package org.ops4j.pax.web.itest.container.war.jsp;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractJspSimpleIntegrationTest extends AbstractContainerTestBase {

	private Bundle wab;

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("war-simple", () -> {
			installAndStartWebBundle("war-simple", "/jsp-simple");
		});
	}

	@Test
	public void testSimpleJspWithCookies() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
						resp -> resp.contains("Hello, World, from JSP"))
				.withResponseHeaderAssertion("Response should contain customized session cookie",
						headers -> headers.anyMatch(header -> header.getKey().equals("Set-Cookie")
								&& header.getValue().contains("J_S_ID")
								&& header.getValue().contains("Expires")))
				.doGETandExecuteTest("http://localhost:8181/jsp-simple/");
	}

	@Test
	public void testSimpleJsp() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
						resp -> resp.contains("Hello, World, from JSP"))
				.doGETandExecuteTest("http://localhost:8181/jsp-simple/");
	}

}
