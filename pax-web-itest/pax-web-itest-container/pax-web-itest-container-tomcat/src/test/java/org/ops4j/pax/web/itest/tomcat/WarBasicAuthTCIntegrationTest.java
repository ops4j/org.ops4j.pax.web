package org.ops4j.pax.web.itest.tomcat;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
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
public class WarBasicAuthTCIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WarBasicAuthTCIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configuration() {
		return combine(
				configureTomcat(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("tomcat-auth-config-fragment")
						.version(VersionUtil.getProjectVersion()).noStart());

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-authentication/"
				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH
				+ "=/war-authentication";
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
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE
					&& b.getState() != Bundle.RESOLVED) {
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

		testWebPath("http://127.0.0.1:8282/war-authentication/wc",
				"<h1>Hello World</h1>");

	}

	@Test
	public void testWebContainerExample() throws Exception {

		testWebPath("http://127.0.0.1:8282/war-authentication/wc/example",
				null, 401, false);

		testWebPath("http://127.0.0.1:8282/war-authentication/wc/example",
				"<h1>Hello World</h1>", 200, true);

	}
	
	@Test
	public void testWebContainerAdditionalSample() throws Exception {

		testWebPath("http://127.0.0.1:8282/war-authentication/wc/additionalsample",
				null, 401, false);

		testWebPath("http://127.0.0.1:8282/war-authentication/wc/additionalsample",
				"<h1>Hello World</h1>", 200, true);

	}

	@Test
	public void testWebContainerSN() throws Exception {

		testWebPath("http://127.0.0.1:8282/war-authentication/wc/sn",
				"<h1>Hello World</h1>");

	}

	@Test
	public void testSlash() throws Exception {

		LOG.info("Starting test ...");
		testWebPath("http://127.0.0.1:8282/war-authentication/",
				"<h1>Hello World</h1>");
		LOG.info("...Done");
	}

}
