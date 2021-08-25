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

import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.common.AbstractWebContainerSecuredIntegrationTest;

@RunWith(PaxExam.class)
public class WebContainerSecureWithEncryptedPassword extends AbstractWebContainerSecuredIntegrationTest {

	@Configuration
	public static Option[] configure() {
		return OptionUtils.combine(
				configureJetty(),
				systemProperty("org.osgi.service.http.secure.enabled").value(
						"true"),
				systemProperty("org.ops4j.pax.web.ssl.keystore").value(
						WebContainerSecureWithEncryptedPassword.class.getClassLoader().getResource("wss40rev.jks").getFile()),
				systemProperty("org.ops4j.pax.web.ssl.password").value(
						"ENC(zuhuJxhS0aQYddRG/BMa3msqohW7hp/i)"),
				systemProperty("org.ops4j.pax.web.ssl.keypassword").value(
						"ENC(zuhuJxhS0aQYddRG/BMa3msqohW7hp/i)"),
				systemProperty("org.ops4j.pax.web.ssl.key.alias").value(
                                                "wss40rev"),
				systemProperty("org.ops4j.pax.web.ssl.clientauthneeded").value(
						"required"),
				systemProperty("org.ops4j.pax.web.ssl.truststore").value(
				                WebContainerSecureWithEncryptedPassword.class.getClassLoader().getResource("wss40rev.jks").getFile()),
				systemProperty("org.ops4j.pax.web.ssl.truststore.password").value(
				                "ENC(zuhuJxhS0aQYddRG/BMa3msqohW7hp/i)"),
				systemProperty("org.ops4j.pax.web.enc.enabled").value(
                                                "true"),
				systemProperty("org.ops4j.pax.web.enc.masterpassword").value(
                                                "masterpasswordfortest"));
				
		                
	}

	@Test
	public void testWebContextPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClientWithCRL()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("https://127.0.0.1:8443/helloworld/wc");
	}
}
