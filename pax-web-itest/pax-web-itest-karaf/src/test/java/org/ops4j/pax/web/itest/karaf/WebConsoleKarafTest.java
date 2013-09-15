/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 * 
 */
@RunWith(PaxExam.class)
public class WebConsoleKarafTest extends KarafBaseTest {

	private static Logger LOG = LoggerFactory.getLogger(WebConsoleKarafTest.class);
	
	@Configuration
	public Option[] config() {
		return combine(
				jettyConfig(),
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
		Thread.sleep(4000);
		
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
	
	@Before
	public void setUp() throws Exception {
		initServletListener();

		int count = 0;
		while (!checkServer("http://127.0.0.1:8181/") && count < 200) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		LOG.info("waiting for Server took {} ms", (count * 100));

		if (featuresService == null)
			throw new RuntimeException("Featuresservice is null");
		
		waitForServletListener();
	}

}
