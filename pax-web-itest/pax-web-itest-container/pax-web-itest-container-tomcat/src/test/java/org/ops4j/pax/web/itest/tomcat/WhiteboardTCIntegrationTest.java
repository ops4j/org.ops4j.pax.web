package org.ops4j.pax.web.itest.tomcat;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import javax.servlet.Servlet;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.ServletMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck (anierbeck)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class WhiteboardTCIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WhiteboardTCIntegrationTest.class);
	
	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureTomcat(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(VersionUtil.getProjectVersion()));
	}

	@Before
	public void setUp() throws Exception {
		int count = 0;
		while (!testClient.checkServer("http://127.0.0.1:8282/") && count < 100) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		
		LOG.info("waiting for Server took {} ms", (count * 1000));
		
		initServletListener("jsp");

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
		testClient.testWebPath("http://127.0.0.1:8282/root", "Hello Whiteboard Extender");
	}

	@Test
	@Ignore("Failing for duplicate Context - PAXWEB-597")
	public void testWhiteBoardSlash() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/", "Welcome to the Welcome page");
	}

	@Test
	@Ignore("Failing for duplicate Context - PAXWEB-597")
	public void testWhiteBoardForbidden() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/forbidden", "", 401, false);
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/filtered", "Filter was there before");
	}

	@Test
	public void testImage() throws Exception {
		HttpResponse httpResponse = testClient.getHttpResponse(
				"http://127.0.0.1:8282/images/ops4j.png", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
	}

	@Test
	public void test404() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/doesNotExist",
				"<title>Default 404 page</title>", 404, false);
	}
	
	@Test
	public void testResourceMapping() throws Exception {
		HttpResponse httpResponse = testClient.getHttpResponse(
				"http://127.0.0.1:8282/whiteboardresources/ops4j.png", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
	}
	
	@Test
	public void testJspMapping() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/jsp/simple.jsp", "<h1>Hello World</h1>");
	}
	
	@Test
	public void testTldJsp() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/jsp/using-tld.jsp", "Hello World");
	}

	@Test
	@Ignore
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
				testClient.testWebPath("http://127.0.0.1:8282/alternative/alias",
						"Hello Whiteboard Extender");
			} finally {
				servletRegistration.unregister();
			}
		} finally {
			httpContextMappingRegistration.unregister();
		}
	}

}
