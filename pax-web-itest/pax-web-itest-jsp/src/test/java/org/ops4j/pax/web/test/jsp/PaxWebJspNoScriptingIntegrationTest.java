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
package org.ops4j.pax.web.test.jsp;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.descriptor.JspPropertyGroupDescriptor;
import jakarta.servlet.descriptor.TaglibDescriptor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PaxWebJspNoScriptingIntegrationTest {

	public static Logger log = LoggerFactory.getLogger(PaxWebJspNoScriptingIntegrationTest.class);

	private Servlet jspServlet;
	private ServletContext context;
	private Bundle paxWebJsp;

	@BeforeClass
	public static void initStatic() throws Exception {
		Class<?> jspFactoryClass = Class.forName("jakarta.servlet.jsp.JspFactory");
		Method setDefaultFactory = jspFactoryClass.getDeclaredMethod("setDefaultFactory", jspFactoryClass);
		Class<?> jspFactoryImplClass = Class.forName("org.apache.jasper.runtime.JspFactoryImpl");
		setDefaultFactory.invoke(null, jspFactoryClass.cast(jspFactoryImplClass.getConstructor().newInstance()));
	}

	@Before
	public void init() throws Exception {
		Class<?> servletClass = Class.forName("org.apache.jasper.servlet.JspServlet");
		Constructor<?> c = servletClass.getConstructor();
		jspServlet = (Servlet) c.newInstance();
		assertNotNull(jspServlet);

		File scratchDir = new File("target", UUID.randomUUID().toString());
		scratchDir.mkdirs();
		MockServletContext context = new MockServletContext("src/test/resources/web", new FileSystemResourceLoader()) {
			@Override
			public JspConfigDescriptor getJspConfigDescriptor() {
				return new JspConfigDescriptor() {
					@Override
					public Collection<TaglibDescriptor> getTaglibs() {
						return Collections.emptyList();
					}

					@Override
					public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups() {
						return Collections.singletonList(new JspPropertyGroupDescriptor() {
							@Override
							public Collection<String> getUrlPatterns() {
								return Collections.singletonList("*.jsp");
							}

							@Override
							public String getElIgnored() {
								return "false";
							}

							@Override
							public String getPageEncoding() {
								return "UTF-8";
							}

							@Override
							public String getScriptingInvalid() {
								return "true";
							}

							@Override
							public String getIsXml() {
								return "false";
							}

							@Override
							public Collection<String> getIncludePreludes() {
								return Collections.emptyList();
							}

							@Override
							public Collection<String> getIncludeCodas() {
								return Collections.emptyList();
							}

							@Override
							public String getDeferredSyntaxAllowedAsLiteral() {
								return "false";
							}

							@Override
							public String getTrimDirectiveWhitespaces() {
								return "false";
							}

							@Override
							public String getDefaultContentType() {
								return "text/html";
							}

							@Override
							public String getBuffer() {
								return "4kb";
							}

							@Override
							public String getErrorOnUndeclaredNamespace() {
								return "false";
							}

							@Override
							public String getErrorOnELNotFound() {
								return "false";
							}
						});
					}
				};
			}
		};

		// BundleContext is needed as "osgi-bundlecontext" context attribute, but it won't be used to scan
		// for TLDs
		Bundle bundle = mock(Bundle.class);
		BundleContext bc = mock(BundleContext.class);
		BundleWiring bw = mock(BundleWiring.class);
		when(bc.getBundle()).thenReturn(bundle);
		when(bundle.getBundleContext()).thenReturn(bc);
		when(bundle.adapt(BundleWiring.class)).thenReturn(bw);
		context.setAttribute(PaxWebConstants.CONTEXT_PARAM_BUNDLE_CONTEXT, bc);

		// can get an error in IDE like "java: cannot access org.apache.jasper.servlet.JasperInitializer"
		// but this is OK in Maven build. Base class of org.ops4j.pax.web.jsp.JasperInitializer is re-exported
		// from pax-web-jsp
//		ServletContainerInitializer sci = new JasperInitializer();
		// this method won't cause IDE problems
		ServletContainerInitializer sci = (ServletContainerInitializer) Class.forName("org.ops4j.pax.web.jsp.JasperInitializer").getConstructor().newInstance();
		sci.onStartup(null, context);

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
	public void jspWithNotAllowedScripts() throws Exception {
		MockHttpServletRequest req = req(new MockHttpServletRequest(context), "/", "rt.jsp");
		req.setMethod("GET");
		MockHttpServletResponse res = new MockHttpServletResponse();

		try {
			jspServlet.service(req, res);
			fail("Should throw JasperException");
		} catch (Exception e) {
			assertEquals("org.apache.jasper.JasperException", e.getClass().getName());
			assertTrue(e.getMessage().contains("Scripting elements ( &lt;%!, &lt;jsp:declaration, &lt;%=, &lt;jsp:expression, &lt;%, &lt;jsp:scriptlet ) are disallowed here"));
		}
	}

	private MockHttpServletRequest req(MockHttpServletRequest req, String s, String s1) {
		req.setServletPath(s);
		req.setPathInfo(s1);
		req.setRequestURI(s + s1);
		return req;
	}

}
