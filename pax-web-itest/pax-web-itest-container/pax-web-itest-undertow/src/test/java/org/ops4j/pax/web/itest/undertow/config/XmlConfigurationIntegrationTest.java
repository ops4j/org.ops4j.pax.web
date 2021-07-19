/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.undertow.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

public class XmlConfigurationIntegrationTest extends XmlITestBase {

	@Configuration
	public Option[] configure() throws IOException {
		FileUtils.writeStringToFile(new File("target/users.properties"), "Administrator=admin1,admin", StandardCharsets.UTF_8);
		FileUtils.writeStringToFile(new File("target/sites/docs/d1.txt"), "d1.txt", StandardCharsets.UTF_8);
		FileUtils.writeStringToFile(new File("target/sites/docs/index.txt"), "index.txt1", StandardCharsets.UTF_8);
		FileUtils.writeStringToFile(new File("target/sites/home/index.txt"), "index.txt2", StandardCharsets.UTF_8);
		FileUtils.writeStringToFile(new File("target/sites/home/h1.md"), "h1.md", StandardCharsets.UTF_8);

		return combine(
				configure("src/test/resources/xml/undertow-configs.xml"),
				systemProperty("org.osgi.service.http.port.special").value("8184"));
	}

	@Test
	public void testResourceHandlers() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("index.txt1"))
				.doGETandExecuteTest("http://127.0.0.1:8184/docs");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("d1.txt"))
				.doGETandExecuteTest("http://127.0.0.1:8184/docs/d1.txt");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("index.txt2"))
				.doGETandExecuteTest("http://127.0.0.1:8184/welcome");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("h1.md"))
				.doGETandExecuteTest("http://127.0.0.1:8184/welcome/h1.md");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8184/welcome/h2.md");
	}

	@Test
	public void testFilters() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Response must contain 'Server' header",
						map -> map.anyMatch(e -> "Server".equals(e.getKey())
								&& "Pax-Web/42".equals(e.getValue())))
				.doGETandExecuteTest("http://127.0.0.1:8184/welcome");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Response must contain 'Server' header",
						map -> map.anyMatch(e -> "X-Powered-By".equals(e.getKey())
								&& "OPS4J".equals(e.getValue())))
				.doGETandExecuteTest("http://127.0.0.1:8184/welcome");
	}

}
