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
package org.ops4j.pax.web.itest.server.httpservice;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;

import org.apache.tomcat.websocket.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WebContainerWebSocketsTest extends MultiContainerTestSupport {

	@Override
	protected boolean enableWebSockets() {
		return true;
	}

	@Test
	public void webSocketsTest() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// configure all WebSocket bundles, so we can test if the target runtime filters out non-relevant SCIs
		when(sample1.getBundleContext().getBundles()).thenReturn(new Bundle[] {
				wsGenericBundle,
				wsJettyBundle,
				wsTomcatBundle,
				wsUndertowBundle
		});

		WebContainer wc = container(sample1);

		// register an SCI that registers a filter that should be available between registrations
		wc.registerServletContainerInitializer(new ServletContainerInitializer() {
			@Override
			public void onStartup(Set<Class<?>> c, ServletContext ctx) {
				ctx.addFilter("fd", new Filter() {
					@Override
					public void init(FilterConfig filterConfig) throws ServletException {
						Filter.super.init(filterConfig);
					}

					@Override
					public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
						response.setContentType("text/plain");
						response.getWriter().println("dynamic filter OK");
						response.getWriter().close();
					}
				}).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/fd");
			}
		}, null, null);

		// register a servlet first to check if web sockets can be added later (after the context has started)
		// and this servlet stays registered
		wc.registerServlet("/s", new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().println("static OK");
				resp.getWriter().close();
			}
		}, null, null);

		// register a listener that'll register another servlet to check whether the runtime registers dynamic
		// elements after SCIs are called.
		// Why in WebSockets test? because Undertow registers a filter not in an SCI but in
		// org.ops4j.pax.web.service.undertow.websocket.internal.WebSocketsExtension.WebSocketListener.contextInitialized()
		wc.registerEventListener(new ServletContextListener() {
			private final String name = "ServletContextListener that adds /d servlet";

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				// but the attributes set by listeners should be cleared when the context is restarted
				assertNull(sce.getServletContext().getAttribute("not-set-in-stone"));
				sce.getServletContext().setAttribute("not-set-in-stone", "true");

				sce.getServletContext().addServlet("dynamic", new HttpServlet() {
					@Override
					protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
						resp.setContentType("text/plain");
						resp.setStatus(HttpServletResponse.SC_OK);
						resp.getWriter().println("dynamic OK");
						resp.getWriter().close();
					}
				}).addMapping("/d");
			}
		}, null);

		String response = httpGET(port, "/s");
		assertTrue(response.contains("static OK"));
		response = httpGET(port, "/d");
		assertTrue(response.contains("dynamic OK"));
		response = httpGET(port, "/fd");
		assertTrue(response.contains("dynamic filter OK"));

		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountDownLatch latch3 = new CountDownLatch(1);
		CountDownLatch latch4 = new CountDownLatch(1);

		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		assertNotNull(container);
		ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
		config.getUserProperties().put(Constants.IO_TIMEOUT_MS_PROPERTY, "3600000");

		MyEndpoint1 ep1 = new MyEndpoint1();
		wc.registerWebSocket(ep1, null);

		MyClientEndpoint clientEndpoint1 = new MyClientEndpoint(latch1);
		container.connectToServer(clientEndpoint1, config,
				URI.create("ws://localhost:" + port + "/ep1"));

		assertTrue(latch1.await(5, TimeUnit.SECONDS));
		assertThat(clientEndpoint1.getMessage(),
				equalTo("I got \"Hello from client!\". My ID=" + System.identityHashCode(ep1)));

		response = httpGET(port, "/s");
		assertTrue(response.contains("static OK"));
		response = httpGET(port, "/d");
		assertTrue(response.contains("dynamic OK"));
		response = httpGET(port, "/fd");
		assertTrue(response.contains("dynamic filter OK"));

		wc.registerWebSocket(MyEndpoint2.class, null);

		MyClientEndpoint clientEndpoint2 = new MyClientEndpoint(latch2);
		container.connectToServer(clientEndpoint2, config,
				URI.create("ws://localhost:" + port + "/ep2"));

		assertTrue(latch2.await(5, TimeUnit.SECONDS));
		assertThat(clientEndpoint2.getMessage(), equalTo("I got 2: \"Hello from client!\"."));

		response = httpGET(port, "/s");
		assertTrue(response.contains("static OK"));
		response = httpGET(port, "/d");
		assertTrue(response.contains("dynamic OK"));
		response = httpGET(port, "/fd");
		assertTrue(response.contains("dynamic filter OK"));

		wc.unregisterWebSocket(MyEndpoint2.class, null);

		MyClientEndpoint clientEndpoint3 = new MyClientEndpoint(latch3);
		container.connectToServer(clientEndpoint3, config,
				URI.create("ws://localhost:" + port + "/ep1"));

		assertTrue(latch3.await(5, TimeUnit.SECONDS));
		assertThat(clientEndpoint3.getMessage(),
				equalTo("I got \"Hello from client!\". My ID=" + System.identityHashCode(ep1)));

		// /ep2 should no longer be available
		assertThat(httpGET(port, "/ep2"), startsWith("HTTP/1.1 404"));

		response = httpGET(port, "/s");
		assertTrue(response.contains("static OK"));
		response = httpGET(port, "/d");
		assertTrue(response.contains("dynamic OK"));
		response = httpGET(port, "/fd");
		assertTrue(response.contains("dynamic filter OK"));

		stopContainer(sample1);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void webSocketsTestWithExtraFilter() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// configure all WebSocket bundles, so we can test if the target runtime filters out non-relevant SCIs
		when(sample1.getBundleContext().getBundles()).thenReturn(new Bundle[] {
				wsGenericBundle,
				wsJettyBundle,
				wsTomcatBundle,
				wsUndertowBundle
		});

		WebContainer wc = container(sample1);

		// register an SCI that registers a filter that should be available between registrations
		wc.registerServletContainerInitializer(new ServletContainerInitializer() {
			@Override
			public void onStartup(Set<Class<?>> c, ServletContext ctx) {
				ctx.addFilter("fd", new Filter() {
					@Override
					public void init(FilterConfig filterConfig) throws ServletException {
						Filter.super.init(filterConfig);
					}

					@Override
					public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
						response.setContentType("text/plain");
						response.getWriter().println("dynamic filter OK");
						response.getWriter().close();
					}
				}).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/fd");
			}
		}, null, null);

		// register a servlet first to check if web sockets can be added later (after the context has started)
		// and this servlet stays registered
		wc.registerServlet("/s", new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().println("static OK");
				resp.getWriter().close();
			}
		}, null, null);

		// register a listener that'll register another servlet to check whether the runtime registers dynamic
		// elements after SCIs are called.
		// Why in WebSockets test? because Undertow registers a filter not in an SCI but in
		// org.ops4j.pax.web.service.undertow.websocket.internal.WebSocketsExtension.WebSocketListener.contextInitialized()
		wc.registerEventListener(new ServletContextListener() {
			private final String name = "ServletContextListener that adds /d servlet";

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				// but the attributes set by listeners should be cleared when the context is restarted
				assertNull(sce.getServletContext().getAttribute("not-set-in-stone"));
				sce.getServletContext().setAttribute("not-set-in-stone", "true");

				sce.getServletContext().addServlet("dynamic", new HttpServlet() {
					@Override
					protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
						resp.setContentType("text/plain");
						resp.setStatus(HttpServletResponse.SC_OK);
						resp.getWriter().println("dynamic OK");
						resp.getWriter().close();
					}
				}).addMapping("/d");
			}
		}, null);

		// registering a filter doesn't necessarily restart the container/context, because filters may be
		// added in quick way without restarting the context.
		wc.registerFilter(new Filter() {
			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				Filter.super.init(filterConfig);
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
				response.setContentType("text/plain");
				response.getWriter().println("static filter OK");
				response.getWriter().close();
			}
		}, new String[] { "/fs" }, null, null, null);

		String response = httpGET(port, "/s");
		assertTrue(response.contains("static OK"));
		response = httpGET(port, "/d");
		assertTrue(response.contains("dynamic OK"));
		response = httpGET(port, "/fs");
		assertTrue(response.contains("static filter OK"));
		response = httpGET(port, "/fd");
		assertTrue(response.contains("dynamic filter OK"));

		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountDownLatch latch3 = new CountDownLatch(1);
		CountDownLatch latch4 = new CountDownLatch(1);

		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		assertNotNull(container);
		ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
		config.getUserProperties().put(Constants.IO_TIMEOUT_MS_PROPERTY, "3600000");

		MyEndpoint1 ep1 = new MyEndpoint1();
		wc.registerWebSocket(ep1, null);

		MyClientEndpoint clientEndpoint1 = new MyClientEndpoint(latch1);
		container.connectToServer(clientEndpoint1, config,
				URI.create("ws://localhost:" + port + "/ep1"));

		assertTrue(latch1.await(5, TimeUnit.SECONDS));
		assertThat(clientEndpoint1.getMessage(),
				equalTo("I got \"Hello from client!\". My ID=" + System.identityHashCode(ep1)));

		response = httpGET(port, "/s");
		assertTrue(response.contains("static OK"));
		response = httpGET(port, "/d");
		assertTrue(response.contains("dynamic OK"));
		response = httpGET(port, "/fs");
		assertTrue(response.contains("static filter OK"));
		response = httpGET(port, "/fd");
		assertTrue(response.contains("dynamic filter OK"));

		wc.registerWebSocket(MyEndpoint2.class, null);

		MyClientEndpoint clientEndpoint2 = new MyClientEndpoint(latch2);
		container.connectToServer(clientEndpoint2, config,
				URI.create("ws://localhost:" + port + "/ep2"));

		assertTrue(latch2.await(5, TimeUnit.SECONDS));
		assertThat(clientEndpoint2.getMessage(), equalTo("I got 2: \"Hello from client!\"."));

		response = httpGET(port, "/s");
		assertTrue(response.contains("static OK"));
		response = httpGET(port, "/d");
		assertTrue(response.contains("dynamic OK"));
		response = httpGET(port, "/fs");
		assertTrue(response.contains("static filter OK"));
		response = httpGET(port, "/fd");
		assertTrue(response.contains("dynamic filter OK"));

		wc.unregisterWebSocket(MyEndpoint2.class, null);

		MyClientEndpoint clientEndpoint3 = new MyClientEndpoint(latch3);
		container.connectToServer(clientEndpoint3, config,
				URI.create("ws://localhost:" + port + "/ep1"));

		assertTrue(latch3.await(5, TimeUnit.SECONDS));
		assertThat(clientEndpoint3.getMessage(),
				equalTo("I got \"Hello from client!\". My ID=" + System.identityHashCode(ep1)));

		// /ep2 should no longer be available
		assertThat(httpGET(port, "/ep2"), startsWith("HTTP/1.1 404"));

		response = httpGET(port, "/s");
		assertTrue(response.contains("static OK"));
		response = httpGET(port, "/d");
		assertTrue(response.contains("dynamic OK"));
		response = httpGET(port, "/fs");
		assertTrue(response.contains("static filter OK"));
		response = httpGET(port, "/fd");
		assertTrue(response.contains("dynamic filter OK"));

		stopContainer(sample1);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@ServerEndpoint("/ep1")
	public static class MyEndpoint1 {
		@OnMessage
		public String processGreeting(String message, Session session) {
			return "I got \"" + message + "\". My ID=" + System.identityHashCode(this);
		}
	}

	@ServerEndpoint("/ep2")
	public static class MyEndpoint2 {
		@OnMessage
		public String processGreeting(String message, Session session) {
			return "I got 2: \"" + message + "\".";
		}
	}

	public static final class MyClientEndpoint extends Endpoint {
		private final CountDownLatch latch;
		private String message = null;

		public MyClientEndpoint(CountDownLatch latch) {
			this.latch = latch;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public void onOpen(Session session, EndpointConfig config) {
			RemoteEndpoint.Basic remote = session.getBasicRemote();
			session.addMessageHandler(new MessageHandler.Whole<String>() {
				@Override
				public void onMessage(String message) {
					MyClientEndpoint.this.message = message;
					try {
						session.close();
						if (latch != null) {
							latch.countDown();
						}
					} catch (IOException e) {
						LOG.warn(e.getMessage());
					}
				}
			});
			try {
				remote.sendText("Hello from client!");
			} catch (IOException e) {
				LOG.warn(e.getMessage());
			}
		}
	}

}
