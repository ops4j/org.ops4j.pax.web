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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

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
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain message from Karaf!",
						resp -> resp.contains("Apache Karaf"))
				.doGETandExecuteTest("http://127.0.0.1:8181/karaf-doc");
	}
}