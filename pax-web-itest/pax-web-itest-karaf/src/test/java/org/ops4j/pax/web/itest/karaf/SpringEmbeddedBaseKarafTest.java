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
package org.ops4j.pax.web.itest.karaf;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

import static org.junit.Assert.assertTrue;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public abstract class SpringEmbeddedBaseKarafTest extends AbstractKarafTestBase {

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-spring", () -> {
			installAndStartBundle(sampleWarURI("war-spring"));
		});
	}

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-web-war")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-web-whiteboard")));
	}

	@Test
	public void testWC() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h2>Spring MVC - Hello World</h2>'",
						resp -> resp.contains("<h2>Spring MVC - Hello World</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-spring");
	}

	@Test
	public void testCallController() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h2>Spring MVC - Hello World</h2>'",
						resp -> resp.contains("<h2>Spring MVC - Hello World</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-spring");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Done! Spring MVC works like a charm!'",
						resp -> resp.contains("Done! Spring MVC works like a charm!"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-spring/helloWorld.do");
	}

}
