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
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class WhiteboardAndHttpServiceTest extends MultiContainerTestSupport {

	@Test
	public void onlyDefaultContexts() throws Exception {
		// a bundle and bundle-scoped HttpService/WebContainer instance with one "default" OsgiContextModel
		// 2020-10-02: we no longer create OsgiContextModel for given bundle-scoped HttpService when no actual
		// web element is registered through such HttpService/WebContainer
		Bundle sample1 = mockBundle("sample1", true);
		WebContainer wc = container(sample1);

		// 2020-10-02: I found that this stopped working after I've disabled implicit creation of a "context"
		// (HttpContext -> OsgiContextModel) for any bundle-scoped HttpServiceEnabled instance created until there's
		// a need to actually register a web element!
		// I had to do it, because I was getting "highest ranked OsgiContextModel" for given context path (mostly "/")
		// related to pax-web-extender-whiteboard bundle which was usually the first obtaining the bundle-scoped
		// HttpService instance.
		// probably that's why "140.10 Integration with Http Service Contexts" says:
		//     A Http Whiteboard service which should be registered with a Http Context from the Http Service can
		//     achieve this by targeting a ServletContextHelper with the registration property
		//     osgi.http.whiteboard.context.httpservice. [...] Association with Http Context from the Http Service can
		//     only be done for servlet filters, error pages and listeners. Servlets and resources cannot be associated
		//     with Http Contexts managed by the Http Service.
		// could it be that CMPN designers anticipated such problem? The problem related to lack of OsgiContextModel
		// for HttpService when no actual web element was registered through it? Or maybe simply they imagined
		// servlets being registered through HttpService and then the request pipelines being affected by Whiteboard
		// filters/listeners/error pages?
		//
		// in Whiteboard, rank of "default" (built-in) context is 0. In HttpService the rank of the context
		// used/created as an argument to httpService.register(..., context) is "highest"
		// "140.4 Registering Servlets":
		//     The Servlet Context of the Http Service is treated in the same way as all contexts managed by the
		//     Whiteboard implementation. The highest ranking is associated with the context of the Http Service.
		//
		// The reason is probably:
		//  - it's impossible to specify context path with CMPN HttpService
		//  - OSGi CMPN HttpService spec assumed that if you httpService.register(..., context), you expect exactly
		//    this context to be used and not some Whiteboard context for "/" path
		//
		// To be honest, it was easier yesterday (2020-09-30) before I swapped the ranks of contexts from:
		//  - context from HttpService ranked at Integer.MIN_VALUE
		//  - context from Whiteboard ranked at Integer.MIN-VALUE / 2
		// to:
		//  - context from HttpService ranked at Integer.MAX_VALUE
		//  - context from Whiteboard ranked at 0
		// but well...
		//
		// On drawback before the change was that if I wanted proper/expected context to be passed to SCIs, I had
		// to register Whiteboard context with ranking > Integer.MIN_VALUE/2 to "take over" the context for HttpService
		// (bundle-scoped)
		// on the other hand it WAS possible to register such Whiteboard ServletContextModel that could replace
		// any (even the one passed as "null") HttpService context. Now it's NOT POSSIBLE - but let's assume
		// such case was explicitly anticipated by OSGi CMPN designers...
		// UPDATE: it is possible with special tracking of contexts that are EXACTLY the same contexts which are
		// already used for bundle-name combination in HttpService/WebContainer. So if we Whiteboard-register
		// a context that comes from org.ops4j.pax.web.service.WebContainer.createDefaultHttpContext()
		// (or org.osgi.service.http.HttpService.createDefaultHttpContext()), whiteboard tracker will detect that
		// it's the same/existing instance and will updated existing, associated OsgiContextModel, so we:
		// - can install different OsgiContextModel with proper httpContext that will lower the rank of existing
		//   OsgiContextModel related to the httpxContext, so we can later register DIFFERENT OsgiContextModel
		//   that could be used in HttpService scenario, or
		// - can install different OsgiContextModel with proper httpContext that won't change the rank (leaving place
		//   for other Whiteboard context), but will simply update the existing context directly

		// we can force creation of an OsgiContextModel matching "(osgi.http.whiteboard.context.httpservice=default)"
		// selector in three ways:
		//  - by registering some artificial servlet/filter/listner/error-page/welcome-file/...
		//    through bundle-scoped HttpService/WebContainer instance,
		//  - using Pax Web trick with Whiteboard and registration of HttpContext/HttpContextMapping which is a
		//    singleton (ServletContextHelper/ServletContextHelperMapping can never be configured as the equivalence
		//    of the context for HttpService) - see subclasses of
		//    org.ops4j.pax.web.extender.whiteboard.internal.tracker.AbstractContextTracker
		//  - calling createDefaultHttpContext()

		// 1. registering artificial servlet
//		wc.registerResources("/", "/", null);

		// 2. registration of org.osgi.service.http.HttpContext service
//		Hashtable<String, Object> properties = new Hashtable<>();
//		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
//		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/");
//		getHttpContextCustomizer().addingService(mockReference(sample1,
//				HttpContext.class, properties, () -> new DefaultHttpContext(sample1), 0L, 0));

		// 3. Just call org.osgi.service.http.HttpService.createDefaultHttpContext() - the "default" context for "/"
		//    path and bundle for which the WebContainer instance is scoped will be created and properly sent to
		//    ServerController and configured in ServerModel
		wc.createDefaultHttpContext();

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

		// out of two "contexts", the above servlet should be registered with the HttpService one. Not the Whiteboard
		// one because of service ranking even if there are 3 contexts in total at this point:
		//  - OsgiContextModel{HS,id=OCM-5,name='default',path='/',bundle=sample1,context=DefaultHttpContext{bundle=Bundle "sample1",contextId='default'}}
		//  - OsgiContextModel{HS,id=OCM-3,name='shared',path='/',shared=true,context=DefaultMultiBundleHttpContext{contextId='shared'}}
		//  - OsgiContextModel{WB,id=OCM-2,name='default',path='/',bundle=org.ops4j.pax.web.pax-web-extender-whiteboard,context=(supplier)}
		// from the above 3, OCM-2 (the "default" whiteboard context) and OCM-5 (the "default" httpService context) are
		// considered. OCM-3 is "shared" and is not taken into account
		Hashtable<String, Object> props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));

		models.clear();

		// explicit registration to Whiteboard context
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(osgi.http.whiteboard.context.name=default)");
		model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c/s"), startsWith("HTTP/1.1 404"));

		getServletCustomizer().removedService(servletRef, model);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		// now, we've explicitly said to register Whiteboard servlet to Whiteboard "default" context
		props = models.get(0).getContextRegistrationProperties();
		assertNull(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME), equalTo("default"));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));
	}

	@Test
	public void overridenDefaultContextsWithWhiteboardServlet() throws Exception {
		Bundle sample1 = mockBundle("sample1");

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
		assertNull(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME), equalTo("default"));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));

		getHttpContextCustomizer().removedService(reference1, model1);
		getServletContextHelperCustomizer().removedService(reference2, model2);
	}

	@Test
	public void overridenDefaultContextsWithHttpServiceServlet() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		WebContainer wc = container(sample1);

		// at this stage even if we obtained HttpService instance scoped to sample1 bundle, there's no
		// OsgiContextModel created with Integer.MAX_VALUE ranking which is always higher ranked than any
		// Whiteboard-registered context ...

		// ... that's why it's possible to override "default" HttpService context using Whiteboard registration, to
		// override a context used with HttpService - even without specifying anything in httpService.registerServlet()
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

		// register HttpService context with lower ranking than Whiteboard context we'll register soon, but this one
		// will be used anyway
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
		// the HttpService-related context - it won't be passed as bundle-scoped, singleton HttpContext
		// (WebContainerContext) to be used in HttpService scenario
		ServiceReference<ServletContextHelper> reference2 = mockReference(sample1,
				ServletContextHelper.class, properties, () -> context2, 0L, 1042);
		OsgiContextModel model2 = getServletContextHelperCustomizer().addingService(reference2);

		final List<OsgiContextModel> models = new LinkedList<>();

		TestServlet servlet = new TestServlet("1", models);
		// use "null" as HttpContext, so "default" will be used, but this has just "default" name and
		// sample1 bundle as identity, so the overriden HttpService context will be used
		// The spec is not quite clear about the definition of "same"...
		// 2020-10-02: Note that because we didn't register anything yet, there was no OsgiContextModel
		// created with Integer.MAX_VALUE ranking according to 140.4 "The highest ranking is associated with the
		// context of the Http Service."!
		wc.registerServlet(servlet, new String[] { "/s/*" }, null, null);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/s?token=1"), endsWith("S(1)"));
		assertThat(httpGET(port, "/s?token=2"), startsWith("HTTP/1.1 403"));

		wc.unregisterServlet(servlet);

		// servlet should be associated with HttpService context
		Hashtable<String, Object> props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));

		getHttpContextCustomizer().removedService(reference1, model1);
		getServletContextHelperCustomizer().removedService(reference2, model2);
	}

	@Test
	public void whiteboardContextIsLowerRankedThanHttpServiceContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		WebContainer wc = container(sample1);

		// at this stage even if we obtained HttpService instance scoped to sample1 bundle, there's no
		// OsgiContextModel created with Integer.MAX_VALUE ranking which is always higher ranked than any
		// Whiteboard-registered context

		HttpContext defaultContext = wc.createDefaultHttpContext();

		// now there's such OsgiContextModel and we can't override it AT ALL by registering any object as
		// HttpContext/HttpContextMapping.getHttpContext() OSGi service which is DIFFERENT that the one obtained
		// above
		// Also it is NEVER possible by registering ANY ServletContextHelper/ServletContextHelperMapping

		// TRY TO override "default" HttpService context with special context and handleSecurity()
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

		// register this HttpService context with highest possible rank - it won't matter anyway
		ServiceReference<HttpContext> reference1 = mockReference(sample1,
				HttpContext.class, properties, () -> context1, 0L, Integer.MAX_VALUE);
		OsgiContextModel model1 = getHttpContextCustomizer().addingService(reference1);

		final List<OsgiContextModel> models = new LinkedList<>();

		TestServlet servlet = new TestServlet("1", models);
		// use "null" as HttpContext, so "default" will be used, but we won't get our Whiteboard-registered
		// HttpContext, because createDefaultHttpContext() created default one
		wc.registerServlet(servlet, new String[] { "/s/*" }, null, null);
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/s?token=1"), endsWith("S(1)"));
		assertThat(httpGET(port, "/s?token=2"), endsWith("S(1)"));

		wc.unregisterServlet(servlet);

		getHttpContextCustomizer().removedService(reference1, model1);
	}

	@Test
	public void overrideHttpContextWithSameInstance() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		WebContainer wc = container(sample1);

		HttpContext defaultContext = wc.createDefaultHttpContext();

		final List<OsgiContextModel> models = new LinkedList<>();
		TestServlet servlet = new TestServlet("1", models);

		// registration to "default" context maintained by HttpService and scoped to "sample1" bundle
		wc.registerServlet(servlet, new String[] { "/s/*" }, null, null);
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/new-path/s"), startsWith("HTTP/1.1 404"));

		Hashtable<String, Object> props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));

		// register HttpContext with ANY rank, but using the same INSTANCE we've got from
		// org.osgi.service.http.HttpService.createDefaultHttpContext() - this should replace the default "default"
		// context
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/new-path");
		ServiceReference<HttpContext> reference1 = mockReference(sample1,
				HttpContext.class, properties, () -> defaultContext, 0L, 13322);
		OsgiContextModel model1 = getHttpContextCustomizer().addingService(reference1);

		models.clear();
		assertThat(httpGET(port, "/new-path/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/new-path"));

		// now register yet another context - this time however, with lower ranking nothing should change, because
		// even if there's something to replace (by HttpContext instance) it'd be a Whiteboard context, so normal
		// ranking rules should apply
		DefaultHttpContextMapping mapping = new DefaultHttpContextMapping();
		mapping.setHttpContext(defaultContext);
		mapping.setContextId("default");
		mapping.setContextPath("/new-path2");
		ServiceReference<HttpContextMapping> reference2 = mockReference(sample1,
				HttpContextMapping.class, properties, () -> mapping, 0L, 42);
		OsgiContextModel model2 = getHttpContextMappingCustomizer().addingService(reference2);

		models.clear();
		assertThat(httpGET(port, "/new-path2/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/new-path/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/new-path"));

		// now unregister the first context - we should switch to remaining, lower ranked Whiteboard HttpContext
		getHttpContextCustomizer().removedService(reference1, model1);

		models.clear();
		assertThat(httpGET(port, "/new-path2/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/new-path/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/new-path2"));

		// now unregister the last Whiteboard context - we should get back to HttpService context (the default "default" one)
		getHttpContextMappingCustomizer().removedService(reference2, model2);

		models.clear();
		assertThat(httpGET(port, "/new-path2/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/new-path/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));

		props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));
		assertNull(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		assertThat(props.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH), equalTo("/"));

		wc.unregisterServlet(servlet);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void overrideHttpContextWithSameInstanceAndFiltersOnly() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		WebContainer wc = container(sample1);

		HttpContext defaultContext = wc.createDefaultHttpContext();

		final List<OsgiContextModel> models = new LinkedList<>();
		TestFilter filter = new TestFilter("1", models);

		// registration to "default" context maintained by HttpService and scoped to "sample1" bundle
		wc.registerFilter(filter, new String[] { "/f/*" }, null, null, null);
		assertThat(httpGET(port, "/f/test?terminate=1"), endsWith(">F(1)<F(1)"));
		assertThat(httpGET(port, "/new-path/f?terminate=1"), startsWith("HTTP/1.1 404"));

		assertThat(models.size(), equalTo(1));
		Hashtable<String, Object> props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));

		// register HttpContext with ANY rank, but using the same INSTANCE we've got from
		// org.osgi.service.http.HttpService.createDefaultHttpContext() - this should replace the default "default"
		// context
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/new-path");
		ServiceReference<HttpContext> reference1 = mockReference(sample1,
				HttpContext.class, properties, () -> defaultContext, 0L, 13322);
		OsgiContextModel model1 = getHttpContextCustomizer().addingService(reference1);

		models.clear();
		assertThat(httpGET(port, "/new-path/f/test?terminate=1"), endsWith(">F(1)<F(1)"));
		assertThat(httpGET(port, "/f/test?terminate=1"), startsWith("HTTP/1.1 404"));

		assertThat(models.size(), equalTo(1));
		assertSame(model1, models.get(0));

		// now register yet another context - this time however, with lower ranking nothing should change, because
		// even if there's something to replace (by HttpContext instance) it'd be a Whiteboard context, so normal
		// ranking rules should apply
		DefaultHttpContextMapping mapping = new DefaultHttpContextMapping();
		mapping.setHttpContext(defaultContext);
		mapping.setContextId("default");
		mapping.setContextPath("/new-path2");
		ServiceReference<HttpContextMapping> reference2 = mockReference(sample1,
				HttpContextMapping.class, properties, () -> mapping, 0L, 42);
		OsgiContextModel model2 = getHttpContextMappingCustomizer().addingService(reference2);

		models.clear();
		assertThat(httpGET(port, "/new-path2/f/test?terminate=1"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/new-path/f/test?terminate=1"), endsWith(">F(1)<F(1)"));
		assertThat(httpGET(port, "/f/test?terminate=1"), startsWith("HTTP/1.1 404"));

		assertThat(models.size(), equalTo(1));
		assertSame(model1, models.get(0));

		// now unregister the first context - we should switch to remaining, lower ranked Whiteboard HttpContext
		getHttpContextCustomizer().removedService(reference1, model1);

		models.clear();
		assertThat(httpGET(port, "/new-path2/f/test?terminate=1"), endsWith(">F(1)<F(1)"));
		assertThat(httpGET(port, "/new-path/f/test?terminate=1"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/f/test?terminate=1"), startsWith("HTTP/1.1 404"));

		assertThat(models.size(), equalTo(1));
		assertSame(model2, models.get(0));

		// now register yet another context - with the same path as currently effective, to check if there still is
		// a switch of contexts
		DefaultHttpContextMapping mapping2 = new DefaultHttpContextMapping();
		mapping2.setHttpContext(defaultContext);
		mapping2.setContextId("default");
		mapping2.setContextPath("/new-path2");
		ServiceReference<HttpContextMapping> reference3 = mockReference(sample1,
				HttpContextMapping.class, null, () -> mapping2, 0L, 43);
		OsgiContextModel model3 = getHttpContextMappingCustomizer().addingService(reference3);

		models.clear();
		assertThat(httpGET(port, "/new-path2/f/test?terminate=1"), endsWith(">F(1)<F(1)"));
		assertThat(httpGET(port, "/new-path/f/test?terminate=1"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/f/test?terminate=1"), startsWith("HTTP/1.1 404"));

		assertThat(models.size(), equalTo(1));
		assertSame(model3, models.get(0));

		// now unregister the last Whiteboard context - we should get back to HttpService context (the default "default" one)
		getHttpContextMappingCustomizer().removedService(reference2, model2);
		getHttpContextMappingCustomizer().removedService(reference3, model3);

		models.clear();
		assertThat(httpGET(port, "/new-path2/f/test?terminate=1"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/new-path/f/test?terminate=1"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/f/test?terminate=1"), endsWith(">F(1)<F(1)"));

		assertThat(models.size(), equalTo(1));
		props = models.get(0).getContextRegistrationProperties();
		assertThat(props.get(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY), equalTo("default"));

		wc.unregisterFilter(filter);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void multipleWhiteboardContextsAndHttpServiceServlet() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		WebContainer wc = container(sample1);

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

		stopContainer(sample1);
	}

	@Test
	public void httpServiceServletAndWhiteboardFilter() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		WebContainer wc = container(sample1);

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
		wc.registerServlet(servlet, new String[] { "/s/*" }, null, null);
		// filter1 is installed to "default" Whiteboard context, which still is valid under "/"
		assertThat(httpGET(port, "/s?terminate=1"), endsWith(">F(1)<F(1)"));
		// however HttpService "default" context is reconfigured (Pax Web FTW!) to "/c"
		assertThat(httpGET(port, "/c/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c/s?token=1"), endsWith(">F(2)S(1)<F(2)"));

		wc.unregisterServlet(servlet);
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
		WebContainer wc = container(sample1);

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
		wc.registerServlet(servlet1, "s1", new String[] { "/s/*" }, null, context1);
		Servlet servlet2 = new Utils.MyIdServlet("2");
		wc.registerServlet(servlet2, "s2", new String[] { "/t/*" }, null, null);

		assertThat(httpGET(port, "/s/1"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/s/1?token=1"), endsWith("S(1)"));
		assertThat(httpGET(port, "/t/1"), endsWith("S(2)"));
		// when targetting "404 servlet", the chosen context will be the highest ranked one at particular
		// ServerController level. And because our "custom" context has rank 0 for "/" context, it is higher
		// than "/" Whiteboard context (Integer.MIN_VALUE/2) and "/" HttpService context (Integer.MIN_VALUE)
		assertThat(httpGET(port, "/u/1"), startsWith("HTTP/1.1 403"));

		wc.unregisterServlet(servlet1);
		wc.unregisterServlet(servlet2);

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

	private static class TestFilter extends Utils.MyIdFilter {

		private final List<OsgiContextModel> models;

		TestFilter(String id, List<OsgiContextModel> models) {
			super(id);
			this.models = models;
		}

		@Override
		protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
			models.add(((OsgiScopedServletContext) getFilterConfig().getServletContext()).getOsgiContextModel());
			super.doFilter(req, resp, chain);
		}
	}

}
