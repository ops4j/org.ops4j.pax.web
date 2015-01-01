package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.fail;

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
public class WarManualIntegrationTest extends KarafBaseTest {

	private static final Logger LOG = LoggerFactory.getLogger(WarManualIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public Option[] config() {
		return jettyConfig();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");
		
		initWebListener();
		
		String bundlePath = "webbundle:mvn:org.ops4j.pax.web/pax-web-manual/"
				+ VersionUtil.getProjectVersion() + "/war?Web-ContextPath=/pax-web-manual";
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
			if (b.getState() != Bundle.ACTIVE) {
				fail("Bundle should be active: " + b);
			}

			Dictionary<String,String> headers = b.getHeaders();
			String ctxtPath = (String) headers.get("Web-ContextPath");
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
	public void testManual() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/pax-web-manual", "<title>Pax Web</title>");
			
	}

}

