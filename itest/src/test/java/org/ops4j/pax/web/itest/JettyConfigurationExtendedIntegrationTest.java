package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests Web-Connectors and Web-VirtualHosts MANIFEST headers. Based on
 * JettyConfigurationIntegrationTest.java
 * 
 * @author Gareth Collins
 */
@RunWith(JUnit4TestRunner.class)
public class JettyConfigurationExtendedIntegrationTest extends ITestBase {

	Logger LOG = LoggerFactory
			.getLogger(JettyConfigurationExtendedIntegrationTest.class);

	private Bundle installWarBundle;

	private WebListener webListener;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("jetty-config-fragment")
						.version(getProjectVersion()).noStart());
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		// setUpITestBase();

		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class.getName(), webListener,
				null);
		String bundlePath = WEB_BUNDLE + "mvn:org.ops4j.pax.web.samples/war/"
				+ getProjectVersion() + "/war?" + WEB_CONTEXT_PATH + "=/test&"
				+ WEB_CONNECTORS + "=jettyConn1&" + WEB_VIRTUAL_HOSTS
				+ "=localhost";
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
	public void testWeb() throws Exception {
		testWebPath("http://localhost:8181/test/wc/example", 404);
	}

	@Test
	public void testWebIP() throws Exception {
		testWebPath("http://127.0.0.1:8181/test/wc/example", 404);
	}

	@Test
	public void testWebJettyIP() throws Exception {
		testWebPath("http://127.0.0.1:8282/test/wc/example", 404);
	}

	@Test
	public void testWebJetty() throws Exception {
		testWebPath("http://localhost:8282/test/wc/example",
				"<h1>Hello World</h1>");
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
