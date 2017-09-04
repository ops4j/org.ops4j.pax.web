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
package org.ops4j.pax.web.itest.undertow;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.BundleException;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

public class XmlAuthConfigurationIntegrationTest extends XmlITestBase {

	@Configuration
	public static Option[] configure() throws IOException {
		FileUtils.writeStringToFile(new File("target/users.properties"), "Administrator=admin1,admin");
		FileUtils.writeStringToFile(new File("target/sites/docs/d1.txt"), "d1.txt");
		FileUtils.writeStringToFile(new File("target/sites/docs/index.txt"), "index.txt1");
		FileUtils.writeStringToFile(new File("target/sites/home/index.txt"), "index.txt2");
		FileUtils.writeStringToFile(new File("target/sites/home/h1.md"), "h1.md");

		return combine(
				configure("src/test/resources/xml/undertow-configs.xml"),
				systemProperty("org.osgi.service.http.port.special").value("8186"));
	}

	@Before
	@Override
	public void setUp() throws BundleException, InterruptedException {
		initServletListener();
		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-authentication/"
				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH
				+ "=/war-authentication";
		installWarBundle = installAndStartBundle(bundlePath);
		waitForServletListener();
	}

	@Test
	public void testWebContainerExample() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseAssertion("Response must contain 'Unauthorized'",
						resp -> resp.contains("Unauthorized"))
				.doGETandExecuteTest("http://127.0.0.1:8186/war-authentication/wc/example");
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("Administrator", "admin2", "Test Realm")
				.withReturnCode(401)
				.withResponseAssertion("Response must contain 'Unauthorized'",
						resp -> resp.contains("Unauthorized"))
				.doGETandExecuteTest("http://127.0.0.1:8186/war-authentication/wc/example");
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("Administrator", "admin1", "Test Realm")
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8186/war-authentication/wc/example");
	}

}
