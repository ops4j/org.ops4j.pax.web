package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.util.Dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Achim Nierbeck
 */
@RunWith(JUnit4TestRunner.class)
public class JettyAnnotationWebappIntegrationTest extends ITestBase {

	Logger LOG = LoggerFactory
			.getLogger(JettyAnnotationWebappIntegrationTest.class);

	private Bundle installWarBundle;

	private WebListener webListener;

	@Configuration
	public static Option[] configurationDetailed() {
		return options(mavenBundle().groupId("org.ops4j.pax.web.samples")
				.artifactId("jetty-auth-config-fragment")
				.version("2.0.0-SNAPSHOT"));
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class.getName(), webListener,
				null);
		String bundlePath = WEB_BUNDLE
				+ "mvn:org.mortbay.jetty/test-annotation-webapp/8.0.0.M2/war?"
				+ WEB_CONTEXT_PATH + "=/test-annotation-webapp";
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
			if (b.getState() != Bundle.ACTIVE && b.getState() != Bundle.RESOLVED)
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
	public void testLoginPage() throws Exception {

		testWebPath("http://127.0.0.1:8181/test-annotation-webapp/login.html", "<H1> Enter your username and password to login </H1>");

	}

	@Test
	public void testLoginPageDoLogin() throws Exception {

		testWebPath("http://127.0.0.1:8181/test-annotation-webapp/login.html", "<H1> Enter your username and password to login </H1>", 200, false );
		
//		testWebPath("http://127.0.0.1:8181/test-annotation-webapp/j_security_check", "role", 200, true);

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
