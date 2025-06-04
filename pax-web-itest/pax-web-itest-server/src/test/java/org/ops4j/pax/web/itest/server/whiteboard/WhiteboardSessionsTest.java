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
import java.util.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
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

		String res;
		BasicHttpClientResponseHandler responseHandler = new BasicHttpClientResponseHandler();

		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/s?op=create"), responseHandler);
		assertThat(res, endsWith("session=not-null"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/s?op=dont_create"), responseHandler);
		assertThat(res, endsWith("session=not-null"));

		// session should be created now:
		//  - from runtime perspective in context /c
		//  - from whitebord perspective in helper c1 for /c

		// session should be visible through servlet /t in context /c
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/t?op=dont_create"), responseHandler);
		assertThat(res, endsWith("session=not-null"));
		// but not in /d through any servlet
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=dont_create"), responseHandler);
		assertThat(res, endsWith("session=null"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=dont_create"), responseHandler);
		assertThat(res, endsWith("session=null"));

		// we'll create a session in /d through servlet /s (thus using d1)
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=create"), responseHandler);
		assertThat(res, endsWith("session=not-null"));
		// which means that /t in /d should NOT see the session, because it's using d2
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=dont_create"), responseHandler);
		assertThat(res, endsWith("session=null"));

		// now we'll create new /d session through /t (d2)
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=create"), responseHandler);
		assertThat(res, endsWith("session=not-null"));

		// set the same attribute in two sessions
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=set1"), responseHandler);
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=set2"), responseHandler);
		// and check the values
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=get"), responseHandler);
		assertThat(res, endsWith("v=1"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=get"), responseHandler);
		assertThat(res, endsWith("v=2"));

		client.close();

		getServletCustomizer().removedService(servlet1Ref, model1);
		getServletCustomizer().removedService(servlet2Ref, model2);
	}

	@Test
	public void sessionListeners() throws Exception {
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

		final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();

		ServiceReference<EventListener> sessionListenerRef = mockListenerReference(sample1, () -> new HttpSessionListener() {
			@Override
			public void sessionCreated(HttpSessionEvent se) {
				LOG.info("Session created: {}", se.getSession());
				sessions.put(se.getSession().getId(), se.getSession());
			}

			@Override
			public void sessionDestroyed(HttpSessionEvent se) {
				LOG.info("Session destroyed: {}", se.getSession());
				sessions.remove(se.getSession().getId(), se.getSession());
			}
		}, 0L, 0);
		when(sessionListenerRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=d1)(osgi.http.whiteboard.context.name=d2))");
		when(sessionListenerRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER)).thenReturn("true");
		EventListenerModel elModel1 = getListenerCustomizer().addingService(sessionListenerRef);

		ServiceReference<EventListener> sessionAttributeListenerRef = mockListenerReference(sample1, () -> new HttpSessionAttributeListener() {
			@Override
			public void attributeAdded(HttpSessionBindingEvent event) {
				LOG.info("Attribute added: {} ({})", event.getName(), event);
			}

			@Override
			public void attributeRemoved(HttpSessionBindingEvent event) {
				LOG.info("Attribute removed: {} ({})", event.getName(), event);
			}

			@Override
			public void attributeReplaced(HttpSessionBindingEvent event) {
				LOG.info("Attribute replaced: {} ({})", event.getName(), event);
			}
		}, 0L, 0);
		when(sessionAttributeListenerRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=d1)(osgi.http.whiteboard.context.name=d2))");
		when(sessionAttributeListenerRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER)).thenReturn("true");
		EventListenerModel elModel2 = getListenerCustomizer().addingService(sessionAttributeListenerRef);

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

		RequestConfig rc = RequestConfig.custom()
				.setResponseTimeout(3600, TimeUnit.SECONDS)
				.build();
		ConnectionConfig cc = ConnectionConfig.custom()
				.setConnectTimeout(3600, TimeUnit.SECONDS)
				.build();
		final HttpClientConnectionManager cm
				= PoolingHttpClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(cc)
				.build();
		CloseableHttpClient client = HttpClients.custom()
				.setConnectionManager(cm)
				.setDefaultCookieStore(store).build();
		HttpClientContext clientContext = HttpClientContext.create();
		clientContext.setRequestConfig(rc);

		String res;

		// request to non-existing servlet - 404 and no session should be created
		BasicHttpClientResponseHandler responseHandler = new BasicHttpClientResponseHandler() {
			@Override
			public String handleResponse(ClassicHttpResponse response) throws IOException {
				try {
					return super.handleResponse(response);
				} catch (HttpResponseException e) {
					if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
						return null;
					} else {
						throw e;
					}
				}
			}
		};
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/x"), clientContext, responseHandler);
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/x"), clientContext, responseHandler);
		assertThat(sessions.size(), equalTo(0));
		assertThat(store.getCookies().size(), equalTo(0));

		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/s?op=create"), clientContext, responseHandler);
		assertThat(res, endsWith("session=not-null"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/s?op=dont_create"), clientContext, responseHandler);
		assertThat(res, endsWith("session=not-null"));

		// session should be created now:
		//  - from runtime perspective in context /c
		//  - from whitebord perspective in helper c1 for /c
		assertThat(sessions.size(), equalTo(1));
		assertThat(store.getCookies().size(), equalTo(1));

		// session should be visible through servlet /t in context /c
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/c/t?op=dont_create"), clientContext, responseHandler);
		assertThat(res, endsWith("session=not-null"));
		// but not in /d through any servlet
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=dont_create"), clientContext, responseHandler);
		assertThat(res, endsWith("session=null"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=dont_create"), clientContext, responseHandler);
		assertThat(res, endsWith("session=null"));
		assertThat(sessions.size(), equalTo(1));
		assertThat(store.getCookies().size(), equalTo(1));

		// we'll create a session in /d through servlet /s (thus using d1)
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=create"), clientContext, responseHandler);
		assertThat(res, endsWith("session=not-null"));
		assertThat(sessions.size(), equalTo(2));
		assertThat(store.getCookies().size(), equalTo(2));
		// which means that /t in /d should NOT see the session, because it's using d2
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=dont_create"), clientContext, responseHandler);
		assertThat(res, endsWith("session=null"));
		assertThat(sessions.size(), equalTo(2));
		assertThat(store.getCookies().size(), equalTo(2));

		// now we'll create new /d session through /t (d2)
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=create"), clientContext, responseHandler);
		assertThat(res, endsWith("session=not-null"));
		assertThat(sessions.size(), equalTo(3));
		assertThat(store.getCookies().size(), equalTo(2));

		// set the same attribute in two sessions
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=set1"), clientContext, responseHandler);
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=set2"), clientContext, responseHandler);
		// and check the values
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=get"), clientContext, responseHandler);
		assertThat(res, endsWith("v=1"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=get"), clientContext, responseHandler);
		assertThat(res, endsWith("v=2"));

		// change the attribute in d2
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=change3"), clientContext, responseHandler);
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=get"), clientContext, responseHandler);
		assertThat(res, endsWith("v=1"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=get"), clientContext, responseHandler);
		assertThat(res, endsWith("v=3"));

		// delete the attribute in d1
		client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=delete"), clientContext, responseHandler);
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/s?op=get"), clientContext, responseHandler);
		assertThat(res, endsWith("v=null"));
		res = client.execute(new HttpGet("http://127.0.0.1:" + port + "/d/t?op=get"), clientContext, responseHandler);
		assertThat(res, endsWith("v=3"));

		assertThat(sessions.size(), equalTo(3));

		client.close();

		getServletCustomizer().removedService(servlet1Ref, model1);
		getServletCustomizer().removedService(servlet2Ref, model2);
		getListenerCustomizer().removedService(sessionAttributeListenerRef, elModel2);
		getListenerCustomizer().removedService(sessionListenerRef, elModel1);
	}

	private static class SessionServlet extends Utils.MyIdServlet {

		SessionServlet(String id) {
			super(id);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
				case "change3":
					req.getSession().setAttribute("v", "3");
					break;
				case "delete":
					req.getSession().removeAttribute("v");
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
