/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import static org.ops4j.pax.exam.OptionUtils.combine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
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
@RunWith(PaxExam.class)
public class WarTCKarafTest extends KarafBaseTest {

	Logger LOG = LoggerFactory.getLogger(WarTCKarafTest.class);

	private Bundle warBundle;

	@Configuration
	public Option[] config() {

		return combine(tomcatConfig(), new VMOption("-DMyFacesVersion="
				+ getMyFacesVersion()));
	}

	@Test
	public void testWC() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war/wc", "<h1>Hello World</h1>");

	}

	@Test
	public void testWC_example() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war/wc/example",
				"<h1>Hello World</h1>");

		testClient.testWebPath("http://127.0.0.1:8181/war/images/logo.png", "", 200, false);

	}

	@Test
	public void testWC_SN() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war/wc/sn", "<h1>Hello World</h1>");

	}

	@Test
	public void testSlash() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war/", "<h1>Error Page</h1>", 404, false);

	}

	@Test
	public void testSubJSP() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war/wc/subjsp",
				"<h2>Hello World!</h2>");

	}

	@Test
	public void testErrorJSPCall() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc/error.jsp",
				"<h1>Error Page</h1>", 404, false);
	}

	@Test
	public void testWrongServlet() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wrong/", "<h1>Error Page</h1>",
				404, false);
	}

	@Before
	public void setUp() throws Exception {

		initWebListener();

		String warUrl = "webbundle:mvn:org.ops4j.pax.web.samples/war/"
				+ getProjectVersion() + "/war?Web-ContextPath=/war";
		warBundle = bundleContext.installBundle(warUrl);
		warBundle.start();

		waitForWebListener();

	}

	@After
	public void tearDown() throws BundleException {
		if (warBundle != null) {
			warBundle.stop();
			warBundle.uninstall();
		}
	}

}