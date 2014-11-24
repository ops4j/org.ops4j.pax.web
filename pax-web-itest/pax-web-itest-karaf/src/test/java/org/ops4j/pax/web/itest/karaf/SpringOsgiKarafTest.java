/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

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

/**
 * @author achim
 * 
 */
@RunWith(PaxExam.class)
@Ignore("Ignored for unknown reason")
public class SpringOsgiKarafTest extends KarafBaseTest {

	Logger LOG = LoggerFactory.getLogger(SpringOsgiKarafTest.class);

	// private org.ops4j.pax.web.itest.karaf.SpringOsgiKarafTest.WebListenerImpl
	// webListener;

	private Bundle warBundle;

	@Configuration
	public Option[] config() {

		return combine(
				jettyConfig(),
				features(
						maven().groupId("org.apache.karaf.features")
								.artifactId("spring").type("xml")
								.classifier("features").versionAsInProject(),
						"spring-dm"),
				mavenBundle().groupId("org.springframework")
						.artifactId("org.springframework.web").versionAsInProject().start(true),
				mavenBundle().groupId("org.springframework")
						.artifactId("org.springframework.web.servlet").versionAsInProject().start(true),
				mavenBundle().groupId("org.springframework.osgi")
						.artifactId("spring-osgi-web").versionAsInProject().start(true));
	}

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-war")));
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-whiteboard")));
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("spring-dm")));
	}

	@Test
	public void testWC() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war-spring",
				"<h2>Spring MVC - Hello World</h2>");

	}

	@Test
	public void testCallController() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war-spring",
				"<h2>Spring MVC - Hello World</h2>");
		testClient.testWebPath("http://127.0.0.1:8181/war-spring/helloWorld.do",
				"Done! Spring MVC works like a charm!");
	}

	@Before
	public void setUp() throws Exception {

		if (featuresService == null)
			throw new RuntimeException("Featuresservice is null");

		boolean installed = featuresService.isInstalled(featuresService
				.getFeature("spring-dm"));

		int counter = 0;
		while (!installed && counter < 100) {
			Thread.sleep(500);
			installed = featuresService.isInstalled(featuresService
					.getFeature("spring-dm"));
			counter++;
		}
		LOG.info("waited {} ms for Spring-DM feature to appear", counter*500);
		if (!installed)
			throw new RuntimeException("No Spring-Dm available ...");
		

		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			String symbolicName = bundle.getSymbolicName();
			if (symbolicName.startsWith("org.springframework.osgi.web")) {
				LOG.info("found bundle {} in state {}", symbolicName, bundle.getState());
				if (bundle.getState() != Bundle.ACTIVE)
					throw new RuntimeException("Required bundle spring-dm-web isn't active");
			}
		}
		
		initWebListener();
		String warUrl = "mvn:org.ops4j.pax.web.samples/war-spring-osgi/"
				+ getProjectVersion() + "/war";
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