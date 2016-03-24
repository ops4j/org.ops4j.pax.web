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
 package org.ops4j.pax.web.itest.jetty;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * Tests default virtual host and connector configuration for web apps Based on
 * JettyConfigurationIntegrationTest.java
 * 
 * @author Gareth Collins
 */
@RunWith(PaxExam.class)
public class JettyConfigurationExtendedTwoIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(JettyConfigurationExtendedTwoIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("jetty-config-fragment")
						.version(VersionUtil.getProjectVersion()).noStart(),
				systemProperty("org.ops4j.pax.web.default.virtualhosts").value(
						"127.0.0.1"),
				systemProperty("org.ops4j.pax.web.default.connectors").value(
						"default"));

	}

	@Before
	public void setUp() throws BundleException, InterruptedException, ServletException, NamespaceException {
		LOG.info("Setting up test");

		initWebListener();

		final String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war/" + VersionUtil.getProjectVersion()
				+ "/war?" + WEB_CONTEXT_PATH + "=/test";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		waitForWebListener();
		
		HttpService httpService = getHttpService(bundleContext);
		
		initServletListener(null);

		TestServlet servlet = new TestServlet();
		httpService.registerServlet("/test2", servlet, null, null);
		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet.isInitCalled());
		
		waitForServletListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
		HttpService httpService = getHttpService(bundleContext);
		httpService.unregister("/test2");
	}


	// It should work if the port == 8181 (it appears that if connector is set virtual host is ignored)
	@Test
	public void testWeb() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8181/test/wc/example");

//		testClient.testWebPath("http://localhost:8181/test/wc/example", "<h1>Hello World</h1>");
	}

	@Test
	public void testWebIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test/wc/example");

//		testClient.testWebPath("http://127.0.0.1:8181/test/wc/example",
//				"<h1>Hello World</h1>");
	}

	@Test
	public void testWebJettyIP_wrongPort() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8282/test/wc/example");

//		testClient.testWebPath("http://127.0.0.1:8282/test/wc/example", 404);
	}

	@Test
	public void testWebJetty_wrongPort() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/test/wc/example");

//		testClient.testWebPath("http://localhost:8282/test/wc/example", 404);
	}
	
	
	@Test
	public void testHttpService() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://localhost:8181/test2");

//		testClient.testWebPath("http://localhost:8181/test2", "TEST OK");
	}
	
	@Test
	public void testHttpServiceIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2");

//		testClient.testWebPath("http://127.0.0.1:8181/test2", "TEST OK");
	}
	
	@Test
	public void testHttpServiceJettyIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8282/test2");

//		testClient.testWebPath("http://127.0.0.1:8282/test2", 404);
	}
	
	@Test
	public void testHttpServiceJetty() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/test2");

//		testClient.testWebPath("http://localhost:8282/test2", 404);
	}
}
