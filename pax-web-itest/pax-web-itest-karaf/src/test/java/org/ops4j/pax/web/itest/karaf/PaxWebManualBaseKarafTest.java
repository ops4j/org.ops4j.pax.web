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
package org.ops4j.pax.web.itest.karaf;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public abstract class PaxWebManualBaseKarafTest extends AbstractKarafTestBase {

	@Before
	public void setup() throws Exception {
		configureAndWaitForDeployment(() -> installAndStartWebBundle("org.ops4j.pax.web", "pax-web-manual",
				System.getProperty("pax-web.version"), "pax-web-manual", "/pax-web-manual", null));
	}

	@Test
	public void testManual() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text from Pax-Web-Manuel served by Karaf!",
						resp -> resp.contains("an implementation of these 3 chapters of OSGi CMPN specification"))
				.doGETandExecuteTest("http://127.0.0.1:8181/pax-web-manual");
	}

}
