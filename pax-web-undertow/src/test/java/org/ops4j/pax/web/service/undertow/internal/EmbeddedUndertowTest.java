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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class EmbeddedUndertowTest {

	public static Logger LOG = LoggerFactory.getLogger(EmbeddedUndertowTest.class);

	@Test
	public void undertowWithSingleContextAndServlet() throws Exception {
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

		ServletInfo servlet = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet.addMapping("/s1/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c1")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/c1", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		String response;

		response = send(port, "/c1/s1");
		assertTrue(response.endsWith("| /c1 | /s1 | null |"));

		response = send(port, "/c1/s2");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/c2/s1");
		assertTrue(response.contains("HTTP/1.1 404"));

		server.stop();
	}

	@Test
	public void addContextAfterServerHasStarted() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
			@Override
			public void init(ServletConfig config) throws ServletException {
				LOG.info("init() called on {} with {}", this, config);
				LOG.info(" - servlet name: {}", config.getServletName());
				LOG.info(" - context path: {}", config.getServletContext().getContextPath());
				super.init(config);
			}

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

		ServletInfo servlet = Servlets.servlet("c1s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet.addMapping("/s1/*");

		// io.undertow.servlet.api.DeploymentInfo is equivalent of javax.servlet.ServletContext
		DeploymentInfo deploymentInfo1 = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c1")
				.setDisplayName("Default Application")
				.setDeploymentName("d1")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet);

		ServletContainer container = Servlets.newContainer();

		DeploymentManager dm1 = container.addDeployment(deploymentInfo1);
		dm1.deploy();
		HttpHandler handler = dm1.start();

		path.addPrefixPath("/c1", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		String response;

		response = send(port, "/c1/s1");
		assertTrue(response.endsWith("| /c1 | /s1 | null |"));

		response = send(port, "/c1/s2");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/c2/s1");
		assertTrue(response.contains("HTTP/1.1 404"));

		// add new context

		ServletInfo servlet2 = Servlets.servlet("c2s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet2.addMapping("/s1/*");

		DeploymentInfo deploymentInfo2 = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c2")
				.setDisplayName("Default Application 2")
				.setDeploymentName("d2")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet2);

		DeploymentManager dm2 = container.addDeployment(deploymentInfo2);
		// deploy() will initialize io.undertow.servlet.core.DeploymentManagerImpl.deployment
		dm2.deploy();
		// start() produces actual io.undertow.server.HttpHandler
		HttpHandler handler2 = dm2.start();
		path.addPrefixPath("/c2", handler2);

		// add new servlet to existing context

		ServletInfo servlet3 = Servlets.servlet("c1s2", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet3.addMapping("/s2/*");

		deploymentInfo1.addServlet(servlet3);

		// either removal and addition of the same deployment:
//		container.getDeployment(deploymentInfo.getDeploymentName()).undeploy();
//		container.removeDeployment(deploymentInfo);
//		dm = container.addDeployment(deploymentInfo);
//		dm.deploy();
//		handler = dm.start();
		// where handler needs to be replaced:
//		path.removePrefixPath("/c1");
//		path.addPrefixPath("/c1", handler);

		// or adding a servlet to existing io.undertow.servlet.api.DeploymentManager without altering the handlers
//		container.getDeploymentByPath("/c1").getDeployment().getServlets().addServlet(servlet3);
		dm1.getDeployment().getServlets().addServlet(servlet3);

		response = send(port, "/c1/s1");
		assertTrue(response.endsWith("| /c1 | /s1 | null |"));

		response = send(port, "/c1/s2");
		assertTrue(response.endsWith("| /c1 | /s2 | null |"));

		response = send(port, "/c2/s1");
		assertTrue(response.endsWith("| /c2 | /s1 | null |"));

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

	private String send(int port, String request) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", port));

		s.getOutputStream().write((
				"GET " + request + " HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + port + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read = -1;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.close();

		return sw.toString();
	}

}
