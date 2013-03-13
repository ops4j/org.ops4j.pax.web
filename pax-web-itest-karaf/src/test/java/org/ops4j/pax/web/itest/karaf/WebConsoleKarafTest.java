/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

/**
 * @author achim
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class WebConsoleKarafTest extends KarafBaseTest {

	@Configuration
	public Option[] config() {
		return combine(
				baseConfig(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.bundlerepository")
						.version("1.6.2"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.configadmin")
						.version("1.2.8"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.shell").version("1.4.2"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.shell.tui")
						.version("1.4.1"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.webconsole")
						.version("3.1.8"));
	}

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-war")));
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-whiteboard")));
	}

	@Test
	public void testBundlesPath() throws Exception {

		testWebPath("http://localhost:8181/system/console/bundles", "", 401,
				false);

		testWebPath("http://localhost:8181/system/console/bundles",
				"Apache Felix Web Console<br/>Bundles", 200, true);

	}

}
