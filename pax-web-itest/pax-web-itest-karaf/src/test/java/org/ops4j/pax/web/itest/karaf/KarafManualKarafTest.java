/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 * 
 */
@RunWith(PaxExam.class)
@Ignore("A Failure of Pax Exam is provoked.")
public class KarafManualKarafTest extends KarafBaseTest {

	Logger LOG = LoggerFactory.getLogger(KarafManualKarafTest.class);

	@Configuration
	public Option[] config() {

		return combine(jettyConfig(), new VMOption("-DMyFacesVersion="
				+ getMyFacesVersion()),
				mavenBundle().groupId("org.apache.karaf")
				.artifactId("manual").type("war").version(asInProject()));
	}

	@Test
	public void testSlash() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/karaf-doc", "Apache Karaf");

	}
}