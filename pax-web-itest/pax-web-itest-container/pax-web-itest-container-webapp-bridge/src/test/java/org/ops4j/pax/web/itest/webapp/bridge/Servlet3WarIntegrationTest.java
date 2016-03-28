package org.ops4j.pax.web.itest.webapp.bridge;

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
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
//@Ignore("PAXWEB-974")
public class Servlet3WarIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(Servlet3WarIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
		System.out.println("Configuring Test Bridge");
		return configureBridge();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/helloworld-servlet3/"
				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH + "=/war";

		installWarBundle = installAndStartBundle(bundlePath);

		System.out.println("Waiting for deployment to finish...");
		Thread.sleep(10000); // let the web.xml parser finish his job
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
	public void testWC() throws Exception {

		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/hello", "<h1>Hello World</h1>");

	}

	@Test
	@Ignore("PAXWEB-972")
	public void testFilterInit() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/hello/filter", "Have bundle context in filter: true");
	}
	
	@Test
	public void testDuplicateDefinitionServlet() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/duplicate", "<h1>Duplicate Servlet</h1>");
	}
	
	@Test
	public void testMimeImage() throws Exception {
		testWC();

		HttpResponse httpResponse = testClient.getHttpResponse(
				"http://localhost:9080/Pax-Exam-Probe/war/images/logo.png", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
	}

	@Test
	@Ignore("PAXWEB-978")
	public void testMimeStyle() throws Exception {
		testWC();

		HttpResponse httpResponse = testClient.getHttpResponse(
				"http://localhost:9080/Pax-Exam-Probe/war/css/content.css", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("text/css", header.getValue());
	}
	
	@Test
	@Ignore("PAXWEB-973")
	public void testWrongServlet() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wrong/", "<h1>Error Page</h1>", 404, false);
	}
}
