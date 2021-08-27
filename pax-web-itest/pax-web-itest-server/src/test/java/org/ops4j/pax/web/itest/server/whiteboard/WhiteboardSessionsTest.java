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
package org.ops4j.pax.web.itest.server.whiteboard;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardSessionsTest extends MultiContainerTestSupport {

	@Test
	public void oneServletWithTwoContexts() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		ServletContextHelper helper1 = new ServletContextHelper() { };
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "c1",
				() -> helper1, 0L, 0, "/c"));

		ServletContextHelper helper2 = new ServletContextHelper() { };
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "d1",
				() -> helper2, 0L, 0, "/d"));

		// 2nd /d context with different helper and higher ranking
		ServletContextHelper helper3 = new ServletContextHelper() { };
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "d2",
				() -> helper3, 0L, 1, "/d"));

		// servlet registered to /c and /d (with helper d1)
		ServiceReference<Servlet> servlet1Ref = mockServletReference(sample1, "servlet1",
				() -> new SessionServlet("1"), 0L, 0, "/s");
		when(servlet1Ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=d1))");
		ServletModel model1 = getServletCustomizer().addingService(servlet1Ref);

		// servlet registered to /c and /d (with helper d2)
		ServiceReference<Servlet> servlet2Ref = mockServletReference(sample1, "servlet2",
				() -> new SessionServlet("2"), 0L, 0, "/t");
		when(servlet2Ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=d2))");
		ServletModel model2 = getServletCustomizer().addingService(servlet2Ref);

		// sessions managed with httpclient5
		BasicCookieStore store = new BasicCookieStore();
		CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(store).build();

		assertThat(httpGET(port, "/c/s?op=dont_create"), endsWith("session=null"));
		assertThat(httpGET(port, "/c/s?op=create"), endsWith("session=not-null"));
		assertThat(httpGET(port, "/c/t?op=dont_create"), endsWith("session=null"));
		assertThat(httpGET(port, "/c/t?op=create"), endsWith("session=not-null"));
		assertThat(httpGET(port, "/d/s?op=dont_create"), endsWith("session=null"));
		assertThat(httpGET(port, "/d/s?op=create"), endsWith("session=not-null"));
		assertThat(httpGET(port, "/d/t?op=dont_create"), endsWith("session=null"));
		assertThat(httpGET(port, "/d/t?op=create"), endsWith("session=not-null"));

		CloseableHttpResponse res;

		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/s?op=create"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("session=not-null"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/s?op=dont_create"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("session=not-null"));

		// session should be created now:
		//  - from runtime perspective in context /c
		//  - from whitebord perspective in helper c1 for /c

		// session should be visible through servlet /t in context /c
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/t?op=dont_create"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("session=not-null"));
		// but not in /d through any servlet
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=dont_create"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("session=null"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=dont_create"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("session=null"));

		// we'll create a session in /d through servlet /s (thus using d1)
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=create"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("session=not-null"));
		// which means that /t in /d should NOT see the session, because it's using d2
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=dont_create"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("session=null"));

		// now we'll create new /d session through /t (d2)
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=create"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("session=not-null"));

		// set the same attribute in two sessions
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=set1"));
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=set2"));
		// and check the values
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=get"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("v=1"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=get"));
		assertThat(EntityUtils.toString(res.getEntity()), endsWith("v=2"));

		getServletCustomizer().removedService(servlet1Ref, model1);
		getServletCustomizer().removedService(servlet2Ref, model2);
	}

	private static class SessionServlet extends Utils.MyIdServlet {

		SessionServlet(String id) {
			super(id);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			String op = req.getParameter("op");
			if (op == null) {
				return;
			}

			// request is org.ops4j.pax.web.service.spi.servlet.OsgiHttpServletRequestWrapper
			// delegates to:
			// - Jetty: org.eclipse.jetty.server.Request, which takes the session from request's
			//   org.eclipse.jetty.server.session.SessionHandler
			// - Tomcat: org.apache.catalina.connector.Request, which takes the session from request's context's
			//   org.apache.catalina.session.StandardManager
			// - Undertow: io.undertow.servlet.spec.HttpServletRequestImpl, which takes the session from request's
			//   context's deployment's io.undertow.server.session.SessionManager
			switch (op) {
				case "dont_create":
					resp.getWriter().print("session=" + (req.getSession(false) == null ? "null" : "not-null"));
					break;
				case "create":
					resp.getWriter().print("session=" + (req.getSession(true) == null ? "null" : "not-null"));
					break;
				case "set1":
					req.getSession().setAttribute("v", "1");
					break;
				case "set2":
					req.getSession().setAttribute("v", "2");
					break;
				case "get":
					resp.getWriter().print("v=" + (req.getSession().getAttribute("v")));
					break;
				default:
					break;
			}
		}
	}

}
