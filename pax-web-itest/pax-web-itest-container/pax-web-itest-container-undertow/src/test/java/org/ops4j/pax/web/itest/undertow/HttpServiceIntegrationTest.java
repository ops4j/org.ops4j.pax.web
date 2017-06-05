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
package org.ops4j.pax.web.itest.undertow;

import org.junit.*;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WaitCondition;
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

import javax.servlet.*;
import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpServiceIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureUndertow();
	}

	@Before
	public void setUp() throws Exception {

		waitForServer("http://127.0.0.1:8181/");
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

//		testClient.testWebPath("http://127.0.0.1:8181/", "");
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

		waitForServletListener();

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test");

		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet.isInitCalled());
	}

	@Test
	public void testRegisterMultipleServlets() throws Exception {
		HttpService httpService = getHttpService(bundleContext);

		initServletListener(null);
		TestServlet servlet1 = new TestServlet();
		httpService.registerServlet("/test1", servlet1, null, null);
		waitForServletListener();

		initServletListener(null);
		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, null);
		waitForServletListener();

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test1");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2");

		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());
		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
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

		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<>();
		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<>();
		bundleContext.registerService(ServletListener.class, new ServletListener() {
			@Override
			public void servletEvent(ServletEvent servletEvent) {
				if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test1".equals(servletEvent.getAlias())) {
					httpContext1.set(servletEvent.getHttpContext());
				}
				if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
					httpContext2.set(servletEvent.getHttpContext());
				}
			}
		}, null);

		TestServlet servlet1 = new TestServlet();
		httpService.registerServlet("/test1", servlet1, null, null);

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

		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());

		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());

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

		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());

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
		bundleContext.registerService(WebListener.class, new WebListener() {
			@Override
			public void webEvent(WebEvent webEvent) {
				if (webEvent.getType() == WebEvent.DEPLOYED) {
					httpContext1.set(webEvent.getHttpContext());
				}
			}
		}, null);

		logger.debug("installing war-simple war");

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

		logger.debug("context registered, calling web request ...");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
						resp -> resp.contains("Hello, World, from JSP"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war");

		final HttpService httpService = getHttpService(installWarBundle.getBundleContext());

		logger.debug("... adding additional content to war");

		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<>();
		installWarBundle.getBundleContext().registerService(ServletListener.class, servletEvent -> {
			if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
				httpContext2.set(servletEvent.getHttpContext());
			}
		}, null);

		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());

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

		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
	}

	/**
	 * Test works, when using bundle context of web bundle that has servlets for which we want to register filters
	 * @throws Exception
	 */
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
		filter.doFilter(null, (ServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ServletResponse.class}, new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName().equals("getWriter")) {
					return new PrintWriter(writer);
				}
				return null;
			}
		}), null);
		//CHECKSTYLE:OFF
		//Check if our example filter do write the string to the writer...
		Assert.assertEquals(fullContent, writer.toString());
		//Now register the Filter under some alias...
		service.registerFilter(filter, new String[]{"*", "/*", "/", "/some/random/path"}, null, null, null);
		//If it works, always the filter should take over and return the same string regardeless of the URL
		String expectedContent = "content is Filtered by";

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '" + expectedContent + "'",
						resp -> resp.contains(expectedContent))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-context/some/random/path");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '" + expectedContent + "'",
						resp -> resp.contains(expectedContent))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-context/some/notregistered/random/path");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '" + expectedContent + "'",
						resp -> resp.contains(expectedContent))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-context/");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '" + expectedContent + "'",
						resp -> resp.contains(expectedContent))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '" + expectedContent + "'",
						resp -> resp.contains(expectedContent))
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

	@Test
	public void testNCSALogger() throws Exception {
		testServletPath();


		final File logFile = new File("target/logs/request.log");

		if (!logFile.exists()) {
			logFile.getParentFile().mkdirs();
		}

		logger.info("Log-File: {}", logFile.getAbsoluteFile());

		assertNotNull(logFile);

		new WaitCondition("logfile") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return logFile.exists();
			}
		}.waitForCondition();

		boolean exists = logFile.getAbsoluteFile().exists();

		assertTrue(exists);

		FileInputStream fstream = new FileInputStream(logFile.getAbsoluteFile());
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = br.readLine();
		assertNotNull(strLine);
		in.close();
		fstream.close();
	}
}
