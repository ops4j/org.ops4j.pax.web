package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;

import java.util.Dictionary;

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
 * @author Achim Nierbeck
 */
@RunWith(JUnit4TestRunner.class)
public class MultipleWarIntegrationTest extends ITestBase {

 Logger LOG = LoggerFactory.getLogger(MultipleWarIntegrationTest.class);

//	private Bundle installWarBundle;

	private WebListener webListener;
	
	@Configuration
	public static Option[] configure() {
		return baseConfigure();
	}


	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class.getName(), webListener,
				null);
	}

	@After
	public void tearDown() throws BundleException {
//		if (installWarBundle != null) {
//			installWarBundle.stop();
//			installWarBundle.uninstall();
//		}
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() throws Exception {
		
		String bundlePath = WEB_BUNDLE
			+ "mvn:org.ops4j.pax.web.samples/war/1.1.0-SNAPSHOT/war?"
			+ WEB_CONTEXT_PATH + "=/war";
		
		String bundleBasicAuthPath = WEB_BUNDLE
			+ "mvn:org.ops4j.pax.web.samples/war-authentication/1.1.0-SNAPSHOT/war?"
			+ WEB_CONTEXT_PATH + "=/war-authentication";
		
		long tStamp1 = System.currentTimeMillis();
		
		Bundle installWarBundle = bundleContext.installBundle(bundlePath);
		
		LOG.info("Deployed Bundle 1 in "+(System.currentTimeMillis() - tStamp1)+" ms.");
		long tStamp2 = System.currentTimeMillis();
		
		Bundle installBasicAuthBundle = bundleContext.installBundle(bundleBasicAuthPath);
		LOG.info("Deployed Bundle 2 in "+(System.currentTimeMillis() - tStamp2)+" ms.");

		installWarBundle.start();
		installBasicAuthBundle.start();
		
		LOG.info("Started 2 Bundles in "+(System.currentTimeMillis() - tStamp1)+" ms.");
		
		int count = 0;
		while (!((WebListenerImpl) webListener).gotEvent() && count < 50) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}

		
		
		for (Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE)
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
