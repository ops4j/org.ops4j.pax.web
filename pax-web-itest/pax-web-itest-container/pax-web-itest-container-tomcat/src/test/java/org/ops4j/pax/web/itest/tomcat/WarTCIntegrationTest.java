package org.ops4j.pax.web.itest.tomcat;

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
import org.ops4j.pax.web.itest.base.VersionUtil;
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
	public Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = WEB_BUNDLE + "mvn:org.ops4j.pax.web.samples/war/"
				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH + "=/war";
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
	@Ignore("PAXWEB-851")
	@Test
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

	@Ignore("PAXWEB-851")
	@Test
	public void testWC() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/wc", "<h1>Hello World</h1>");

	}

	@Ignore("PAXWEB-851")
	@Test
	public void testWebContainerExample() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/wc/example",
				"<h1>Hello World</h1>");

		testClient.testWebPath("http://127.0.0.1:8282/war/images/logo.png", "", 200, false);

	}

	@Ignore("PAXWEB-851")
	@Test
	public void testWebContainerSN() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/wc/sn", "<h1>Hello World</h1>");

	}

	@Ignore("PAXWEB-851")
	@Test
	public void testSlash() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/", "<h1>Error Page</h1>", 404, false);

	}

	@Ignore("PAXWEB-851")
	@Test
	public void testSubJSP() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/wc/subjsp",
				"<h2>Hello World!</h2>");

	}

	@Ignore("PAXWEB-851")
	@Test
	public void testErrorJSPCall() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/war/wc/error.jsp", "<h1>Error Page</h1>",  404, false);
	}

	@Ignore("PAXWEB-851")
	@Test
	public void testWrongServlet() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/war/wrong/", "<h1>Error Page</h1>",
				404, false);
	}
	
	@Test
	@Ignore("Occasionally fails to unknown reason")
	public void testAdditionalWar() throws Exception {
		initWebListener();

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-dispatch-jsp/"
				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH
				+ "=/war-dispatch-jsp";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		waitForWebListener();
		
		testClient.testWebPath("http://127.0.0.1:8282/war/wc", "<h1>Hello World</h1>");
		testClient.testWebPath("http://127.0.0.1:8282/war-dispatch-jsp/wc/dispatch/jsp", "<h2>Hello World!</h2>");
	}

}