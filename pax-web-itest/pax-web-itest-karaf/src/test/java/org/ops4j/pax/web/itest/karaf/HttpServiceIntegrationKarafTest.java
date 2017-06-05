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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * PAXWEB-1099
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
public class HttpServiceIntegrationKarafTest extends KarafBaseTest {

	private Bundle bundle;

	@Configuration
	public Option[] configuration() {
		return jettyConfig();
	}

	@Before
	public void setUp() throws Exception {
		initWebListener();

		String warUrl = "mvn:org.ops4j.pax.web.samples/helloworld-hs/"
				+ getProjectVersion();
		bundle = bundleContext.installBundle(warUrl);
		bundle.start();

		waitForWebListener();

		int failCount = 0;
		while (bundle.getState() != Bundle.ACTIVE) {
			Thread.sleep(500);
			if (failCount > 500) {
				throw new RuntimeException(
						"Required helloworld-hs is never active");
			}
			failCount++;
		}
	}

	@Test
	public void testWC() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(200)
				.withResponseHeaderAssertion("Hello World resources should be available under /images",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/images/logo.png");

		createTestClientForKaraf()
				.withReturnCode(200)
				.withResponseHeaderAssertion("Hello World resources should be available under /alt-images",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/alt-images/logo.png");

		createTestClientForKaraf()
				.withReturnCode(200)
				.withResponseHeaderAssertion("Other resource paths will be served by servlet mapped at /*",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().startsWith("text/html")))
				.doGETandExecuteTest("http://127.0.0.1:8181/alt2-images/logo.png");
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle != null) {
			bundle.stop();
			bundle.uninstall();
		}
	}

}
