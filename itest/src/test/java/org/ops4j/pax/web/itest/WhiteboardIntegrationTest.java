package org.ops4j.pax.web.itest;

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

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard/"
				+ getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
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
		testWebPath("http://127.0.0.1:8181/root", "Hello Whiteboard Extender");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		testWebPath("http://127.0.0.1:8181/", "Welcome to the Welcome page");
	}

	@Test
	public void testWhiteBoardForbidden() throws Exception {
		testWebPath("http://127.0.0.1:8181/forbidden", "", 401, false);
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		testWebPath("http://127.0.0.1:8181/filtered", "Filter was there before");
	}

	@Test
	public void testImage() throws Exception {
		HttpResponse httpResponse = getHttpResponse(
				"http://127.0.0.1:8181/images/ops4j.png", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
	}

	@Test
	public void test404() throws Exception {
		testWebPath("http://127.0.0.1:8181/doesNotExist",
				"<title>Default 404 page</title>", 404, false);
	}
	
	@Test
	public void testResourceMapping() throws Exception {
		HttpResponse httpResponse = getHttpResponse(
				"http://127.0.0.1:8181/whiteboardresources/ops4j.png", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
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
				testWebPath("http://127.0.0.1:8181/alternative/alias",
						"Hello Whiteboard Extender");
			} finally {
				servletRegistration.unregister();
			}
		} finally {
			httpContextMappingRegistration.unregister();
		}
	}

}
