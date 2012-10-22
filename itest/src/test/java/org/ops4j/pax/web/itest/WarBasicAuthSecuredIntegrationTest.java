package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(JUnit4TestRunner.class)
public class WarBasicAuthSecuredIntegrationTest extends ITestBase {

	Logger LOG = LoggerFactory.getLogger(WarBasicAuthSecuredIntegrationTest.class);

	private Bundle installWarBundle;

	private WebListener webListener;

	@Configuration
	public static Option[] configuration() {
		return OptionUtils.combine(
				configureJetty(),
				systemProperty("org.osgi.service.http.secure.enabled").value("true"),
				systemProperty("org.ops4j.pax.web.ssl.keystore").value("src/test/resources/keystore"),
				systemProperty("org.ops4j.pax.web.ssl.password").value("password"),
				systemProperty("org.ops4j.pax.web.ssl.keypassword").value("password"),
				systemProperty("org.ops4j.pax.web.ssl.clientauthneeded").value("required"),
				mavenBundle()
					.groupId("org.ops4j.pax.web.samples")
					.artifactId("jetty-auth-config-fragment")
					.version(getProjectVersion()).noStart()
				);
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class.getName(), webListener,
				null);
		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-authentication/"
				+ getProjectVersion() + "/war?"
				+ WEB_CONTEXT_PATH + "=/war-authentication";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		int count = 0;
		while (!((WebListenerImpl) webListener).gotEvent() && count < 50) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
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
					&& b.getState() != Bundle.RESOLVED)
				fail("Bundle should be active: " + b);

			Dictionary headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null)
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			else
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
		}

	}

	@Test
	public void testWC() throws Exception {

		testWebPath("https://127.0.0.1:8443/war-authentication/wc",
				"<h1>Hello World</h1>");

	}

	@Test
	public void testWC_example() throws Exception {

		testWebPath("https://127.0.0.1:8443/war-authentication/wc/example",
				"Unauthorized", 401, false);

		testWebPath("https://127.0.0.1:8443/war-authentication/wc/example",
				"<h1>Hello World</h1>", 200, true);

	}

	@Test
	public void testWC_SN() throws Exception {

		testWebPath("https://127.0.0.1:8443/war-authentication/wc/sn",
				"<h1>Hello World</h1>");

	}

	@Test
	public void testSlash() throws Exception {

		LOG.info("Starting test ...");
		testWebPath("https://127.0.0.1:8443/war-authentication/",
				"<h1>Hello World</h1>");
		LOG.info("...Done");
	}

	private class WebListenerImpl implements WebListener {

		private boolean event = false;

		public void webEvent(WebEvent event) {
			LOG.info("Got event: " + event);
			if (event.getType() == 2)
				this.event = true;
		}

		public boolean gotEvent() {
			return event;
		}

	}

}
