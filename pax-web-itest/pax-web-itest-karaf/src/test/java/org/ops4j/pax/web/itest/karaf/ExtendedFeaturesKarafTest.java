/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.util.Hashtable;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

/**
 * @author achim
 * 
 */
@RunWith(PaxExam.class)
public class ExtendedFeaturesKarafTest extends KarafBaseTest {

	@Configuration
	public Option[] config() {
		return combine(baseConfig(), 
				features(
						maven().groupId("org.ops4j.pax.web")
                                .artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-http")
				);
	}

	@Test
	public void testHttpFeature() throws Exception {
		
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http")));
		
	}
	
	@Test
	public void testWhiteboardFeature() throws Exception {

		featuresService.installFeature("pax-http-whiteboard");
		
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-whiteboard")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-http")));
	}
		
	@Test
	public void testWebFeature() throws Exception {
		
		featuresService.installFeature("pax-war");
		
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-war")));
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-whiteboard")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-http")));
		
	}
}
