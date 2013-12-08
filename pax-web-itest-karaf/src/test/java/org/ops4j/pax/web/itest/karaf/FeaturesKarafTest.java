/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;


import static org.junit.Assert.assertTrue;

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
public class FeaturesKarafTest extends KarafBaseTest {

	@Configuration
	public Option[] config() {
		return baseConfig();
	}

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-war")));
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-whiteboard")));
	}

}
