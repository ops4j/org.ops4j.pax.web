package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class WarFragmentKarafTest extends KarafBaseTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(WarFragmentKarafTest.class);
	
	private Bundle warBundle, fragmentBundle;

	@Configuration
	public Option[] config() {
		return jettyConfig();
	}

	@Ignore("PAXWEB-850")
	@Test
	public void test() throws Exception {
		Thread.sleep(4000);
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-war")));
	}

	@Ignore("PAXWEB-850")
	@Test
	public void testWC() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc", "<h1>Hello World</h1>");
	}

	@Ignore("PAXWEB-850")
	@Test
	public void testFilterInit() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc", "Have bundle context in filter: true");
	}

	@Ignore("PAXWEB-850")
	@Test
	public void testWebContainerExample() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc/example", "<h1>Hello World</h1>");
		testClient.testWebPath("http://127.0.0.1:8181/war/images/logo.png", "", 200, false);
		
	}

	@Ignore("PAXWEB-850")
	@Test
	public void testWebContainerSN() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc/sn", "<h1>Hello World</h1>");
	}

	@Ignore("PAXWEB-850")
	@Test
	public void testSubJSP() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc/subjsp", "<h2>Hello World!</h2>");
	}

	@Ignore("PAXWEB-850")
	@Test
	public void testErrorJSPCall() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc/error.jsp", "<h1>Error Page</h1>", 404, false);
	}

	@Ignore("PAXWEB-850")
	@Test
	public void testWrongServlet() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wrong/", "<h1>Error Page</h1>", 404, false);
	}
	
	
	@Before
	public void setUp() throws Exception {

		warBundle = bundleContext.installBundle("mvn:org.ops4j.pax.web.samples.web-fragment/war/" + getProjectVersion());
		fragmentBundle = bundleContext.installBundle("mvn:org.ops4j.pax.web.samples.web-fragment/fragment/" + getProjectVersion());

		initWebListener();
		
		warBundle.start();
		fragmentBundle.start();

		waitForWebListener();

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

}
