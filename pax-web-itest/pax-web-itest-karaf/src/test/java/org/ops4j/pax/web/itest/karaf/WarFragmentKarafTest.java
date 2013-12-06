package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class WarFragmentKarafTest extends KarafBaseTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(WarFragmentKarafTest.class);
	
	private WebListenerImpl webListener;
	private Bundle warBundle, fragmentBundle;

	@Configuration
	public Option[] config() {
		return jettyConfig();
	}
	
	@Test
	public void test() throws Exception {
		Thread.sleep(4000);
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-war")));
	}
	
	@Test
	public void testWC() throws Exception {
		testWebPath("http://127.0.0.1:8181/war/wc", "<h1>Hello World</h1>");
	}

	@Test
	public void testFilterInit() throws Exception {
		testWebPath("http://127.0.0.1:8181/war/wc", "Have bundle context in filter: true");
	}
	
	@Test
	public void testWebContainerExample() throws Exception {
		testWebPath("http://127.0.0.1:8181/war/wc/example", "<h1>Hello World</h1>");
		testWebPath("http://127.0.0.1:8181/war/images/logo.png", "", 200, false);
		
	}
	
	@Test
	public void testWebContainerSN() throws Exception {
		testWebPath("http://127.0.0.1:8181/war/wc/sn", "<h1>Hello World</h1>");
	}
	
	@Test
	public void testSubJSP() throws Exception {
		testWebPath("http://127.0.0.1:8181/war/wc/subjsp", "<h2>Hello World!</h2>");
	}
	
	@Test
	public void testErrorJSPCall() throws Exception {
		testWebPath("http://127.0.0.1:8181/war/wc/error.jsp", "<h1>Error Page</h1>", 404, false);
	}
	
	@Test
	public void testWrongServlet() throws Exception {
		testWebPath("http://127.0.0.1:8181/war/wrong/", "<h1>Error Page</h1>", 404, false);
	}
	
	
	@Before
	public void setUp() throws Exception {

		int count = 0;
		while (!checkServer("http://127.0.0.1:8181/") && count < 200) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		LOG.info("waiting for Server took {} ms", (count * 1000));

		warBundle = bundleContext.installBundle("mvn:org.ops4j.pax.web.samples.web-fragment/war/" + getProjectVersion());
		fragmentBundle = bundleContext.installBundle("mvn:org.ops4j.pax.web.samples.web-fragment/fragment/" + getProjectVersion());
		
		warBundle.start();
		fragmentBundle.start();

		webListener = new WebListenerImpl();

		int failCount = 0;
		while (warBundle.getState() != Bundle.ACTIVE || fragmentBundle.getState() != Bundle.ACTIVE) {
			Thread.sleep(500);
			if (failCount > 500)
				throw new RuntimeException(
						"Required war-bundles is never active");
			failCount++;
		}

		count = 0;
		while (!((WebListenerImpl) webListener).gotEvent() && count < 100) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		LOG.info("waiting for Server took {} ms", (count * 1000));
	}

	@After
	public void tearDown() throws BundleException {
		if (warBundle != null) {
			warBundle.stop();
			warBundle.uninstall();
		}
		if (fragmentBundle != null) {
			fragmentBundle.stop();
			fragmentBundle.uninstall();
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
