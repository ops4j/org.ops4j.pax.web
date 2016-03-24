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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.ServletMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

import javax.servlet.Servlet;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureUndertow();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}


	@Test
	public void testWhiteBoardRoot() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");

//		testClient.testWebPath("http://127.0.0.1:8181/root", "Hello Whiteboard Extender");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to the Welcome page'",
						resp -> resp.contains("Welcome to the Welcome page"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");

//		testClient.testWebPath("http://127.0.0.1:8181/", "Welcome to the Welcome page");
	}

	@Test
	public void testWhiteBoardForbidden() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.doGETandExecuteTest("http://127.0.0.1:8181/forbidden");

//		testClient.testWebPath("http://127.0.0.1:8181/forbidden", "", 401, false);
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter was there before'",
						resp -> resp.contains("Filter was there before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/filtered");

//		testClient.testWebPath("http://127.0.0.1:8181/filtered", "Filter was there before");
	}

	@Test
	public void testWhiteBoardSecondFilter() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Filter was there before"))
				.withResponseAssertion("Response must contain 'SecondFilter - filtered'",
						resp -> resp.contains("SecondFilter - filtered"))
				.doGETandExecuteTest("http://127.0.0.1:8181/second");

//		testClient.testWebPath("http://127.0.0.1:8181/second", "Filter was there before");
//		testClient.testWebPath("http://127.0.0.1:8181/second", "SecondFilter - filtered");
	}
	
	@Test
	public void testWhiteBoardFilteredInitialized() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
						resp -> resp.contains("Have bundle context in filter: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/filtered");

//		testClient.testWebPath("http://127.0.0.1:8181/filtered", "Have bundle context in filter: true");
	}

	@Test
	public void testImage() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Header 'Content-Type' must be 'image/png'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/images/ops4j.png");

//		HttpResponse httpResponse = testClient.getHttpResponse(
//				"http://127.0.0.1:8181/images/ops4j.png", false, null, false);
//		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
//		assertEquals("image/png", header.getValue());
	}

	@Test
	public void test404() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain '<title>Default 404 page</title>'",
						resp -> resp.contains("<title>Default 404 page</title>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/doesNotExist");

//		testClient.testWebPath("http://127.0.0.1:8181/doesNotExist",
//				"<title>Default 404 page</title>", 404, false);
	}
	
	@Test
	public void testResourceMapping() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Header 'Content-Type' must be 'image/png'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboardresources/ops4j.png");

//		HttpResponse httpResponse = testClient.getHttpResponse(
//				"http://127.0.0.1:8181/whiteboardresources/ops4j.png", false, null, false);
//		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
//		assertEquals("image/png", header.getValue());
	}
	
	@Test
	public void testJspMapping() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jsp/simple.jsp");

//		testClient.testWebPath("http://127.0.0.1:8181/jsp/simple.jsp", "<h1>Hello World</h1>");
	}
	
	@Test
	public void testTldJsp() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jsp/using-tld.jsp");

//		testClient.testWebPath("http://127.0.0.1:8181/jsp/using-tld.jsp", "Hello World");
	}

	@Test
	public void testMultipleContextMappings() throws Exception {
		BundleContext bundleContext = installWarBundle.getBundleContext();
		DefaultHttpContextMapping httpContextMapping = new DefaultHttpContextMapping();
		httpContextMapping.setHttpContextId("alternative");
		httpContextMapping.setPath("alternative");
		ServiceRegistration<HttpContextMapping> httpContextMappingRegistration = bundleContext
				.registerService(HttpContextMapping.class,
						httpContextMapping, null);
		try {
			Servlet servlet = new WhiteboardServlet("/alias");
			DefaultServletMapping servletMapping = new DefaultServletMapping();
			servletMapping.setServlet(servlet);
			servletMapping.setAlias("/alias");
			String httpContextId = httpContextMapping.getHttpContextId();
			servletMapping.setHttpContextId(httpContextId);
			ServiceRegistration<ServletMapping> servletRegistration = bundleContext
					.registerService(ServletMapping.class,
							servletMapping, null);
			try {
				HttpTestClientFactory.createDefaultTestClient()
						.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
								resp -> resp.contains("Hello Whiteboard Extender"))
						.doGETandExecuteTest("http://127.0.0.1:8181/alternative/alias");

//				testClient.testWebPath("http://127.0.0.1:8181/alternative/alias",
//						"Hello Whiteboard Extender");
			} finally {
				servletRegistration.unregister();
			}
		} finally {
			httpContextMappingRegistration.unregister();
		}
	}

}
