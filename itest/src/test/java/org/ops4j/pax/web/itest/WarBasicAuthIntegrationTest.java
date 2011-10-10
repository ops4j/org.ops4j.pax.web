package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.util.Dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.logging.Log;
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
public class WarBasicAuthIntegrationTest extends ITestBase {

 Logger LOG = LoggerFactory.getLogger(WarBasicAuthIntegrationTest.class);

	private Bundle installWarBundle;

	private WebListener webListener;
	
	@Configuration
	    public static Option[] configurationDetailed()
	    {
	        return options(
	        		mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("jetty-auth-config-fragment").version(getProjectVersion())
	        );
	    }


	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");
		
//		String fragmentPath = "mvn:org.ops4j.pax.web.samples/jetty-auth-config-fragment/1.1.0-SNAPSHOT";
//		Bundle fragmentBundle = bundleContext.installBundle(fragmentPath);
//		
//		Bundle[] bundles = bundleContext.getBundles();
//		for (Bundle bundle : bundles) {
//			if (bundle.getSymbolicName().equalsIgnoreCase("org.ops4j.pax.web.pax-web-jetty-bundle")) {
//				bundle.update();
//			}
//		}
		
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
	public void testWC() throws Exception {
		
		testWebPath("http://127.0.0.1:8181/war-authentication/wc", "<h1>Hello World</h1>");
			
	}


	@Test
	public void testWC_example() throws Exception {

			
		testWebPath("http://127.0.0.1:8181/war-authentication/wc/example", "Unauthorized", 401, false );
		
		testWebPath("http://127.0.0.1:8181/war-authentication/wc/example", "<h1>Hello World</h1>", 200, true);
			
	}

	@Test
	public void testWC_SN() throws Exception {

			
		testWebPath("http://127.0.0.1:8181/war-authentication/wc/sn", "<h1>Hello World</h1>");

	}
	
	@Test
	public void testSlash() throws Exception {

		LOG.info("Starting test ...");
		testWebPath("http://127.0.0.1:8181/war-authentication/", "<h1>Hello World</h1>");
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
