package org.ops4j.pax.web.itest.webapp.bridge;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 */
@RunWith(PaxExam.class)
@Ignore("PAXWEB-975")
public class WhiteboardIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
		System.out.println("Configuring Test Bridge");
		return configureBridge();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		System.out.println("Waiting for deployment to finish...");
		Thread.sleep(10000); // let the web.xml parser finish his job
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (Bundle b : getBundleContext().getBundles()) {
			Dictionary<String,String> headers = b.getHeaders();

			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath + " ("+b.getState()+")");
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " ("+b.getState()+")");
			}
		}
	}

	@Test
	public void testWhiteBoardRoot() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/root", "Hello Whiteboard Extender");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/", "Welcome to the Welcome page");
	}

	@Test
	public void testWhiteBoardForbidden() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/forbidden", "", 401, false);
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/filtered", "Filter was there before");
	}

	@Test
	public void testWhiteBoardSecondFilter() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/second", "Filter was there before");
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/second", "SecondFilter - filtered");
	}
	
	@Test
	public void testWhiteBoardFilteredInitialized() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/filtered", "Have bundle context in filter: true");
	}

	@Test
	public void testImage() throws Exception {
		HttpResponse httpResponse = testClient.getHttpResponse(
				"http://localhost:9080/Pax-Exam-Probe/images/ops4j.png", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
	}

	@Test
	public void test404() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/doesNotExist",
				"<title>Default 404 page</title>", 404, false);
	}
	
	@Test
	public void testResourceMapping() throws Exception {
		HttpResponse httpResponse = testClient.getHttpResponse(
				"http://localhost:9080/Pax-Exam-Probe/whiteboardresources/ops4j.png", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
	}
	
	@Test
	public void testJspMapping() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/jsp/simple.jsp", "<h1>Hello World</h1>");
	}
	
	@Test
	public void testTldJsp() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/jsp/using-tld.jsp", "Hello World");
	}

//	@Test
//	public void testMultipleContextMappings() throws Exception {
//		BundleContext bundleContext = installWarBundle.getBundleContext();
//		DefaultHttpContextMapping httpContextMapping = new DefaultHttpContextMapping();
//		httpContextMapping.setHttpContextId("alternative");
//		httpContextMapping.setPath("alternative");
//		ServiceRegistration<HttpContextMapping> httpContextMappingRegistration = bundleContext
//				.registerService(HttpContextMapping.class,
//						httpContextMapping, null);
//		try {
//			Servlet servlet = new WhiteboardServlet("/alias");
//			DefaultServletMapping servletMapping = new DefaultServletMapping();
//			servletMapping.setServlet(servlet);
//			servletMapping.setAlias("/alias");
//			String httpContextId = httpContextMapping.getHttpContextId();
//			servletMapping.setHttpContextId(httpContextId);
//			ServiceRegistration<ServletMapping> servletRegistration = bundleContext
//					.registerService(ServletMapping.class,
//							servletMapping, null);
//			try {
//				testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/alternative/alias",
//						"Hello Whiteboard Extender");
//			} finally {
//				servletRegistration.unregister();
//			}
//		} finally {
//			httpContextMappingRegistration.unregister();
//		}
//	}

}
