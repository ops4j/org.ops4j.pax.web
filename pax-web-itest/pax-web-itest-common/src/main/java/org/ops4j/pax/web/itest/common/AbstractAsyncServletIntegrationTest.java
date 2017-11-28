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
package org.ops4j.pax.web.itest.common;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.ops4j.pax.web.itest.common.support.AsyncServlet;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;

/**
 * @author Grzegorz Grzybek
 */
public abstract class AbstractAsyncServletIntegrationTest extends ITestBase {
	
	@Before
	public void setUp() throws Exception {
		waitForServer("http://127.0.0.1:8181/");
		WebContainer httpService = getWebContainer(bundleContext);

		initServletListener(null);

		// servlets in different contexts
		httpService.registerServlet("/test", new TestServlet(), null, new CustomHttpContext(httpService.createDefaultHttpContext()));
		httpService.registerServlet("/async", new AsyncServlet(), null, 1, true, new CustomHttpContext(httpService.createDefaultHttpContext()));
		waitForServletListener();
	}

	@Test
	public void testAsyncResponse() throws Exception {
		byte[] bytes = new byte[AsyncServlet.SIZE];
		Arrays.fill(bytes, (byte) 0x42);
		String expected = new String(bytes);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Must get async response",
						resp -> resp.contains(expected))
				.doGET("http://127.0.0.1:8181/async")
				.executeTest();
	}

	public static class CustomHttpContext implements HttpContext {

		private HttpContext delegate;

		public CustomHttpContext(HttpContext delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
			return delegate.handleSecurity(request, response);
		}

		@Override
		public URL getResource(String name) {
			return delegate.getResource(name);
		}

		@Override
		public String getMimeType(String name) {
			return delegate.getMimeType(name);
		}
	}
}
