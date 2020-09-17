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
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;
import javax.servlet.http.HttpServletResponse;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This test class should only be run by maven-failsafe-plugin after properly packaging pax-web-jsp bundle.
 */
public class PaxWebCustomTldIntegrationTest {

	public static Logger log = LoggerFactory.getLogger(PaxWebCustomTldIntegrationTest.class);

	private static File scratchDir;

	private Servlet jspServlet;
	private ServletContext context;
	private Bundle paxWebJsp;

	@BeforeClass
	@SuppressWarnings("JavaReflectionInvocation")
	public static void initStatic() throws Exception {
		Class<?> jspFactoryClass = Class.forName("javax.servlet.jsp.JspFactory");
		Method setDefaultFactory = jspFactoryClass.getDeclaredMethod("setDefaultFactory", jspFactoryClass);
		Class<?> jspFactoryImplClass = Class.forName("org.apache.jasper.runtime.JspFactoryImpl");
		setDefaultFactory.invoke(null, jspFactoryClass.cast(jspFactoryImplClass.newInstance()));

		scratchDir = new File("target", "jsp");
		FileUtils.deleteDirectory(scratchDir);
		scratchDir.mkdirs();
	}

	@Before
	public void init() throws Exception {
		Class<?> servletClass = Class.forName("org.apache.jasper.servlet.JspServlet");
		Constructor<?> c = servletClass.getConstructor();
		jspServlet = (Servlet) c.newInstance();
		assertNotNull(jspServlet);

		MockServletContext context = new MockServletContext("src/test/resources/web", new FileSystemResourceLoader()) {
			@Override
			public JspConfigDescriptor getJspConfigDescriptor() {
				return new JspConfigDescriptor() {
					@Override
					public Collection<TaglibDescriptor> getTaglibs() {
						return Collections.singletonList(new TaglibDescriptor() {
							@Override
							public String getTaglibURI() {
								return "urn:x";
							}

							@Override
							public String getTaglibLocation() {
								// according to JSP.7.3.2, it should be context-relative path. If not starting
								// with slash, it should be prepended with "/WEB-INF/"
								return "tlds/test.tld";
							}
						});
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
						});
					}
				};
			}
		};

		// BundleContext is needed as "osgi-bundlecontext" context attribute, but it won't be used to scan
		// for TLDs
		Bundle bundle = mock(Bundle.class);
		BundleContext bc = mock(BundleContext.class);
		when(bc.getBundle()).thenReturn(bundle);
		context.setAttribute(PaxWebConstants.CONTEXT_PARAM_BUNDLE_CONTEXT, bc);

		ServletContainerInitializer sci = (ServletContainerInitializer) Class.forName("org.ops4j.pax.web.jsp.JasperInitializer").newInstance();
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
	public void jspWithCustomTag() throws Exception {
		MockHttpServletRequest req = req(new MockHttpServletRequest(context), "/", "customTags.jsp");
		req.setMethod("GET");
		MockHttpServletResponse res = new MockHttpServletResponse();

		jspServlet.service(req, res);

		assertThat(res.getStatus(), equalTo(HttpServletResponse.SC_OK));
		String response = res.getContentAsString();
		log.info("Response: {}", response);

		assertTrue(response.contains("<span>Hello!</span>"));
	}

	private MockHttpServletRequest req(MockHttpServletRequest req, String servletPath, String pathInfo) {
		req.setServletPath(servletPath);
		req.setPathInfo(pathInfo);
		req.setRequestURI(servletPath + pathInfo);
		return req;
	}

}
