package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarTCIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WarTCIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = WEB_BUNDLE + "mvn:org.ops4j.pax.web.samples/war/"
				+ getProjectVersion() + "/war?" + WEB_CONTEXT_PATH + "=/war";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

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
	@Ignore
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE) {
				fail("Bundle should be active: " + b);
			}

			Dictionary<String, String> headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

	}

	@Test
	public void testWC() throws Exception {

		testWebPath("http://127.0.0.1:8282/war/wc", "<h1>Hello World</h1>");

	}

	@Test
	@Ignore
	public void testWC_example() throws Exception { //CHECKSTYLE:SKIP

		testWebPath("http://127.0.0.1:8282/war/wc/example",
				"<h1>Hello World</h1>");

		testWebPath("http://127.0.0.1:8282/war/images/logo.png", "", 200, false);

	}

	@Test
	@Ignore
	public void testWC_SN() throws Exception { //CHECKSTYLE:SKIP

		testWebPath("http://127.0.0.1:8282/war/wc/sn", "<h1>Hello World</h1>");

	}

	@Test
	@Ignore
	public void testSlash() throws Exception {

		testWebPath("http://127.0.0.1:8282/war/", "<h1>Error Page</h1>", 404,
				false);

	}

	@Test
	@Ignore
	public void testSubJSP() throws Exception {

		testWebPath("http://127.0.0.1:8282/war/wc/subjsp",
				"<h2>Hello World!</h2>");

	}

	@Test
	@Ignore
	public void testErrorJSPCall() throws Exception {
		testWebPath("http://127.0.0.1:8282/war/wc/error.jsp",
				"<h1>Error Page</h1>", 404, false);
	}

	@Test
	@Ignore
	public void testWrongServlet() throws Exception {
		testWebPath("http://127.0.0.1:8282/war/wrong/", "<h1>Error Page</h1>",
				404, false);
	}

}