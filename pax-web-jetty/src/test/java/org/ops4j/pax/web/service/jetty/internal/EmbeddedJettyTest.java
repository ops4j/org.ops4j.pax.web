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
package org.ops4j.pax.web.service.jetty.internal;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.ServletMapping;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.PathResourceFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EmbeddedJettyTest {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedJettyTest.class);

	@Test
	public void embeddedServerWithTrivialHandler() throws Exception {
		QueuedThreadPool qtp = new QueuedThreadPool(10);
		qtp.setName("jetty-qtp");

		// main class for a "server" in Jetty. It:
		// - contains connectors (http receivers)
		// - contains request handlers (being a handler itself)
		// - contains a thread pool used by connectors to run request handlers
		Server server = new Server(qtp);

		// "connector" accepts remote connections and data. ServerConnector is the main, NIO based connector
		// that can handle HTTP, HTTP/2, SSL and websocket connections
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		LOG.info("Local port before start: {}", connector.getLocalPort());

		// initially server doesn't have connectors, so we have to set them
		server.setConnectors(new Connector[] { connector });

		// this is done implicitly anyway
		server.setErrorHandler(new ErrorHandler());

		// "handler" is invoked by connector when request is received. Jetty comes with this nice
		// hierarchy of handlers and Pax Web itself adds few more
		server.setHandler(new Handler.Abstract() {
			@Override
			protected void doStart() {
				LOG.info("Starting custom handler during server startup");
			}

			@Override
			public boolean handle(Request request, Response response, Callback callback) {
				response.setStatus(200);
				response.getHeaders().add(HttpHeader.CONTENT_TYPE, "text/plain; charset=UTF-8");
				response.write(true, BufferUtil.toBuffer("OK\n"), callback);
				callback.succeeded();
				return true;
			}
		});

		// starting Jetty server performs these tasks:
		// - ensuring that org.eclipse.jetty.server.handler.ErrorHandler is available
		// - org.eclipse.jetty.server.NetworkConnector#open() called for each org.eclipse.jetty.server.NetworkConnector
		// - all beans () are started. With the above setup, these are the beans:
		//   _beans = {java.util.concurrent.CopyOnWriteArrayList@3769}  size = 6
		//     0 = {org.eclipse.jetty.util.component.ContainerLifeCycle$Bean@3920} "{QueuedThreadPool[jetty-qtp]@36328d33{STOPPED,8<=0<=10,i=0,r=-1,t=-17079333ms,q=0}[NO_TRY],AUTO}"
		//     1 = {org.eclipse.jetty.util.component.ContainerLifeCycle$Bean@3921} "{oejut.ScheduledExecutorScheduler@35f26e72{STOPPED},AUTO}"
		//     2 = {org.eclipse.jetty.util.component.ContainerLifeCycle$Bean@3922} "{org.eclipse.jetty.io.ArrayByteBufferPool@2f48b3d2{min=0,max=65536,buckets=16,heap=0/1041760256,direct=0/1041760256},POJO}"
		//     3 = {org.eclipse.jetty.util.component.ContainerLifeCycle$Bean@3923} "{org.eclipse.jetty.util.resource.FileSystemPool@6b7906b3,UNMANAGED}"
		//     4 = {org.eclipse.jetty.util.component.ContainerLifeCycle$Bean@3924} "{ServerConnector@8692d67{HTTP/1.1, (http/1.1)}{0.0.0.0:34027},AUTO}"
		//     5 = {org.eclipse.jetty.util.component.ContainerLifeCycle$Bean@3925} "{org.eclipse.jetty.server.handler.ErrorHandler@3a1dd365,POJO}"
		// - org.eclipse.jetty.util.component.LifeCycle#start() called for each connector
		// - local port of the connector is set in org.eclipse.jetty.server.ServerConnector#open()
		server.start();

		LOG.info("Local port after start: {}", connector.getLocalPort());

		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", connector.getLocalPort()));

		s.getOutputStream().write((
				"GET / HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithServletHandler() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		// empty ServletHandler has empty org.eclipse.jetty.ee10.servlet.ServletHandler._servletMappings
		// so (if org.eclipse.jetty.ee10.servlet.ServletHandler._ensureDefaultServlet == true),
		// org.eclipse.jetty.ee10.servlet.ServletHandler.Default404Servlet is mapped to "/"
		ServletHandler handler = new ServletHandler();
		handler.setEnsureDefaultServlet(false);

		// this method just adds servlet to org.eclipse.jetty.ee10.servlet.ServletHandler._servlets, it's not enough
		// servlet needs a name (in ServletHolder) to allow its mapping
		handler.addServlet(new ServletHolder("default-servlet", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK\n");
				resp.getWriter().close();
			}
		}));

		// adding a mapping ensures proper entry in ServletHandler._servletMappings
		// when ServletHandler starts, ServletHandler._servletNameMap is also updated
		ServletMapping mapping = new ServletMapping();
		mapping.setServletName("default-servlet");
		mapping.setPathSpec("/");
		handler.addServletMapping(mapping);

		server.setHandler(handler);
		try {
			server.start();
			fail();
		} catch (IllegalStateException ignored) {
			// In Jetty 12 for EE10 you can't use ServletHandler without ServletContextHandler
		}
	}

	@Test
	public void embeddedServerWithContextHandler() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		// ServletHandler requires ServletContextHandler, so we need more primitive handlers
		Handler plainHandler1 = new Handler.Abstract() {
			@Override
			public boolean handle(Request request, Response response, Callback callback) {
				response.setStatus(200);
				response.getHeaders().add(HttpHeader.CONTENT_TYPE, "text/plain; charset=UTF-8");
				response.write(true, BufferUtil.toBuffer("OK1\n"), callback);
				callback.succeeded();
				return true;
			}
		};

		Handler plainHandler2 = new Handler.Abstract() {
			@Override
			public boolean handle(Request request, Response response, Callback callback) {
				response.setStatus(200);
				response.getHeaders().add(HttpHeader.CONTENT_TYPE, "text/plain; charset=UTF-8");
				response.write(true, BufferUtil.toBuffer("OK2\n"), callback);
				callback.succeeded();
				return true;
			}
		};

		// context handler sets "context" for the request. "Context" consists of class loader, context path, ...
		ContextHandler handler1 = new ContextHandler("/c1");
		handler1.setHandler(plainHandler1);
		// without it, we'll need "GET /c1/ HTTP/1.1" requests
		// or just follow `HTTP/1.1 302 Found` redirect from /c1 to /c1/
		handler1.setAllowNullPathInContext(true);
		ContextHandler handler2 = new ContextHandler("/c2");
		handler2.setHandler(plainHandler2);
		// without it, we'll need "GET /c2/ HTTP/1.1" requests
		handler2.setAllowNullPathInContext(true);

		ContextHandlerCollection chc = new ContextHandlerCollection(handler1, handler2);

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		s1.getOutputStream().write((
				"GET /c1 HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK1\n"));

		Socket s2 = new Socket();
		s2.connect(new InetSocketAddress("127.0.0.1", port));

		s2.getOutputStream().write((
				"GET /c2 HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		buf = new byte[64];
		sw = new StringWriter();
		while ((read = s2.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s2.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK2\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithServletContextHandler() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		// passing chc to ServletContextHandler is a bit confusing, because it's not kept as field there. It's
		// only used to call chc.addHandler()
		// fortunately null can be passed as 1st argument in ServletContextHandler constructor
		ContextHandlerCollection chc = new ContextHandlerCollection();

		// servlet context handler extends ContextHandler for easier ContextHandler with _handler = ServletHandler
		// created ServletContextHandler will already have session, security handlers (depending on options) and
		// ServletHandler and we can add servlets/filters through ServletContextHandler
		ServletContextHandler handler1 = new ServletContextHandler(null, "/c1", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(true); // for ServletContextHandler
		handler1.setAllowNullPathInContext(true); // for ContextHandler
		// this single method adds both ServletHolder and ServletMapping
		// calling org.eclipse.jetty.servlet.ServletHandler.addServletWithMapping()
		handler1.addServlet(new ServletHolder("default-servlet", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK1\n");
				resp.getWriter().close();
			}
		}), "/");

		ServletContextHandler handler2 = new ServletContextHandler(null, "/c2", ServletContextHandler.NO_SESSIONS);
		handler2.setAllowNullPathInfo(true);
		handler2.setAllowNullPathInContext(true);
		handler2.addServlet(new ServletHolder("default-servlet", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK2\n");
				resp.getWriter().close();
			}
		}), "/");

		chc.addHandler(handler1);
		chc.addHandler(handler2);

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		// TODO: replace /c1/ with /c1 when https://github.com/eclipse/jetty.project/issues/9906 is fixed
		s1.getOutputStream().write((
				"GET /c1/ HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK1\n"));

		Socket s2 = new Socket();
		s2.connect(new InetSocketAddress("127.0.0.1", port));

		// TODO: replace /c2/ with /c2 when https://github.com/eclipse/jetty.project/issues/9906 is fixed
		s2.getOutputStream().write((
				"GET /c2/ HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		buf = new byte[64];
		sw = new StringWriter();
		while ((read = s2.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s2.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK2\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithServletContextHandlerAndDynamicInitializers() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();

		ServletContextHandler handler1 = new ServletContextHandler(null, "/c1", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(true);
		handler1.setAllowNullPathInContext(true);

		// SCI that adds a ServletContextListener which tries to add ServletContextListener
		handler1.addServletContainerInitializer(new ServletContainerInitializer() {
			@Override
			public void onStartup(Set<Class<?>> c, ServletContext ctx) {
				// ServletContextListener added from SCI - this is real "programmatic listener"
				ctx.addListener(new ServletContextListener() {
					@Override
					public void contextInitialized(ServletContextEvent sce) {
						// ServletContextListener added from a "programmatic listener"
						// throws (according to the spec) java.lang.UnsupportedOperationException
//						sce.getServletContext().addListener(new ServletContextListener() {
//							@Override
//							public void contextInitialized(ServletContextEvent sce) {
//								ServletContextListener.super.contextInitialized(sce);
//							}
//						});
					}
				});
			}
		});

		// ServletContextListener added "from web.xml"
		handler1.addEventListener(new ServletContextListener() {
			@Override
			public void contextInitialized(ServletContextEvent sce) {
				// ServletContextListener added from a listener - not possible:
				//     java.lang.IllegalArgumentException: Inappropriate listener class org.ops4j.pax.web.service.jetty.internal.EmbeddedJettyTest$8$1
//				sce.getServletContext().addListener(new ServletContextListener() {
//					@Override
//					public void contextInitialized(ServletContextEvent sce) {
//						ServletContextListener.super.contextInitialized(sce);
//					}
//				});
			}
		});

		handler1.addServlet(new ServletHolder("default-servlet", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK1\n");
				resp.getWriter().close();

				// can't add new servlet - org.eclipse.jetty.ee10.servlet.ServletContextHandler$ServletContextApi.checkDynamic()
				// prevents it
//				req.getServletContext().addServlet("new-servlet", new HttpServlet() {
//					@Override
//					protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//						resp.setContentType("text/plain");
//						resp.setCharacterEncoding("UTF-8");
//						resp.getWriter().write("OK2\n");
//						resp.getWriter().close();
//					}
//				}).addMapping("/s2");
			}
		}), "/s1");

		chc.addHandler(handler1);

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		String response;

		response = send(connector.getLocalPort(), "/c1/s2");
		assertTrue(response.startsWith("HTTP/1.1 404"));
		response = send(connector.getLocalPort(), "/c1/s1");
		assertTrue(response.endsWith("\r\n\r\nOK1\n"));
//		// call servlet added dynamically from the servlet
//		response = send(connector.getLocalPort(), "/c1/s2");
//		assertTrue(response.endsWith("\r\n\r\nOK2\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithJettyResourceServlet() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();
		ServletContextHandler handler1 = new ServletContextHandler(null, "/", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(true);

		ServletHolder sh1 = new ServletHolder("default", new DefaultServlet());
		sh1.setInitParameter("dirAllowed", "false");
		sh1.setInitParameter("etags", "true");
		sh1.setInitParameter("baseResource", new File("target").getAbsolutePath());
		sh1.setInitParameter("maxCachedFiles", "1000");
		handler1.addServlet(sh1, "/");

		chc.addHandler(handler1);
		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		// Jetty generates ETag using org.eclipse.jetty.util.resource.Resource.getWeakETag(java.lang.String)
		// which is 'W/"' + base64(hash of name ^ lastModified) + base64(hash of name ^ length) + '"'

		String response = send(port, "/test-classes/log4j2-test.properties");
		Map<String, String> headers = extractHeaders(response);
		assertTrue(response.contains("ETag: W/"));
		assertTrue(response.contains("rootLogger.appenderRef.file.ref = file"));

		response = send(port, "/test-classes/log4j2-test.properties",
				"If-None-Match: " + headers.get("ETag"),
				"If-Modified-Since: " + headers.get("Date"));
		assertTrue(response.contains("HTTP/1.1 304"));
		assertFalse(response.contains("rootLogger.appenderRef.file.ref = file"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithServletContextHandlerAndOnlyFilter() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();

		ServletContextHandler handler1 = new ServletContextHandler(null, "/c1", ServletContextHandler.NO_SESSIONS);

		// without "default 404 servlet", jetty won't invoke a "pipeline" that has only a filter.
		handler1.getServletHandler().setEnsureDefaultServlet(true);

		handler1.setAllowNullPathInfo(true);
		handler1.setAllowNullPathInContext(true);
		handler1.addFilter(new FilterHolder(new Filter() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse resp, FilterChain chain) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK\n");
				resp.getWriter().close();
			}
		}), "/*", EnumSet.of(DispatcherType.REQUEST));

		chc.addHandler(handler1);

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		s1.getOutputStream().write((
				"GET /c1/anything HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithServletContextHandlerAddedAfterServerHasStarted() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();

		ServletContextHandler handler1 = new ServletContextHandler(chc, "/c1", ServletContextHandler.NO_SESSIONS);
		handler1.addServlet(new ServletHolder("s1", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK1\n");
				resp.getWriter().close();
			}
		}), "/s1");

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		String response;

		response = send(connector.getLocalPort(), "/c1/s1");
		assertTrue(response.endsWith("\r\n\r\nOK1\n"));

		response = send(connector.getLocalPort(), "/c1/s2");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(connector.getLocalPort(), "/c2/s1");
		assertTrue(response.contains("HTTP/1.1 404"));

		// add new context

		ServletContextHandler handler2 = new ServletContextHandler(chc, "/c2", ServletContextHandler.NO_SESSIONS);
		handler2.setAllowNullPathInfo(true);
		handler2.addServlet(new ServletHolder("s1", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK2\n");
				resp.getWriter().close();
			}
		}), "/s1");
		handler2.start();

		// add new servlet to existing context

		handler1.addServlet(new ServletHolder("s2", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK3\n");
				resp.getWriter().close();
			}
		}), "/s2");

		response = send(connector.getLocalPort(), "/c1/s1");
		assertTrue(response.endsWith("\r\n\r\nOK1\n"));

		response = send(connector.getLocalPort(), "/c1/s2");
		assertTrue(response.endsWith("\r\n\r\nOK3\n"));

		response = send(connector.getLocalPort(), "/c2/s1");
		assertTrue(response.endsWith("\r\n\r\nOK2\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithWebAppContext() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();

		// and finally an extension of ServletContextHandler - WebAppContext, which is again a ServletContextHandler
		// but objects (filters, servlets, ...) are added by org.eclipse.jetty.ee10.webapp.Configuration and
		// org.eclipse.jetty.ee10.webapp.DescriptorProcessor processors
		WebAppContext wac1 = new WebAppContext();
		wac1.setContextPath("/app1");
		// by default, null path info is not allowed and redirect (with added "/") is sent when requesting just
		// the context URL
		wac1.setAllowNullPathInfo(false);
		wac1.setAllowNullPathInContext(false);
		// when we don't pass handler collection (or handler wrapper) in constructor, we have to add this
		// specialized context handler manually
		chc.addHandler(wac1);

		// org.eclipse.jetty.ee10.webapp.StandardDescriptorProcessor.end() just adds 4 component lists to WebAppContext's
		// org.eclipse.jetty.ee10.servlet.ServletContextHandler._servletHandler:
		// - servlets
		// - filters
		// - servlet mappings
		// - filter mappings
		//
		// when WebAppContext.doStart() calls org.eclipse.jetty.ee10.webapp.WebAppContext.preConfigure(), all
		// org.eclipse.jetty.ee10.webapp.WebAppContext._configurations.__known are turned into actual configurators
		// default configuration classes are:
		// __known = {java.util.ArrayList@4215}  size = 13
		//  0 = {org.eclipse.jetty.ee10.webapp.JmxConfiguration@4241}
		//  1 = {org.eclipse.jetty.ee10.webapp.WebInfConfiguration@4242}
		//  2 = {org.eclipse.jetty.ee10.webapp.WebXmlConfiguration@4243}
		//  3 = {org.eclipse.jetty.ee10.webapp.MetaInfConfiguration@4244}
		//  4 = {org.eclipse.jetty.ee10.webapp.FragmentConfiguration@4245}
		//  5 = {org.eclipse.jetty.ee10.plus.webapp.EnvConfiguration@4246}
		//  6 = {org.eclipse.jetty.ee10.plus.webapp.PlusConfiguration@4247}
		//  7 = {org.eclipse.jetty.ee10.webapp.JaasConfiguration@4248}
		//  8 = {org.eclipse.jetty.ee10.webapp.JndiConfiguration@4249}
		//  9 = {org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketConfiguration@4250}
		//  10 = {org.eclipse.jetty.ee10.webapp.WebAppConfiguration@4251}
		//  11 = {org.eclipse.jetty.ee10.annotations.AnnotationConfiguration@4252}
		//  12 = {org.eclipse.jetty.ee10.webapp.JettyWebXmlConfiguration@4253}
		wac1.setConfigurationClasses(new String[] {
				"org.eclipse.jetty.ee10.webapp.WebXmlConfiguration"
		});

		// to impact WebXmlConfiguration, we need few settings
		wac1.setDefaultsDescriptor(null); // to override "org/eclipse/jetty/ee10/webapp/webdefault-ee10.xml"

		// prepare pure web.xml without any web app structure
		File webXml = new File("target/web-" + UUID.randomUUID() + ".xml");
		webXml.delete();

		try (FileWriter writer = new FileWriter(webXml)) {
			writer.write("""
                    <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_6_0.xsd"
                        version="6.0">

                        <servlet>
                            <servlet-name>test-servlet</servlet-name>
                            <servlet-class>org.ops4j.pax.web.service.jetty.internal.EmbeddedJettyTest$TestServlet</servlet-class>
                        </servlet>

                        <servlet-mapping>
                            <servlet-name>test-servlet</servlet-name>
                            <url-pattern>/ts</url-pattern>
                        </servlet-mapping>

                    </web-app>
                    """);
		}

		// all the metadata from different (webdefaults.xml, web.xml, ...) descriptors are kept in
		// org.eclipse.jetty.webapp.MetaData object inside org.eclipse.jetty.webapp.WebAppContext._metadata
		wac1.setDescriptor(webXml.toURI().toURL().toString());

		// when WebAppContext is started, registered descriptor processors process the descriptors
		// org.eclipse.jetty.webapp.StandardDescriptorProcessor.start():
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._filterHolderMap with existing filters
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._filterHolders with existing filters
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._filterMappings with existing filters
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._servletHolderMap with existing servlets
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._servletHolders with existing servlets
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._servletMappings with existing servlets
		//    from ServletContextHandler._servletHandler
		//
		// org.eclipse.jetty.ee10.webapp.StandardDescriptorProcessor.visit() calls one of 21 visitors:
		// _visitors = {java.util.HashMap@3830}  size = 21
		//  - "context-param" -> StandardDescriptorProcessor.visitContextParam()
		//  - (NEW) "default-context-path" -> StandardDescriptorProcessor.visitDefaultContextPath()
		//  - "deny-uncovered-http-methods" -> StandardDescriptorProcessor.visitDenyUncoveredHttpMethods()
		//  - (REMOVED) "distributable"
		//  - "display-name" -> StandardDescriptorProcessor.visitDisplayName()
		//  - "error-page" -> StandardDescriptorProcessor.visitErrorPage()
		//  - "filter-mapping" -> StandardDescriptorProcessor.visitFilterMapping()
		//  - "filter" -> StandardDescriptorProcessor.visitFilter()
		//  - "jsp-config" -> StandardDescriptorProcessor.visitJspConfig()
		//  - "listener" -> StandardDescriptorProcessor.visitListener()
		//  - "locale-encoding-mapping-list" -> StandardDescriptorProcessor.visitLocaleEncodingList()
		//  - "login-config" -> StandardDescriptorProcessor.visitLoginConfig()
		//  - "mime-mapping" -> StandardDescriptorProcessor.visitMimeMapping()
		//  - (NEW) "request-character-encoding" -> StandardDescriptorProcessor.visitRequestCharacterEncoding()
		//  - (NEW) "response-character-encoding" -> StandardDescriptorProcessor.visitResponseCharacterEncoding()
		//  - "security-constraint" -> StandardDescriptorProcessor.visitSecurityConstraint()
		//  - "security-role" -> StandardDescriptorProcessor.visitSecurityRole()
		//  - "servlet-mapping" -> StandardDescriptorProcessor.visitServletMapping()
		//  - "servlet" -> StandardDescriptorProcessor.visitServlet()
		//  - "session-config" -> StandardDescriptorProcessor.visitSessionConfig()
		//  - "taglib" -> StandardDescriptorProcessor.visitTagLib()
		//  - "welcome-file-list" -> StandardDescriptorProcessor.visitWelcomeFileList()
		// org.eclipse.jetty.webapp.StandardDescriptorProcessor.end() calls
		// (on org.eclipse.jetty.servlet.ServletContextHandler._servletHandler):
		//  - org.eclipse.jetty.servlet.ServletHandler.setFilters()
		//  - org.eclipse.jetty.servlet.ServletHandler.setServlets()
		//  - org.eclipse.jetty.servlet.ServletHandler.setFilterMappings()
		//  - org.eclipse.jetty.servlet.ServletHandler.setServletMappings()
		//
		// visitServlet()        creates new org.eclipse.jetty.servlet.ServletHolder
		// visitServletMapping() creates new org.eclipse.jetty.servlet.ServletMapping

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		s1.getOutputStream().write((
				"GET /app1/ts HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithExternalConfiguration() throws Exception {
		// order is important
		String[] xmls = new String[] {
				"etc/jetty-threadpool.xml",
				"etc/jetty.xml",
				"etc/jetty-connectors.xml",
				"etc/jetty-handlercollection.xml",
				"etc/jetty-webapp.xml",
		};

		// prepare pure web.xml without any web app structure
		File webXml = new File("target/web.xml");
		webXml.delete();

		try (FileWriter writer = new FileWriter(webXml)) {
			writer.write("""
                    <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_6_0.xsd"
                        version="6.0">

                        <servlet>
                            <servlet-name>test-servlet</servlet-name>
                            <servlet-class>org.ops4j.pax.web.service.jetty.internal.EmbeddedJettyTest$TestServlet</servlet-class>
                        </servlet>

                        <servlet-mapping>
                            <servlet-name>test-servlet</servlet-name>
                            <url-pattern>/ts</url-pattern>
                        </servlet-mapping>

                    </web-app>
                    """);
		}

		// loop taken from org.eclipse.jetty.xml.XmlConfiguration.main()

		XmlConfiguration last = null;
		Map<String, Object> objects = new LinkedHashMap<>();

		for (String xml : xmls) {
			File f = new File("target/test-classes/" + xml);
			Resource r = new PathResourceFactory().newResource(f.getPath());
			XmlConfiguration configuration = new XmlConfiguration(r);

			if (last != null) {
				configuration.getIdMap().putAll(last.getIdMap());
			}
			configuration.getProperties().put("thread.name.prefix", "jetty-qtp");

			configuration.configure();

			objects.putAll(configuration.getIdMap());

			last = configuration;
		}

		final ServerConnector[] connector = { null };
		objects.forEach((k, v) -> {
			LOG.info("Created {} -> {}", k, v);
			if (connector[0] == null && v instanceof ServerConnector) {
				connector[0] = (ServerConnector) v;
			}
		});

		Server server = (Server) objects.get("Server");
		assertThat(((QueuedThreadPool) server.getThreadPool()).getName()).isEqualTo("jetty-qtp");

		server.start();

		int port = connector[0].getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		s1.getOutputStream().write((
				"GET /app1/ts HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector[0].getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		server.stop();
		server.join();
	}

	@Test
	public void parseEmptyResource() throws Exception {
		Resource r = new PathResourceFactory().newResource(getClass().getResource("/jetty-empty.xml"));
		XmlConfiguration configuration = new XmlConfiguration(r);
		assertThat(configuration.configure()).isEqualTo("OK");
	}

	public static class TestServlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			resp.setContentType("text/plain");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().write("OK\n");
			resp.getWriter().close();
		}
	}

//	private void map(ServletContextHandler h, String name, String[] uris) {
//		ServletMapping mapping = new ServletMapping();
//		mapping.setServletName(name);
//		mapping.setPathSpecs(uris);
//		h.getServletHandler().addServletMapping(mapping);
//	}

	private String send(int port, String request, String ... headers) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", port));

		s.getOutputStream().write((
				"GET " + request + " HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + port + "\r\n").getBytes());
		for (String header : headers) {
			s.getOutputStream().write((header + "\r\n").getBytes());
		}
		s.getOutputStream().write(("Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read = -1;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.close();

		return sw.toString();
	}

	private Map<String, String> extractHeaders(String response) throws IOException {
		Map<String, String> headers = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(new StringReader(response))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.trim().equals("")) {
					break;
				}
				// I know, security when parsing headers is very important...
				String[] kv = line.split(": ");
				String header = kv[0];
				String value = String.join("", Arrays.asList(kv).subList(1, kv.length));
				headers.put(header, value);
			}
		}
		return headers;
	}

}
