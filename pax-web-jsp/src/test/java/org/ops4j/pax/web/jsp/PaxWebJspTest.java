/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.jsp;

import java.io.File;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspFactory;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PaxWebJspTest {

	public static Logger log = LoggerFactory.getLogger(PaxWebJspTest.class);

	private static File scratchDir;

	private Servlet jspServlet;
	private ServletContext context;
	private Bundle paxWebJsp;

	@BeforeClass
	public static void initStaticDirectly() throws Exception {
		JspFactory.setDefaultFactory(new org.apache.jasper.runtime.JspFactoryImpl());

		scratchDir = new File("target", "jsp");
		FileUtils.deleteDirectory(scratchDir);
		scratchDir.mkdirs();
	}

	@Before
	public void init() throws Exception {
		jspServlet = new org.apache.jasper.servlet.JspServlet();

		MockServletContext context = new MockServletContext("src/test/resources/web", new FileSystemResourceLoader()) {
			@Override
			public JspConfigDescriptor getJspConfigDescriptor() {
				return null;
			}
		};

		Bundle bundle = mock(Bundle.class);
		BundleContext bc = mock(BundleContext.class);
		when(bc.getBundle()).thenReturn(bundle);
		context.setAttribute(PaxWebConstants.CONTEXT_PARAM_BUNDLE_CONTEXT, bc);

		new JasperInitializer().onStartup(null, context);

		MockServletConfig config = new MockServletConfig(context, "jsp");
		config.addInitParameter("compilerClassName", "org.apache.jasper.compiler.JDTCompiler"); // default
		config.addInitParameter("development", "false");
		config.addInitParameter("javaEncoding", "UTF-8");
		config.addInitParameter("keepgenerated", "true");
		config.addInitParameter("scratchdir", scratchDir.getCanonicalPath());
		config.addInitParameter("trimSpaces", "true");
		config.addInitParameter("xpoweredBy", "true");
		jspServlet.init(config);
	}

	@Test
	public void simpleJspWithEl() throws Exception {
		MockHttpServletRequest req = req(new MockHttpServletRequest(context), "/", "simple.jsp");
		req.setMethod("GET");
		req.setAttribute("user", new User("Grzegorz"));
		MockHttpServletResponse res = new MockHttpServletResponse();

		jspServlet.service(req, res);

		assertThat(res.getStatus(), equalTo(HttpServletResponse.SC_OK));
		String response = res.getContentAsString();
		log.info("Response: {}", response);

		assertTrue(response.contains("<p id=\"p1\">Welcome Grzegorz"));
		assertTrue(response.contains("<p id=\"p2\">[hello Grzegorz]"));
	}

	@Test
	public void jspWithElFunctions() throws Exception {
		MockHttpServletRequest req = req(new MockHttpServletRequest(context), "/", "functions.jsp");
		req.setMethod("GET");
		req.setAttribute("user", new User("Grzegorz"));
		MockHttpServletResponse res = new MockHttpServletResponse();

		jspServlet.service(req, res);

		assertThat(res.getStatus(), equalTo(HttpServletResponse.SC_OK));
		String response = res.getContentAsString();
		log.info("Response: {}", response);

		assertTrue(response.contains("<p id=\"p1\">size: 8</p>"));
	}

	@Test
	public void jspWithScripts() throws Exception {
		MockHttpServletRequest req = req(new MockHttpServletRequest(context), "/", "rt.jsp");
		req.setMethod("GET");
		MockHttpServletResponse res = new MockHttpServletResponse();

		jspServlet.service(req, res);

		assertThat(res.getStatus(), equalTo(HttpServletResponse.SC_OK));
		String response = res.getContentAsString();
		log.info("Response: {}", response);

		assertTrue(response.contains("<p id=\"p1\">It's 02:03 o'clock"));
	}

	@Test
	public void jspWithPageInclude() throws Exception {
		MockHttpServletRequest req = req(new MockHttpServletRequest(context), "/", "including.jsp");
		req.setMethod("GET");
		req.setAttribute("user", new User("Grzegorz"));
		MockHttpServletResponse res = new MockHttpServletResponse();

		jspServlet.service(req, res);

		assertThat(res.getStatus(), equalTo(HttpServletResponse.SC_OK));
		String response = res.getContentAsString();
		log.info("Response: {}", response);

		assertTrue(response.contains("Hello Grzegorz"));
	}

	@Test
	public void jspWithTagFiles() throws Exception {
		MockHttpServletRequest req = req(new MockHttpServletRequest(context), "/", "tagfiles.jsp");
		req.setMethod("GET");
		req.setAttribute("user", new User("Grzegorz"));
		MockHttpServletResponse res = new MockHttpServletResponse();

		jspServlet.service(req, res);

		assertThat(res.getStatus(), equalTo(HttpServletResponse.SC_OK));
		String response = res.getContentAsString();
		log.info("Response: {}", response);

		assertTrue(response.contains("<input type=\"text\" name=\"firstName\" value=\"Kun\" maxlength=\"30\" />"));
		assertTrue(response.contains("<input type=\"text\" name=\"lastName\" value=\"Grzegorz\"  />"));

		req.setAttribute("RO", Boolean.TRUE);

		jspServlet.service(req, res);

		assertThat(res.getStatus(), equalTo(HttpServletResponse.SC_OK));
		response = res.getContentAsString();
		log.info("Response: {}", response);

		assertTrue(response.contains("<span style=\"color: blue;\">Kun&#160;</span>"));
		assertTrue(response.contains("<span style=\"color: blue;\">Grzegorz&#160;</span>"));
	}

	private MockHttpServletRequest req(MockHttpServletRequest req, String s, String s1) {
		req.setServletPath(s);
		req.setPathInfo(s1);
		req.setRequestURI(s + s1);
		return req;
	}

	public static class User {

		private String name;

		public User(String name) {
			this.name = name;
		}

		public String hello(String arg) {
			return String.format("[hello %s]", arg);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
