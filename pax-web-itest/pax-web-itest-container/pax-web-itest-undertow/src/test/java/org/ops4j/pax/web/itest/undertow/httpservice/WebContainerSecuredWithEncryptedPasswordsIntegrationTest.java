/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.itest.undertow.httpservice;

import java.util.Dictionary;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.httpservice.AbstractWebContainerSecuredIntegrationTest;
import org.ops4j.pax.web.service.PaxWebConfig;

import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WebContainerSecuredWithEncryptedPasswordsIntegrationTest extends AbstractWebContainerSecuredIntegrationTest {

	@Configuration
	public Option[] configure() {
		return combine(combine(combine(baseConfigure(), paxWebUndertow()), configAdmin()), jasypt());
	}

	@Override
	protected void additionalConfiguration(Dictionary<String, Object> properties) {
		properties.put(PaxWebConfig.PID_CFG_ENC_ENABLED, "true");
		properties.put(PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD, "masterpasswordfortest");
		properties.put(PaxWebConfig.PID_CFG_ENC_ITERATION_COUNT, "1042");
		properties.put(PaxWebConfig.PID_CFG_ENC_ALGORITHM, "PBEWithHmacSHA256AndAES_256");

		// replace plain text with encrypted passwords ("passw0rd")
		String encrypted = "ENC(Ygz9vvdqsAgorT3xnWacQw+HDMl5RVYRwH/bQ1nvM7Jyng7m9bMJqg15MIB4racf)";
		properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PASSWORD, encrypted);
		properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE_PASSWORD, encrypted);
		properties.put(PaxWebConfig.PID_CFG_SSL_KEY_PASSWORD, encrypted);
	}

}
