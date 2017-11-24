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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OldHttpServiceIntegrationTest extends ITestBase {

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
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r1/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (static)'",
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r2/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r3/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/r4/readme.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'registerResources test (ROOT)'",
						resp -> resp.contains("registerResources test"))
				.doGETandExecuteTest("http://127.0.0.1:8181/readme.txt");

		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());

		Assert.assertSame(httpContext1.get(), httpContext2.get());
		Assert.assertSame(servlet1.getServletContext(), servlet2.getServletContext());
	}
}
