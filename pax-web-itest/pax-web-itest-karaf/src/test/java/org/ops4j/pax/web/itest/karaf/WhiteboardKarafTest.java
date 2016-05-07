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

import static org.junit.Assert.assertEquals;

import javax.servlet.Servlet;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class WhiteboardKarafTest extends KarafBaseTest {

	private static Logger LOG = LoggerFactory.getLogger(WhiteboardKarafTest.class);

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

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			System.out.println("Bundle " + b.getBundleId() + " : "
					+ b.getSymbolicName());
		}

	}

	@Test
	public void testWhiteBoardRoot() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/root", "Hello Whiteboard Extender");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/", "Welcome to the Welcome page");
	}

	@Test
	public void testWhiteBoardForbidden() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/forbidden", "", 401, false);
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/filtered", "Filter was there before");
	}

	@Test
	public void testImage() throws Exception {
		HttpResponse httpResponse = testClient.getHttpResponse(
				"http://127.0.0.1:8181/images/ops4j.png", false, null, false);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
	}

	@Test
	public void test404() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/doesNotExist",
				"<title>Default 404 page</title>", 404, false);
	}
	
	@Test
	public void testResourceMapping() throws Exception {
		HttpResponse httpResponse = testClient.getHttpResponse(
				"http://127.0.0.1:8181/whiteboardresources/ops4j.png", false, null, false);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
	}
	
	@Test
	public void testJspMapping() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/jsp/simple.jsp", "<h1>Hello World</h1>");
	}
	
	@Test
	public void testTldJsp() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/jsp/using-tld.jsp", "Hello World");
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
				testClient.testWebPath("http://127.0.0.1:8181/alternative/alias",
						"Hello Whiteboard Extender");
			} finally {
				servletRegistration.unregister();
			}
		} finally {
			httpContextMappingRegistration.unregister();
		}
	}
}
