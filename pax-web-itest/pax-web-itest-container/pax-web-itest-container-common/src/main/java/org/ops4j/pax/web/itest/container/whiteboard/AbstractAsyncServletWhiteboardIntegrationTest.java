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

import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;

/**
 * @author Grzegorz Grzybek
 */
public abstract class AbstractAsyncServletWhiteboardIntegrationTest extends AbstractControlledTestBase {

//	@Before
//	public void setUp() throws Exception {
//		waitForServer("http://127.0.0.1:8181/");
//
//		initServletListener(null);
//
//		// servlets in different contexts
//		Hashtable<String, Object> params = new Hashtable<>();
//		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/test");
//		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "test");
//		bundleContext.registerService(Servlet.class, new TestServlet(), params);
//
//		params.clear();
//		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, true);
//		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/async");
//		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "async");
//		bundleContext.registerService(Servlet.class, new AsyncServlet(), params);
//
//		params.clear();
//		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED, true);
//		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/async");
//		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "async-filter");
//		bundleContext.registerService(Filter.class, new AsyncFilter(), params);
//
//		waitForServletListener();
//	}
//
//	@Test
//	public void testAsyncResponse() throws Exception {
//		byte[] bytes = new byte[AsyncServlet.SIZE];
//		Arrays.fill(bytes, (byte) 0x42);
//		String expected = new String(bytes);
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Must get async response",
//						resp -> resp.contains(expected))
//				.withResponseHeaderAssertion("Async filter should be called",
//						headers -> headers.anyMatch(e -> e.getKey().equals("_Async-Done")))
//				.doGET("http://127.0.0.1:8181/async")
//				.executeTest();
//	}
//
//	public static class CustomHttpContext implements HttpContext {
//
//		private HttpContext delegate;
//
//		public CustomHttpContext(HttpContext delegate) {
//			this.delegate = delegate;
//		}
//
//		@Override
//		public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
//			return delegate.handleSecurity(request, response);
//		}
//
//		@Override
//		public URL getResource(String name) {
//			return delegate.getResource(name);
//		}
//
//		@Override
//		public String getMimeType(String name) {
//			return delegate.getMimeType(name);
//		}
//
//	}

}
