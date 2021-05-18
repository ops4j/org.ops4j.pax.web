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
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
public abstract class AbstractWarWelcomeFilesIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarWelcomeFilesIntegrationTest.class);

	private Bundle wab;

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("war-introspection", () -> {
			installAndStartWebBundle("war-introspection", "/war-bundle");
		});
	}

	@Test
	public void testWelcomeFiles() throws Exception {
		// redirect to http://127.0.0.1:8181/war-bundle/ and then 404 because there's no /start.txt or /start.md
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle");
		// no redirect, just 404
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/");
		// redirect to http://127.0.0.1:8181/war-bundle/static/ and then 404 because there's no /static/start.txt or
		// /static/start.md
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static");
		// no redirect, just 404
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Should return start.txt content",
						r -> r.contains("static/misc/start.txt"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/misc");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Should return start.txt content",
						r -> r.contains("static/misc/start.txt"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/misc/");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Should return start.txt content",
						r -> r.contains("static/war-bundle/start.txt"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/war-bundle");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Should return start.txt content",
						r -> r.contains("static/war-bundle/start.txt"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/static/war-bundle/");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Should return start.txt content",
						r -> r.contains("war-bundle/start.txt"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/war-bundle");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Should return start.txt content",
						r -> r.contains("war-bundle/start.txt"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/war-bundle/");
	}

}
