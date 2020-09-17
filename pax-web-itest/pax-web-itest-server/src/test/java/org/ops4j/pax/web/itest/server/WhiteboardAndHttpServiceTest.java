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
import java.util.LinkedList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class WhiteboardAndHttpServiceTest extends MultiContainerTestSupport {

	@Test
	public void onlyDefaultContexts() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// creating bundle-scoped HttpService/WebContainer should create one bundle-scoped OsgiContextModel
		// inside global ServerModel
		new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		final List<OsgiContextModel> models = new LinkedList<>();

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> new TestServlet("1", models), 0L, 0, "/s");
		// register to default HttpService context and to default Whiteboard context
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.httpservice=default)"
						+ "(osgi.http.whiteboard.context.name=default))");
		ServletModel model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c/s"), startsWith("HTTP/1.1 404"));

		getServletCustomizer().removedService(servletRef, model);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		// out of two "contexts", the above servlet should be registered with the Whiteboard one. Not the HttpService
		// one because of service ranking even if there are 4 contexts in total at this point:
		//  - OsgiContextModel{id=OCM-2,name='default',path='/',bundle=org.ops4j.pax.web.pax-web-extender-whiteboard,context=(supplier)}"
		//  - OsgiContextModel{id=OCM-3,name='shared',path='/',shared=true,context=DefaultMultiBundleHttpContext{contextId='shared'}}"
		//  - OsgiContextModel{id=OCM-5,name='default',path='/',bundle=org.ops4j.pax.web.pax-web-extender-whiteboard,context=DefaultHttpContext{bundle=Bundle "org.ops4j.pax.web.pax-web-extender-whiteboard",contextId='default'}}"
		//  - OsgiContextModel{id=OCM-6,name='default',path='/',bundle=sample1,context=DefaultHttpContext{bundle=Bundle "sample1",contextId='default'}}"
		// from the above 4, OCM-2 (the "default" whiteboard context) and OCM-6 (the "default" httpService context) are
		// considered. OCM-3 is "shared" and OCM-5 is associated with wrong bundle to be considered.
		Hashtable<String, Object> props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));

		models.clear();

		// explicit registration to HttpService context
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(osgi.http.whiteboard.context.httpservice=default)");
		model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c/s"), startsWith("HTTP/1.1 404"));

		getServletCustomizer().removedService(servletRef, model);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		// now, we've explicitly said to register Whiteboard servlet to HttpService bundle-scoped "context"
		props = models.get(0).getContextRegistrationProperties();
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));
	}

	@Test
	public void overridenDefaultContextsWithWhiteboardServlet() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// creating bundle-scoped HttpService/WebContainer should create one bundle-scoped OsgiContextModel
		// inside global ServerModel
		new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		// override "default" HttpService context
		HttpContext context1 = new HttpContext() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "1".equals(request.getParameter("token"));
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
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/");

		// register HttpService context with very high ranking - to use it for normal Whiteboard servlets
		ServiceReference<HttpContext> reference1 = mockReference(sample1,
				HttpContext.class, properties, () -> context1, 0L, 1042);
		OsgiContextModel model1 = getHttpContextCustomizer().addingService(reference1);

		// override "default" Whiteboard context
		ServletContextHelper context2 = new ServletContextHelper() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "2".equals(request.getParameter("token"));
			}
		};
		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "default");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
		// lower ranking than the HttpService context
		ServiceReference<ServletContextHelper> reference2 = mockReference(sample1,
				ServletContextHelper.class, properties, () -> context2, 0L, 42);
		OsgiContextModel model2 = getServletContextHelperCustomizer().addingService(reference2);

		final List<OsgiContextModel> models = new LinkedList<>();

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> new TestServlet("1", models), 0L, 0, "/s");
		// register to default HttpService context and to default Whiteboard context, but because all use
		// "default" name, ranking will be used to pick up one and HttpContext-based context will be used
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.httpservice=default)"
						+ "(osgi.http.whiteboard.context.name=default))");
		ServletModel model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/s?token=1"), endsWith("S(1)"));
		assertThat(httpGET(port, "/s?token=2"), startsWith("HTTP/1.1 403"));

		getServletCustomizer().removedService(servletRef, model);

		// servlet should be associated with HttpService context
		Hashtable<String, Object> props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));

		models.clear();

		// register to whiteboard only (default/empty selector)
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT)).thenReturn(null);
		model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/s?token=1"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/s?token=2"), endsWith("S(1)"));

		getServletCustomizer().removedService(servletRef, model);

		props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));

		getHttpContextCustomizer().removedService(reference1, model1);
		getServletContextHelperCustomizer().removedService(reference2, model2);
	}

	@Test
	public void overridenDefaultContextsWithHttpServiceServlet() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// creating bundle-scoped HttpService/WebContainer should create one bundle-scoped OsgiContextModel
		// inside global ServerModel
		HttpServiceEnabled sample1Container = new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		// override "default" HttpService context, so we can use Whiteboard method to override a context used
		// with HttpService - even without specifying anything in httpService.registerServlet()
		HttpContext context1 = new HttpContext() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "1".equals(request.getParameter("token"));
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
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/");

		// register HttpService context with lower ranking than Whiteboard context, but this one will be used anyway
		ServiceReference<HttpContext> reference1 = mockReference(sample1,
				HttpContext.class, properties, () -> context1, 0L, 42);
		OsgiContextModel model1 = getHttpContextCustomizer().addingService(reference1);

		// override "default" Whiteboard context
		ServletContextHelper context2 = new ServletContextHelper() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "2".equals(request.getParameter("token"));
			}
		};
		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "default");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
		// higher ranking than the HttpService context, but ServletContextHelper will never override
		// the HttpService-related context
		ServiceReference<ServletContextHelper> reference2 = mockReference(sample1,
				ServletContextHelper.class, properties, () -> context2, 0L, 1042);
		OsgiContextModel model2 = getServletContextHelperCustomizer().addingService(reference2);

		final List<OsgiContextModel> models = new LinkedList<>();

		TestServlet servlet = new TestServlet("1", models);
		// use "null" as HttpContext, so "default" will be used, but this has just "default" name and
		// sample1Container's bundle as identity, so the overriden HttpService context will be used
		// The spec is not quite clear about the definition of "same"...
		sample1Container.registerServlet(servlet, new String[] { "/s/*" }, null, null);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/s?token=1"), endsWith("S(1)"));
		assertThat(httpGET(port, "/s?token=2"), startsWith("HTTP/1.1 403"));

		sample1Container.unregisterServlet(servlet);

		// servlet should be associated with HttpService context
		Hashtable<String, Object> props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));

		getHttpContextCustomizer().removedService(reference1, model1);
		getServletContextHelperCustomizer().removedService(reference2, model2);
	}

	@Test
	public void multipleWhiteboardContextsAndHttpServiceServlet() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		HttpServiceEnabled wc = new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		HttpContext context1 = new HttpContext() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "1".equals(request.getParameter("token"));
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
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/");
		ServiceReference<HttpContext> reference1 = mockReference(sample1,
				HttpContext.class, properties, () -> context1, 0L, 42);
		OsgiContextModel model1 = getHttpContextCustomizer().addingService(reference1);

		HttpContext context2 = new HttpContext() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "2".equals(request.getParameter("token"));
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
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/");
		ServiceReference<HttpContext> reference2 = mockReference(sample1,
				HttpContext.class, properties, () -> context2, 0L, 21);
		OsgiContextModel model2 = getHttpContextCustomizer().addingService(reference2);

		// after registering 2nd Whiteboard HttpContext (legacy), the list of contexts that can be used directly
		// from HttpServiceEnabled for this bundle is:
		// bundleContexts: java.util.Map  = {java.util.HashMap@4786}  size = 2
		// {org.ops4j.pax.web.service.spi.model.ContextKey@4805} "Key{default, Bundle "sample1"}" -> {java.util.TreeSet@4806}  size = 3
		//  key: org.ops4j.pax.web.service.spi.model.ContextKey  = {org.ops4j.pax.web.service.spi.model.ContextKey@4805} "Key{default, Bundle "sample1"}"
		//  value: java.util.TreeSet  = {java.util.TreeSet@4806}  size = 3
		//   0 = {org.ops4j.pax.web.service.spi.model.OsgiContextModel@3723} "OsgiContextModel{id=OCM-7,name='default',path='/',bundle=sample1,context=WebContainerContextWrapper{bundle=Bundle "sample1",contextId='default',delegate=org.ops4j.pax.web.itest.server.WhiteboardAndHttpServiceTest$5@2364305a}}"
		//   1 = {org.ops4j.pax.web.service.spi.model.OsgiContextModel@4676} "OsgiContextModel{id=OCM-8,name='default',path='/',bundle=sample1,context=WebContainerContextWrapper{bundle=Bundle "sample1",contextId='default',delegate=org.ops4j.pax.web.itest.server.WhiteboardAndHttpServiceTest$6@63192798}}"
		//   2 = {org.ops4j.pax.web.service.spi.model.OsgiContextModel@4811} "OsgiContextModel{id=OCM-6,name='default',path='/',bundle=sample1,context=DefaultHttpContext{bundle=Bundle "sample1",contextId='default'}}"
		//
		// the TreeSet is correctly sorted by priority, where OCM-6 is the really default context for this bundle
		// and "default" name and its rank is -2147483648 (0x80000000)

		final List<OsgiContextModel> models = new LinkedList<>();

		TestServlet servlet = new TestServlet("1", models);

		wc.registerServlet(servlet, new String[] { "/s/*" }, null, null);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 403"));
		// context with rank 42 should be used
		assertThat(httpGET(port, "/s?token=1"), endsWith("S(1)"));
		assertThat(httpGET(port, "/s?token=2"), startsWith("HTTP/1.1 403"));

		wc.unregisterServlet(servlet);

		// servlet should be associated with HttpService context
		Hashtable<String, Object> props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));
		assertThat(props.get(Constants.SERVICE_RANKING), equalTo(42));

		getHttpContextCustomizer().removedService(reference1, model1);
		getHttpContextCustomizer().removedService(reference2, model2);

		wc.stop();
	}

	@Test
	public void httpServiceServletAndWhiteboardFilter() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		HttpServiceEnabled sample1Container = new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		// override "default" HttpService context, to specify its context path (and security of course)
		HttpContext context1 = new HttpContext() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "1".equals(request.getParameter("token"));
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
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/c");
		ServiceReference<HttpContext> reference1 = mockReference(sample1,
				HttpContext.class, properties, () -> context1, 0L, 1);
		OsgiContextModel model1 = getHttpContextCustomizer().addingService(reference1);

		// filter1 will be registered without any context - so should go to Whiteboard's default context
		// It won't be associated with servlet, becuase it'd have to target it using
		// osgi.http.whiteboard.context.httpservice=default selector
		ServiceReference<Filter> filter1Ref = mockFilterReference(sample1, "filter1",
				() -> new Utils.MyIdFilter("1"), 0L, 0, "/*");
		when(filter1Ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn(null);
		FilterModel fm1 = getFilterCustomizer().addingService(filter1Ref);

		// filter2 will be explicitly registered to HttpService context
		ServiceReference<Filter> filter2Ref = mockFilterReference(sample1, "filter2",
				() -> new Utils.MyIdFilter("2"), 0L, 0, "/*");
		when(filter2Ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(osgi.http.whiteboard.context.httpservice=default)");
		FilterModel fm2 = getFilterCustomizer().addingService(filter2Ref);

		final List<OsgiContextModel> models = new LinkedList<>();

		TestServlet servlet = new TestServlet("1", models);
		sample1Container.registerServlet(servlet, new String[] { "/s/*" }, null, null);
		// filter1 is installed to "default" Whiteboard context, which still is valid under "/"
		assertThat(httpGET(port, "/s?terminate=1"), endsWith(">F(1)<F(1)"));
		// however HttpService "default" context is reconfigured (Pax Web FTW!) to "/c"
		assertThat(httpGET(port, "/c/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c/s?token=1"), endsWith(">F(2)S(1)<F(2)"));

		sample1Container.unregisterServlet(servlet);
		getFilterCustomizer().removedService(filter1Ref, fm1);
		getFilterCustomizer().removedService(filter2Ref, fm2);

		// servlet should be associated with HttpService context
		Hashtable<String, Object> props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/c"));

		getHttpContextCustomizer().removedService(reference1, model1);
	}

	@Test
	public void httpServiceServletAndFilterWithCustomHttpContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		HttpServiceEnabled sample1Container = new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		HttpContext context1 = new HttpContext() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "1".equals(request.getParameter("token"));
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
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "custom");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/");
		ServiceReference<HttpContext> reference1 = mockReference(sample1,
				HttpContext.class, properties, () -> context1, 0L, 0);
		OsgiContextModel model1 = getHttpContextCustomizer().addingService(reference1);

		// we've Whiteboard-registered legacy HttpContext, but we can reference it directly with HttpService
		// registration. We just have to use the same instance which was registered as Whiteboard service.
		// Actualy name+bundle should match
		Servlet servlet1 = new Utils.MyIdServlet("1");
		sample1Container.registerServlet(servlet1, "s1", new String[] { "/s/*" }, null, context1);
		Servlet servlet2 = new Utils.MyIdServlet("2");
		sample1Container.registerServlet(servlet2, "s2", new String[] { "/t/*" }, null, null);

		assertThat(httpGET(port, "/s/1"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/s/1?token=1"), endsWith("S(1)"));
		assertThat(httpGET(port, "/t/1"), endsWith("S(2)"));
		// when targetting "404 servlet", the chosen context will be the highest ranked one at particular
		// ServerController level. And because our "custom" context has rank 0 for "/" context, it is higher
		// than "/" Whiteboard context (Integer.MIN_VALUE/2) and "/" HttpService context (Integer.MIN_VALUE)
		assertThat(httpGET(port, "/u/1"), startsWith("HTTP/1.1 403"));

		sample1Container.unregisterServlet(servlet1);
		sample1Container.unregisterServlet(servlet2);

		getHttpContextCustomizer().removedService(reference1, model1);
	}

	private static class TestServlet extends Utils.MyIdServlet {

		private final List<OsgiContextModel> models;

		TestServlet(String id, List<OsgiContextModel> models) {
			super(id);
			this.models = models;
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			// result = {org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext@4038}
			// osgiContext: org.ops4j.pax.web.service.spi.servlet.OsgiServletContext  = {org.ops4j.pax.web.service.spi.servlet.OsgiServletContext@4041}
			//  containerServletContext: javax.servlet.ServletContext  = {org.eclipse.jetty.servlet.ServletContextHandler$Context@4046} "ServletContext@o.e.j.s.ServletContextHandler@2dd29a59{/,null,AVAILABLE}"
			//  osgiContextModel: org.ops4j.pax.web.service.spi.model.OsgiContextModel  = {org.ops4j.pax.web.service.spi.model.OsgiContextModel@4047} "OsgiContextModel{id=OsgiContextModel-3,name='default',contextPath='/',context=null,bundle=Bundle "org.ops4j.pax.web.pax-web-extender-whiteboard"}"
			//   LOG: org.slf4j.Logger  = {org.apache.logging.slf4j.Log4jLogger@4065}
			//   DEFAULT_CONTEXT_MODEL: org.ops4j.pax.web.service.spi.model.OsgiContextModel  = {org.ops4j.pax.web.service.spi.model.OsgiContextModel@4047} "OsgiContextModel{id=OsgiContextModel-3,name='default',contextPath='/',context=null,bundle=Bundle "org.ops4j.pax.web.pax-web-extender-whiteboard"}"
			//   contextPath: java.lang.String  = "/"
			//   name: java.lang.String  = "default"
			//   httpContext: org.ops4j.pax.web.service.WebContainerContext  = null
			//   contextReference: org.osgi.framework.ServiceReference  = null
			//   contextSupplier: java.util.function.BiFunction  = {org.ops4j.pax.web.service.spi.model.OsgiContextModel$$Lambda$27.1131592118@4067}
			//   contextRegistrationProperties: java.util.Hashtable  = {java.util.Hashtable@4068}  size = 4
			//    "service.id" -> {java.lang.Long@4086} 0
			//    "osgi.http.whiteboard.context.name" -> "default"
			//    "service.ranking" -> {java.lang.Integer@4089} -1073741824
			//    "osgi.http.whiteboard.context.path" -> "/"
			models.add(((OsgiScopedServletContext) req.getServletContext()).getOsgiContextModel());

			super.doGet(req, resp);
		}
	}

}
