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
package org.ops4j.pax.web.itest.tomcat;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.common.AbstractWebContainerSecuredIntegrationTest;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebContainerSecuredIntegrationTest extends AbstractWebContainerSecuredIntegrationTest {

	@Configuration
	public static Option[] configure() {
		return OptionUtils.combine(
				configureTomcat(),
				systemProperty("org.osgi.service.http.secure.enabled").value(
						"true"),
				systemProperty("org.ops4j.pax.web.ssl.keystore").value(
						WebContainerSecuredIntegrationTest.class.getClassLoader().getResource("keystore").getFile()),
				systemProperty("org.ops4j.pax.web.ssl.password").value(
						"password"),
				systemProperty("org.ops4j.pax.web.ssl.keypassword").value(
						"password"),
				systemProperty("org.ops4j.pax.web.ssl.clientauthneeded").value(
						"required"),
				systemProperty("org.ops4j.pax.web.ssl.cyphersuites.included").value("TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA"),
				systemProperty("org.ops4j.pax.web.ssl.cyphersuites.excluded").value("SSL_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_DES_CBC_SHA"));
	}
}
