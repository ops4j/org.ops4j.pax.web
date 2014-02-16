package org.ops4j.pax.web.itest.karaf;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarBasicAuthIntegrationKarafTest extends KarafBaseTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(WarBasicAuthIntegrationKarafTest.class);

	private Bundle warBundle;

	private WebListenerImpl webListener;

	@Configuration
	public Option[] configuration() {
		return jettyConfig();
	}
	
	@Test
	public void testWC() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc",
				"<h1>Hello World</h1>");

	}

	@Test
	@Ignore("Ignored due to strange effects on JAAS with Karaf")
	public void testWCExample() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/example",
				"Unauthorized", 401, false);

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/example",
				"<h1>Hello World</h1>", 200, true);

	}

	@Test
	@Ignore("Ignored due to strange effects with JAAS in Karaf")
	public void testWCAdditionalSample() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/additionalsample",
				"Unauthorized", 401, false);

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/additionalsample",
				"<h1>Hello World</h1>", 200, true);

	}
	
	@Test
	public void testWcSn() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/wc/sn",
				"<h1>Hello World</h1>");

	}

	@Test
	public void testSlash() throws Exception {

		LOG.info("Starting test ...");
		testClient.testWebPath("http://127.0.0.1:8181/war-authentication/",
				"<h1>Hello World</h1>");
		LOG.info("...Done");
	}
	
	@Before
	public void setUp() throws Exception {

		int count = 0;
		while (!testClient.checkServer("http://127.0.0.1:8181/") && count < 200) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		LOG.info("waiting for Server took {} ms", (count * 1000));

		String warUrl = "webbundle:mvn:org.ops4j.pax.web.samples/war-authentication/"
				+ getProjectVersion() + "/war?Web-ContextPath=/war-authentication";
		warBundle = bundleContext.installBundle(warUrl);
		warBundle.start();

		webListener = new WebListenerImpl();

		int failCount = 0;
		while (warBundle.getState() != Bundle.ACTIVE) {
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
