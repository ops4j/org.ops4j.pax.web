package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
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
 * Tests default virtual host and connector configuration for web apps Based on
 * JettyConfigurationIntegrationTest.java
 * 
 * @author Gareth Collins
 */
@RunWith(PaxExam.class)
public class JettyConfigurationExtendedTwoIntegrationTest extends ITestBase {

	Logger LOG = LoggerFactory
			.getLogger(JettyConfigurationExtendedTwoIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
				.artifactId("jetty-config-fragment")
				.version(getProjectVersion()).noStart(),
				systemProperty("org.ops4j.pax.web.default.virtualhosts").value(
						"127.0.0.1"),
						systemProperty("org.ops4j.pax.web.default.connectors").value(
								"default"));

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		final String bundlePath = WEB_BUNDLE + "mvn:org.ops4j.pax.web.samples/war/"
				+ getProjectVersion() + "/war?" + WEB_CONTEXT_PATH + "=/test";
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
		for (final Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE
					&& b.getState() != Bundle.RESOLVED) {
				fail("Bundle should be active: " + b);
			}

			final Dictionary headers = b.getHeaders();
			final String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
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
	public void testWeb() throws Exception {
		testWebPath("http://localhost:8181/test/wc/example", 404);
	}

	@Test
	public void testWebIP() throws Exception {
		testWebPath("http://127.0.0.1:8181/test/wc/example",
				"<h1>Hello World</h1>");
	}

	@Test
	public void testWebJettyIP() throws Exception {
		testWebPath("http://127.0.0.1:8282/test/wc/example", 404);
	}

	@Test
	public void testWebJetty() throws Exception {
		testWebPath("http://localhost:8282/test/wc/example", 404);
	}
}
