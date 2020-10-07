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
package org.ops4j.pax.web.itest.container.whiteboard;

import java.util.Arrays;
import java.util.Hashtable;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.AsyncFilter;
import org.ops4j.pax.web.itest.utils.web.AsyncServlet;
import org.ops4j.pax.web.itest.utils.web.TestServlet;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Grzegorz Grzybek
 */
public abstract class AbstractAsyncServletWhiteboardIntegrationTest extends AbstractContainerTestBase {

	@Before
	public void setUp() throws Exception {
		configureAndWaitForFilterWithMapping("/async", () -> {
			// servlets in different contexts
			Hashtable<String, Object> params = new Hashtable<>();
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/test");
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "test");
			context.registerService(Servlet.class, new TestServlet(), params);

			params.clear();
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, true);
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/async");
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "async");
			context.registerService(Servlet.class, new AsyncServlet(), params);

			params.clear();
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED, true);
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/async");
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "async-filter");
			context.registerService(Filter.class, new AsyncFilter(), params);
		});
	}

	@Test
	public void testAsyncResponse() throws Exception {
		byte[] bytes = new byte[AsyncServlet.SIZE];
		Arrays.fill(bytes, (byte) 0x42);
		String expected = new String(bytes);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Must get async response",
						resp -> resp.contains(expected))
				.withResponseHeaderAssertion("Async filter should be called",
						headers -> headers.anyMatch(e -> e.getKey().equals("_Async-Done")))
				.doGET("http://127.0.0.1:8181/async")
				.executeTest();
	}

}
