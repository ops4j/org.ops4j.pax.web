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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWarHttpMethodsIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarIntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-http-methods", () -> {
			installAndStartWebBundle("war-http-methods", "/war-http-methods");
		});
	}

	@Test
	public void testOptions() throws Exception {
		// default servlet
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.withResponseHeaderAssertion("Allow header should be returned", headers -> {
					return headers.anyMatch(e -> {
						if (e.getKey().equalsIgnoreCase("allow")) {
							Set<String> values = new HashSet<>(Arrays.asList(e.getValue().split("\\s*,\\s*")));
							return values.size() == 4 && values.contains("GET") && values.contains("POST")
									&& values.contains("HEAD") && values.contains("OPTIONS");
						}
						return false;
					});
				})
				.doOPTIONS("http://127.0.0.1:8181", "/war-http-methods/readme.txt")
				.executeTest();

		// connector/listener level
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.withResponseHeaderAssertion("Allow header should be returned", headers -> {
					return headers.anyMatch(e -> {
						if (e.getKey().equalsIgnoreCase("allow")) {
							Set<String> values = new HashSet<>(Arrays.asList(e.getValue().split("\\s*,\\s*")));
							return values.size() == 6 && values.contains("GET") && values.contains("POST")
									&& values.contains("HEAD") && values.contains("OPTIONS")
									&& values.contains("PUT") && values.contains("DELETE");
						}
						return false;
					});
				})
				.doOPTIONS("http://127.0.0.1:8181", "*")
				.executeTest();
	}

}
