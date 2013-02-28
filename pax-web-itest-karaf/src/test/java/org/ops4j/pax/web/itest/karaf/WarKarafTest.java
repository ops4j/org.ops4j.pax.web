/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 *
 */
@RunWith(JUnit4TestRunner.class)
public class WarKarafTest extends KarafBaseTest {
	
	private Bundle installWarBundle;
	
	Logger LOG = LoggerFactory.getLogger(WarKarafTest.class);

	private org.ops4j.pax.web.itest.karaf.WarKarafTest.WebListenerImpl webListener;

	private Bundle facesApiBundle;

	private Bundle facesImplBundle;

	private Bundle warBundle;

	@Configuration
	public Option[] config() {
		
		return combine(baseConfig(), new VMOption("-DMyFacesVersion="+getMyFacesVersion()));
	}
	

	@Test
	public void testWC() throws Exception {

		testWebPath("http://127.0.0.1:8181/war/wc", "<h1>Hello World</h1>");
			
	}

	@Test
	public void testWC_example() throws Exception {

			
		testWebPath("http://127.0.0.1:8181/war/wc/example", "<h1>Hello World</h1>");

		
		testWebPath("http://127.0.0.1:8181/war/images/logo.png", "", 200, false);
		
	}

	
	@Test
	public void testWC_SN() throws Exception {

			
		testWebPath("http://127.0.0.1:8181/war/wc/sn", "<h1>Hello World</h1>");

	}
	
	@Test
	public void testSlash() throws Exception {

			
		testWebPath("http://127.0.0.1:8181/war/", "<h1>Error Page</h1>", 404, false);

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
		while (!checkServer() && count < 200) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		LOG.info("waiting for Server took {} ms", (count * 1000));
		
		String warUrl = "webbundle:mvn:org.ops4j.pax.web.samples/war/"+getProjectVersion()+"/war?Web-ContextPath=/war";
		warBundle = bundleContext.installBundle(warUrl);
		warBundle.start();

		webListener = new WebListenerImpl();

		int failCount = 0;
		while (warBundle.getState() != Bundle.ACTIVE) {
			Thread.sleep(500);
			if (failCount > 500)
				throw new RuntimeException("Required war-bundles is never active");
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