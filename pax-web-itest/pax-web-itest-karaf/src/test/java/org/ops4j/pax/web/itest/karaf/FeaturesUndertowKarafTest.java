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
public class FeaturesUndertowKarafTest extends KarafBaseTest {

	@Configuration
	public Option[] config() {
		return undertowConfig();
	}

	@Test
	public void test() throws Exception {
		
		//this is needed since the test is a bit to fast :)
		Thread.sleep(2000);
		
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-undertow")));

	}
	
}
