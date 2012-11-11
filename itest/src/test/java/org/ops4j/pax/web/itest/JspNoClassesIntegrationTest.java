package org.ops4j.pax.web.itest;

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
 * Regression test for PAXWEB-409.
 * 
 * @author Harald Wellmann
 */
@RunWith(JUnit4TestRunner.class)
public class JspNoClassesIntegrationTest extends ITestBase {

	Logger LOG = LoggerFactory.getLogger(JspNoClassesIntegrationTest.class);

	private Bundle installWarBundle;
	
	private WebListener webListener;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}


	@Before
	public void setUp() throws BundleException, InterruptedException {
		
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class.getName(), webListener,
				null);

		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-jsp-noclasses/" + getProjectVersion();
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		while (installWarBundle.getState() != Bundle.ACTIVE) {
			this.wait(100);
		}
		
		int count = 0;
		while (!((WebListenerImpl) webListener).gotEvent() && count < 100) {
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

	@Test
	public void testSimpleJsp() throws Exception {

		testWebPath("http://localhost:8181/jspnc/welcome.jsp", "Welcome");
			
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
