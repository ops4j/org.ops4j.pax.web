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
package org.ops4j.pax.web.itest.container.whiteboard;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.CookieState;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractWhiteboardSessionCookieIntegrationTest extends AbstractContainerTestBase {

	private ServiceRegistration<Servlet> service;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/sc", () -> {
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "sc");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/sc");
			HttpServlet servlet = new HttpServlet() {
				@Override
				protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
					req.getSession(true);
					resp.getWriter().println("Session should be created");
				}
			};
			service = context.registerService(Servlet.class, servlet, props);
		});
	}

	@After
	public void tearDown() {
		if (service != null) {
			service.unregister();
		}
	}

	@Test
	public void testSessionCookie() throws Exception {
		CookieState cookieJar = new CookieState();

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Session should be created'",
						resp -> resp.contains("Session should be created"))
				.useCookieState(cookieJar)
				.doGETandExecuteTest("http://127.0.0.1:8181/sc");

		assertThat(cookieJar.getCookieStore().getCookies().get(0).getName(), equalTo("JSID"));
	}

}
