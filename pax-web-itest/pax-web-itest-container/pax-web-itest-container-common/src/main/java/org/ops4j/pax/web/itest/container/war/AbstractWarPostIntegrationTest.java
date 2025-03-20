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

import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWarPostIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarIntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-limited-post", () -> {
			installAndStartWebBundle("war-limited-post", "/war-limited-post");
		});
	}

	@Test
	public void testPost() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-limited-post/index.html");

		// max POST size is 42, but this is for entire data=0123456789012345678901234567890123456 body
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'POST data size is: 37'",
						resp -> resp.contains("POST data size is: 37"))
				.doPOST("http://127.0.0.1:8181/war-limited-post/upload-check")
				.addParameter("data", "0123456789012345678901234567890123456")
				.executeTest();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(getPostSizeExceededHttpResponseCode())
				.doPOST("http://127.0.0.1:8181/war-limited-post/upload-check")
				.addParameter("data", "012345678901234567890123456789012345678")
				.executeTest();
	}

	protected abstract int getPostSizeExceededHttpResponseCode();

}
