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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.internal.DefaultHttpContext;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.HttpContext;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

/**
 * These tests show basic usage for servlet and filter registration through {@link WebContainer} interface.
 */
@RunWith(Parameterized.class)
public class ServerControllerBasicRegistrationTest extends MultiContainerTestSupport {

	@Test
	public void registerSingleServletUsingExplicitBatch() throws Exception {
		ServerController controller = Utils.create(properties -> {
			new File("target/ncsa").mkdirs();
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED, "true");
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR, "target/ncsa");

			properties.put(PaxWebConfig.PID_CFG_SERVER_THREAD_NAME_PREFIX, "XNIO-registerSingleServletUsingExplicitBatch");

			if (runtime == Runtime.JETTY) {
				// this file should be used to reconfigure thread pool already set inside Pax Web version of Jetty Server
				properties.put(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES, "target/test-classes/jetty-server.xml");
			}
		}, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);

		WebContainerContext wcc = new DefaultHttpContext(bundle) {
			@Override
			public URL getResource(String name) {
				// this should be used when calling ServletContext.getResource
				try {
					return new URL("file://" + name);
				} catch (MalformedURLException ignored) {
					return null;
				}
			}

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				LOG.info("handleSecurity(" + request + ")");
				return request.getHeader("Let-Me-In") != null;
			}
		};

		Servlet servlet = new HttpServlet() {
			private ServletConfig config;

			private final Map<ServletContext, Boolean> contexts = new IdentityHashMap<>();

			@Override
			public void init(ServletConfig config) throws ServletException {
				super.init(config);
				assertThat(config.getInitParameter("p1"), equalTo("v1"));
				assertThat(super.getInitParameter("p1"), equalTo("v1"));
				contexts.put(config.getServletContext(), true);
				this.config = config;
			}

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setContentType("text/plain");

				contexts.put(getServletContext(), true);
				contexts.put(req.getServletContext(), true);
				contexts.put(req.getSession().getServletContext(), true);
				contexts.put(config.getServletContext(), true);
				contexts.put(getServletConfig().getServletContext(), true);

				assertThat(contexts.size(), equalTo(1));

				assertThat(super.getInitParameter("p1"), equalTo("v1"));

				// this should give us "file:/something"
				resp.getWriter().print(req.getServletContext().getResource("/something").toString());
			}
		};

		Batch batch = new Batch("Register Single Servlet");

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());
		ServletContextModel context = new ServletContextModel("/c");
		batch.addServletContextModel(server, context);

		OsgiContextModel osgiContext = new OsgiContextModel(wcc, bundle);
		osgiContext.setServletContextModel(context);
		batch.addOsgiContextModel(osgiContext);

		Map<String, String> initParams = new HashMap<>();
		initParams.put("p1", "v1");

		batch.addServletModel(server, new ServletModel.Builder()
				.withServletName("my-servlet")
				.withUrlPatterns(new String[] { "/s/*" })
				.withServlet(servlet)
				.withInitParams(initParams)
				.withOsgiContextModel(osgiContext)
				.withRegisteringBundle(bundle)
				.build());

		controller.sendBatch(batch);

		String response = httpGET(port, "/c/s/1/registerSingleServletUsingExplicitBatch", "Let-Me-In: true");
		assertTrue(response.endsWith("file:/something"));

		response = httpGET(port, "/c/s/1/registerSingleServletUsingExplicitBatch");
		assertTrue(response.contains("HTTP/1.1 403"));

		controller.stop();
	}

	@Test
	public void registerSingleServletUsingWebContainer() throws Exception {
		ServerController controller = Utils.create(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleWiring wiring = mock(BundleWiring.class);
		when(bundle.adapt(BundleWiring.class)).thenReturn(wiring);
		when(wiring.getClassLoader()).thenReturn(this.getClass().getClassLoader());

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());

		WebContainer wc = new HttpServiceEnabled(bundle, controller, server, null, controller.getConfiguration());

		HttpContext context = new HttpContext() {
			@Override
			public URL getResource(String name) {
				// this should be used when calling ServletContext.getResource
				try {
					return new URL("file://" + name);
				} catch (MalformedURLException ignored) {
					return null;
				}
			}

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				LOG.info("handleSecurity(" + request + ")");
				return request.getHeader("Let-Me-In") != null;
			}

			@Override
			public String getMimeType(String name) {
				return null;
			}
		};

		Servlet servlet = new HttpServlet() {
			private ServletConfig config;

			private final Map<ServletContext, Boolean> contexts = new IdentityHashMap<>();

			@Override
			public void init(ServletConfig config) throws ServletException {
				super.init(config);
				assertThat(config.getInitParameter("p1"), equalTo("v1"));
				assertThat(super.getInitParameter("p1"), equalTo("v1"));
				contexts.put(config.getServletContext(), true);
				this.config = config;
			}

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setContentType("text/plain");

				contexts.put(getServletContext(), true);
				contexts.put(req.getServletContext(), true);
				contexts.put(req.getSession().getServletContext(), true);
				contexts.put(config.getServletContext(), true);
				contexts.put(getServletConfig().getServletContext(), true);

				assertThat(contexts.size(), equalTo(1));

				assertThat(super.getInitParameter("p1"), equalTo("v1"));

				// this should give us "file:/something"
				resp.getWriter().print(req.getServletContext().getResource("/something").toString());
			}
		};

		Dictionary<String, String> initParams = new Hashtable<>();
		initParams.put("p1", "v1");

		wc.registerServlet(servlet, "my-servlet", new String[] { "/s/*" }, initParams, context);

		String response = httpGET(port, "/s/1?t=registerSingleServletUsingWebContainer", "Let-Me-In: true");
		assertTrue(response.endsWith("file:/something"));

		response = httpGET(port, "/s/1?t=registerSingleServletUsingWebContainer");
		assertTrue(response.contains("HTTP/1.1 403"));

		controller.stop();
	}

	@Test
	public void registerFilterAndServletUsingExcplicitBatch() throws Exception {
		ServerController controller = Utils.create(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleContext context = mock(BundleContext.class);
		when(bundle.getBundleContext()).thenReturn(context);

		WebContainerContext wcc1 = new DefaultHttpContext(bundle);
		WebContainerContext wcc2 = new DefaultHttpContext(bundle, "special");

		// when single instance is added more than once (passed in ServletModel), init(ServletConfig)
		// operates on single instance and even the Whiteboard Service specification suggests using Prototype
		// Service. Otherwise, init() would be called more than once on single instance providing different
		// ServletConfig objects (with different - and usually wrong ServletContext)
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> ref = mock(ServiceReference.class);
		when(context.getService(ref)).thenReturn(new Utils.MyHttpServlet("1"));

		Filter filter = new HttpFilter() {
			@Override
			protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().write(getFilterName() + "1");
				if (!"/d".equals(req.getServletContext().getContextPath())) {
					// in /d we know we don't map to any servlet
					chain.doFilter(req, resp);
				}
				resp.getWriter().write(getFilterName() + "2");
				resp.getWriter().close();
			}
		};

		Batch batch = new Batch("Register Servlet and Filter");

		// two contexts. servlet will be registered to /c, filter - to /c and /d
		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());

		ServletContextModel contextC = new ServletContextModel("/c");
		ServletContextModel contextD = new ServletContextModel("/d");
		ServletContextModel contextE = new ServletContextModel("/e");
		batch.addServletContextModel(server, contextC);
		batch.addServletContextModel(server, contextD);
		batch.addServletContextModel(server, contextE);

		OsgiContextModel osgiContextC = new OsgiContextModel(wcc1, bundle, contextC);
		OsgiContextModel osgiContextC2 = new OsgiContextModel(wcc2, bundle, contextC);
		OsgiContextModel osgiContextD = new OsgiContextModel(wcc1, bundle, contextD);
		OsgiContextModel osgiContextE = new OsgiContextModel(wcc1, bundle, contextE);
		batch.addOsgiContextModel(osgiContextC);
		batch.addOsgiContextModel(osgiContextC2);
		batch.addOsgiContextModel(osgiContextD);
		batch.addOsgiContextModel(osgiContextE);

		Map<String, String> initParams = new HashMap<>();
		initParams.put("p1", "v1");

		batch.addServletModel(server, new ServletModel.Builder()
				.withServletName("my-servlet1")
				.withUrlPatterns(new String[] { "/s/*" }) // responds to /*/s/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.withOsgiContextModel(osgiContextC) // responds to /c/s/*
				.withOsgiContextModel(osgiContextE) // responds to /e/s/*
				.withRegisteringBundle(bundle)
				.build());
		batch.addServletModel(server, new ServletModel.Builder()
				.withServletName("my-servlet2")
				.withUrlPatterns(new String[] { "/s2/*" }) // responds to /*/s2/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.withOsgiContextModel(osgiContextC2) // responds to /c/s2/*
				.withRegisteringBundle(bundle)
				.build());

		Map<String, TreeSet<FilterModel>> filters = new HashMap<>();
		TreeSet<FilterModel> set = new TreeSet<>();
		// this filter is NOT registered to osgiContextC2, so should NOT be mapped to /c/s2/*
		set.add(new FilterModel.Builder()
				.withFilterName("my-filter")
				.withUrlPatterns(new String[] { "/*" }) // maps to /*/* depending on associated contexts
				.withFilter(filter)
				.withOsgiContextModel(osgiContextC) // maps to /c/*
				.withOsgiContextModel(osgiContextD) // maps to /d/*
				.withRegisteringBundle(bundle)
				.build());
		filters.put("/c", set);
		filters.put("/d", set);
		batch.updateFilters(filters);

		controller.sendBatch(batch);

		// filter -> servlet
		String response;
		response = httpGET(port, "/c/s/1?t=registerFilterAndServletUsingExcplicitBatch");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-servlet1[/c]my-filter2"));

		// just one filter in the chain, without target servlet
		response = httpGET(port, "/d/s/1?t=registerFilterAndServletUsingExcplicitBatch");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-filter2"));

		// just servlet, because /* filter doesn't use my-servlet2's ServletContextHelper
		response = httpGET(port, "/c/s2/1?t=registerFilterAndServletUsingExcplicitBatch");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet2[/c]"));

		// just servlet, because /* filter isn't associated with OsgiContext for /e
		response = httpGET(port, "/e/s/1?t=registerFilterAndServletUsingExcplicitBatch");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet1[/e]"));

		controller.stop();
	}

	@Test
	public void registerFilterAndServletUsingWebContainer() throws Exception {
		ServerController controller = Utils.create(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleContext context = mock(BundleContext.class);
		when(bundle.getBundleContext()).thenReturn(context);

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());

		Configuration config = controller.getConfiguration();
		HttpServiceEnabled wc = new HttpServiceEnabled(bundle, controller, server, null, config);

		// 3 physical servlet context models
		Batch batch = new Batch("Initialization Batch");
		server.getOrCreateServletContextModel("/c", batch);
		server.getOrCreateServletContextModel("/d", batch);
		server.getOrCreateServletContextModel("/e", batch);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		WebContainerContext wccC1 = wc.createDefaultHttpContext("wccC1");
		WebContainerContext wccC2 = wc.createDefaultHttpContext("wccC2");
		WebContainerContext wccD1 = wc.createDefaultHttpContext("wccD1");
		WebContainerContext wccE1 = wc.createDefaultHttpContext("wccE1");

		// 4 logical OSGi context models
		batch = new Batch("Initialization Batch");
		server.associateHttpContext(wccC1, server.createNewContextModel(wccC1, "/c", bundle, batch));
		server.associateHttpContext(wccC2, server.createNewContextModel(wccC2, "/c", bundle, batch));
		server.associateHttpContext(wccD1, server.createNewContextModel(wccD1, "/d", bundle, batch));
		server.associateHttpContext(wccE1, server.createNewContextModel(wccE1, "/e", bundle, batch));
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		final int[] id = { 1 };
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> ref = mock(ServiceReference.class);
		when(context.getService(ref)).thenAnswer(invocation -> new Utils.MyHttpServlet(Integer.toString(id[0]++)));

		Filter filter = new HttpFilter() {
			@Override
			public void init() {
				LOG.info("Filter {} initialized in {}", System.identityHashCode(this), getServletContext().getContextPath());
			}

			@Override
			public void destroy() {
				LOG.info("Filter {} destroyed in {}", System.identityHashCode(this), getServletContext().getContextPath());
			}

			@Override
			protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().write(getFilterName() + "1");
				if (!"/d".equals(req.getServletContext().getContextPath())) {
					// in /d we know we don't map to any servlet
					chain.doFilter(req, resp);
				}
				resp.getWriter().write(getFilterName() + "2");
				resp.getWriter().close();
			}
		};

		Map<String, String> initParams = new HashMap<>();
		initParams.put("p1", "v1");

		wc.doRegisterServlet(Arrays.asList(wccC1, wccE1), new ServletModel.Builder()
				.withServletName("my-servlet1")
				.withUrlPatterns(new String[] { "/s/*" }) // responds to /c/s/* or /e/s/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.build());
		wc.doRegisterServlet(Collections.singletonList(wccC2), new ServletModel.Builder()
				.withServletName("my-servlet2")
				.withUrlPatterns(new String[] { "/s2/*" }) // responds to /c/s2/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.build());

		// this filter is NOT registered to osgiContextC2, so should NOT be mapped to /c/s2/*
		wc.doRegisterFilter(Arrays.asList(wccC1, wccD1), new FilterModel.Builder()
				.withFilterName("my-filter")
				.withUrlPatterns(new String[] { "/*" }) // maps to /c/* or /d/* depending on associated contexts
				.withFilter(filter)
				.withRegisteringBundle(bundle)
				.build());

		// filter -> servlet
		String response;
		response = httpGET(port, "/c/s/1");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-servlet1[/c]my-filter2"));

		// just one filter in the chain, without target servlet
		response = httpGET(port, "/d/s/1");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-filter2"));

		// just servlet, because /* filter doesn't use my-servlet2's ServletContextHelper
		response = httpGET(port, "/c/s2/1");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet2[/c]"));

		// just servlet, because /* filter isn't associated with OsgiContext for /e
		response = httpGET(port, "/e/s/1");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet1[/e]"));

		controller.stop();
	}

	/**
	 * <p>Test for Whiteboard service registration of servlets to different OSGi contexts and handling name
	 * conflicts.</p>
	 *
	 * <p>Have 3 contexts (each with single OSGi context associated):<ul>
	 *     <li>/c1</li>
	 *     <li>/c2</li>
	 *     <li>/c3</li>
	 *     <li>/c4</li>
	 * </ul>
	 * Servlet registration plan (newer servlet has higher service.id):<ul>
	 *     <li>"s1"(1) with rank=0 ragistered to /c1 and /c2 - should be OK</li>
	 *     <li>"s1"(2) with rank=3 registered to /c3 - should be OK</li>
	 *     <li>"s1"(3) with rank=0 registered to /c1 - should be registered as disabled</li>
	 *     <li>"s1"(4) with rank=2 registered to /c2 and /c3 - should be registered as disabled because of "s1"(2)</li>
	 *     <li>"s1"(5) with rank=1 registered to /c2 and /c4 - should deactivate "s1"(1) from /c1 and /c2, should
	 *     reactivate "s1"(3) in /c1, which was previously disabled, should activate "s1"(4) in /c2 instead of "s1"(5),
	 *     but "s1"(4) is still shadowed in /c3 by "s1"(2), so "s1"(5) is the one active in /c2</li>
	 *     <li>"s1"(6) with rank=0 registered to /c4 - as disabled, because shadowed by "s1"(5)</li>
	 *     <li>"s1"(2) is unregistered from /c3 - should activate "s1"(4) in /c3 and even in /c2, because in /c2
	 *     "s1"(5) is active, but with lower rank - this has to change just as if "s1"(4) was newly registered. so
	 *     "s1"(5) is deactivated - in both /c2 and /c4, so leading to reactivation of "s1"(6) in /c4</li>
	 * </ul></p>
	 *
	 * @throws Exception
	 */
	@Test
	public void registerServletsConflictingByName() throws Exception {
		ServerController controller = Utils.create(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleContext context = mock(BundleContext.class);
		when(bundle.getBundleContext()).thenReturn(context);

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());

		Configuration config = controller.getConfiguration();
		HttpServiceEnabled wc = new HttpServiceEnabled(bundle, controller, server, null, config);

		Batch batch = new Batch("Initialization Batch");
		server.getOrCreateServletContextModel("/c1", batch);
		server.getOrCreateServletContextModel("/c2", batch);
		server.getOrCreateServletContextModel("/c3", batch);
		server.getOrCreateServletContextModel("/c4", batch);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		WebContainerContext wcc1 = wc.createDefaultHttpContext("wcc1");
		WebContainerContext wcc2 = wc.createDefaultHttpContext("wcc2");
		WebContainerContext wcc3 = wc.createDefaultHttpContext("wcc3");
		WebContainerContext wcc4 = wc.createDefaultHttpContext("wcc4");

		// 4 logical OSGi context models
		batch = new Batch("Initialization Batch");
		OsgiContextModel cm1 = server.createNewContextModel(wcc1, "/c1", bundle, batch);
		OsgiContextModel cm2 = server.createNewContextModel(wcc2, "/c2", bundle, batch);
		OsgiContextModel cm3 = server.createNewContextModel(wcc3, "/c3", bundle, batch);
		OsgiContextModel cm4 = server.createNewContextModel(wcc3, "/c4", bundle, batch);
		server.associateHttpContext(wcc1, cm1);
		server.associateHttpContext(wcc2, cm2);
		server.associateHttpContext(wcc3, cm3);
		server.associateHttpContext(wcc4, cm4);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> s11 = mock(ServiceReference.class);
		when(context.getService(s11)).thenAnswer(invocation -> new Utils.MyIdServlet("1"));
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> s12 = mock(ServiceReference.class);
		when(context.getService(s12)).thenAnswer(invocation -> new Utils.MyIdServlet("2"));
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> s13 = mock(ServiceReference.class);
		when(context.getService(s13)).thenAnswer(invocation -> new Utils.MyIdServlet("3"));
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> s14 = mock(ServiceReference.class);
		when(context.getService(s14)).thenAnswer(invocation -> new Utils.MyIdServlet("4"));
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> s15 = mock(ServiceReference.class);
		when(context.getService(s15)).thenAnswer(invocation -> new Utils.MyIdServlet("5"));
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> s16 = mock(ServiceReference.class);
		when(context.getService(s16)).thenAnswer(invocation -> new Utils.MyIdServlet("6"));

		long serviceId = 0;

		// servlet#1 registered in /c1 and /c2
		wc.doRegisterServlet(Arrays.asList(wcc1, wcc2), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServletReference(s11)
				.withServiceRankAndId(0, ++serviceId)
				.build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c2/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c3/s"), startsWith("HTTP/1.1 404"));

		// servlet#2 registered in /c3 - no conflict
		wc.doRegisterServlet(Collections.singletonList(wcc3), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServletReference(s12)
				.withServiceRankAndId(3, ++serviceId)
				.build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c2/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c3/s"), endsWith("S(2)"));

		// servlet#3 registered to /c1, but with higher service ID - should be marked as disabled
		wc.doRegisterServlet(Collections.singletonList(wcc1), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServletReference(s13)
				.withServiceRankAndId(0, ++serviceId)
				.build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c2/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c3/s"), endsWith("S(2)"));

		// servlet#4 registered to /c2 and /c3 - ranked higher than s#1 in /c2, but ranked lower than s#2 in /c3
		wc.doRegisterServlet(Arrays.asList(wcc2, wcc3), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServletReference(s14)
				.withServiceRankAndId(2, ++serviceId)
				.build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c2/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c3/s"), endsWith("S(2)"));

		// servlet#5 registered to /c2 and /c4 - ranked higher than s#1 in /c2, so:
		//  - s#1 is deactivated in /c1 and /c2
		//  - s#3 is activated in /c1
		//  - s#5 MAY be activated in /c2 and /c4, but in /c2, s#4 is ranked higher than s#5
		//  - s#4 is ranked lower than s#2 in /c3, so it won't be activated ANYWHERE
		//  - s#5 will thus be activated in /c2 and /c4
		wc.doRegisterServlet(Arrays.asList(wcc2, wcc4), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServletReference(s15)
				.withServiceRankAndId(1, ++serviceId)
				.build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(3)"));
		assertThat(httpGET(port, "/c2/s"), endsWith("S(5)"));
		assertThat(httpGET(port, "/c3/s"), endsWith("S(2)"));
		assertThat(httpGET(port, "/c4/s"), endsWith("S(5)"));

		// servlet#6 registered to /c4 - ranked lower than s#5 in /c4, so added as disabled
		wc.doRegisterServlet(Collections.singletonList(wcc4), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServletReference(s16)
				.withServiceRankAndId(0, ++serviceId)
				.build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(3)"));
		assertThat(httpGET(port, "/c2/s"), endsWith("S(5)"));
		assertThat(httpGET(port, "/c3/s"), endsWith("S(2)"));
		assertThat(httpGET(port, "/c4/s"), endsWith("S(5)"));

		// servlet#2 unregistered, s#4 can be activated in /c3 and can be activated in /c2 because s#5 in /c2 is ranked
		// lower than s#4, so s#5 disabled in /c4, so s#6 enabled in /c4
		wc.doUnregisterServlet(new ServletModel.Builder()
				.withServletReference(s12)
				.withOsgiContextModel(cm3)
				.remove());

		assertTrue(httpGET(port, "/c1/s").endsWith("S(3)"));
		assertTrue(httpGET(port, "/c2/s").endsWith("S(4)"));
		assertTrue(httpGET(port, "/c3/s").endsWith("S(4)"));
		assertTrue(httpGET(port, "/c4/s").endsWith("S(6)"));

		controller.stop();
	}

}
