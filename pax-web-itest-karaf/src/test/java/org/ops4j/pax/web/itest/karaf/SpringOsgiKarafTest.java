/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

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
 * @author achim
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class SpringOsgiKarafTest extends KarafBaseTest {

	Logger LOG = LoggerFactory.getLogger(SpringOsgiKarafTest.class);

	private org.ops4j.pax.web.itest.karaf.SpringOsgiKarafTest.WebListenerImpl webListener;

	private Bundle warBundle;

	@Configuration
	public Option[] config() {

		return combine(baseConfig(), 
				scanFeatures(
						maven().groupId("org.apache.karaf.assemblies.features")
								.artifactId("standard").type("xml")
								.classifier("features").versionAsInProject(),
						"spring-dm").start(), 
				mavenBundle().groupId("org.springframework").artifactId("spring-web").versionAsInProject(),
				mavenBundle().groupId("org.springframework").artifactId("spring-webmvc").versionAsInProject(),
				mavenBundle().groupId("org.springframework.osgi").artifactId("spring-osgi-web").versionAsInProject()
				);
	}

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-war")));
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-whiteboard")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("spring-dm")));
	}
	
	@Test
	public void testWC() throws Exception {

		testWebPath("http://127.0.0.1:8181/war-spring", "<h2>Spring MVC - Hello World</h2>");
			
	}

	@Test
	public void testCallController() throws Exception {
		
		testWebPath("http://127.0.0.1:8181/war-spring", "<h2>Spring MVC - Hello World</h2>");
		testWebPath("http://127.0.0.1:8181/war-spring/helloWorld", "Done! Spring MVC works like a charm!");
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
		
		if (featuresService == null)
			throw new RuntimeException("Featuresservice is null");
		
		boolean installed = featuresService.isInstalled(featuresService.getFeature("spring-dm"));
		
		int counter = 0;
		while (!installed && counter > 50) { 
			Thread.sleep(500);
			installed = featuresService.isInstalled(featuresService.getFeature("spring-dm"));
			counter++;
		}
		if (!installed)
			throw new RuntimeException("No Sprint-Dm available ...");
		
		String warUrl = "mvn:org.ops4j.pax.web.samples/war-spring-osgi/"
				+ getProjectVersion() + "/war";
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

		counter = 0;
		while (!((WebListenerImpl) webListener).gotEvent() && counter < 100) {
			synchronized (this) {
				this.wait(100);
				counter++;
			}
		}
		LOG.info("waiting for Server took {} ms", (counter * 1000));
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