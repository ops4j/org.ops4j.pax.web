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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.resource.PathResource;
import org.junit.Test;
import org.ops4j.pax.web.service.jetty.internal.web.JettyResourceServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test that has matching tests in pax-web-tomcat and pax-web-undertow
 */
public class UnifiedJettyTest {

	public static final Logger LOG = LoggerFactory.getLogger(UnifiedJettyTest.class);

	@Test
	public void twoResourceServletsWithDifferentBases() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();
		ServletContextHandler handler1 = new ServletContextHandler(null, "/", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(true);
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "dirAllowed", "false");
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "etags", "true");
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "resourceBase", new File("target").getAbsolutePath());
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "maxCachedFiles", "1000");
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "pathInfoOnly", "true");

		// in Jetty, DefaultServlet implements org.eclipse.jetty.util.resource.ResourceFactory used by the
		// cache to populate in-memory storage. So org.eclipse.jetty.util.resource.ResourceFactory is actually
		// the interface to load actual org.eclipse.jetty.util.resource.Resource when needed. Returned Resource
		// should have metadata (lastModified) allowing it to be cached (and evicted) safely

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		FileWriter fw1 = new FileWriter(new File(b1, "hello.txt"));
		IOUtils.write("b1", fw1);
		fw1.close();

		File b2 = new File("target/b2");
		FileUtils.deleteDirectory(b2);
		b2.mkdirs();
		FileWriter fw2 = new FileWriter(new File(b2, "hello.txt"));
		IOUtils.write("b2", fw2);
		fw2.close();

		final PathResource p1 = new PathResource(b1);
		final PathResource p2 = new PathResource(b2);

		handler1.addServlet(new ServletHolder("default1", new JettyResourceServlet(p1, null)), "/d1/*");
		handler1.addServlet(new ServletHolder("default2", new JettyResourceServlet(p2, null)), "/d2/*");

		chc.addHandler(handler1);
		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		String response = send(port, "/hello.txt");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/d1/hello.txt");
		Map<String, String> headers = extractHeaders(response);
		assertTrue(response.contains("ETag: W/"));
		assertTrue(response.endsWith("b1"));

		response = send(port, "/d1/hello.txt",
				"If-None-Match: " + headers.get("ETag"),
				"If-Modified-Since: " + headers.get("Date"));
		assertTrue(response.contains("HTTP/1.1 304"));
		assertFalse(response.endsWith("b1"));

		response = send(port, "/d2/hello.txt");
		headers = extractHeaders(response);
		assertTrue(response.contains("ETag: W/"));
		assertTrue(response.endsWith("b2"));

		response = send(port, "/d2/hello.txt",
				"If-None-Match: " + headers.get("ETag"),
				"If-Modified-Since: " + headers.get("Date"));
		assertTrue(response.contains("HTTP/1.1 304"));
		assertFalse(response.endsWith("b2"));

		response = send(port, "/d1/../hello.txt");
		assertTrue(response.contains("HTTP/1.1 404"));
		response = send(port, "/d2/../../../../../../hello.txt");
		assertTrue(response.contains("HTTP/1.1 400"));

		response = send(port, "/d3/hello.txt");
		assertTrue(response.contains("HTTP/1.1 404"));
		response = send(port, "/d3/");
		assertTrue(response.contains("HTTP/1.1 404"));
		response = send(port, "/d3");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/d2");
		assertTrue(response.contains("HTTP/1.1 302"));
		response = send(port, "/d2/");
		assertTrue(response.contains("HTTP/1.1 403"));

		server.stop();
		server.join();
	}

	@Test
	public void standardWelcomePages() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();
		ServletContextHandler handler1 = new ServletContextHandler(null, "/", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(true);
		// a bug when "false"? When mapped to "/" it doesn't matter, but fails
		// when mapped to "/x" resources would have to be under /x....
		//		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "pathInfoOnly", "true");

		handler1.setWelcomeFiles(new String[] { "index.x", "indexx" });

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/index.x"))) {
			IOUtils.write("'sub/index'", fw1);
		}

		ServletHolder defaultServlet = new ServletHolder("default", new DefaultServlet());
		defaultServlet.setInitParameter("welcomeServlets", "true");
		// a bug with "/"? We can't set it to "true", but this may mean we can't map the default servlet to something
		// different than "/"
		defaultServlet.setInitParameter("pathInfoOnly", "false");
		defaultServlet.setInitParameter("resourceBase", b1.getAbsolutePath());
		handler1.addServlet(defaultServlet, "/");

		ServletHolder indexxServlet = new ServletHolder("indexx", new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().print("'indexx'");
			}
		});
		handler1.addServlet(indexxServlet, "/indexx/*");

		chc.addHandler(handler1);
		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		String response = send(port, "/");
		assertThat(response, endsWith("'indexx'"));
		response = send(port, "/sub/");
		assertThat(response, endsWith("'sub/index'"));
		response = send(port, "/sub");
		assertThat(response, startsWith("HTTP/1.1 302"));

		server.stop();
		server.join();
	}

	@Test
	public void standardWelcomePagesWithDifferentContext() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();
		ServletContextHandler handler1 = new ServletContextHandler(null, "/c", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(false);
		// a bug when "false"? When mapped to "/" it doesn't matter, but fails
		// when mapped to "/x" resources would have to be under /x....
		//		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "pathInfoOnly", "true");

		handler1.setWelcomeFiles(new String[] { "index.x", "indexx" });

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/index.x"))) {
			IOUtils.write("'sub/index'", fw1);
		}

		ServletHolder defaultServlet = new ServletHolder("default", new DefaultServlet());
		defaultServlet.setInitParameter("welcomeServlets", "true");
		// a bug with "/"? We can't set it to "true", but this may mean we can't map the default servlet to something
		// different than "/"
		defaultServlet.setInitParameter("pathInfoOnly", "false");
		defaultServlet.setInitParameter("resourceBase", b1.getAbsolutePath());
		handler1.addServlet(defaultServlet, "/");

		ServletHolder indexxServlet = new ServletHolder("indexx", new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().print("'indexx'");
			}
		});
		handler1.addServlet(indexxServlet, "/indexx/*");

		chc.addHandler(handler1);
		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		String response = send(port, "/");
		assertThat(response, startsWith("HTTP/1.1 404"));
		response = send(port, "/sub/");
		assertThat(response, startsWith("HTTP/1.1 404"));
		response = send(port, "/sub");
		assertThat(response, startsWith("HTTP/1.1 404"));

		response = send(port, "/c");
		assertThat(response, startsWith("HTTP/1.1 302"));
		response = send(port, "/c/");
		assertThat(response, endsWith("'indexx'"));
		response = send(port, "/c/sub/");
		assertThat(response, endsWith("'sub/index'"));
		response = send(port, "/c/sub");
		assertThat(response, startsWith("HTTP/1.1 302"));

		server.stop();
		server.join();
	}

	@Test
	public void resourceServletWithWelcomePages() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();
		ServletContextHandler handler1 = new ServletContextHandler(null, "/", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(true);
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "pathInfoOnly", "true");

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "hello.txt"))) {
			IOUtils.write("'hello.txt'", fw1);
		}
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/hello.txt"))) {
			IOUtils.write("'sub/hello.txt'", fw1);
		}
		try (FileWriter fw1 = new FileWriter(new File(b1, "index.txt"))) {
			IOUtils.write("'index.txt'", fw1);
		}
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/index.txt"))) {
			IOUtils.write("'sub/index.txt'", fw1);
		}

		final PathResource p1 = new PathResource(b1);

		JettyResourceServlet servlet = new JettyResourceServlet(p1, null);
		servlet.setWelcomeFiles(new String[] { "index.txt" });
		handler1.addServlet(new ServletHolder("default1", servlet), "/d1/*");

		chc.addHandler(handler1);
		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		String response = send(port, "/hello.txt");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/d1/hello.txt");
		assertThat(response, endsWith("'hello.txt'"));
		response = send(port, "/d1/sub/hello.txt");
		assertThat(response, endsWith("'sub/hello.txt'"));

		response = send(port, "/d1/../hello.txt");
		assertTrue(response.contains("HTTP/1.1 404"));

		// here's where problems started. Jetty's default servlet itself handles welcome files, while in
		// Tomcat and Undertow, such support has to be added.

		response = send(port, "/d1/");
		assertThat(response, endsWith("'index.txt'"));
		response = send(port, "/d1/sub/");
		assertThat(response, endsWith("'sub/index.txt'"));
		response = send(port, "/d1");
		assertThat(response, startsWith("HTTP/1.1 302"));
		response = send(port, "/d1/sub");
		assertThat(response, startsWith("HTTP/1.1 302"));

		server.stop();
		server.join();
	}

	@Test
	public void paxWebWelcomePages() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();
		ServletContextHandler handler = new ServletContextHandler(null, "/", ServletContextHandler.NO_SESSIONS);
		handler.setAllowNullPathInfo(true);
		// pathInfoOnly will be set at servlet level
//		handler.setInitParameter(DefaultServlet.CONTEXT_INIT + "pathInfoOnly", "true");

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/index.x"))) {
			IOUtils.write("'sub/index-b1'", fw1);
		}
		try (FileWriter fw1 = new FileWriter(new File(b1, "index.z"))) {
			IOUtils.write("'index-z-b1'", fw1);
		}
		File b2 = new File("target/b2");
		FileUtils.deleteDirectory(b2);
		b2.mkdirs();
		new File(b2, "sub").mkdirs();
		try (FileWriter fw2 = new FileWriter(new File(b2, "sub/index.x"))) {
			IOUtils.write("'sub/index-b2'", fw2);
		}
		File b3 = new File("target/b3");
		FileUtils.deleteDirectory(b3);
		b3.mkdirs();
		new File(b3, "sub").mkdirs();
		try (FileWriter fw3 = new FileWriter(new File(b3, "sub/index.x"))) {
			IOUtils.write("'sub/index-b3'", fw3);
		}

		final PathResource p1 = new PathResource(b1);
		final PathResource p2 = new PathResource(b2);
		final PathResource p3 = new PathResource(b3);

		// the "/" default & resource servlet
		JettyResourceServlet jrs1 = new JettyResourceServlet(p1, null);
		jrs1.setWelcomeFiles(new String[] { "index.y", "index.x" });
		ServletHolder defaultServlet = new ServletHolder("default", jrs1);
		defaultServlet.setInitParameter("redirectWelcome", "false");
		defaultServlet.setInitParameter("welcomeServlets", "true");
		// with "true" it leads to endless redirect... Also in Tomcat it has to be "false" because servletPath
		// is returned incorrectly
		defaultServlet.setInitParameter("pathInfoOnly", "false");
		defaultServlet.setInitParameter("resourceBase", b1.getAbsolutePath());
		handler.addServlet(defaultServlet, "/");

		// the "/r" resource servlet
		JettyResourceServlet jrs2 = new JettyResourceServlet(p2, null);
		jrs2.setWelcomeFiles(new String[] { "index.y", "index.x" });
		ServletHolder resourceServlet = new ServletHolder("resource", jrs2);
		resourceServlet.setInitParameter("redirectWelcome", "false");
		resourceServlet.setInitParameter("welcomeServlets", "true");
		resourceServlet.setInitParameter("pathInfoOnly", "true");
		resourceServlet.setInitParameter("resourceBase", b2.getAbsolutePath());
		handler.addServlet(resourceServlet, "/r/*");

		// the "/s" resource servlet - with redirected welcome files
		JettyResourceServlet jrs3 = new JettyResourceServlet(p3, null);
		jrs3.setWelcomeFiles(new String[] { "index.y", "index.x" });
		ServletHolder resource2Servlet = new ServletHolder("resource2", jrs3);
		resource2Servlet.setInitParameter("redirectWelcome", "true");
		resource2Servlet.setInitParameter("welcomeServlets", "true");
		resource2Servlet.setInitParameter("pathInfoOnly", "true");
		resource2Servlet.setInitParameter("resourceBase", b3.getAbsolutePath());
		handler.addServlet(resource2Servlet, "/s/*");

		// the "/indexx/*" (and *.y and *.x) servlet which should be available through welcome files
		ServletHolder indexxServlet = new ServletHolder("indexx", new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().println("'indexx servlet'");
				resp.getWriter().println("req.request_uri=\"" + req.getRequestURI() + "\"");
				resp.getWriter().println("req.context_path=\"" + req.getContextPath() + "\"");
				resp.getWriter().println("req.servlet_path=\"" + req.getServletPath() + "\"");
				resp.getWriter().println("req.path_info=\"" + req.getPathInfo() + "\"");
				resp.getWriter().println("req.query_string=\"" + req.getQueryString() + "\"");
				resp.getWriter().println("javax.servlet.forward.mapping=\"" + req.getAttribute("javax.servlet.forward.mapping") + "\"");
				resp.getWriter().println("javax.servlet.forward.request_uri=\"" + req.getAttribute("javax.servlet.forward.request_uri") + "\"");
				resp.getWriter().println("javax.servlet.forward.context_path=\"" + req.getAttribute("javax.servlet.forward.context_path") + "\"");
				resp.getWriter().println("javax.servlet.forward.servlet_path=\"" + req.getAttribute("javax.servlet.forward.servlet_path") + "\"");
				resp.getWriter().println("javax.servlet.forward.path_info=\"" + req.getAttribute("javax.servlet.forward.path_info") + "\"");
				resp.getWriter().println("javax.servlet.forward.query_string=\"" + req.getAttribute("javax.servlet.forward.query_string") + "\"");
				resp.getWriter().println("javax.servlet.include.mapping=\"" + req.getAttribute("javax.servlet.include.mapping") + "\"");
				resp.getWriter().println("javax.servlet.include.request_uri=\"" + req.getAttribute("javax.servlet.include.request_uri") + "\"");
				resp.getWriter().println("javax.servlet.include.context_path=\"" + req.getAttribute("javax.servlet.include.context_path") + "\"");
				resp.getWriter().println("javax.servlet.include.servlet_path=\"" + req.getAttribute("javax.servlet.include.servlet_path") + "\"");
				resp.getWriter().println("javax.servlet.include.path_info=\"" + req.getAttribute("javax.servlet.include.path_info") + "\"");
				resp.getWriter().println("javax.servlet.include.query_string=\"" + req.getAttribute("javax.servlet.include.query_string") + "\"");
			}
		});
		ServletMapping indexxMapping = new ServletMapping();
		indexxMapping.setServletName("indexx");
		indexxMapping.setPathSpecs(new String[] { "*.x", "*.y" });
		handler.getServletHandler().addServlet(indexxServlet);
		handler.getServletHandler().addServletMapping(indexxMapping);

		// the "/gateway/*" servlet through which we'll forward to/include other servlets
		ServletHolder gatewayServlet = new ServletHolder("gateway", new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				String what = req.getParameter("what");
				String where = req.getParameter("where");
				switch (what) {
					case "redirect":
						resp.sendRedirect(where);
						return;
					case "forward":
						// we can't send anything when forwarding
						req.getRequestDispatcher(where).forward(req, resp);
						return;
					case "include":
						resp.getWriter().print(">>>");
						req.getRequestDispatcher(where).include(req, resp);
						resp.getWriter().print("<<<");
						return;
					default:
				}
			}
		});
		handler.addServlet(gatewayServlet, "/gateway/*");

		chc.addHandler(handler);
		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		// --- resource access through "/" servlet

		// sanity check for physical resource at root of resource servlet
		String response = send(port, "/index.z");
		assertTrue(response.endsWith("'index-z-b1'"));

		// "/" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// forward is performed implicitly by Jetty's DefaultServlet
		response = send(port, "/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/index.y\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/\""));

		// Forward vs. Include:
		// in forward method:
		//  - original servletPath, pathInfo, requestURI are available ONLY through javax.servlet.forward.* attributes
		//  - values used to obtain the dispatcher are available through request object
		// in include method:
		//  - original servletPath, pathInfo, requestURI are available through request object
		//  - values used to obtain the dispatcher are available through javax.servlet.include.* attributes

		// "/" (but through gateway) - similar forward, but performed explicitly by gateway servlet
		// 9.4 The Forward Method:
		//     The path elements of the request object exposed to the target servlet must reflect the
		//     path used to obtain the RequestDispatcher.
		// so "gateway" forwards to "/", "/" is handled by "default" which forwards to "/index.y"
		response = send(port, "/gateway/x?what=forward&where=/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/index.y\""));
		// see https://github.com/eclipse/jetty.project/issues/9119
		assertTrue(response.contains("javax.servlet.forward.context_path=\"/\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("javax.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("javax.servlet.forward.path_info=\"/x\""));

		// "/", but included by gateway servlet
		// "gateway" includes "/" which includes "/index.y"
		response = send(port, "/gateway/x?what=include&where=/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("javax.servlet.include.context_path=\"\""));
		assertTrue(response.contains("javax.servlet.include.request_uri=\"/index.y\""));
		assertTrue(response.contains("javax.servlet.include.servlet_path=\"/index.y\""));
		assertTrue(response.contains("javax.servlet.include.path_info=\"null\""));

		response = send(port, "/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/gateway/x?what=forward&where=/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// included servlet (here - "default") can't set Location header
		response = send(port, "/gateway/x?what=include&where=/sub");
		assertTrue(response.contains(">>><<<"));

		// "/sub/" + "index.x" welcome files is forwarded and mapped to indexx servlet
		// According to 10.10 "Welcome Files":
		//    The Web server must append each welcome file in the order specified in the deployment descriptor to the
		//    partial request and check whether a static resource in the WAR is mapped to that
		//    request URI. If no match is found, the Web server MUST again append each
		//    welcome file in the order specified in the deployment descriptor to the partial
		//    request and check if a servlet is mapped to that request URI.
		//    [...]
		//    The container may send the request to the welcome resource with a forward, a redirect, or a
		//    container specific mechanism that is indistinguishable from a direct request.
		// Jetty detects /sub/index.y (first welcome file) can be mapped to indexx servlet, but continues the
		// search for physical resource. /sub/index.x is actual physical resource, so forward is chosen, which
		// is eventually mapped to indexx again - with index.x, not index.y
		response = send(port, "/sub/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/sub/index.x\""));
		// see https://github.com/eclipse/jetty.project/issues/9119
		assertTrue(response.contains("javax.servlet.forward.context_path=\"/\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/sub/\""));
		assertTrue(response.contains("javax.servlet.forward.servlet_path=\"/sub/\""));
		assertTrue(response.contains("javax.servlet.forward.path_info=\"null\""));

		response = send(port, "/gateway/x?what=forward&where=/sub/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/sub/index.x\""));
		// see https://github.com/eclipse/jetty.project/issues/9119
		assertTrue(response.contains("javax.servlet.forward.context_path=\"/\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("javax.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("javax.servlet.forward.path_info=\"/x\""));

		response = send(port, "/gateway/x?what=include&where=/sub/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("javax.servlet.include.context_path=\"\""));
		assertTrue(response.contains("javax.servlet.include.request_uri=\"/sub/index.x\""));
		assertTrue(response.contains("javax.servlet.include.servlet_path=\"/sub/index.x\""));
		assertTrue(response.contains("javax.servlet.include.path_info=\"null\""));

		// --- resource access through "/r" servlet

		response = send(port, "/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/gateway/x?what=forward&where=/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// included servlet/resource can't be redirected
		response = send(port, "/gateway/x?what=include&where=/r");
		assertTrue(response.endsWith(">>><<<"));

		// "/r" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// forward is performed implicitly by Jetty's DefaultServlet (even if mapped to /r/*), forward URI is
		// "/r/index.y" (first welcome), but this time, "/r/*" is a mapping with higher priority than "*.y"
		// (with "/" servlet, "*.y" had higher priority than "/"), so "resource" servlet is called, this time
		// with full URI (no welcome files are checked). Such resource is not found, so we have 404
		response = send(port, "/r/");
		assertTrue(response.startsWith("HTTP/1.1 404"));

		response = send(port, "/gateway/x?what=forward&where=/r/");
		assertTrue(response.startsWith("HTTP/1.1 404"));
		response = send(port, "/gateway/x?what=include&where=/r/");
		// HTTP 500 according to 9.3 "The Include Method"
		assertTrue(response.startsWith("HTTP/1.1 500"));

		response = send(port, "/r/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/gateway/x?what=forward&where=/r/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/gateway/x?what=include&where=/r/sub");
		assertTrue(response.contains(">>><<<"));

		// this time, welcome file is /sub/index.x and even if it maps to existing servlet (*.x), physical
		// resource exists and is returned
		response = send(port, "/r/sub/");
		assertTrue(response.endsWith("'sub/index-b2'"));
		response = send(port, "/gateway/x?what=forward&where=/r/sub/");
		assertTrue(response.endsWith("'sub/index-b2'"));
		response = send(port, "/gateway/x?what=include&where=/r/sub/");
		assertTrue(response.endsWith(">>>'sub/index-b2'<<<"));

		// --- resource access through "/s" servlet - welcome files with redirect

		response = send(port, "/s");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/gateway/x?what=forward&where=/s");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// included servlet/resource can't be redirected
		response = send(port, "/gateway/x?what=include&where=/s");
		assertTrue(response.endsWith(">>><<<"));

		response = send(port, "/s/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// redirect to first welcome page with found *.y mapping, but another mapping will be found using /s/*
		assertTrue(extractHeaders(response).get("Location").endsWith("/s/index.y"));

		response = send(port, "/gateway/x?what=forward&where=/s/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/s/index.y?what=forward&where=/s/"));
		response = send(port, "/gateway/x?what=include&where=/s/");
		assertTrue(response.contains(">>><<<"));

		response = send(port, "/s/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/s/sub/"));
		response = send(port, "/gateway/x?what=forward&where=/s/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/s/sub/?what=forward&where=/s/sub"));
		response = send(port, "/gateway/x?what=include&where=/s/sub");
		assertTrue(response.contains(">>><<<"));

		// this time, welcome file is /sub/index.x and even if it maps to existing servlet (*.x), physical
		// resource exists and is returned
		response = send(port, "/s/sub/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/s/sub/index.x"));
		response = send(port, "/gateway/x?what=forward&where=/s/sub/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/s/sub/index.x?what=forward&where=/s/sub/"));
		response = send(port, "/gateway/x?what=include&where=/s/sub/");
		assertTrue(response.contains(">>><<<"));

		server.stop();
		server.join();
	}

	@Test
	public void paxWebWelcomePagesWithDifferentContext() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();
		ServletContextHandler handler = new ServletContextHandler(null, "/c", ServletContextHandler.NO_SESSIONS);
		handler.setAllowNullPathInfo(false);
		// pathInfoOnly will be set at servlet level
//		handler.setInitParameter(DefaultServlet.CONTEXT_INIT + "pathInfoOnly", "true");
		handler.setInitParameter(DefaultServlet.CONTEXT_INIT + "dirAllowed", "false");

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/index.x"))) {
			IOUtils.write("'sub/index-b1'", fw1);
		}
		try (FileWriter fw1 = new FileWriter(new File(b1, "index.z"))) {
			IOUtils.write("'index-z-b1'", fw1);
		}
		File b2 = new File("target/b2");
		FileUtils.deleteDirectory(b2);
		b2.mkdirs();
		new File(b2, "sub").mkdirs();
		try (FileWriter fw2 = new FileWriter(new File(b2, "sub/index.x"))) {
			IOUtils.write("'sub/index-b2'", fw2);
		}
		File b3 = new File("target/b3");
		FileUtils.deleteDirectory(b3);
		b3.mkdirs();
		new File(b3, "sub").mkdirs();
		try (FileWriter fw3 = new FileWriter(new File(b3, "sub/index.x"))) {
			IOUtils.write("'sub/index-b3'", fw3);
		}

		final PathResource p1 = new PathResource(b1);
		final PathResource p2 = new PathResource(b2);
		final PathResource p3 = new PathResource(b3);

		// the "/" default & resource servlet
		JettyResourceServlet jrs1 = new JettyResourceServlet(p1, null);
		jrs1.setWelcomeFiles(new String[] { "index.y", "index.x" });
		ServletHolder defaultServlet = new ServletHolder("default", jrs1);
		defaultServlet.setInitParameter("redirectWelcome", "false");
		defaultServlet.setInitParameter("welcomeServlets", "true");
		// with "true" it leads to endless redirect... Also in Tomcat it has to be "false" because servletPath
		// is returned incorrectly
		defaultServlet.setInitParameter("pathInfoOnly", "false");
		defaultServlet.setInitParameter("resourceBase", b1.getAbsolutePath());
		handler.addServlet(defaultServlet, "/");

		// the "/r" resource servlet
		JettyResourceServlet jrs2 = new JettyResourceServlet(p2, null);
		jrs2.setWelcomeFiles(new String[] { "index.y", "index.x" });
		ServletHolder resourceServlet = new ServletHolder("resource", jrs2);
		resourceServlet.setInitParameter("redirectWelcome", "false");
		resourceServlet.setInitParameter("welcomeServlets", "true");
		resourceServlet.setInitParameter("pathInfoOnly", "true");
		resourceServlet.setInitParameter("resourceBase", b2.getAbsolutePath());
		handler.addServlet(resourceServlet, "/r/*");

		// the "/s" resource servlet - with redirected welcome files
		JettyResourceServlet jrs3 = new JettyResourceServlet(p3, null);
		jrs3.setWelcomeFiles(new String[] { "index.y", "index.x" });
		ServletHolder resource2Servlet = new ServletHolder("resource2", jrs3);
		resource2Servlet.setInitParameter("redirectWelcome", "true");
		resource2Servlet.setInitParameter("welcomeServlets", "true");
		resource2Servlet.setInitParameter("pathInfoOnly", "true");
		resource2Servlet.setInitParameter("resourceBase", b3.getAbsolutePath());
		handler.addServlet(resource2Servlet, "/s/*");

		// the "/indexx/*" (and *.y and *.x) servlet which should be available through welcome files
		ServletHolder indexxServlet = new ServletHolder("indexx", new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().println("'indexx servlet'");
				resp.getWriter().println("req.request_uri=\"" + req.getRequestURI() + "\"");
				resp.getWriter().println("req.context_path=\"" + req.getContextPath() + "\"");
				resp.getWriter().println("req.servlet_path=\"" + req.getServletPath() + "\"");
				resp.getWriter().println("req.path_info=\"" + req.getPathInfo() + "\"");
				resp.getWriter().println("req.query_string=\"" + req.getQueryString() + "\"");
				resp.getWriter().println("javax.servlet.forward.mapping=\"" + req.getAttribute("javax.servlet.forward.mapping") + "\"");
				resp.getWriter().println("javax.servlet.forward.request_uri=\"" + req.getAttribute("javax.servlet.forward.request_uri") + "\"");
				resp.getWriter().println("javax.servlet.forward.context_path=\"" + req.getAttribute("javax.servlet.forward.context_path") + "\"");
				resp.getWriter().println("javax.servlet.forward.servlet_path=\"" + req.getAttribute("javax.servlet.forward.servlet_path") + "\"");
				resp.getWriter().println("javax.servlet.forward.path_info=\"" + req.getAttribute("javax.servlet.forward.path_info") + "\"");
				resp.getWriter().println("javax.servlet.forward.query_string=\"" + req.getAttribute("javax.servlet.forward.query_string") + "\"");
				resp.getWriter().println("javax.servlet.include.mapping=\"" + req.getAttribute("javax.servlet.include.mapping") + "\"");
				resp.getWriter().println("javax.servlet.include.request_uri=\"" + req.getAttribute("javax.servlet.include.request_uri") + "\"");
				resp.getWriter().println("javax.servlet.include.context_path=\"" + req.getAttribute("javax.servlet.include.context_path") + "\"");
				resp.getWriter().println("javax.servlet.include.servlet_path=\"" + req.getAttribute("javax.servlet.include.servlet_path") + "\"");
				resp.getWriter().println("javax.servlet.include.path_info=\"" + req.getAttribute("javax.servlet.include.path_info") + "\"");
				resp.getWriter().println("javax.servlet.include.query_string=\"" + req.getAttribute("javax.servlet.include.query_string") + "\"");
			}
		});
		ServletMapping indexxMapping = new ServletMapping();
		indexxMapping.setServletName("indexx");
		indexxMapping.setPathSpecs(new String[] { "*.x", "*.y" });
		handler.getServletHandler().addServlet(indexxServlet);
		handler.getServletHandler().addServletMapping(indexxMapping);

		// the "/gateway/*" servlet through which we'll forward to/include other servlets
		ServletHolder gatewayServlet = new ServletHolder("gateway", new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				String what = req.getParameter("what");
				String where = req.getParameter("where");
				switch (what) {
					case "redirect":
						resp.sendRedirect(where);
						return;
					case "forward":
						// we can't send anything when forwarding
						req.getRequestDispatcher(where).forward(req, resp);
						return;
					case "include":
						resp.getWriter().print(">>>");
						req.getRequestDispatcher(where).include(req, resp);
						resp.getWriter().print("<<<");
						return;
					default:
				}
			}
		});
		handler.addServlet(gatewayServlet, "/gateway/*");

		chc.addHandler(handler);
		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		// --- resource access through "/" servlet

		// sanity check for physical resource at root of resource servlet
		String response = send(port, "/c/index.z");
		assertTrue(response.endsWith("'index-z-b1'"));

		// "/" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// forward is performed implicitly by Jetty's DefaultServlet
		response = send(port, "/c/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/index.y\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/c/\""));

		// Forward vs. Include:
		// in forward method:
		//  - original servletPath, pathInfo, requestURI are available ONLY through javax.servlet.forward.* attributes
		//  - values used to obtain the dispatcher are available through request object
		// in include method:
		//  - original servletPath, pathInfo, requestURI are available through request object
		//  - values used to obtain the dispatcher are available through javax.servlet.include.* attributes

		// "/" (but through gateway) - similar forward, but performed explicitly by gateway servlet
		// 9.4 The Forward Method:
		//     The path elements of the request object exposed to the target servlet must reflect the
		//     path used to obtain the RequestDispatcher.
		// so "gateway" forwards to "/", "/" is handled by "default" which forwards to "/index.y"
		response = send(port, "/c/gateway/x?what=forward&where=/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/index.y\""));
		assertTrue(response.contains("javax.servlet.forward.context_path=\"/c\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("javax.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("javax.servlet.forward.path_info=\"/x\""));

		// "/", but included by gateway servlet
		// "gateway" includes "/" which includes "/index.y"
		response = send(port, "/c/gateway/x?what=include&where=/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("javax.servlet.include.context_path=\"/c\""));
		assertTrue(response.contains("javax.servlet.include.request_uri=\"/c/index.y\""));
		assertTrue(response.contains("javax.servlet.include.servlet_path=\"/index.y\""));
		assertTrue(response.contains("javax.servlet.include.path_info=\"null\""));

		response = send(port, "/c/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/c/gateway/x?what=forward&where=/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// included servlet (here - "default") can't set Location header (org.eclipse.jetty.server.Response.isMutable() returns false)
		response = send(port, "/c/gateway/x?what=include&where=/sub");
		assertTrue(response.contains(">>><<<"));

		// "/sub/" + "index.x" welcome files is forwarded and mapped to indexx servlet
		// According to 10.10 "Welcome Files":
		//    The Web server must append each welcome file in the order specified in the deployment descriptor to the
		//    partial request and check whether a static resource in the WAR is mapped to that
		//    request URI. If no match is found, the Web server MUST again append each
		//    welcome file in the order specified in the deployment descriptor to the partial
		//    request and check if a servlet is mapped to that request URI.
		//    [...]
		//    The container may send the request to the welcome resource with a forward, a redirect, or a
		//    container specific mechanism that is indistinguishable from a direct request.
		// Jetty detects /sub/index.y (first welcome file) can be mapped to indexx servlet, but continues the
		// search for physical resource. /sub/index.x is actual physical resource, so forward is chosen, which
		// is eventually mapped to indexx again - with index.x, not index.y
		response = send(port, "/c/sub/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/sub/index.x\""));
		assertTrue(response.contains("javax.servlet.forward.context_path=\"/c\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/c/sub/\""));
		assertTrue(response.contains("javax.servlet.forward.servlet_path=\"/sub/\""));
		assertTrue(response.contains("javax.servlet.forward.path_info=\"null\""));

		response = send(port, "/c/gateway/x?what=forward&where=/sub/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/sub/index.x\""));
		assertTrue(response.contains("javax.servlet.forward.context_path=\"/c\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("javax.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("javax.servlet.forward.path_info=\"/x\""));

		response = send(port, "/c/gateway/x?what=include&where=/sub/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("javax.servlet.include.context_path=\"/c\""));
		assertTrue(response.contains("javax.servlet.include.request_uri=\"/c/sub/index.x\""));
		assertTrue(response.contains("javax.servlet.include.servlet_path=\"/sub/index.x\""));
		assertTrue(response.contains("javax.servlet.include.path_info=\"null\""));

		// --- resource access through "/r" servlet

		response = send(port, "/c/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/c/gateway/x?what=forward&where=/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// included servlet/resource can't be redirected
		response = send(port, "/c/gateway/x?what=include&where=/r");
		assertTrue(response.endsWith(">>><<<"));

		// "/r" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// forward is performed implicitly by Jetty's DefaultServlet (even if mapped to /r/*), forward URI is
		// "/r/index.y" (first welcome), but this time, "/r/*" is a mapping with higher priority than "*.y"
		// (with "/" servlet, "*.y" had higher priority than "/"), so "resource" servlet is called, this time
		// with full URI (no welcome files are checked). Such resource is not found, so we have 404
		response = send(port, "/c/r/");
		assertTrue(response.startsWith("HTTP/1.1 404"));

		response = send(port, "/c/gateway/x?what=forward&where=/r/");
		assertTrue(response.startsWith("HTTP/1.1 404"));
		response = send(port, "/c/gateway/x?what=include&where=/r/");
		// HTTP 500 according to 9.3 "The Include Method"
		assertTrue(response.startsWith("HTTP/1.1 500"));

		response = send(port, "/c/r/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/c/gateway/x?what=forward&where=/r/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/c/gateway/x?what=include&where=/r/sub");
		assertTrue(response.contains(">>><<<"));

		// this time, welcome file is /sub/index.x and even if it maps to existing servlet (*.x), physical
		// resource exists and is returned
		response = send(port, "/c/r/sub/");
		assertTrue(response.endsWith("'sub/index-b2'"));
		response = send(port, "/c/gateway/x?what=forward&where=/r/sub/");
		assertTrue(response.endsWith("'sub/index-b2'"));
		response = send(port, "/c/gateway/x?what=include&where=/r/sub/");
		assertTrue(response.endsWith(">>>'sub/index-b2'<<<"));

		// --- resource access through "/s" servlet - welcome files with redirect

		response = send(port, "/c/s");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/c/gateway/x?what=forward&where=/s");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// included servlet/resource can't be redirected
		response = send(port, "/c/gateway/x?what=include&where=/s");
		assertTrue(response.endsWith(">>><<<"));

		response = send(port, "/c/s/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// redirect to first welcome page with found *.y mapping, but another mapping will be found using /s/*
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/s/index.y"));

		response = send(port, "/c/gateway/x?what=forward&where=/s/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/s/index.y?what=forward&where=/s/"));
		response = send(port, "/c/gateway/x?what=include&where=/s/");
		assertTrue(response.contains(">>><<<"));

		response = send(port, "/c/s/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/s/sub/"));
		response = send(port, "/c/gateway/x?what=forward&where=/s/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/s/sub/?what=forward&where=/s/sub"));
		response = send(port, "/c/gateway/x?what=include&where=/s/sub");
		assertTrue(response.contains(">>><<<"));

		// this time, welcome file is /sub/index.x and even if it maps to existing servlet (*.x), physical
		// resource exists and is returned
		response = send(port, "/c/s/sub/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/s/sub/index.x"));
		response = send(port, "/c/gateway/x?what=forward&where=/s/sub/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/s/sub/index.x?what=forward&where=/s/sub/"));
		response = send(port, "/c/gateway/x?what=include&where=/s/sub/");
		assertTrue(response.contains(">>><<<"));

		server.stop();
		server.join();
	}

	@Test
	public void jettyUrlMapping() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();

		Servlet servlet = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		ServletContextHandler rootHandler = new ServletContextHandler(chc, "", ServletContextHandler.NO_SESSIONS);
		rootHandler.setAllowNullPathInfo(true);
		rootHandler.getServletHandler().addServlet(new ServletHolder("default-servlet", servlet));
		map(rootHandler, "default-servlet", new String[] {
				"/p/*",
				"*.action",
				"",
//				"/",
				"/x"
		});

		ServletContextHandler otherHandler = new ServletContextHandler(chc, "/c1", ServletContextHandler.NO_SESSIONS);
		otherHandler.setAllowNullPathInfo(true);
		otherHandler.getServletHandler().addServlet(new ServletHolder("default-servlet", servlet));
		map(otherHandler, "default-servlet", new String[] {
				"/p/*",
				"*.action",
				"",
//				"/",
				"/x"
		});

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		// Jetty mapping is done in 3 stages:
		// - host finding:
		//    - in Jetty, vhost is checked at the level of each handler from the collection
		//    - org.eclipse.jetty.server.handler.ContextHandler.checkVirtualHost() returns a match based on
		//      javax.servlet.ServletRequest.getServerName() ("Host" HTTP header)
		// - context finding:
		//    - on each handler from the collection, org.eclipse.jetty.server.handler.ContextHandler.checkContextPath()
		//      is called
		//    - if there are many ContextHandlers in the collection, it's the collection that first matches
		//      request URI to a context. Virtual Host seems to be checked later...
		// - servlet finding:
		//    - org.eclipse.jetty.servlet.ServletHandler.getMappedServlet() where ServletHandler is a field of
		//      ServletContextHandler

		String response;

		// ROOT context
		response = send(connector.getLocalPort(), "/p/anything");
		assertTrue(response.endsWith("|  | /p | /anything |"));
		response = send(connector.getLocalPort(), "/anything.action");
		assertTrue(response.endsWith("|  | /anything.action | null |"));
		// just can't send `GET  HTTP/1.1` request
//		response = send(connector.getLocalPort(), "");
		response = send(connector.getLocalPort(), "/");
		// Jetty fixed https://github.com/eclipse-ee4j/servlet-api/issues/300
		// with https://github.com/eclipse/jetty.project/issues/4542
		assertTrue("Special, strange Servlet API 4 mapping rule", response.endsWith("|  |  | / |"));
		response = send(connector.getLocalPort(), "/x");
		assertTrue(response.endsWith("|  | /x | null |"));
		response = send(connector.getLocalPort(), "/y");
		assertTrue(response.contains("HTTP/1.1 404"));

		// /c1 context
		response = send(connector.getLocalPort(), "/c1/p/anything");
		assertTrue(response.endsWith("| /c1 | /p | /anything |"));
		response = send(connector.getLocalPort(), "/c1/anything.action");
		assertTrue(response.endsWith("| /c1 | /anything.action | null |"));
		response = send(connector.getLocalPort(), "/c1");
		// if org.eclipse.jetty.server.handler.ContextHandler.setAllowNullPathInfo(false):
//		assertTrue(response.contains("HTTP/1.1 302"));
		// still, treating as special "" mapping rule, it should be |  |  | / |
		// but IMO specification is wrong - context path should not be "", but should be ... context path
		assertTrue(response.endsWith("| /c1 |  | / |"));
		response = send(connector.getLocalPort(), "/c1/");
		// Jetty and Tomcat return (still incorrectly according to Servlet 4 spec) | /c1 |  | / | - but at least
		// consistently wrt findings from https://github.com/eclipse-ee4j/servlet-api/issues/300
		assertTrue("Special, strange Servlet API 4 mapping rule", response.endsWith("| /c1 |  | / |"));
		response = send(connector.getLocalPort(), "/c1/x");
		assertTrue(response.endsWith("| /c1 | /x | null |"));
		response = send(connector.getLocalPort(), "/c1/y");
		assertTrue(response.contains("HTTP/1.1 404"));

		server.stop();
		server.join();
	}

	private void map(ServletContextHandler h, String name, String[] uris) {
		ServletMapping mapping = new ServletMapping();
		mapping.setServletName(name);
		mapping.setPathSpecs(uris);
		h.getServletHandler().addServletMapping(mapping);
	}

	private String send(int port, String request, String... headers) throws IOException {
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
