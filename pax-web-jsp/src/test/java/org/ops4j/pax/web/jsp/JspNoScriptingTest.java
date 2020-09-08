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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;
import javax.servlet.jsp.JspFactory;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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

public class JspNoScriptingTest {

	public static Logger log = LoggerFactory.getLogger(JspNoScriptingTest.class);

	private static File scratchDir;

	private Servlet jspServlet;
	private ServletContext context;
	private Bundle paxWebJsp;

//	@BeforeClass
	@SuppressWarnings("JavaReflectionInvocation")
	public static void initStaticUsingReflection() throws Exception {
		Class<?> jspFactoryClass = Class.forName("javax.servlet.jsp.JspFactory");
		Method setDefaultFactory = jspFactoryClass.getDeclaredMethod("setDefaultFactory", jspFactoryClass);
		Class<?> jspFactoryImplClass = Class.forName("org.apache.jasper.runtime.JspFactoryImpl");
		setDefaultFactory.invoke(null, jspFactoryClass.cast(jspFactoryImplClass.newInstance()));
	}

	@BeforeClass
	public static void initStaticDirectly() throws Exception {
		JspFactory.setDefaultFactory(new org.apache.jasper.runtime.JspFactoryImpl());

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
						});
					}
				};
			}
		};

		// mocking to return META-INF/*.tld files from pax-web-jsp jar dependency (no OSGi here)

		paxWebJsp = mock(Bundle.class);
		BundleWiring paxWebJspWiring = mock(BundleWiring.class);
		when(paxWebJsp.adapt(BundleWiring.class)).thenReturn(paxWebJspWiring);

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
		final Vector<URL> urls = Arrays.stream(resolver.getResources("classpath*:/META-INF/*.tld")).map(r -> {
			try {
				return r.getURL();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}).collect(Vector::new, Vector::add, Vector::addAll);
		when(paxWebJsp.findEntries("META-INF", "*.tld", true)).thenReturn(urls.elements());
		when(paxWebJsp.getResources(ArgumentMatchers.anyString())).thenAnswer((Answer<Enumeration<URL>>) invocation -> {
			String name = (String) invocation.getArguments()[0];
			Vector<URL> v = new Vector<>(1);
			v.add(new URL(name));
			return v.elements();
		});
		when(paxWebJspWiring.listResources("META-INF", "*.tld", BundleWiring.LISTRESOURCES_RECURSE))
				.thenAnswer((Answer<Collection<String>>) invocation ->
						urls.stream()
								.map(URL::toString)
								.collect(LinkedList::new, LinkedList::add, LinkedList::addAll));

		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
//        Thread.currentThread().setContextClassLoader(new ResourceDelegatingBundleClassLoader(Collections.singletonList(paxWebJsp)));
//        try {
//            // setup org.apache.jasper.compiler.TldCache and org.apache.tomcat.InstanceManager
            new org.apache.jasper.servlet.JasperInitializer().onStartup(null, context);
//        } finally {
//            Thread.currentThread().setContextClassLoader(tccl);
//        }

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
