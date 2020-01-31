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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.junit.Assert.assertTrue;

public class EmbeddedTomcatTest {

	public static Logger LOG = LoggerFactory.getLogger(EmbeddedTomcatTest.class);

	@BeforeClass
	public static void initClass() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@AfterClass
	public static void cleanupClass() {
		SLF4JBridgeHandler.uninstall();
	}

	@Test
	public void useBootstrap() throws Exception {
		// org.apache.catalina.startup.Bootstrap is the class used by normal Tomcat installation
		// it loads org.apache.catalina.startup.Catalina class, sets classloader inside and calls various methods
		// by reflection (due to classloader isolation)
		//
		// with `bin/catalina.sh start` command, the called methods are:
		// - org.apache.catalina.startup.Catalina.load():
		//    - commons-digester parses server.xml and calls different methods on object tree with Catalina as root
		//    - digester is configured explicitly in Java code in
		//      org.apache.catalina.startup.Catalina.createStartDigester()
		//    - org.apache.catalina.startup.Catalina.server <- org.apache.catalina.core.StandardServer
		// - org.apache.catalina.startup.Catalina.start()
		//    - org.apache.catalina.core.StandardServer.startInternal()

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

		Context context = new StandardContext();
		context.setName("ROOT");
		context.setPath("");
		// Fix startup sequence - required if you don't use web.xml. The start() method in context will set
		// 'configured' to false - and expects a listener to set it back to true.
//		context.addLifecycleListener(new Tomcat.FixContextListener());
		context.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				context.setConfigured(true);
			}
		});
		host.addChild(context);

		Servlet servlet = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK\n");
				resp.getWriter().close();
			}
		};

		Wrapper wrapper = new StandardWrapper();
		wrapper.setServlet(servlet);
		wrapper.setName("s1");
		context.addChild(wrapper);
		context.addServletMappingDecoded("/", wrapper.getName(), false);

		server.start();

		LOG.info("Local port after start: {}", connector.getLocalPort());

		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", connector.getLocalPort()));

		s.getOutputStream().write((
				"GET / HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read = -1;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		server.stop();
		server.destroy();
	}

	@Test
	public void useTomcat() throws Exception {
		// org.apache.catalina.startup.Tomcat class is designed for embedded usage, it doesn't require parsing
		// server.xml file and instead exposes lots of addXXX() methods.
		// it's used similarly to Catalina:
		// - org.apache.catalina.startup.Tomcat.init() which calls org.apache.catalina.startup.Catalina.load()
		// - org.apache.catalina.startup.Tomcat.start() which calls org.apache.catalina.core.StandardServer.startInternal()

		Tomcat tomcat = new Tomcat();
		tomcat.setPort(0);

		tomcat.setBaseDir("target");

		// this method adds missing nested objects: Catalina -> Server -> Service -> Engine -> Host -> Context
		Context ctx = tomcat.addContext("", ".");

		Servlet servlet = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK\n");
				resp.getWriter().close();
			}
		};

		// adding a servlet can be done through Tomcat instance
		//tomcat.addServlet(...)
		Wrapper wrapper = new Tomcat.ExistingStandardWrapper(servlet);
		wrapper.setName("s1");
		ctx.addChild(wrapper);

		// adding a mapping
		ctx.addServletMappingDecoded("/", "s1");

		// by default, org.apache.catalina.core.StandardHost.appBase is "webapps"
		tomcat.getHost().setAppBase(".");

		tomcat.start();

		LOG.info("Local port after start: {}", tomcat.getConnector().getLocalPort());

		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", tomcat.getConnector().getLocalPort()));

		s.getOutputStream().write((
				"GET / HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + tomcat.getConnector().getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read = -1;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		tomcat.stop();
		tomcat.destroy();
	}

}
