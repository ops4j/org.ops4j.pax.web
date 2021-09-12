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

public abstract class AbstractWarVaadin08SpringIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarVaadin08SpringIntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeployment(() -> installAndStartBundle(sampleWarURI("war-vaadin08-spring")));
	}

	@Test
	public void testSlash() throws Exception {
		if (javaMajorVersion() <= 8) {
			HttpTestClientFactory.createDefaultTestClient()
					.withResponseAssertion("Response must contain 'vaadin.initApplication(\"warvaadin08spring'",
							resp -> resp.contains("vaadin.initApplication(\"warvaadin08spring"))
					.doGETandExecuteTest("http://127.0.0.1:8181/war-vaadin08-spring");
		} else {
			LOG.warn("Vaadin 8 WAR with Spring 4.3 has problems (CGLIB proxies) on JDK9+");
		}
	}

}
