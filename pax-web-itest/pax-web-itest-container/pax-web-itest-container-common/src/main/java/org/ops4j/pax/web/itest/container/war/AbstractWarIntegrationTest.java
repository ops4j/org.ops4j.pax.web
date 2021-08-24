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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.TestServlet;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventListener;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWarIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarIntegrationTest.class);

	private Bundle wab;

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("war", () -> {
			installAndStartWebBundle("war", "/war");
		});
	}

	@Test
	public void testWC() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
	}

	@Test
	public void testImage() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8181/war/images/logo.png");
	}

	@Test
	public void testFilterInit() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
						resp -> resp.contains("Have bundle context in filter: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
	}

	@Test
	public void testStartStopBundle() throws Exception {
		LOG.debug("start/stopping bundle");

		wab.stop();
		configureAndWaitForDeployment(() -> wab.start());

		LOG.debug("Update done, testing bundle");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
						resp -> resp.contains("Have bundle context in filter: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
	}

	@Test
	public void testUpdateBundle() throws Exception {
		LOG.debug("updating bundle");

		configureAndWaitForDeployment(() -> wab.update());

		LOG.info("Update done, testing bundle");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
	}

	@Test
	public void testWebContainerExample() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/example");

		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8181/war/images/logo.png");
	}

	@Test
	public void testWebContainerSN() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/sn");
	}

	@Test
	public void testWebContainerAlias() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/alias");
	}

	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/");
	}

	@Test
	public void testSubJSP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h2>Hello World!</h2>'",
						resp -> resp.contains("<h2>Hello World!</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/subjsp");
	}

	@Test
	public void testErrorJSPCall() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/error.jsp");
	}

	@Test
	public void testWrongServlet() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wrong/");
	}

	@Test
	public void testTalkativeServlet() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Silent Servlet activated</h1>'",
						resp -> resp.contains("<h1>Silent Servlet activated</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/talkative");
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
		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);

		context.registerService(WebApplicationEventListener.class, webEvent -> {
			if (webEvent.getType() == WebApplicationEvent.State.DEPLOYED) {
				if (webEvent.getContext() != null) {
					httpContext1.set(webEvent.getContext());
					latch1.countDown();
				}
			}
		}, null);

		LOG.debug("installing war-simple war");

		Bundle simpleWar = configureAndWaitForDeploymentUnlessInstalled("war-simple", () -> {
			installAndStartWebBundle("war-simple", "/war-simple");
		});

		assertTrue(latch1.await(5, TimeUnit.SECONDS));

		LOG.debug("context registered, calling web request ...");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
						resp -> resp.contains("Hello, World, from JSP"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple");

		final HttpService httpService = getHttpService(simpleWar.getBundleContext());

		LOG.debug("... adding additional content to war");

		context.registerService(WebElementEventListener.class, webEvent -> {
			if (webEvent.getType() == WebElementEvent.State.DEPLOYED) {
				if (webEvent.getData() instanceof ServletEventData) {
					if ("/test2".equals(((ServletEventData) webEvent.getData()).getAlias())) {
						httpContext2.set(webEvent.getData().getHttpContext());
						latch2.countDown();
					}
				}
			}
		}, null);

		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());

		// register resources to different context
		// "/" will be changed to "" anyway
		// these resources will be loaded from original bundle that "created" this http context

		// here we're using "default" httpContext for "war-simple" bundle, so the default implementation is
		// org.ops4j.pax.web.service.spi.context.DefaultHttpContext and getResource() will use
		// org.osgi.framework.Bundle.getResource() (classloader access method)
		httpService.registerResources("/r1", "/static", null);
		httpService.registerResources("/r2", "static", null);
		httpService.registerResources("/r3", "/", null);
		httpService.registerResources("/r4", "", null);

		// here we're using the context taken the WAB and all its web elements, so the default implementation is
		// org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper wrapping an instance of
		// org.ops4j.pax.web.extender.war.internal.WebApplicationHelper and getResource() will use
		// org.osgi.framework.Bundle.findEntries() (non-classloader access method)
		httpService.registerResources("/r1", "/static", httpContext1.get());
		httpService.registerResources("/r2", "static", httpContext1.get());
		httpService.registerResources("/r3", "/", httpContext1.get());
		httpService.registerResources("/r4", "", httpContext1.get());
		// case when "resource name" == "context"
		httpService.registerResources("/war-simple", "/static", httpContext1.get());

		// can't replace WAR's "default" resource servlet:
		// org.osgi.service.http.NamespaceException: \
		//    ServletModel{id=ServletModel-40,name='default',alias='/',urlPatterns=[/],contexts=[{HS,OCM-23,default,/war-simple}]} \
		//    can't be registered. \
		//    ServletContextModel{id=ServletContextModel-22,contextPath='/war-simple'} already contains servlet named default: \
		//    ServletModel{id=ServletModel-24,name='default',urlPatterns=[/],contexts=[{HS,OCM-23,default,/war-simple}]}
//		httpService.registerResources("/", "/static", httpContext1.get());

		assertTrue(latch2.await(5, TimeUnit.SECONDS));

		assertSame(httpContext1.get(), httpContext2.get());

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello, World, from JSP'",
						resp -> resp.contains("Hello, World, from JSP"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple");

		// Jetty has condition: "if (_servlet == null && (_initOnStartup || isInstance()))", so dynamically
		// added servlet with the instance set is immediately initialized
//		assertFalse("Servlet.init(ServletConfig) was called", servlet2.isInitCalled());

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple/test2");

		assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());

		// resources
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static) - entry"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple/r1/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static) - entry"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple/r2/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test (ROOT) - entry"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple/r3/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test (ROOT) - entry"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple/r4/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static) - entry"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple/war-simple/readme.txt");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static) - classpath"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r1/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (static) - classpath"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r2/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test (ROOT) - classpath"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r3/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test (ROOT) - classpath"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r4/readme.txt");
		// this is interesting - because /war-simple is both a context path and resource prefix in ROOT context,
		// the longest context path prefix takes precedence
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test (ROOT) - entry"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple/readme.txt");
		// This is where I explicitly fail to comply to "140.2 The Servlet Context" of the whiteboard specification.
		// This chapter mentions two ServletContextHelpers with two paths:
		// - osgi.http.whiteboard.context.path = /foo
		// - osgi.http.whiteboard.context.path = /foo/bar
		// and a request URI http://localhost/foo/bar/someServlet is said to be resolved in this order:
		// 1. /foo/bar context looking for a pattern to match /someServlet
		// 2. /foo context looking for a pattern to match /bar/someServlet
		// However this is not correct resolution in chapter "12.1 Use of URL Paths" of Servlet API 4:
		//   Upon receipt of a client request, the Web container determines the Web application
		//   to which to forward it. The Web application selected must have the longest context
		//   path that matches the start of the request URL. The matched part of the URL is the
		//   context path when mapping to servlets.
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'registerResources test (static)'",
//						resp -> resp.contains("registerResources test (static) - classpath"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/war-simple/readme2.txt");
	}

}
