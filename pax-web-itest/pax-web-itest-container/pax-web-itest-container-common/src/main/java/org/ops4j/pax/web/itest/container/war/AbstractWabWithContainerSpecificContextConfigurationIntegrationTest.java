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
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Beryozkin
 */
public abstract class AbstractWabWithContainerSpecificContextConfigurationIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWabWithContainerSpecificContextConfigurationIntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("wab-container-context-config", () -> {
			installAndStartBundle(sampleURI("wab-container-context-config"));
		});
	}

	// Note: there's no Undertow-specific test for this scenario.

}
