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

package org.ops4j.pax.web.itest.container.war;

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWarIntegrationTest extends AbstractContainerTestBase {

//	protected static final String TEST_BUNDLE_SYMBOLIC_NAME = "test-bundle";
//
//	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarIntegrationTest.class);
//
//	private Bundle installWarBundle;
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		LOG.info("Setting up test");
//
//		initWebListener();
//
//		String bundlePath = WEB_BUNDLE
//				+ "mvn:org.ops4j.pax.web.samples/war/"
//				+ VersionUtil.getProjectVersion() + "/war?"
//				+ WEB_CONTEXT_PATH + "=/war";
//		installWarBundle = bundleContext.installBundle(bundlePath);
//		installWarBundle.start();
//
//		waitForWebListener();
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//		if (installWarBundle != null) {
//			installWarBundle.stop();
//			installWarBundle.uninstall();
//		}
//	}
//
//	@Test
//	public void testWC() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
//	}
//
//	@Test
//	public void testImage() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/images/logo.png");
//	}
//
//	@Test
//	public void testFilterInit() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
//						resp -> resp.contains("Have bundle context in filter: true"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
//	}
//
//	@Test
//	public void testStartStopBundle() throws Exception {
//		LOG.debug("start/stopping bundle");
//		initWebListener();
//
//		initServletListener(null);
//
//		installWarBundle.stop();
//
//		installWarBundle.start();
//
//		waitForWebListener();
//		waitForServletListener();
//		LOG.debug("Update done, testing bundle");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
//						resp -> resp.contains("Have bundle context in filter: true"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
//	}
//
//	@Test
//	public void testUpdateBundle() throws Exception {
//		LOG.debug("updating bundle");
//		initWebListener();
//
//		initServletListener(null);
//
//		installWarBundle.update();
//
//		waitForWebListener();
//		waitForServletListener();
//		LOG.info("Update done, testing bundle");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
//	}
//
//	@Test
//	public void testWebContainerExample() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/example");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/images/logo.png");
//	}
//
//	@Test
//	public void testWebContainerSN() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/sn");
//	}
//
//	@Test
//	public void testWebContainerAlias() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/alias");
//	}
//
//	/**
//	 * Note: Undertow (in current pax-web-undertow implementation) doesn't use custom (servlet) error pages
//	 * for HTTP codes returned from {@code io.undertow.server.handlers.resource.ResourceHandler}. So even
//	 * if we get HTTP 403, we won't get our custom error page.
//	 * @throws Exception
//	 */
//	@Test
//	public void testSlash() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(403)
////				.withResponseAssertion("Response must contain '<h1>Error 403 Page</h1>'",
////						resp -> resp.contains("<h1>Error 403 Page</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/");
//	}
//
//	@Test
//	public void testSubJSP() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h2>Hello World!</h2>'",
//						resp -> resp.contains("<h2>Hello World!</h2>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/subjsp");
//	}
//
//	@Test
//	public void testErrorJSPCall() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(404)
//				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
//						resp -> resp.contains("<h1>Error Page</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/error.jsp");
//	}
//
//	@Test
//	public void testWrongServlet() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(404)
//				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
//						resp -> resp.contains("<h1>Error Page</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wrong/");
//	}
//
//	@Test
//	public void testTalkativeServlet() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Silent Servlet activated</h1>'",
//						resp -> resp.contains("<h1>Silent Servlet activated</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/talkative");
//	}
//
////	/**
////	 * This test registers a servlet to a already configured web context created
////	 * by the war extender.
////	 * It checks that Servlet.init() was called after the invocation of
////	 * registerServlet() and that the servlet uses the same http context that
////	 * the webapp uses.
////	 */
////	@Test
////	public void testRegisterServletToWarContext() throws Exception {
////		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<>();
////		bundleContext.registerService(WebListener.class, webEvent -> {
////			if (webEvent.getType() == WebEvent.DEPLOYED) {
////				httpContext1.set(webEvent.getHttpContext());
////			}
////		}, null);
////
////		LOG.debug("installing war-simple war");
////
////		String bundlePath = WEB_BUNDLE
////				+ "mvn:org.ops4j.pax.web.samples/war-simple/"
////				+ VersionUtil.getProjectVersion()
////				+ "/war?"
////				+ WEB_CONTEXT_PATH
////				+ "=/war";
////		Bundle installWarBundle = installAndStartBundle(bundlePath);
////
////		for (int count = 0; count < 100; count++) {
////			if (httpContext1.get() == null) {
////				Thread.sleep(100);
////			}
////		}
////		if (httpContext1.get() == null) {
////			Assert.fail("Timout waiting for web event");
////		}
////
////		LOG.debug("context registered, calling web request ...");
////
////		HttpTestClientFactory.createDefaultTestClient()
////				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
////						resp -> resp.contains("Hello, World, from JSP"))
////				.doGETandExecuteTest("http://127.0.0.1:8181/war");
////
////		// ---
////
////		final HttpService httpService = getHttpService(installWarBundle.getBundleContext());
////
////		LOG.debug("... adding additional content to war");
////
////		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<>();
////		bundleContext.registerService(ServletListener.class, servletEvent -> {
////			if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
////				httpContext2.set(servletEvent.getHttpContext());
////			}
////		}, null);
////
////		TestServlet servlet2 = new TestServlet();
////		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());
////
////		// register resources to different context
////		// "/" will be changed to "" anyway
////		// without more changes, these resources will be loaded from original bundle that "created" this http context
////		httpService.registerResources("/r1", "/static", httpContext1.get());
////		httpService.registerResources("/r2", "static", httpContext1.get());
////		httpService.registerResources("/r3", "/", httpContext1.get());
////		httpService.registerResources("/r4", "", httpContext1.get());
////		// case when "resource name" == "context"
////		httpService.registerResources("/war", "/static", httpContext1.get());
////		// can't replace WAR's "default" resource servlet:
////		// "org.osgi.service.http.NamespaceException: alias: '/war' is already in use in this or another context"
////		//		httpService.registerResources("/", "/static", httpContext1.get());
////
////		if (isInitEager()) {
////			Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
////		}
////
////		for (int count = 0; count < 100; count++) {
////			if (httpContext2.get() == null) {
////				Thread.sleep(100);
////			}
////		}
////		if (httpContext2.get() == null) {
////			Assert.fail("Timout waiting for servlet event");
////		}
////
////		Assert.assertSame(httpContext1.get(), httpContext2.get());
////
////		HttpTestClientFactory.createDefaultTestClient()
////				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
////						resp -> resp.contains("Hello, World, from JSP"))
////				.doGETandExecuteTest("http://127.0.0.1:8181/war");
////
////		HttpTestClientFactory.createDefaultTestClient()
////				.withResponseAssertion("Response must contain 'TEST OK'",
////						resp -> resp.contains("TEST OK"))
////				.doGETandExecuteTest("http://127.0.0.1:8181/war/test2");
////
////		// resources
////		HttpTestClientFactory.createDefaultTestClient()
////				.withResponseAssertion("Response must contain 'registerResources test (static)'",
////						resp -> resp.contains("registerResources test"))
////				.doGETandExecuteTest("http://127.0.0.1:8181/war/r1/readme.txt");
////		HttpTestClientFactory.createDefaultTestClient()
////				.withResponseAssertion("Response must contain 'registerResources test (static)'",
////						resp -> resp.contains("registerResources test"))
////				.doGETandExecuteTest("http://127.0.0.1:8181/war/r2/readme.txt");
////		HttpTestClientFactory.createDefaultTestClient()
////				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
////						resp -> resp.contains("registerResources test"))
////				.doGETandExecuteTest("http://127.0.0.1:8181/war/r3/readme.txt");
////		HttpTestClientFactory.createDefaultTestClient()
////				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
////						resp -> resp.contains("registerResources test"))
////				.doGETandExecuteTest("http://127.0.0.1:8181/war/r4/readme.txt");
////		HttpTestClientFactory.createDefaultTestClient()
////				.withResponseAssertion("Response must contain 'registerResources test (static)'",
////						resp -> resp.contains("registerResources test"))
////				.doGETandExecuteTest("http://127.0.0.1:8181/war/war/readme.txt");
////	}

}
