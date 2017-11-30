/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.SimpleOnlyFilter;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractHttpServiceIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractHttpServiceIntegrationTest.class);

	private Bundle installWarBundle;

	protected abstract boolean isInitEager();
	
	@Before
	public void setUp() throws Exception {
		initServletListener(null);
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForServletListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}


	@Test
	public void testSubPath() throws Exception {

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
		// test image-serving
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Hello World resources should be available under /images",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/images/logo.png");
		// test image-serving from different alias
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Hello World resources should be available under /alt-images",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/alt-images/logo.png");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseHeaderAssertion("Other resource paths will be served by servlet mapped at /*",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().startsWith("text/html")))
				.doGETandExecuteTest("http://127.0.0.1:8181/alt2-images/logo.png");
	}

	@Test
	public void testRootPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

	@Test
	public void testServletPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet Path: '",
						resp -> resp.contains("Servlet Path: "))
				.withResponseAssertion("Response must contain 'Path Info: /lall/blubb'",
						resp -> resp.contains("Path Info: /lall/blubb"))
				.doGETandExecuteTest("http://127.0.0.1:8181/lall/blubb");
	}

	@Test
	public void testServletDeRegistration() throws BundleException, ServletException, NamespaceException {

		if (installWarBundle != null) {
			installWarBundle.stop();
		}
		// TODO check that deregistration worked
	}


	@Test
	public void testRegisterServlet() throws Exception {
		HttpService httpService = getHttpService(bundleContext);

		initServletListener(null);

		TestServlet servlet = new TestServlet();
		httpService.registerServlet("/test", servlet, null, null);
		if (isInitEager()) {
			Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet.isInitCalled());
		}

		waitForServletListener();

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test");
	}

	@Test
	public void testRegisterMultipleServlets() throws Exception {
		HttpService httpService = getHttpService(bundleContext);

		initServletListener(null);
		TestServlet servlet1 = new TestServlet();
		httpService.registerServlet("/test1", servlet1, null, null);
		if (isInitEager()) {
			Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());
		}
		waitForServletListener();

		initServletListener(null);
		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, null);
		if (isInitEager()) {
			Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
		}
		waitForServletListener();

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test1");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2");
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
		final HttpService httpService = getHttpService(bundleContext);

		// the helloworld-hs bundle registers a servlet under the root
		// we stop this to prevent this to interfere with the resource registered
		// in this test
		installWarBundle.stop();

		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<>();
		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<>();
		bundleContext.registerService(ServletListener.class, servletEvent -> {
			if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test1".equals(servletEvent.getAlias())) {
				httpContext1.set(servletEvent.getHttpContext());
			}
			if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
				httpContext2.set(servletEvent.getHttpContext());
			}
		}, null);

		TestServlet servlet1 = new TestServlet();
		httpService.registerServlet("/test1", servlet1, null, null);
		if (isInitEager()) {
			Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());
		}

		for (int count = 0; count < 100; count++) {
			if (httpContext1.get() == null) {
				Thread.sleep(100);
			}
		}
		if (httpContext1.get() == null) {
			Assert.fail("Timout waiting for servlet event");
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test1");

		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());
		if (isInitEager()) {
			Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
		}

		// register resources to different context
		// "/" will be changed to "" anyway
		httpService.registerResources("/r1", "/static", httpContext1.get());
		httpService.registerResources("/r2", "static", httpContext1.get());
		httpService.registerResources("/r3", "/", httpContext1.get());
		httpService.registerResources("/r4", "", httpContext1.get());
		httpService.registerResources("/", "/static", httpContext1.get());

		for (int count = 0; count < 100; count++) {
			if (httpContext2.get() == null) {
				Thread.sleep(100);
			}
		}
		if (httpContext2.get() == null) {
			Assert.fail("Timout waiting for servlet event");
		}

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

		Assert.assertSame(httpContext1.get(), httpContext2.get());
		Assert.assertSame(servlet1.getServletContext(), servlet2.getServletContext());
	}

	/**
	 * This test registers a servlet to a already configured web context created
	 * by the war extender.
	 * It checks that Servlet.init() was called after the invocation of
	 * registerServlet() and that the servlet uses the same http context that
	 * the webapp uses.
	 */
	@Test
	public void testRegisterServletToWarContext() throws Exception {
		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<>();
		bundleContext.registerService(WebListener.class, webEvent -> {
			if (webEvent.getType() == WebEvent.DEPLOYED) {
				httpContext1.set(webEvent.getHttpContext());
			}
		}, null);

		LOG.debug("installing war-simple war");

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-simple/"
				+ VersionUtil.getProjectVersion()
				+ "/war?"
				+ WEB_CONTEXT_PATH
				+ "=/war";
		Bundle installWarBundle = installAndStartBundle(bundlePath);

		for (int count = 0; count < 100; count++) {
			if (httpContext1.get() == null) {
				Thread.sleep(100);
			}
		}
		if (httpContext1.get() == null) {
			Assert.fail("Timout waiting for web event");
		}

		LOG.debug("context registered, calling web request ...");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
						resp -> resp.contains("Hello, World, from JSP"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war");

		// ---

		final HttpService httpService = getHttpService(installWarBundle.getBundleContext());

		LOG.debug("... adding additional content to war");

		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<>();
		bundleContext.registerService(ServletListener.class, servletEvent -> {
			if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
				httpContext2.set(servletEvent.getHttpContext());
			}
		}, null);

		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());

		// register resources to different context
		// "/" will be changed to "" anyway
		// without more changes, these resources will be loaded from original bundle that "created" this http context
		httpService.registerResources("/r1", "/static", httpContext1.get());
		httpService.registerResources("/r2", "static", httpContext1.get());
		httpService.registerResources("/r3", "/", httpContext1.get());
		httpService.registerResources("/r4", "", httpContext1.get());
		// case when "resource name" == "context"
		httpService.registerResources("/war", "/static", httpContext1.get());
		// can't replace WAR's "default" resource servlet:
		// "org.osgi.service.http.NamespaceException: alias: '/war' is already in use in this or another context"
		//		httpService.registerResources("/", "/static", httpContext1.get());

		if (isInitEager()) {
			Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
		}

		for (int count = 0; count < 100; count++) {
			if (httpContext2.get() == null) {
				Thread.sleep(100);
			}
		}
		if (httpContext2.get() == null) {
			Assert.fail("Timout waiting for servlet event");
		}

		Assert.assertSame(httpContext1.get(), httpContext2.get());

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
						resp -> resp.contains("Hello, World, from JSP"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/test2");

		// resources
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/r1/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/r2/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/r3/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/r4/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/war/readme.txt");
	}


	@Test
	public void testRootFilterRegistration() throws Exception {
		ServiceTracker<WebContainer, WebContainer> tracker = new ServiceTracker<>(installWarBundle.getBundleContext(), WebContainer.class, null);
		tracker.open();
		WebContainer service = tracker.waitForService(TimeUnit.SECONDS.toMillis(20));
		final String fullContent = "This content is Filtered by a javax.servlet.Filter";
		Filter filter = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				PrintWriter writer = response.getWriter();
				writer.write(fullContent);
				writer.flush();
			}

			@Override
			public void destroy() {
			}
		};
		final StringWriter writer = new StringWriter();
		//CHECKSTYLE:OFF
		filter.doFilter(null, (ServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ServletResponse.class}, (proxy, method, args) -> {
			if (method.getName().equals("getWriter")) {
				return new PrintWriter(writer);
			}
			return null;
		}), null);
		//CHECKSTYLE:OFF
		//Check if our example filter do write the string to the writer...
		Assert.assertEquals(fullContent, writer.toString());
		//Now register the Filter under some alias...
		service.registerFilter(filter, new String[]{"*", "/*", "/", "/some/random/path"}, null, null, null);
		//If it works, always the filter should take over and return the same string regardeless of the URL
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-context/some/random/path");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-context/some/notregistered/random/path");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-context/");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/images/logo.png");

		//of course we should be able to deregister :-)
		service.unregisterFilter(filter);
		tracker.close();
	}

	@Test
	@Ignore("PAXWEB-483: Filtering without a Servlet doesn't work with Http-Service but within a war")
	public void testFilterOnly() throws Exception {
		ServiceTracker<WebContainer, WebContainer> tracker = new ServiceTracker<>(bundleContext, WebContainer.class, null);
		tracker.open();
		WebContainer service = tracker.waitForService(TimeUnit.SECONDS.toMillis(20));
		Filter filter = new SimpleOnlyFilter();
		service.registerFilter(filter, new String[]{"/testFilter/*",}, null, null, null);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Filter'",
						resp -> resp.contains("Hello Whiteboard Filter"))
				.doGETandExecuteTest("http://127.0.0.1:8181/testFilter/filterMe");

		service.unregisterFilter(filter);
	}
}
