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
package org.ops4j.pax.web.service.undertow.internal;

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

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.ops4j.pax.web.service.undertow.internal.web.UndertowResourceServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test that has matching tests in pax-web-jetty and pax-web-tomcat
 */
public class UnifiedUndertowTest {

	public static final Logger LOG = LoggerFactory.getLogger(UnifiedUndertowTest.class);

	@Test
	public void twoResourceServletsWithDifferentBases() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

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

		DirectBufferCache cache1 = new DirectBufferCache(1024, 64, 1024 * 1024);
		UndertowResourceServlet servlet1Instance = new UndertowResourceServlet(new File("target/b1"), null);
		CachingResourceManager manager1 = new CachingResourceManager(1024, 1024 * 1024, cache1,
				servlet1Instance, 3_600_000/*ms*/);
		servlet1Instance.setCachingResourceManager(manager1);

		DirectBufferCache cache2 = new DirectBufferCache(1024, 64, 1024 * 1024);
		UndertowResourceServlet servlet2Instance = new UndertowResourceServlet(new File("target/b2"), null);
		CachingResourceManager manager2 = new CachingResourceManager(1024, 1024 * 1024, cache2,
				servlet2Instance, 3_600_000/*ms*/);
		servlet2Instance.setCachingResourceManager(manager2);

		ServletInfo servlet1 = Servlets.servlet("default1", servlet1Instance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servlet1Instance));
		servlet1.addInitParam("directory-listing", "false");
		ServletInfo servlet2 = Servlets.servlet("default2", servlet2Instance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servlet2Instance));
		servlet2.addInitParam("directory-listing", "false");

		servlet1.addMapping("/d1/*");
		servlet2.addMapping("/d2/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet1, servlet2);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();

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

		// that's because of io.undertow.util.CanonicalPathUtils.canonicalize()
		response = send(port, "/d1/../hello.txt");
		assertTrue(response.endsWith("b1"));
		response = send(port, "/d2/../../../../../../hello.txt");
		assertTrue(response.endsWith("b2"));

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
	}

	@Test
	public void standardWelcomePages() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/index.x"))) {
			IOUtils.write("'sub/index'", fw1);
		}

		DefaultServlet servletInstance = new DefaultServlet();
		ServletInfo servlet1 = Servlets.servlet("default", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet1.addInitParam("directory-listing", "false");
		servlet1.addInitParam("resolve-against-context-root", "false");
		servlet1.addMapping("/");

		Servlet indexxInstance = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().print("'indexx'");
			}
		};
		ServletInfo servlet2 = Servlets.servlet("indexx", indexxInstance.getClass(), new ImmediateInstanceFactory<Servlet>(indexxInstance));
		servlet2.addMapping("/indexx/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addWelcomePages("index.x", "indexx")
				.setResourceManager(FileResourceManager.builder().setBase(b1.toPath()).build())
				// seems like Undertow doesn't need default servlet to handle welcome files
				.addServlets(/*servlet1, */servlet2);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();

		String response = send(port, "/");
		assertThat(response, endsWith("'indexx'"));
		response = send(port, "/sub/");
		assertThat(response, endsWith("'sub/index'"));
		response = send(port, "/sub");
		assertThat(response, startsWith("HTTP/1.1 302"));

		server.stop();
	}

	@Test
	public void standardWelcomePagesWithDifferentContext() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "sub/index.x"))) {
			IOUtils.write("'sub/index'", fw1);
		}

		DefaultServlet servletInstance = new DefaultServlet();
		ServletInfo servlet1 = Servlets.servlet("default", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet1.addInitParam("directory-listing", "false");
		servlet1.addInitParam("resolve-against-context-root", "false");
		servlet1.addMapping("/");

		Servlet indexxInstance = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().print("'indexx'");
			}
		};
		ServletInfo servlet2 = Servlets.servlet("indexx", indexxInstance.getClass(), new ImmediateInstanceFactory<Servlet>(indexxInstance));
		servlet2.addMapping("/indexx/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addWelcomePages("index.x", "indexx")
				.setResourceManager(FileResourceManager.builder().setBase(b1.toPath()).build())
				// seems like Undertow doesn't need default servlet to handle welcome files
				.addServlets(/*servlet1, */servlet2);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/c", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();

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
	}

	@Test
	public void resourceServletWithWelcomePages() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		File b1 = new File("target/b1");
		FileUtils.deleteDirectory(b1);
		b1.mkdirs();
		new File(b1, "sub").mkdirs();
		try (FileWriter fw1 = new FileWriter(new File(b1, "hello.txt"))) {
			IOUtils.write("'hello.txt'", fw1);
		}
		try (FileWriter fw1 = new FileWriter(new File(b1, "index.z"))) {
			IOUtils.write("'index-z-b1'", fw1);
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

		DirectBufferCache cache1 = new DirectBufferCache(1024, 64, 1024 * 1024);
		UndertowResourceServlet servlet1Instance = new UndertowResourceServlet(new File("target/b1"), null);
		servlet1Instance.setWelcomeFiles(new String[] { "index.txt" });
		CachingResourceManager manager1 = new CachingResourceManager(1024, 1024 * 1024, cache1,
				servlet1Instance, 3_600_000/*ms*/);
		servlet1Instance.setCachingResourceManager(manager1);

		ServletInfo servlet1 = Servlets.servlet("default1", servlet1Instance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servlet1Instance));
		servlet1.addInitParam("directory-listing", "false");
		servlet1.addInitParam("resolve-against-context-root", "false");

		servlet1.addMapping("/d1/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addWelcomePage("index.txt")
				.addServlets(servlet1);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();

		String response = send(port, "/hello.txt");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/d1/hello.txt");
		assertThat(response, endsWith("'hello.txt'"));
		response = send(port, "/d1/sub/hello.txt");
		assertThat(response, endsWith("'sub/hello.txt'"));

		response = send(port, "/d1/../hello.txt");
		// because Undertow canonicalizes the path without returning "bad request"
		assertThat(response, endsWith("'hello.txt'"));

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
	}

	@Test
	public void paxWebWelcomePages() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

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
		DirectBufferCache cache1 = new DirectBufferCache(1024, 64, 1024 * 1024);
		UndertowResourceServlet servlet1Instance = new UndertowResourceServlet(new File("target/b1"), null);
		servlet1Instance.setWelcomeFiles(new String[] { "index.y", "index.x" });
		CachingResourceManager manager1 = new CachingResourceManager(1024, 1024 * 1024, cache1,
				servlet1Instance, 3_600_000/*ms*/);
		servlet1Instance.setCachingResourceManager(manager1);
		ServletInfo servlet1 = Servlets.servlet("default", servlet1Instance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servlet1Instance));
		servlet1.addInitParam("directory-listing", "false");
		// for Jetty and Tomcat, "/" resource servlet needs "yes" here (i.e., include servletPath)
		servlet1.addInitParam("resolve-against-context-root", "true");
		servlet1.addInitParam("pathInfoOnly", "false");
		servlet1.addMapping("/");

		// the "/r" resource servlet
		DirectBufferCache cache2 = new DirectBufferCache(1024, 64, 1024 * 1024);
		UndertowResourceServlet servlet2Instance = new UndertowResourceServlet(new File("target/b2"), null);
		servlet2Instance.setWelcomeFiles(new String[] { "index.y", "index.x" });
		CachingResourceManager manager2 = new CachingResourceManager(1024, 1024 * 1024, cache2,
				servlet2Instance, 3_600_000/*ms*/);
		servlet2Instance.setCachingResourceManager(manager2);
		ServletInfo servlet2 = Servlets.servlet("resource", servlet2Instance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servlet2Instance));
		servlet2.addInitParam("directory-listing", "false");
		servlet2.addInitParam("resolve-against-context-root", "false");
		servlet2.addMapping("/r/*");

		// the "/s" resource servlet - with redirected welcome files
		DirectBufferCache cache3 = new DirectBufferCache(1024, 64, 1024 * 1024);
		UndertowResourceServlet servlet3Instance = new UndertowResourceServlet(new File("target/b3"), null);
		servlet3Instance.setWelcomeFiles(new String[] { "index.y", "index.x" });
		CachingResourceManager manager3 = new CachingResourceManager(1024, 1024 * 1024, cache3,
				servlet3Instance, 3_600_000/*ms*/);
		servlet3Instance.setCachingResourceManager(manager3);
		ServletInfo servlet3 = Servlets.servlet("resource2", servlet3Instance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servlet3Instance));
		servlet3.addInitParam("directory-listing", "false");
		servlet3.addInitParam("resolve-against-context-root", "false");
		servlet3.addInitParam("redirectWelcome", "true");
		servlet3.addMapping("/s/*");

		// the "/indexx/*" (and *.y and *.x) servlet which should be available through welcome files
		HttpServlet indexxServlet = new HttpServlet() {
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
		};
		ServletInfo indexxServletInfo = Servlets.servlet("indexx", indexxServlet.getClass(), new ImmediateInstanceFactory<HttpServlet>(indexxServlet));
		indexxServletInfo.addMappings("*.x", "*.y");

		// the "/gateway/*" servlet through which we'll forward to/include other servlets
		HttpServlet gatewayServlet = new HttpServlet() {
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
		ServletInfo gatewayServletInfo = Servlets.servlet("gateway", gatewayServlet.getClass(), new ImmediateInstanceFactory<HttpServlet>(gatewayServlet));
		gatewayServletInfo.addMapping("/gateway/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet1, servlet2, servlet3, indexxServletInfo, gatewayServletInfo);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();

		// --- resource access through "/" servlet

		// sanity check for physical resource at root of resource servlet
		String response = send(port, "/index.z");
		assertTrue(response.endsWith("'index-z-b1'"));

		// "/" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// forward is performed implicitly by Tomcat's DefaultServlet
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
		assertTrue(response.contains("javax.servlet.forward.context_path=\"\""));
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
		// Undertow detects /sub/index.y (first welcome file) can be mapped to indexx servlet, but continues the
		// search for physical resource. /sub/index.x is actual physical resource, so forward is chosen, which
		// is eventually mapped to indexx again - with index.x, not index.y
		response = send(port, "/sub/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/sub/index.x\""));
		assertTrue(response.contains("javax.servlet.forward.context_path=\"\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/sub/\""));
		assertTrue(response.contains("javax.servlet.forward.servlet_path=\"/sub/\""));
		assertTrue(response.contains("javax.servlet.forward.path_info=\"null\""));

		response = send(port, "/gateway/x?what=forward&where=/sub/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/sub/index.x\""));
		assertTrue(response.contains("javax.servlet.forward.context_path=\"\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("javax.servlet.forward.servlet_path=\"/gateway\""));
		assertTrue(response.contains("javax.servlet.forward.path_info=\"/x\""));

		response = send(port, "/gateway/x?what=include&where=/sub/");
		assertTrue(response.contains("req.context_path=\"\""));
		assertTrue(response.contains("req.request_uri=\"/gateway/x\""));
		assertTrue(response.contains("javax.servlet.include.context_path=\"\"")); // "/" for Jetty
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
		// forward is performed implicitly by Undertow's DefaultServlet (even if mapped to /r/*), forward URI is
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
		// https://github.com/eclipse/jetty.project/issues/5025
//		response = send(port, "/gateway/x?what=include&where=/r/sub/");
//		assertTrue(response.endsWith(">>>'sub/index-b2'<<<"));

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
	}

	@Test
	public void paxWebWelcomePagesWithDifferentContext() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

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
		DirectBufferCache cache1 = new DirectBufferCache(1024, 64, 1024 * 1024);
		UndertowResourceServlet servlet1Instance = new UndertowResourceServlet(new File("target/b1"), null);
		servlet1Instance.setWelcomeFiles(new String[] { "index.y", "index.x" });
		CachingResourceManager manager1 = new CachingResourceManager(1024, 1024 * 1024, cache1,
				servlet1Instance, 3_600_000/*ms*/);
		servlet1Instance.setCachingResourceManager(manager1);
		ServletInfo servlet1 = Servlets.servlet("default", servlet1Instance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servlet1Instance));
		servlet1.addInitParam("directory-listing", "false");
		// for Jetty and Tomcat, "/" resource servlet needs "yes" here (i.e., include servletPath)
		servlet1.addInitParam("resolve-against-context-root", "true");
		servlet1.addInitParam("pathInfoOnly", "false");
		servlet1.addMapping("/");

		// the "/r" resource servlet
		DirectBufferCache cache2 = new DirectBufferCache(1024, 64, 1024 * 1024);
		UndertowResourceServlet servlet2Instance = new UndertowResourceServlet(new File("target/b2"), null);
		servlet2Instance.setWelcomeFiles(new String[] { "index.y", "index.x" });
		CachingResourceManager manager2 = new CachingResourceManager(1024, 1024 * 1024, cache2,
				servlet2Instance, 3_600_000/*ms*/);
		servlet2Instance.setCachingResourceManager(manager2);
		ServletInfo servlet2 = Servlets.servlet("resource", servlet2Instance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servlet2Instance));
		servlet2.addInitParam("directory-listing", "false");
		servlet2.addInitParam("resolve-against-context-root", "false");
		servlet2.addMapping("/r/*");

		// the "/s" resource servlet - with redirected welcome files
		DirectBufferCache cache3 = new DirectBufferCache(1024, 64, 1024 * 1024);
		UndertowResourceServlet servlet3Instance = new UndertowResourceServlet(new File("target/b3"), null);
		servlet3Instance.setWelcomeFiles(new String[] { "index.y", "index.x" });
		CachingResourceManager manager3 = new CachingResourceManager(1024, 1024 * 1024, cache3,
				servlet3Instance, 3_600_000/*ms*/);
		servlet3Instance.setCachingResourceManager(manager3);
		ServletInfo servlet3 = Servlets.servlet("resource2", servlet3Instance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servlet3Instance));
		servlet3.addInitParam("directory-listing", "false");
		servlet3.addInitParam("resolve-against-context-root", "false");
		servlet3.addInitParam("redirectWelcome", "true");
		servlet3.addMapping("/s/*");

		// the "/indexx/*" (and *.y and *.x) servlet which should be available through welcome files
		HttpServlet indexxServlet = new HttpServlet() {
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
		};
		ServletInfo indexxServletInfo = Servlets.servlet("indexx", indexxServlet.getClass(), new ImmediateInstanceFactory<HttpServlet>(indexxServlet));
		indexxServletInfo.addMappings("*.x", "*.y");

		// the "/gateway/*" servlet through which we'll forward to/include other servlets
		HttpServlet gatewayServlet = new HttpServlet() {
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
		ServletInfo gatewayServletInfo = Servlets.servlet("gateway", gatewayServlet.getClass(), new ImmediateInstanceFactory<HttpServlet>(gatewayServlet));
		gatewayServletInfo.addMapping("/gateway/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet1, servlet2, servlet3, indexxServletInfo, gatewayServletInfo);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/c", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();

		// --- resource access through "/" servlet

		// sanity check for physical resource at root of resource servlet
		String response = send(port, "/c/index.z");
		assertTrue(response.endsWith("'index-z-b1'"));

		// "/" - no "/index.x" or "/index.y" physical resource, but existing mapping for *.y to indexx servlet
		// forward is performed implicitly by Tomcat's DefaultServlet
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
		// Undertow detects /sub/index.y (first welcome file) can be mapped to indexx servlet, but continues the
		// search for physical resource. /sub/index.x is actual physical resource, so forward is chosen, which
		// is eventually mapped to indexx again - with index.x, not index.y
		response = send(port, "/c/sub/");
		assertTrue(response.contains("req.context_path=\"/c\""));
		assertTrue(response.contains("req.request_uri=\"/c/sub/index.x\""));
		assertTrue(response.contains("javax.servlet.forward.context_path=\"/c\""));
		assertTrue(response.contains("javax.servlet.forward.request_uri=\"/c/sub/\""));
		assertTrue(response.contains("javax.servlet.forward.servlet_path=\"/sub/\"")); // TOCHECK: why not "/"?
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
		// forward is performed implicitly by Undertow's DefaultServlet (even if mapped to /r/*), forward URI is
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
		// https://github.com/eclipse/jetty.project/issues/5025
//		response = send(port, "/gateway/x?what=include&where=/r/sub/");
//		assertTrue(response.endsWith(">>>'sub/index-b2'<<<"));

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
	}

	@Test
	public void undertowUrlMapping() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
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

		ServletInfo servletForRoot = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));

		// Servlet API 4, 12.2 Specification of Mappings

		// A string beginning with a '/' character and ending with a '/*' suffix is used for path mapping.
		servletForRoot.addMapping("/p/*");

		// A string beginning with a '*.' prefix is used as an extension mapping.
		servletForRoot.addMapping("*.action");

		// The empty string ("") is a special URL pattern that exactly maps to the application's context root, i.e.,
		// requests of the form http://host:port/<context-root>/. In this case the path info is '/' and the
		// servlet path and context path is empty string ("").
		servletForRoot.addMapping("");

		// A string containing only the '/' character indicates the "default" servlet of the application.
		// In this case the servlet path is the request URI minus the context path and the path info is null.
//		servletForRoot.addMapping("/");

		// All other strings are used for exact matches only.
		servletForRoot.addMapping("/x");

		ServletInfo servletForOther = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servletForOther.addMapping("/p/*");
		servletForOther.addMapping("*.action");
		servletForOther.addMapping("");
//		servletForOther.addMapping("/");
		servletForOther.addMapping("/x");

		DeploymentInfo rootContext = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/") // null or "" are converted to "/"
				.setDeploymentName("d1")
				.setUrlEncoding("UTF-8")
				.addServlets(servletForRoot);

		DeploymentInfo otherContext = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c1")
				.setDeploymentName("d1")
				.setUrlEncoding("UTF-8")
				.addServlets(servletForOther);

		ServletContainer container = Servlets.newContainer();

		DeploymentManager rootDeployment = container.addDeployment(rootContext);
		rootDeployment.deploy();
		HttpHandler rootHandler = rootDeployment.start();
		DeploymentManager otherDeployment = container.addDeployment(otherContext);
		otherDeployment.deploy();
		HttpHandler otherHandler = otherDeployment.start();

		// the above handlers consist of:
		// rootHandler = {io.undertow.server.handlers.URLDecodingHandler@2350}
		//  next: io.undertow.server.HttpHandler  = {io.undertow.server.handlers.HttpContinueReadHandler@2365}
		//   handler: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.ServletInitialHandler@2368}
		//    next: io.undertow.server.HttpHandler  = {io.undertow.server.handlers.PredicateHandler@2371}
		//     trueHandler: io.undertow.server.HttpHandler  = {io.undertow.server.handlers.PredicateHandler@2382}
		//      trueHandler: io.undertow.server.HttpHandler  = {io.undertow.security.handlers.SecurityInitialHandler@2383}
		//       next: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler@2387}
		//        next: io.undertow.server.HttpHandler  = {io.undertow.security.handlers.AuthenticationMechanismsHandler@2393}
		//         next: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.security.ServletConfidentialityConstraintHandler@2394}
		//          next: io.undertow.server.HttpHandler  = {io.undertow.server.handlers.PredicateHandler@2397}
		//           falseHandler: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.security.ServletAuthenticationCallHandler@2400}
		//            next: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.security.SSLInformationAssociationHandler@2401}
		//             next: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.ServletDispatchingHandler@2384}

		path.addPrefixPath(otherContext.getContextPath(), otherHandler);
		path.addPrefixPath(rootContext.getContextPath(), rootHandler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		// Undertow mapping is done in more flexible way (than in Jetty and Tomcat):
		// - host finding: only if root handler is io.undertow.server.handlers.NameVirtualHostHandler
		// - context finding: checking io.undertow.util.PathMatcher.paths in io.undertow.server.handlers.PathHandler
		// - servlet finding:
		//    - io.undertow.servlet.handlers.ServletInitialHandler.handleRequest() gets
		//      io.undertow.servlet.handlers.ServletPathMatch from io.undertow.servlet.handlers.ServletInitialHandler.paths
		//      and puts it into new io.undertow.servlet.handlers.ServletRequestContext() as exchange attachment
		//    - io.undertow.servlet.handlers.ServletDispatchingHandler.handleRequest() takes this attachment, its chain
		//      and calls io.undertow.servlet.handlers.ServletChain.handler.handleRequest()
		//    - the above handler calls init() if needed (for the first time) and passes to
		//      io.undertow.servlet.handlers.ServletHandler.handleRequest()
		//    - javax.servlet.Servlet.service() is called

		String response;

		// ROOT context
		response = send(port, "/p/anything");
		assertTrue(response.endsWith("|  | /p | /anything |"));
		response = send(port, "/anything.action");
		assertTrue(response.endsWith("|  | /anything.action | null |"));
		// just can't send `GET  HTTP/1.1` request
//		response = send(port, "");
		response = send(port, "/");
		assertTrue("Special, strange Servlet API 4 mapping rule", response.endsWith("|  | / | null |"));
		response = send(port, "/x");
		assertTrue(response.endsWith("|  | /x | null |"));
		response = send(port, "/y");
		assertTrue(response.contains("HTTP/1.1 404"));

		// /c1 context
		response = send(port, "/c1/p/anything");
		assertTrue(response.endsWith("| /c1 | /p | /anything |"));
		response = send(port, "/c1/anything.action");
		assertTrue(response.endsWith("| /c1 | /anything.action | null |"));
		response = send(port, "/c1");
		// if org.apache.catalina.Context.setMapperContextRootRedirectEnabled(true):
//		assertTrue(response.contains("HTTP/1.1 302"));
		assertTrue(response.contains("HTTP/1.1 404"));
		response = send(port, "/c1/");
		// https://bz.apache.org/bugzilla/show_bug.cgi?id=64109
		assertTrue("Special, strange Servlet API 4 mapping rule", response.endsWith("| /c1 | / | null |"));
		response = send(port, "/c1/x");
		assertTrue(response.endsWith("| /c1 | /x | null |"));
		response = send(port, "/c1/y");
		assertTrue(response.contains("HTTP/1.1 404"));

		server.stop();
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
