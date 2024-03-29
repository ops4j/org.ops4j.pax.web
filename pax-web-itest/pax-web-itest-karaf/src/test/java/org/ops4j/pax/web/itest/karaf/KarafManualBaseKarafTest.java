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
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public abstract class KarafManualBaseKarafTest extends AbstractKarafTestBase {

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeployment(() -> {
			installAndStartBundle("mvn:org.apache.karaf/manual/" + System.getProperty("karaf.version"));
		});
	}

	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Apache Karaf'",
						resp -> resp.contains("Apache Karaf"))
				.doGETandExecuteTest("http://127.0.0.1:8181/documentation");
	}

}
