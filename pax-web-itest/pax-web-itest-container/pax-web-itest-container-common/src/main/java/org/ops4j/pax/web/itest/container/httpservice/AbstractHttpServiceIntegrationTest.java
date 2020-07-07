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
package org.ops4j.pax.web.itest.container.httpservice;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.support.SimpleOnlyFilter;
import org.ops4j.pax.web.itest.utils.support.TestServlet;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.events.ElementEvent;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementListener;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractHttpServiceIntegrationTest extends AbstractControlledTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractHttpServiceIntegrationTest.class);

	@Test
	public void testRegisterServlet() throws Exception {
		HttpService httpService = getHttpService(context);

		final TestServlet servlet = new TestServlet();
		configureAndWaitForNamedServlet("testRegisterServlet",
				() -> httpService.registerServlet("/test", servlet, legacyName("testRegisterServlet"), null));

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test");

		assertTrue("Servlet.init(ServletConfig) was not called", servlet.isInitCalled());

		httpService.unregister("/test");
	}

	@Test
	public void testRegisterMultipleServlets() throws Exception {
		HttpService httpService = getHttpService(context);

		final TestServlet servlet1 = new TestServlet();
		configureAndWaitForNamedServlet("t1",
				() -> httpService.registerServlet("/test1", servlet1, legacyName("t1"), null));

		final TestServlet servlet2 = new TestServlet();
		configureAndWaitForNamedServlet("t2",
				() -> httpService.registerServlet("/test2", servlet2, legacyName("t2"), null));

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test1");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2");

		assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());
		assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());

		httpService.unregister("/test1");
		httpService.unregister("/test2");
	}

	/**
	 * This test registers a servlet using HttpService.registerServlet().
	 * It listens do the servlet-deployed event and then registers a second
	 * servlet on the same context.
	 * It checks that Servlet.init() was called after every invocation of
	 * registerServlet() and that both servlets live in the same servlet context.
	 */
	@Test
	public void testRegisterMultipleServletsSameContext() throws Exception {
		final HttpService httpService = getHttpService(context);

		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<>();
		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<>();

		context.registerService(WebElementListener.class, servletEvent -> {
			if (!(servletEvent.getData() instanceof ServletEventData)) {
				return;
			}
			ServletEventData data = (ServletEventData) servletEvent.getData();
			if (servletEvent.getType() == ElementEvent.State.DEPLOYED && "/test1".equals(data.getAlias())) {
				httpContext1.set(data.getHttpContext());
				latch1.countDown();
			}
			if (servletEvent.getType() == ElementEvent.State.DEPLOYED && "/test2".equals(data.getAlias())) {
				httpContext2.set(data.getHttpContext());
				latch2.countDown();
			}
		}, null);

		TestServlet servlet1 = new TestServlet();
		httpService.registerServlet("/test1", servlet1, legacyName("test1"), null);

		// init() will be called after first request
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test1");

		assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());
		assertTrue("Timeout waiting for test1 servlet registration", latch1.await(5, TimeUnit.SECONDS));

		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, legacyName("test2"), httpContext1.get());

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2");

		assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
		assertTrue("Timeout waiting for test2 servlet registration", latch2.await(5, TimeUnit.SECONDS));

		// register resources to different context
		// "/" will be changed to "" anyway
		httpService.registerResources("/r1", "/static", httpContext1.get());
		httpService.registerResources("/r2", "static", httpContext1.get());
		httpService.registerResources("/r3", "/", httpContext1.get());
		httpService.registerResources("/r4", "", httpContext1.get());
		httpService.registerResources("/", "/static", httpContext1.get());

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test1");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2");

		// resources
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r1/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r2/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test (ROOT)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r3/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test (ROOT)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r4/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/readme.txt");

		assertSame(httpContext1.get(), httpContext2.get());
		assertNotSame(servlet1.getServletContext(), servlet2.getServletContext());
		assertSame(((OsgiScopedServletContext) servlet1.getServletContext()).getContainerServletContext(),
				((OsgiScopedServletContext) servlet2.getServletContext()).getContainerServletContext());

		httpService.unregister("/test1");
		httpService.unregister("/test2");
		httpService.unregister("/");
		httpService.unregister("/r1");
		httpService.unregister("/r2");
		httpService.unregister("/r3");
		httpService.unregister("/r4");
	}

//	/**
//	 * This test registers a servlet to a already configured web context created
//	 * by the war extender.
//	 * It checks that Servlet.init() was called after the invocation of
//	 * registerServlet() and that the servlet uses the same http context that
//	 * the webapp uses.
//	 */
//	@Test
//	public void testRegisterServletToWarContext() throws Exception {
//		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<>();
//		bundleContext.registerService(WebListener.class, webEvent -> {
//			if (webEvent.getType() == WebEvent.DEPLOYED) {
//				httpContext1.set(webEvent.getHttpContext());
//			}
//		}, null);
//
//		LOG.debug("installing war-simple war");
//
//		String bundlePath = WEB_BUNDLE
//				+ "mvn:org.ops4j.pax.web.samples/war-simple/"
//				+ VersionUtil.getProjectVersion()
//				+ "/war?"
//				+ WEB_CONTEXT_PATH
//				+ "=/war";
//		Bundle installWarBundle = installAndStartBundle(bundlePath);
//
//		for (int count = 0; count < 100; count++) {
//			if (httpContext1.get() == null) {
//				Thread.sleep(100);
//			}
//		}
//		if (httpContext1.get() == null) {
//			Assert.fail("Timout waiting for web event");
//		}
//
//		LOG.debug("context registered, calling web request ...");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
//						resp -> resp.contains("Hello, World, from JSP"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war");
//
//		// ---
//
//		final HttpService httpService = getHttpService(installWarBundle.getBundleContext());
//
//		LOG.debug("... adding additional content to war");
//
//		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<>();
//		bundleContext.registerService(ServletListener.class, servletEvent -> {
//			if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
//				httpContext2.set(servletEvent.getHttpContext());
//			}
//		}, null);
//
//		TestServlet servlet2 = new TestServlet();
//		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());
//
//		// register resources to different context
//		// "/" will be changed to "" anyway
//		// without more changes, these resources will be loaded from original bundle that "created" this http context
//		httpService.registerResources("/r1", "/static", httpContext1.get());
//		httpService.registerResources("/r2", "static", httpContext1.get());
//		httpService.registerResources("/r3", "/", httpContext1.get());
//		httpService.registerResources("/r4", "", httpContext1.get());
//		// case when "resource name" == "context"
//		httpService.registerResources("/war", "/static", httpContext1.get());
//		// can't replace WAR's "default" resource servlet:
//		// "org.osgi.service.http.NamespaceException: alias: '/war' is already in use in this or another context"
//		//		httpService.registerResources("/", "/static", httpContext1.get());
//
//		if (isInitEager()) {
//			Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
//		}
//
//		for (int count = 0; count < 100; count++) {
//			if (httpContext2.get() == null) {
//				Thread.sleep(100);
//			}
//		}
//		if (httpContext2.get() == null) {
//			Assert.fail("Timout waiting for servlet event");
//		}
//
//		Assert.assertSame(httpContext1.get(), httpContext2.get());
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
//						resp -> resp.contains("Hello, World, from JSP"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'TEST OK'",
//						resp -> resp.contains("TEST OK"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/test2");
//
//		// resources
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'registerResources test (static)'",
//						resp -> resp.contains("registerResources test"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/r1/readme.txt");
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'registerResources test (static)'",
//						resp -> resp.contains("registerResources test"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/r2/readme.txt");
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
//						resp -> resp.contains("registerResources test"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/r3/readme.txt");
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
//						resp -> resp.contains("registerResources test"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/r4/readme.txt");
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'registerResources test (static)'",
//						resp -> resp.contains("registerResources test"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/war/readme.txt");
//	}

	@Test
	public void testFilterOnly() throws Exception {
		ServiceTracker<WebContainer, WebContainer> tracker = new ServiceTracker<>(context, WebContainer.class, null);
		tracker.open();
		WebContainer service = tracker.waitForService(TimeUnit.SECONDS.toMillis(20));
		Filter filter = new SimpleOnlyFilter();
		configureAndWaitForFilterWithMapping("/testFilter/*",
				() -> service.registerFilter(filter, new String[] { "/testFilter/*", }, null, null, null));

		// pass chain=false, so filter won't call chain.doFilter(). If it does so, target servlet will be 404 one
		// and we'll get 404 error from the servlet.
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Filter'",
						resp -> resp.contains("Hello Whiteboard Filter"))
				.doGETandExecuteTest("http://127.0.0.1:8181/testFilter/filterMe?chain=false");

		service.unregisterFilter(filter);
	}

	@Test
	public void testRegisterResourcesWithDefaultContext() throws Exception {
		final HttpService httpService = getHttpService(context);

		configureAndWaitForServletWithMapping("/r5/*", () -> {
			httpService.registerResources("/", "static", null);
			httpService.registerResources("/r1", "/static", null);
			httpService.registerResources("/r2", "static", null);
			httpService.registerResources("/r3", "static/", null);
			httpService.registerResources("/r4", "/", null);
			httpService.registerResources("/r5", "", null);
		});

		// normal access

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r1/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r2/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r3/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test (ROOT)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r4/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test (ROOT)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r5/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/r6/readme.txt");

		// unsecure access

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_BAD_REQUEST)
				.doGETandExecuteTest("http://127.0.0.1:8181/../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r1/../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r2/../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r3/../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r4/../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r5/../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r6/../readme.txt");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_BAD_REQUEST)
				.doGETandExecuteTest("http://127.0.0.1:8181/../../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_BAD_REQUEST)
				.doGETandExecuteTest("http://127.0.0.1:8181/r1/../../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_BAD_REQUEST)
				.doGETandExecuteTest("http://127.0.0.1:8181/r2/../../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_BAD_REQUEST)
				.doGETandExecuteTest("http://127.0.0.1:8181/r3/../../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_BAD_REQUEST)
				.doGETandExecuteTest("http://127.0.0.1:8181/r4/../../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_BAD_REQUEST)
				.doGETandExecuteTest("http://127.0.0.1:8181/r5/../../readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_BAD_REQUEST)
				.doGETandExecuteTest("http://127.0.0.1:8181/r6/../../readme.txt");

		// directory access

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r1");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r1/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r2/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r3");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r3/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r4");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r4/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r5");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_FORBIDDEN)
				.doGETandExecuteTest("http://127.0.0.1:8181/r5/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/r6");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/r6/");

		httpService.unregister("/");
		httpService.unregister("/r1");
		httpService.unregister("/r2");
		httpService.unregister("/r3");
		httpService.unregister("/r4");
		httpService.unregister("/r5");
	}

}
