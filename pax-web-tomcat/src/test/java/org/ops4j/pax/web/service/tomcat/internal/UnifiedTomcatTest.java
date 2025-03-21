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
package org.ops4j.pax.web.service.tomcat.internal;

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
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ops4j.pax.web.service.tomcat.internal.web.TomcatResourceServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that has matching tests in pax-web-jetty and pax-web-undertow
 */
public class UnifiedTomcatTest {

	public static final Logger LOG = LoggerFactory.getLogger(UnifiedTomcatTest.class);

	@Test
	public void twoResourceServletsWithDifferentBases() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(0);
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Context rootContext = new StandardContext();
		rootContext.setName("");
		rootContext.setPath("");
		rootContext.setMapperContextRootRedirectEnabled(false);
		rootContext.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				rootContext.setConfigured(true);
			}
		});
		host.addChild(rootContext);

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

		StandardWrapper default1Wrapper = new StandardWrapper();
		default1Wrapper.setName("default1");
		default1Wrapper.setServlet(new TomcatResourceServlet(b1, null, null));
		default1Wrapper.addInitParameter("listings", "false");

		rootContext.addChild(default1Wrapper);
		rootContext.addServletMappingDecoded("/d1/*", "default1");

		StandardWrapper default2Wrapper = new StandardWrapper();
		default2Wrapper.setName("default2");
		default2Wrapper.setServlet(new TomcatResourceServlet(b2, null, null));
		default2Wrapper.addInitParameter("listings", "false");

		rootContext.addChild(default2Wrapper);
		rootContext.addServletMappingDecoded("/d2/*", "default2");

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
		server.destroy();
	}

	@Test
	public void standardWelcomePages() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(0);
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Context rootContext = new StandardContext();
		rootContext.setName("");
		rootContext.setPath("");
		rootContext.setMapperContextRootRedirectEnabled(false);
		rootContext.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				rootContext.setConfigured(true);
			}
		});
		host.addChild(rootContext);
		rootContext.addWelcomeFile("index.x");
		rootContext.addWelcomeFile("indexx");

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/index.x"))) {
			IOUtils.write("'sub/index'", fw1);
		}

		// important in StandardRoot.createMainResourceSet()
		rootContext.setDocBase(b1.getAbsolutePath());

		StandardWrapper defaultWrapper = new StandardWrapper();
		defaultWrapper.setName("default");
		defaultWrapper.setServlet(new DefaultServlet());
		defaultWrapper.addInitParameter("listings", "false");

		StandardWrapper indexxWrapper = new StandardWrapper();
		indexxWrapper.setName("indexx");
		indexxWrapper.setServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().print("'indexx'");
			}
		});

		rootContext.addChild(defaultWrapper);
		rootContext.addServletMappingDecoded("/", "default");
		rootContext.addChild(indexxWrapper);
		// this mapping should be triggered at welcome file mapping stage, before dispatching to '/' servlet
		rootContext.addServletMappingDecoded("/indexx/*", "indexx");

		server.start();

		int port = connector.getLocalPort();

		String response = send(port, "/");
		assertThat(response).endsWith("'indexx'");
		response = send(port, "/sub/");
		assertThat(response).endsWith("'sub/index'");
		response = send(port, "/sub");
		assertThat(response).startsWith("HTTP/1.1 302");

		server.stop();
		server.destroy();
	}

	@Test
	public void standardWelcomePagesWithDifferentContext() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(0);
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Context rootContext = new StandardContext();
		rootContext.setName("");
		rootContext.setPath("/c");
		rootContext.setMapperContextRootRedirectEnabled(false);
		rootContext.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				rootContext.setConfigured(true);
			}
		});
		host.addChild(rootContext);
		rootContext.addWelcomeFile("index.x");
		rootContext.addWelcomeFile("indexx");

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/index.x"))) {
			IOUtils.write("'sub/index'", fw1);
		}

		// important in StandardRoot.createMainResourceSet()
		rootContext.setDocBase(b1.getAbsolutePath());

		StandardWrapper defaultWrapper = new StandardWrapper();
		defaultWrapper.setName("default");
		defaultWrapper.setServlet(new DefaultServlet());
		defaultWrapper.addInitParameter("listings", "false");

		StandardWrapper indexxWrapper = new StandardWrapper();
		indexxWrapper.setName("indexx");
		indexxWrapper.setServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().print("'indexx'");
			}
		});

		rootContext.addChild(defaultWrapper);
		rootContext.addServletMappingDecoded("/", "default");
		rootContext.addChild(indexxWrapper);
		// this mapping should be triggered at welcome file mapping stage, before dispatching to '/' servlet
		rootContext.addServletMappingDecoded("/indexx/*", "indexx");

		server.start();

		int port = connector.getLocalPort();

		String response = send(port, "/");
		assertThat(response).startsWith("HTTP/1.1 404");
		response = send(port, "/sub/");
		assertThat(response).startsWith("HTTP/1.1 404");
		response = send(port, "/sub");
		assertThat(response).startsWith("HTTP/1.1 404");

		response = send(port, "/c");
		assertThat(response).startsWith("HTTP/1.1 302");
		response = send(port, "/c/");
		assertThat(response).endsWith("'indexx'");
		response = send(port, "/c/sub/");
		assertThat(response).endsWith("'sub/index'");
		response = send(port, "/c/sub");
		assertThat(response).startsWith("HTTP/1.1 302");

		server.stop();
		server.destroy();
	}

	@Test
	public void resourceServletWithWelcomePages() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(0);
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Context rootContext = new StandardContext();
		rootContext.setName("");
		rootContext.setPath("");
		rootContext.setMapperContextRootRedirectEnabled(false);
		rootContext.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				rootContext.setConfigured(true);
			}
		});
		host.addChild(rootContext);

		// not needed at context level - each TomcatResourceServlet needs own WelcomeFiles
//		rootContext.addWelcomeFile("index.txt");

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

		StandardWrapper default1Wrapper = new StandardWrapper();
		default1Wrapper.setName("default1");
		TomcatResourceServlet servlet = new TomcatResourceServlet(b1, null, null);
		servlet.setWelcomeFiles(new String[] { "index.txt" });
		default1Wrapper.setServlet(servlet);
		default1Wrapper.addInitParameter("listings", "false");

		rootContext.addChild(default1Wrapper);
		rootContext.addServletMappingDecoded("/d1/*", "default1");

		server.start();

		int port = connector.getLocalPort();

		String response = send(port, "/hello.txt");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/d1/hello.txt");
		assertThat(response).endsWith("'hello.txt'");
		response = send(port, "/d1/sub/hello.txt");
		assertThat(response).endsWith("'sub/hello.txt'");

		response = send(port, "/d1/../hello.txt");
		assertTrue(response.contains("HTTP/1.1 404"));

		// here's where problems started. Jetty's default servlet itself handles welcome files, while in
		// Tomcat and Undertow, such support has to be added.

		response = send(port, "/d1/");
		assertThat(response).endsWith("'index.txt'");
		response = send(port, "/d1/sub/");
		assertThat(response).endsWith("'sub/index.txt'");
		response = send(port, "/d1");
		assertThat(response).startsWith("HTTP/1.1 302");
		response = send(port, "/d1/sub");
		assertThat(response).startsWith("HTTP/1.1 302");

		server.stop();
		server.destroy();
	}

	@Test
	@Disabled("Stopped working after moving to Tomcat 10. So maybe just like jetty/jetty.project/issues/10608?")
	public void paxWebWelcomePages() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(0);
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Context rootContext = new StandardContext();
		rootContext.setName("");
		rootContext.setPath("");
		rootContext.setMapperContextRootRedirectEnabled(false);
		rootContext.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				rootContext.setConfigured(true);
			}
		});
		host.addChild(rootContext);

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

		// the "/" default & resource servlet
		StandardWrapper defaultWrapper = new StandardWrapper();
		defaultWrapper.setName("default");
		TomcatResourceServlet servlet1 = new TomcatResourceServlet(b1, null, null);
		servlet1.setWelcomeFiles(new String[] { "index.y", "index.x" });
		defaultWrapper.setServlet(servlet1);
		defaultWrapper.addInitParameter("listings", "false");
		// this is required for servlet mapped to "/", because servletPath is incorrect. In Jetty we have
		// endless redirect instead
		defaultWrapper.addInitParameter("pathInfoOnly", "false");
		rootContext.addChild(defaultWrapper);
		rootContext.addServletMappingDecoded("/", "default");

		// the "/r" resource servlet
		StandardWrapper resourceWrapper = new StandardWrapper();
		resourceWrapper.setName("resource1");
		TomcatResourceServlet servlet2 = new TomcatResourceServlet(b2, null, null);
		servlet2.setWelcomeFiles(new String[] { "index.y", "index.x" });
		resourceWrapper.setServlet(servlet2);
		resourceWrapper.addInitParameter("listings", "false");
		rootContext.addChild(resourceWrapper);
		rootContext.addServletMappingDecoded("/r/*", "resource1");

		// the "/s" resource servlet - with redirected welcome files
		StandardWrapper resource2Wrapper = new StandardWrapper();
		resource2Wrapper.setName("resource2");
		TomcatResourceServlet servlet3 = new TomcatResourceServlet(b3, null, null);
		servlet3.setWelcomeFiles(new String[] { "index.y", "index.x" });
		resource2Wrapper.setServlet(servlet3);
		resource2Wrapper.addInitParameter("listings", "false");
		resource2Wrapper.addInitParameter("redirectWelcome", "true");
		rootContext.addChild(resource2Wrapper);
		rootContext.addServletMappingDecoded("/s/*", "resource2");

		// the "/indexx/*" (and *.y and *.x) servlet which should be available through welcome files
		StandardWrapper indexxWrapper = new StandardWrapper();
		indexxWrapper.setName("indexx");
		Servlet indexxServlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().println("'indexx servlet'");
				resp.getWriter().println("req.request_uri=\"" + req.getRequestURI() + "\"");
				resp.getWriter().println("req.context_path=\"" + req.getContextPath() + "\"");
				resp.getWriter().println("req.servlet_path=\"" + req.getServletPath() + "\"");
				resp.getWriter().println("req.path_info=\"" + req.getPathInfo() + "\"");
				resp.getWriter().println("req.query_string=\"" + req.getQueryString() + "\"");
				resp.getWriter().println("jakarta.servlet.forward.mapping=\"" + req.getAttribute("jakarta.servlet.forward.mapping") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.request_uri=\"" + req.getAttribute("jakarta.servlet.forward.request_uri") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.context_path=\"" + req.getAttribute("jakarta.servlet.forward.context_path") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.servlet_path=\"" + req.getAttribute("jakarta.servlet.forward.servlet_path") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.path_info=\"" + req.getAttribute("jakarta.servlet.forward.path_info") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.query_string=\"" + req.getAttribute("jakarta.servlet.forward.query_string") + "\"");
				resp.getWriter().println("jakarta.servlet.include.mapping=\"" + req.getAttribute("jakarta.servlet.include.mapping") + "\"");
				resp.getWriter().println("jakarta.servlet.include.request_uri=\"" + req.getAttribute("jakarta.servlet.include.request_uri") + "\"");
				resp.getWriter().println("jakarta.servlet.include.context_path=\"" + req.getAttribute("jakarta.servlet.include.context_path") + "\"");
				resp.getWriter().println("jakarta.servlet.include.servlet_path=\"" + req.getAttribute("jakarta.servlet.include.servlet_path") + "\"");
				resp.getWriter().println("jakarta.servlet.include.path_info=\"" + req.getAttribute("jakarta.servlet.include.path_info") + "\"");
				resp.getWriter().println("jakarta.servlet.include.query_string=\"" + req.getAttribute("jakarta.servlet.include.query_string") + "\"");
			}
		};
		indexxWrapper.setServlet(indexxServlet);
		rootContext.addChild(indexxWrapper);
		rootContext.addServletMappingDecoded("*.x", "indexx");
		rootContext.addServletMappingDecoded("*.y", "indexx");

		// the "/gateway/*" servlet through which we'll forward to/include other servlets
		StandardWrapper gatewayWrapper = new StandardWrapper();
		gatewayWrapper.setName("gateway");
		Servlet gatewayServlet = new HttpServlet() {
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
		};
		gatewayWrapper.setServlet(gatewayServlet);
		rootContext.addChild(gatewayWrapper);
		rootContext.addServletMappingDecoded("/gateway/*", "gateway");

		server.start();

		int port = connector.getLocalPort();

		// --- resource access through "/" servlet

		// sanity check for physical resource at root of resource servlet
		String response = send(port, "/index.z");
		assertTrue(response.endsWith("'index-z-b1'"));

		// "/" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// (see order of welcome files). Forward is performed implicitly by Tomcat's DefaultServlet.
		response = send(port, "/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/\""));

		// Forward vs. Include:
		// in forward method:
		//  - original servletPath, pathInfo, requestURI are available ONLY through jakarta.servlet.forward.* attributes
		//  - values used to obtain the dispatcher are available through request object
		// in include method:
		//  - original servletPath, pathInfo, requestURI are available through request object
		//  - values used to obtain the dispatcher are available through jakarta.servlet.include.* attributes

		// "/" (but through gateway) - similar forward, but performed explicitly by gateway servlet
		// 9.4 The Forward Method:
		//     The path elements of the request object exposed to the target servlet must reflect the
		//     path used to obtain the RequestDispatcher.
		// so "gateway" forwards to "/", "/" is handled by "default" which forwards to "/index.y"
		response = send(port, "/gateway/x?what=forward&where=/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"/x\""));

		// "/", but included by gateway servlet
		// "gateway" includes "/" which includes "/index.y"
		response = send(port, "/gateway/x?what=include&where=/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.include.context_path=\"\""));
		assertTrue(response.contains("jakarta.servlet.include.request_uri=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.include.servlet_path=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.include.path_info=\"null\""));

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
		// Tomcat detects /sub/index.y (first welcome file) can be mapped to indexx servlet, but continues the
		// search for physical resource. /sub/index.x is actual physical resource, so forward is chosen, which
		// is eventually mapped to indexx again - with index.x, not index.y
		response = send(port, "/sub/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/sub/index.x\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/sub/\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/sub/\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"null\""));

		response = send(port, "/gateway/x?what=forward&where=/sub/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/sub/index.x\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"/x\""));

		response = send(port, "/gateway/x?what=include&where=/sub/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.include.context_path=\"\""));
		assertTrue(response.contains("jakarta.servlet.include.request_uri=\"/sub/index.x\""));
		assertTrue(response.contains("jakarta.servlet.include.servlet_path=\"/sub/index.x\""));
		assertTrue(response.contains("jakarta.servlet.include.path_info=\"null\""));

		// --- resource access through "/r" servlet

		response = send(port, "/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/gateway/x?what=forward&where=/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// included servlet/resource can't be redirected
		response = send(port, "/gateway/x?what=include&where=/r");
		assertTrue(response.endsWith(">>><<<"));

		// See https://github.com/eclipse/jetty.project/issues/9910
		// for changed welcome file handling with Jetty's own "pathInfoOnly"

		// "/r" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// forward is performed implicitly by Tomcat's DefaultServlet (even if mapped to /r/*), forward URI is
		// "/index.y" (first welcome), which is different than in Jetty before 12, where forward was "/r/index.y".
		// the reasons are explained in https://github.com/eclipse/jetty.project/issues/9910 and quick summary may
		// be just this:
		//  - '/r/' is handled by resource servlet which may (pathInfoOnly=false) or may not (pathInfoOnly=true)
		//    use '/r/' prefix to find resources (with welcome file definitions support)
		//  - at the stage of servlet mapping with welcome files, pahInfoOnly setting was not used by Jetty before 12
		//    and the forward (or redirect) was prepending the URL with servlet path
		//  - so in Pax Web, where we always set pathInfoOnly=true for resource (default) servlets mapped to something
		//    different than '/', we should always reject servlet path - during resource lookup AND during
		//    forward/redirect
		//  - and pathInfoOnly=false is set only for '/' mapped servlets where it's not changing URI construction.
		//    it's used only to prevent endless redirect ;)
		response = send(port, "/r/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/r/\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/r\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"/\""));

		response = send(port, "/gateway/x?what=forward&where=/r/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"/x\""));
		response = send(port, "/gateway/x?what=include&where=/r/");
		// gateway uses dispatcher for /r/ and calls include. /r/* servlet uses welcome files, so index.y servlet mapping
		// is found and include target is /index.y (in Jetty before 12 it was /r/index.x which wasn't found by
		// resource servlet, so should result in HTTP 500 according to 9.3 "The Include Method")
		assertTrue(response.contains(">>>'indexx servlet'"));
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.include.context_path=\"\""));
		assertTrue(response.contains("jakarta.servlet.include.request_uri=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.include.servlet_path=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.include.path_info=\"null\""));
		// HTTP 500 according to 9.3 "The Include Method"
		response = send(port, "/gateway/x?what=include&where=/r/xyz");
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
		// redirect to first welcome page with found *.y mapping, and with redirect NOT using '/s' servlet path
		// (because pathInfoOnly=true)
		assertTrue(extractHeaders(response).get("Location").endsWith("/index.y"));

		response = send(port, "/gateway/x?what=forward&where=/s/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/index.y?what=forward&where=/s/"));
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
		server.destroy();
	}

	@Test
	@Disabled("Stopped working after moving to Tomcat 10. So maybe just like jetty/jetty.project/issues/10608?")
	public void paxWebWelcomePagesWithDifferentContext() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(0);
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Context rootContext = new StandardContext();
		rootContext.setName("");
		rootContext.setPath("/c");
		rootContext.setMapperContextRootRedirectEnabled(false);
		rootContext.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				rootContext.setConfigured(true);
			}
		});
		host.addChild(rootContext);

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

		// the "/" default & resource servlet
		StandardWrapper defaultWrapper = new StandardWrapper();
		defaultWrapper.setName("default");
		TomcatResourceServlet servlet1 = new TomcatResourceServlet(b1, null, null);
		servlet1.setWelcomeFiles(new String[] { "index.y", "index.x" });
		defaultWrapper.setServlet(servlet1);
		defaultWrapper.addInitParameter("listings", "false");
		// this is required for servlet mapped to "/", because servletPath is incorrect. In Jetty we have
		// endless redirect instead
		defaultWrapper.addInitParameter("pathInfoOnly", "false");
		rootContext.addChild(defaultWrapper);
		rootContext.addServletMappingDecoded("/", "default");

		// the "/r" resource servlet
		StandardWrapper resourceWrapper = new StandardWrapper();
		resourceWrapper.setName("resource");
		TomcatResourceServlet servlet2 = new TomcatResourceServlet(b2, null, null);
		servlet2.setWelcomeFiles(new String[] { "index.y", "index.x" });
		resourceWrapper.setServlet(servlet2);
		resourceWrapper.addInitParameter("listings", "false");
		rootContext.addChild(resourceWrapper);
		rootContext.addServletMappingDecoded("/r/*", "resource");

		// the "/s" resource servlet - with redirected welcome files
		StandardWrapper resource2Wrapper = new StandardWrapper();
		resource2Wrapper.setName("resource2");
		TomcatResourceServlet servlet3 = new TomcatResourceServlet(b3, null, null);
		servlet3.setWelcomeFiles(new String[] { "index.y", "index.x" });
		resource2Wrapper.setServlet(servlet3);
		resource2Wrapper.addInitParameter("listings", "false");
		resource2Wrapper.addInitParameter("redirectWelcome", "true");
		rootContext.addChild(resource2Wrapper);
		rootContext.addServletMappingDecoded("/s/*", "resource2");

		// the "/indexx/*" (and *.y and *.x) servlet which should be available through welcome files
		StandardWrapper indexxWrapper = new StandardWrapper();
		indexxWrapper.setName("indexx");
		Servlet indexxServlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().println("'indexx servlet'");
				resp.getWriter().println("req.request_uri=\"" + req.getRequestURI() + "\"");
				resp.getWriter().println("req.context_path=\"" + req.getContextPath() + "\"");
				resp.getWriter().println("req.servlet_path=\"" + req.getServletPath() + "\"");
				resp.getWriter().println("req.path_info=\"" + req.getPathInfo() + "\"");
				resp.getWriter().println("req.query_string=\"" + req.getQueryString() + "\"");
				resp.getWriter().println("jakarta.servlet.forward.mapping=\"" + req.getAttribute("jakarta.servlet.forward.mapping") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.request_uri=\"" + req.getAttribute("jakarta.servlet.forward.request_uri") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.context_path=\"" + req.getAttribute("jakarta.servlet.forward.context_path") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.servlet_path=\"" + req.getAttribute("jakarta.servlet.forward.servlet_path") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.path_info=\"" + req.getAttribute("jakarta.servlet.forward.path_info") + "\"");
				resp.getWriter().println("jakarta.servlet.forward.query_string=\"" + req.getAttribute("jakarta.servlet.forward.query_string") + "\"");
				resp.getWriter().println("jakarta.servlet.include.mapping=\"" + req.getAttribute("jakarta.servlet.include.mapping") + "\"");
				resp.getWriter().println("jakarta.servlet.include.request_uri=\"" + req.getAttribute("jakarta.servlet.include.request_uri") + "\"");
				resp.getWriter().println("jakarta.servlet.include.context_path=\"" + req.getAttribute("jakarta.servlet.include.context_path") + "\"");
				resp.getWriter().println("jakarta.servlet.include.servlet_path=\"" + req.getAttribute("jakarta.servlet.include.servlet_path") + "\"");
				resp.getWriter().println("jakarta.servlet.include.path_info=\"" + req.getAttribute("jakarta.servlet.include.path_info") + "\"");
				resp.getWriter().println("jakarta.servlet.include.query_string=\"" + req.getAttribute("jakarta.servlet.include.query_string") + "\"");
			}
		};
		indexxWrapper.setServlet(indexxServlet);
		rootContext.addChild(indexxWrapper);
		rootContext.addServletMappingDecoded("*.x", "indexx");
		rootContext.addServletMappingDecoded("*.y", "indexx");

		// the "/gateway/*" servlet through which we'll forward to/include other servlets
		StandardWrapper gatewayWrapper = new StandardWrapper();
		gatewayWrapper.setName("gateway");
		Servlet gatewayServlet = new HttpServlet() {
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
		};
		gatewayWrapper.setServlet(gatewayServlet);
		rootContext.addChild(gatewayWrapper);
		rootContext.addServletMappingDecoded("/gateway/*", "gateway");

		server.start();

		int port = connector.getLocalPort();

		// --- resource access through "/" servlet

		// sanity check for physical resource at root of resource servlet
		String response = send(port, "/c/index.z");
		assertTrue(response.endsWith("'index-z-b1'"));

		// "/" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// (see order of welcome files). Forward is performed implicitly by Tomcat's DefaultServlet.
		response = send(port, "/c/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/index.y\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/c/\""));

		// Forward vs. Include:
		// in forward method:
		//  - original servletPath, pathInfo, requestURI are available ONLY through jakarta.servlet.forward.* attributes
		//  - values used to obtain the dispatcher are available through request object
		// in include method:
		//  - original servletPath, pathInfo, requestURI are available through request object
		//  - values used to obtain the dispatcher are available through jakarta.servlet.include.* attributes

		// "/" (but through gateway) - similar forward, but performed explicitly by gateway servlet
		// 9.4 The Forward Method:
		//     The path elements of the request object exposed to the target servlet must reflect the
		//     path used to obtain the RequestDispatcher.
		// so "gateway" forwards to "/", "/" is handled by "default" which forwards to "/index.y"
		response = send(port, "/c/gateway/x?what=forward&where=/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/index.y\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"/c\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"/x\""));

		// "/", but included by gateway servlet
		// "gateway" includes "/" which includes "/index.y"
		response = send(port, "/c/gateway/x?what=include&where=/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.include.context_path=\"/c\""));
		assertTrue(response.contains("jakarta.servlet.include.request_uri=\"/c/index.y\""));
		assertTrue(response.contains("jakarta.servlet.include.servlet_path=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.include.path_info=\"null\""));

		response = send(port, "/c/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/c/gateway/x?what=forward&where=/sub");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// included servlet (here - "default") can't set Location header
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
		// Tomcat detects /sub/index.y (first welcome file) can be mapped to indexx servlet, but continues the
		// search for physical resource. /sub/index.x is actual physical resource, so forward is chosen, which
		// is eventually mapped to indexx again - with index.x, not index.y
		response = send(port, "/c/sub/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/sub/index.x\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"/c\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/c/sub/\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/sub/\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"null\""));

		response = send(port, "/c/gateway/x?what=forward&where=/sub/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/sub/index.x\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"/c\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"/x\""));

		response = send(port, "/c/gateway/x?what=include&where=/sub/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.include.context_path=\"/c\""));
		assertTrue(response.contains("jakarta.servlet.include.request_uri=\"/c/sub/index.x\""));
		assertTrue(response.contains("jakarta.servlet.include.servlet_path=\"/sub/index.x\""));
		assertTrue(response.contains("jakarta.servlet.include.path_info=\"null\""));

		// --- resource access through "/r" servlet

		response = send(port, "/c/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = send(port, "/c/gateway/x?what=forward&where=/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		// included servlet/resource can't be redirected
		response = send(port, "/c/gateway/x?what=include&where=/r");
		assertTrue(response.endsWith(">>><<<"));

		// See https://github.com/eclipse/jetty.project/issues/9910
		// for changed welcome file handling with Jetty's own "pathInfoOnly"

		// "/r" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// forward is performed implicitly by Tomcat's DefaultServlet (even if mapped to /r/*), forward URI is
		// "/index.y" (first welcome), which is different than in Jetty before 12, where forward was "/r/index.y".
		// the reasons are explained in https://github.com/eclipse/jetty.project/issues/9910 and quick summary may
		// be just this:
		//  - '/r/' is handled by resource servlet which may (pathInfoOnly=false) or may not (pathInfoOnly=true)
		//    use '/r/' prefix to find resources (with welcome file definitions support)
		//  - at the stage of servlet mapping with welcome files, pahInfoOnly setting was not used by Jetty before 12
		//    and the forward (or redirect) was prepending the URL with servlet path
		//  - so in Pax Web, where we always set pathInfoOnly=true for resource (default) servlets mapped to something
		//    different than '/', we should always reject servlet path - during resource lookup AND during
		//    forward/redirect
		//  - and pathInfoOnly=false is set only for '/' mapped servlets where it's not changing URI construction.
		//    it's used only to prevent endless redirect ;)
		response = send(port, "/c/r/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/index.y\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"/c\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/c/r/\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/r\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"/\""));

		response = send(port, "/c/gateway/x?what=forward&where=/r/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/index.y\""));
		assertTrue(response.contains("jakarta.servlet.forward.context_path=\"/c\""));
		assertTrue(response.contains("jakarta.servlet.forward.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("jakarta.servlet.forward.path_info=\"/x\""));
		response = send(port, "/c/gateway/x?what=include&where=/r/");
		// gateway uses dispatcher for /r/ and calls include. /r/* servlet uses welcome files, so index.y servlet mapping
		// is found and include target is /index.y (in Jetty before 12 it was /r/index.x which wasn't found by
		// resource servlet, so should result in HTTP 500 according to 9.3 "The Include Method")
		assertTrue(response.contains(">>>'indexx servlet'"));
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/gateway/x\""));
		assertTrue(response.contains("jakarta.servlet.include.context_path=\"/c\""));
		assertTrue(response.contains("jakarta.servlet.include.request_uri=\"/c/index.y\""));
		assertTrue(response.contains("jakarta.servlet.include.servlet_path=\"/index.y\""));
		assertTrue(response.contains("jakarta.servlet.include.path_info=\"null\""));
		// HTTP 500 according to 9.3 "The Include Method"
		response = send(port, "/c/gateway/x?what=include&where=/r/xyz");
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
		// redirect to first welcome page with found *.y mapping, and with redirect NOT using '/s' servlet path
		// (because pathInfoOnly=true)
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/index.y"));

		response = send(port, "/c/gateway/x?what=forward&where=/s/");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/index.y?what=forward&where=/s/"));
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
		server.destroy();
	}

	@Test
	public void tomcatUrlMapping() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(0);
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Context rootContext = new StandardContext();
//		rootContext.getPipeline().setBasic(new StandardContextValve());
		rootContext.setName("");
		rootContext.setPath("");
		rootContext.setMapperContextRootRedirectEnabled(false);
		rootContext.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				rootContext.setConfigured(true);
			}
		});
		host.addChild(rootContext);

		Context otherContext = new StandardContext();
		otherContext.setName("c1");
		// org.apache.catalina.core.StandardContext.setPath()
		//  - "/" or null - warning, conversion to ""
		//  - "" or "/<path>" - ok
		//  - if path ends with "/", warning, slash is trimmed
		otherContext.setPath("/c1");
		otherContext.setMapperContextRootRedirectEnabled(false);
		otherContext.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				otherContext.setConfigured(true);
			}
		});
		host.addChild(otherContext);

		Servlet servlet = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		Wrapper wrapper1 = new StandardWrapper();
		final Valve basicWrapperValve = wrapper1.getPipeline().getBasic();
		wrapper1.getPipeline().addValve(new ValveBase() {
			@Override
			public void invoke(Request request, Response response) throws IOException, ServletException {
				basicWrapperValve.invoke(request, response);
			}
		});
		wrapper1.setServlet(servlet);
		wrapper1.setName("s1");

		Wrapper wrapper2 = new StandardWrapper();
		wrapper2.setServlet(servlet);
		wrapper2.setName("s1");

		// Servlet API 4, 12.2 Specification of Mappings

		rootContext.addChild(wrapper1);

		// A string beginning with a '/' character and ending with a '/*' suffix is used for path mapping.
		// Goes to org.apache.catalina.mapper.Mapper.ContextVersion.wildcardWrappers
		rootContext.addServletMappingDecoded("/p/*", wrapper1.getName(), false);

		// A string beginning with a '*.' prefix is used as an extension mapping.
		// Goes to org.apache.catalina.mapper.Mapper.ContextVersion.extensionWrappers
		rootContext.addServletMappingDecoded("*.action", wrapper1.getName(), false);

		// The empty string ("") is a special URL pattern that exactly maps to the application's context root, i.e.,
		// requests of the form http://host:port/<context-root>/. In this case the path info is '/' and the
		// servlet path and context path is empty string ("").
		// org.apache.catalina.mapper.Mapper.addWrapper() replaces "" with "/" and adds it to
		// org.apache.catalina.mapper.Mapper.ContextVersion.exactWrappers
		//
		// Tomcat handles this "special case" in org.apache.catalina.mapper.Mapper.internalMapExactWrapper()
		//  - if URI is "/" and "/" is found in exactWrappers (normally "/" lands in ContextVersion.defaultWrapper)
		//    then Tomcat explicitly sets (according to 12.2):
		//     - path info = "/"
		//     - servlet path == ""
		//     - context path == ""
		//     - match type = jakarta.servlet.http.MappingMatch.CONTEXT_ROOT
		rootContext.addServletMappingDecoded("", wrapper1.getName(), false);

		// A string containing only the '/' character indicates the "default" servlet of the application.
		// In this case the servlet path is the request URI minus the context path and the path info is null.
		// Goes to org.apache.catalina.mapper.Mapper.ContextVersion.defaultWrapper
//		rootContext.addServletMappingDecoded("/", wrapper1.getName(), false);

		// All other strings are used for exact matches only.
		// Goes to org.apache.catalina.mapper.Mapper.ContextVersion.exactWrappers
		rootContext.addServletMappingDecoded("/x", wrapper1.getName(), false);

		otherContext.addChild(wrapper2);

		otherContext.addServletMappingDecoded("/p/*", wrapper2.getName(), false);
		otherContext.addServletMappingDecoded("*.action", wrapper2.getName(), false);
		otherContext.addServletMappingDecoded("", wrapper2.getName(), false);
//		otherContext.addServletMappingDecoded("/", wrapper2.getName(), false);
		otherContext.addServletMappingDecoded("/x", wrapper2.getName(), false);

		server.start();

		LOG.info("Local port after start: {}", connector.getLocalPort());

		// Tomcat mapping is done in 3 stages:
		// - host finding: org.apache.catalina.connector.CoyoteAdapter.postParseRequest()
		//    - jakarta.servlet.ServletRequest.getServerName() is set to match "Host" HTTP header
		//    - jakarta.servlet.ServletRequest.getLocalName() is set to result of:
		//       - NIO: java.net.Socket.getLocalAddress().getHostName()
		//       - NIO2: java.nio.channels.AsynchronousSocketChannel.getLocalAddress().getHostName()
		//       - APR: org.apache.tomcat.jni.Address.getnameinfo()
		//    - org.apache.catalina.connector.Connector.getUseIPVHosts() == true
		//       - host is chosed directly from jakarta.servlet.ServletRequest.getLocalName()
		//    - org.apache.catalina.connector.Connector.getUseIPVHosts() == false
		//       - host is chosed directly from jakarta.servlet.ServletRequest.getServerName()
		//    - it defaults to what's set in org.apache.catalina.Engine.setDefaultHost()
		// - context finding: org.apache.catalina.mapper.Mapper.internalMap()
		//    - org.apache.catalina.mapper.Mapper.MappedHost.contextList is searched
		//    - we have two:
		//      contextList.contexts = {org.apache.catalina.mapper.Mapper$MappedContext[2]@3454}
		//       0 = {org.apache.catalina.mapper.Mapper$MappedContext@3457}
		//        name: java.lang.String  = ""
		//       1 = {org.apache.catalina.mapper.Mapper$MappedContext@3458}
		//        name: java.lang.String  = "/c1"
		//    - the array (it is sorted) is searched by incoming URI
		//    - the found context's path is available as jakarta.servlet.http.HttpServletRequest.getContextPath()
		// - servlet finding: inside org.apache.catalina.mapper.Mapper.internalMapWrapper()
		//    - org.apache.catalina.mapper.Mapper.ContextVersion.exactWrappers are searched first
		//      contextVersion.exactWrappers = {org.apache.catalina.mapper.Mapper$MappedWrapper[2]@3722}
		//       0 = {org.apache.catalina.mapper.Mapper$MappedWrapper@3737}
		//        jspWildCard: boolean  = false
		//        resourceOnly: boolean  = false
		//        name: java.lang.String  = "/"
		//       1 = {org.apache.catalina.mapper.Mapper$MappedWrapper@3738}
		//        jspWildCard: boolean  = false
		//        resourceOnly: boolean  = false
		//        name: java.lang.String  = "/x"
		//    - org.apache.catalina.mapper.Mapper.ContextVersion.wildcardWrappers are checked
		//      contextVersion.wildcardWrappers = {org.apache.catalina.mapper.Mapper$MappedWrapper[1]@3088}
		//       0 = {org.apache.catalina.mapper.Mapper$MappedWrapper@3397}
		//        jspWildCard: boolean  = false
		//        resourceOnly: boolean  = false
		//        name: java.lang.String  = "/p"
		//    - org.apache.catalina.mapper.Mapper.ContextVersion.extensionWrappers are checked
		//      contextVersion.extensionWrappers = {org.apache.catalina.mapper.Mapper$MappedWrapper[1]@3101}
		//       0 = {org.apache.catalina.mapper.Mapper$MappedWrapper@3413}
		//        jspWildCard: boolean  = false
		//        resourceOnly: boolean  = false
		//        name: java.lang.String  = "action"
		//    - welcome files are checked
		//    - org.apache.catalina.mapper.Mapper.ContextVersion.defaultWrapper is checked
		//      contextVersion.defaultWrapper = {org.apache.catalina.mapper.Mapper$MappedWrapper@3394}
		//       jspWildCard: boolean  = false
		//       resourceOnly: boolean  = false
		//       name: java.lang.String  = ""

		// Tomcat invocation is performed using pipelines of valves. Interesting valves related to request
		// processing are (in order of invocation):
		// - org.apache.catalina.core.StandardEngineValve.invoke()
		//    - requires existing org.apache.catalina.connector.Request.getHost()
		// - org.apache.catalina.valves.ErrorReportValve.invoke()
		// - org.apache.catalina.core.StandardHostValve.invoke()
		//    - requires existing org.apache.catalina.connector.Request.getContext()
		// - org.apache.catalina.core.StandardContextValve.invoke()
		//    - requires existing org.apache.catalina.connector.Request.getWrapper()
		// - org.apache.catalina.core.StandardWrapperValve.invoke()
		//    - important org.apache.catalina.core.StandardWrapper.allocate() call that returns a jakarta.servlet.Servlet
		//    - org.apache.catalina.connector.Request.getFilterChain() is called inside static
		//      org.apache.catalina.core.ApplicationFilterFactory.createFilterChain(), so it'd be better to set
		//      such filter chain earlier. the same static method determines the filters to use and calls
		//      org.apache.catalina.core.ApplicationFilterChain.addFilter() for each matching - that's what Pax Web
		//      has to do.
		// - org.apache.catalina.core.ApplicationFilterChain.doFilter() is called - so Pax Web should definitely
		//   prepare own FilterChain in custom StandardWrapperValve inside StandardContext

		String response;

		// ROOT context
		response = send(connector.getLocalPort(), "/p/anything");
		assertTrue(response.endsWith("|  | /p | /anything |"));
		response = send(connector.getLocalPort(), "/anything.action");
		assertTrue(response.endsWith("|  | /anything.action | null |"));
		// just can't send `GET  HTTP/1.1` request
//		response = send(connector.getLocalPort(), "");
		response = send(connector.getLocalPort(), "/");
		assertTrue(response.endsWith("|  |  | / |"), "Special, strange Servlet API 4 mapping rule");
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
		// if org.apache.catalina.Context.setMapperContextRootRedirectEnabled(true):
//		assertTrue(response.contains("HTTP/1.1 302"));
		assertTrue(response.contains("HTTP/1.1 404"));
		response = send(connector.getLocalPort(), "/c1/");
		// https://bz.apache.org/bugzilla/show_bug.cgi?id=64109
//		assertTrue("Special, strange Servlet API 4 mapping rule", response.endsWith("|  |  | / |"));
		assertTrue(response.endsWith("| /c1 |  | / |"), "Special, strange Servlet API 4 mapping rule");
		response = send(connector.getLocalPort(), "/c1/x");
		assertTrue(response.endsWith("| /c1 | /x | null |"));
		response = send(connector.getLocalPort(), "/c1/y");
		assertTrue(response.contains("HTTP/1.1 404"));

		server.stop();
		server.destroy();
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
