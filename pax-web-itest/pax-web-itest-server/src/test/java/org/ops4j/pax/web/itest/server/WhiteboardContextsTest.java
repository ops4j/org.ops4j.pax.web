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
package org.ops4j.pax.web.itest.server;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletContextHelperMapping;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardContextsTest extends MultiContainerTestSupport {

	@Test
	public void justServletWithCustomContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		ServletContextHelper helper = new ServletContextHelper() { };
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "c1",
				() -> helper, 0L, 0, "/c"));

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(osgi.http.whiteboard.context.name=c1)");
		ServletModel model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c/s"), endsWith("S(1)"));

		getServletCustomizer().removedService(servletRef, model);
		assertThat(httpGET(port, "/c/s"), startsWith("HTTP/1.1 404"));
	}

	@Test
	public void oneServletWithTwoContexts() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		ServletContextHelper helper1 = new ServletContextHelper() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "1".equals(request.getParameter("token"));
			}
		};
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "c1",
				() -> helper1, 0L, 0, "/c"));

		ServletContextHelper helper2 = new ServletContextHelper() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "2".equals(request.getParameter("token"));
			}
		};
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "d1",
				() -> helper2, 0L, 0, "/d"));

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=d1))");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		assertThat(httpGET(port, "/"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c/s?token=1"), endsWith("S(1)"));
		assertThat(httpGET(port, "/d/s?token=2"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c/s?token=2"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/d/s?token=1"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/d/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/e/s"), startsWith("HTTP/1.1 404"));

		getServletCustomizer().removedService(servletRef, model);
		assertThat("No servlet, but handleSecurity() still called", httpGET(port, "/c/s"), startsWith("HTTP/1.1 403"));
		assertThat("No servlet, but handleSecurity() still called", httpGET(port, "/d/s"), startsWith("HTTP/1.1 403"));
		assertThat("No servlet, but handleSecurity() still called", httpGET(port, "/c/x"), startsWith("HTTP/1.1 403"));
		assertThat("No servlet, but handleSecurity() still called", httpGET(port, "/d/x"), startsWith("HTTP/1.1 403"));
		assertThat("Security granted, but no servlet", httpGET(port, "/c/s?token=1"), startsWith("HTTP/1.1 404"));
		assertThat("Security granted, but no servlet", httpGET(port, "/d/s?token=2"), startsWith("HTTP/1.1 404"));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void fourWaysToRegisterWhiteboardContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// 1. Official Whiteboard method
		ServletContextHelper context1 = new ServletContextHelper() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "1".equals(request.getParameter("token"));
			}
		};
		Hashtable<String, Object> properties = new Hashtable<>();
		// OSGi CMPN Whiteboard properties
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "context-param", "c1");
		getServletContextHelperCustomizer().addingService(mockReference(sample1,
				ServletContextHelper.class, properties, () -> context1, 0L, 0));

		// 2. PaxWeb specific Whiteboard method for ServletContextHelper with "mapping" class
		DefaultServletContextHelperMapping context2 = new DefaultServletContextHelperMapping();
		context2.setContextId("c2");
		context2.setContextPath("/c2");
		context2.getInitParameters().put("context-param", "c2");
		context2.setServletContextHelper(new ServletContextHelper() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "2".equals(request.getParameter("token"));
			}
		});
		// no service registration properties needed
		getServletContextHelperMappingCustomizer().addingService(mockReference(sample1,
				ServletContextHelperMapping.class, null, () -> context2, 0L, 0));

		// 3. PaxWeb specific way to register legacy HttpContext
		HttpContext context3 = new HttpContext() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "3".equals(request.getParameter("token"));
			}

			@Override
			public URL getResource(String name) {
				return null;
			}

			@Override
			public String getMimeType(String name) {
				return null;
			}
		};
		properties = new Hashtable<>();
		// legacy properties
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "c3");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/c3");
		properties.put(PaxWebConstants.DEFAULT_INIT_PREFIX_PROP + "context-param", "c3");
		getHttpContextCustomizer().addingService(mockReference(sample1,
				HttpContext.class, properties, () -> context3, 0L, 0));

		// 4. PaxWeb specific way to register legacy HttpContext with "mapping" class
		DefaultHttpContextMapping context4 = new DefaultHttpContextMapping();
		context4.setContextId("c4");
		context4.setContextPath("/c4");
		context4.getInitParameters().put("context-param", "c4");
		context4.setHttpContext(new HttpContext() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "4".equals(request.getParameter("token"));
			}

			@Override
			public URL getResource(String name) {
				return null;
			}

			@Override
			public String getMimeType(String name) {
				return null;
			}
		});
		// no service registration properties needed
		getHttpContextMappingCustomizer().addingService(mockReference(sample1,
				HttpContextMapping.class, null, () -> context4, 0L, 0));

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				null, 0L, 0, "/s");
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(!(|(osgi.http.whiteboard.context.name=default)(osgi.http.whiteboard.context.httpservice=shared)))");
		// PrototypeServiceFactory/ServiceObjects simulation - new instance on each call
		when(sample1.getBundleContext().getService(servletRef)).thenAnswer(invocation -> new TestServlet("1"));
		when(whiteboardBundleContext.getService(servletRef)).thenAnswer(invocation -> new TestServlet("1"));
		ServletModel model = getServletCustomizer().addingService(servletRef);

		assertThat(httpGET(port, "/"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c1/s?token=1"), endsWith(">c1<"));
		assertThat(httpGET(port, "/c2/s?token=2"), endsWith(">c2<"));
		assertThat(httpGET(port, "/c3/s?token=3"), endsWith(">c3<"));
		assertThat(httpGET(port, "/c4/s?token=4"), endsWith(">c4<"));
		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c2/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c3/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c4/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c1/t"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c2/t"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c3/t"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c4/t"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c/s"), startsWith("HTTP/1.1 404"));

		// each context should hold separate map of attributes
		httpGET(port, "/c1/s?token=1&set_a=c1");
		httpGET(port, "/c2/s?token=2&set_a=c2");
		httpGET(port, "/c3/s?token=3&set_a=c3");
		httpGET(port, "/c4/s?token=4&set_a=c4");
		assertThat(httpGET(port, "/c1/s?token=1&get_a=true"), endsWith("}c1{"));
		assertThat(httpGET(port, "/c2/s?token=2&get_a=true"), endsWith("}c2{"));
		assertThat(httpGET(port, "/c3/s?token=3&get_a=true"), endsWith("}c3{"));
		assertThat(httpGET(port, "/c4/s?token=4&get_a=true"), endsWith("}c4{"));

		getServletCustomizer().removedService(servletRef, model);
	}

	@Test
	public void servletAndFiltersInDifferentContexts() throws Exception {
		// in all cases, there are three ServletContextHelpers: c1, c2 and c3
		// servlet is registered to c1 and c2
		// filter 1 is registered to c1 and c3
		// filter 2 is registered to c2 and c3

		// all contexts ranked equal, so first one is the best, servlet is registered to c1, so no filter 2 involved
		servletAndFiltersInDifferentContexts(0, 0, 0, "/s", ">F(1)S(1)<F(1)");
		// c3 is the best, but from servlet perspective, it's c2, so c2 is used, so no filter 1 involved
		servletAndFiltersInDifferentContexts(1, 2, 3, "/s", ">F(2)S(1)<F(2)");
		// c2 is the best, so no filter 1 involved
		servletAndFiltersInDifferentContexts(1, 3, 2, "/s", ">F(2)S(1)<F(2)");

		// all contexts ranked equal, but no servlet mapped to /t, so c1 is the best, so only filter 1
		servletAndFiltersInDifferentContexts(0, 0, 0, "/t?terminate=1", ">F(1)<F(1)");
		// c3 is the best, no /t mapping, filter 1 and 2 used
		servletAndFiltersInDifferentContexts(1, 2, 3, "/t?terminate=2", ">F(1)>F(2)<F(2)<F(1)");
		// c2 is the best, no /t mapping, filter 2 only
		servletAndFiltersInDifferentContexts(1, 3, 2, "/t?terminate=2", ">F(2)<F(2)");
	}

	private void servletAndFiltersInDifferentContexts(int p1, int p2, int p3, String request, String expectedOutput)
			throws IOException {
		Bundle b = mockBundle("bundle-for-everything");

		ServletContextHelper helper1 = new ServletContextHelper() { };
		ServiceReference<ServletContextHelper> sr1 = mockServletContextHelperReference(b, "c1",
				() -> helper1, 1L, p1, "/");
		OsgiContextModel ocm1 = getServletContextHelperCustomizer().addingService(sr1);
		ServletContextHelper helper2 = new ServletContextHelper() { };
		ServiceReference<ServletContextHelper> sr2 = mockServletContextHelperReference(b, "c2",
				() -> helper2, 2L, p2, "/");
		OsgiContextModel ocm2 = getServletContextHelperCustomizer().addingService(sr2);
		ServletContextHelper helper3 = new ServletContextHelper() { };
		ServiceReference<ServletContextHelper> sr3 = mockServletContextHelperReference(b, "c3",
				() -> helper3, 3L, p3, "/");
		OsgiContextModel ocm3 = getServletContextHelperCustomizer().addingService(sr3);

		// servlet will be registered in c1 and c2
		ServiceReference<Servlet> servletRef = mockServletReference(b, "servlet1",
				() -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c2))");
		ServletModel sm1 = getServletCustomizer().addingService(servletRef);

		// filter1 will be registered in c1 and c3
		ServiceReference<Filter> filter1Ref = mockFilterReference(b, "filter1",
				() -> new Utils.MyIdFilter("1"), 0L, 0, "/*");
		when(filter1Ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c3))");
		FilterModel fm1 = getFilterCustomizer().addingService(filter1Ref);

		// filter2 will be registered in c2 and c3
		ServiceReference<Filter> filter2Ref = mockFilterReference(b, "filter2",
				() -> new Utils.MyIdFilter("2"), 0L, 0, "/*");
		when(filter2Ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c2)(osgi.http.whiteboard.context.name=c3))");
		FilterModel fm2 = getFilterCustomizer().addingService(filter2Ref);

		assertThat(httpGET(port, request), endsWith(expectedOutput));

		getServletCustomizer().removedService(servletRef, sm1);
		getFilterCustomizer().removedService(filter1Ref, fm1);
		getFilterCustomizer().removedService(filter2Ref, fm2);
		getServletContextHelperCustomizer().removedService(sr1, ocm1);
		getServletContextHelperCustomizer().removedService(sr2, ocm2);
		getServletContextHelperCustomizer().removedService(sr3, ocm3);
	}

	private static class TestServlet extends Utils.MyIdServlet {
		TestServlet(String id) {
			super(id);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			String value = req.getParameter("set_a");
			if (value != null) {
				req.getServletContext().setAttribute("a", value);
				return;
			}
			value = req.getParameter("get_a");
			if (value != null) {
				resp.getWriter().print("}" + req.getServletContext().getAttribute("a") + "{");
				return;
			}
			resp.getWriter().print(">" + req.getServletContext().getInitParameter("context-param") + "<");
		}
	}

}
