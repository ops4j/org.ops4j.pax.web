/* Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.jetty.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;

public class ResourceServletTest {

	private HttpContext httpContext;
	private HttpServletRequest httpRequest;
	private HttpServletResponse httpResponse;

	@Before
	public void setUp() {
		httpContext = createMock(HttpContext.class);
		httpRequest = createMock(HttpServletRequest.class);
		httpResponse = createMock(HttpServletResponse.class);
	}

	private void checkResourceNameSpaceMapping(String alias, String name,
			String uri, String expected) throws IOException, ServletException {
		// prepare
		expect(httpRequest.getRequestURI()).andReturn(uri);
		expect(
				httpRequest
						.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI))
				.andReturn(null);
		httpResponse.sendError(404);
		expect(httpContext.getResource(expected)).andReturn(null);

		replay(httpContext, httpRequest, httpResponse);
		// execute
		new ResourceServlet(httpContext, "", alias, name).doGet(
				httpRequest, httpResponse);
		// verify
		verify(httpContext, httpRequest, httpResponse);
	}

	@Test
	public void checkResourceNameSpaceMapping01() throws IOException,
			ServletException {
		checkResourceNameSpaceMapping("/", "", "/fudd/bugs", "/fudd/bugs");
	}

	@Test
	public void checkResourceNameSpaceMapping02() throws IOException,
			ServletException {
		checkResourceNameSpaceMapping("/", "/", "/fudd/bugs", "/fudd/bugs");
	}

	@Test
	public void checkResourceNameSpaceMapping03() throws IOException,
			ServletException {
		checkResourceNameSpaceMapping("/", "/tmp", "/fudd/bugs",
				"/tmp/fudd/bugs");
	}

	@Test
	public void checkResourceNameSpaceMapping04() throws IOException,
			ServletException {
		checkResourceNameSpaceMapping("/fudd", "", "/fudd/bugs", "/bugs");
	}

	@Test
	public void checkResourceNameSpaceMapping05() throws IOException,
			ServletException {
		checkResourceNameSpaceMapping("/fudd", "/", "/fudd/bugs", "/bugs");
	}

	@Test
	public void checkResourceNameSpaceMapping06() throws IOException,
			ServletException {
		checkResourceNameSpaceMapping("/fudd", "/tmp", "/fudd/bugs",
				"/tmp/bugs");
	}

	@Test
	public void checkResourceNameSpaceMapping07() throws IOException,
			ServletException {
		checkResourceNameSpaceMapping("/fudd", "tmp", "/fudd/bugs/x.gif",
				"tmp/bugs/x.gif");
	}

	@Test
	public void checkResourceNameSpaceMapping08() throws IOException,
			ServletException {
		checkResourceNameSpaceMapping("/fudd/bugs/x.gif", "tmp/y.gif",
				"/fudd/bugs/x.gif", "tmp/y.gif");
	}

}
