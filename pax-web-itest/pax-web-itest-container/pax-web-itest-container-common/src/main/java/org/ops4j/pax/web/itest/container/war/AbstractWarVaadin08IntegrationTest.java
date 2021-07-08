/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.itest.container.war;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWarVaadin08IntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarVaadin08IntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-vaadin08", () -> {
			// DynamicImport-Package is required for websocket support
//			installAndStartWebBundle("org.ops4j.pax.web.samples", "war-vaadin08", VersionUtils.getProjectVersion(), "war-vaadin08", "/war-vaadin08",
//					uri -> uri + "&DynamicImport-Package=org.eclipse.jetty.*");
			installAndStartWebBundle("war-vaadin08", "/war-vaadin08");
		});
	}

	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'vaadin.initApplication(\"warvaadin08'",
						resp -> resp.contains("vaadin.initApplication(\"warvaadin08"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-vaadin08");
	}

}
