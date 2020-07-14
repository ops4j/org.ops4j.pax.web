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
package org.ops4j.pax.web.itest.container.httpservice;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.support.AsyncServlet;
import org.ops4j.pax.web.service.WebContainer;

/**
 * @author Grzegorz Grzybek
 */
public abstract class AbstractAsyncServletIntegrationTest extends AbstractControlledTestBase {

	@Before
	public void setUp() throws Exception {
		WebContainer httpService = getWebContainer(context);

		configureAndWaitForServletWithMapping("/async/*",
				() -> httpService.registerServlet("/async", new AsyncServlet(), null, 1, true, null));
	}

	@Test
	public void testAsyncResponse() throws Exception {
		byte[] bytes = new byte[AsyncServlet.SIZE];
		Arrays.fill(bytes, (byte) 0x42);
		String expected = new String(bytes);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Must get async response", resp -> resp.contains(expected))
				.doGET("http://127.0.0.1:8181/async")
				.executeTest();
	}

}
