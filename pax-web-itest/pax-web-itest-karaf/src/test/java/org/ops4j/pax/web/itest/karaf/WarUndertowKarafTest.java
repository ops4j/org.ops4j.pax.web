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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public class WarUndertowKarafTest extends KarafBaseTest {

	Logger LOG = LoggerFactory.getLogger(WarUndertowKarafTest.class);

	private Bundle warBundle;

	@Configuration
	public Option[] config() {

		return combine(undertowConfig(), new VMOption("-DMyFacesVersion="
				+ getMyFacesVersion()));
	}

	@Test
	public void testWC() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
	}

	@Test
	public void testWC_example() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/example");

		createTestClientForKaraf()
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/example");

	}

	@Test
	public void testWC_SN() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/sn");
	}

	/**
	 * With <code>war/</code> URI, Undertow/ResourceManager found it's directory access, but index is forbidden,
	 * thus 403.
	 * @throws Exception
	 */
	@Test
	public void test404() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain text from error-page served by Karaf!",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/../../etc/passwd");
	}

	@Test
	public void testSubJSP() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h2>Hello World!</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/subjsp");
	}

	@Test
	public void testErrorJSPCall() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain text from error-page served by Karaf!",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/error.jsp");
	}

	@Test
	public void testWrongServlet() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain text from error-page served by Karaf!",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wrong/");
	}

	@Before
	public void setUp() throws Exception {

		initWebListener();

		String warUrl = "webbundle:mvn:org.ops4j.pax.web.samples/war/"
				+ getProjectVersion() + "/war?Web-ContextPath=/war";
		warBundle = bundleContext.installBundle(warUrl);
		warBundle.start();

		waitForWebListener();

	}

	@After
	public void tearDown() throws BundleException {
		if (warBundle != null) {
			warBundle.stop();
			warBundle.uninstall();
		}
	}

}