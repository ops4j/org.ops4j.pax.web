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

import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

public class XmlSslAuthIntegrationTest extends XmlITestBase {

	@Configuration
	public Option[] configure() {
		return combine(
				configure("src/test/resources/xml/undertow-ssl2.xml"),
				systemProperty("org.osgi.service.http.port.secure.special").value("8485"));
	}

	/**
	 * Use wrong keystore - no client auth required
	 *
	 * @throws Exception
	 */
	@Test
	public void testWebContextPath() {
		try {
			HttpTestClientFactory.createDefaultTestClient()
					.withBundleKeystore("any", "any", "secret1", "secret2")
					.withExternalKeystore("src/test/resources-binary/certs/client2.keystore")
					.doGETandExecuteTest("https://127.0.0.1:8485/helloworld/wc");
			fail("Expected SSL exception");
		} catch (Exception e) {
			LOG.info("Expected {}", e.getMessage());
		}
	}

	/**
	 * Use correct keystore - client auth required
	 *
	 * @throws Exception
	 */
	@Test
	public void testWebContextPathWithClientKeystore() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withExternalKeystore("../../etc/security/server.jks")
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("https://127.0.0.1:8485/helloworld/wc");
	}

}
