/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 /**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

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

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

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

	/**
	 * Executes multiple tests
	 * <ul>
	 *     <li>Checks if relevant modules are installed</li>
	 *     <li>Calls URL to test if Spring-MVC sample is available at all</li>
	 *     <li>Calls Controller-URL</li>
	 * </ul>
	 * @throws Exception
     */
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
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain message from Karaf!",
						resp -> resp.contains("<h2>Spring MVC - Hello World</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-spring");
	}

	@Test
	public void testCallController() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain message from Karaf!",
						resp -> resp.contains("Done! Spring MVC works like a charm!"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-spring/helloWorld.do");
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