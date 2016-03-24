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
 package org.ops4j.pax.web.itest.karaf;

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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

import javax.servlet.Servlet;

@RunWith(PaxExam.class)
public class WhiteboardKarafTest extends KarafBaseTest {

	private Bundle installWarBundle;

	@Configuration
	public Option[] config() {
		return jettyConfig();
	}
	
	@Before
	public void setUp() throws Exception {

		initWebListener();

		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard-blueprint/"
				+ getProjectVersion();
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();
		
		int failCount = 0;
		while (installWarBundle.getState() != Bundle.ACTIVE) {
			Thread.sleep(500);
			if (failCount > 500)
				throw new RuntimeException(
						"Required whiteboard-blueprint bundle is never active");
			failCount++;
		}
		waitForWebListener();

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
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf using Whiteboard-Extender!",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must be served from welcome-page!",
						resp -> resp.contains("Welcome to the Welcome page"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

	@Test
	public void testWhiteBoardForbidden() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(401)
				.doGETandExecuteTest("http://127.0.0.1:8181/forbidden");
	}

	@Test
	public void testWhiteboardFiltered() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must be served from with message from Whiteboard-Filter!",
						resp -> resp.contains("Filter was there before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/filtered");
	}

	@Test
	public void testImage() throws Exception {
		createTestClientForKaraf()
				.withResponseHeaderAssertion("ContentType for image must be 'image/png'",
						headers -> headers.anyMatch(header ->  header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/images/ops4j.png");
	}

	@Test
	public void test404() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Respone must be served from Default 404 page",
						resp -> resp.contains("<title>Default 404 page</title>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/doesNotExist");
	}
	
	@Test
	public void testResourceMapping() throws Exception {
		createTestClientForKaraf()
				.withResponseHeaderAssertion("ContentType for image must be 'image/png'",
						headers -> headers.anyMatch(header ->  header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboardresources/ops4j.png");
	}
	
	@Test
	public void testJspMapping() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jsp/simple.jsp");
	}
	
	@Test
	public void testTldJsp() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jsp/using-tld.jsp");
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
				createTestClientForKaraf()
						.withResponseAssertion("Response must contain text served by Karaf using Whiteboard-Extender Alias!",
								resp -> resp.contains("Hello Whiteboard Extender"))
						.doGETandExecuteTest("http://127.0.0.1:8181/alternative/alias");
			} finally {
				servletRegistration.unregister();
			}
		} finally {
			httpContextMappingRegistration.unregister();
		}
	}
}
