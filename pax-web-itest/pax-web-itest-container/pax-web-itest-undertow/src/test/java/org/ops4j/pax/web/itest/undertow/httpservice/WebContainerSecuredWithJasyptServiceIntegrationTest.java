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
import java.util.Hashtable;

import org.jasypt.commons.CommonUtils;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.httpservice.AbstractWebContainerSecuredIntegrationTest;
import org.ops4j.pax.web.service.PaxWebConfig;

import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WebContainerSecuredWithJasyptServiceIntegrationTest extends AbstractWebContainerSecuredIntegrationTest {

	@Configuration
	public Option[] configure() {
		return combine(combine(combine(baseConfigure(), paxWebUndertow()), configAdmin()), jasypt());
	}

	@Override
	protected void additionalConfiguration(Dictionary<String, Object> properties) {
		properties.put(PaxWebConfig.PID_CFG_ENC_ENABLED, "true");
		properties.put(PaxWebConfig.PID_CFG_ENC_OSGI_DECRYPTOR, "my-decryptor");

		// replace plain text with encrypted passwords ("passw0rd")
		String encrypted = "ENC(Ygz9vvdqsAgorT3xnWacQw+HDMl5RVYRwH/bQ1nvM7Jyng7m9bMJqg15MIB4racf)";
		properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PASSWORD, encrypted);
		properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE_PASSWORD, encrypted);
		properties.put(PaxWebConfig.PID_CFG_SSL_KEY_PASSWORD, encrypted);
	}

	protected void configureWebBundle(org.osgi.service.cm.Configuration config, Dictionary<String, Object> properties)
			throws Exception {
		configureAndWaitForServletWithMapping("/helloworld/wc/error/create", () -> {
			hsBundle = installAndStartBundle(sampleURI("wc-helloworld"));
			Dictionary<String, Object> current = config.getProperties();
			if (current == null || !"8443".equals(current.get(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE))) {
				configureAndWaitForListener(8443, () -> {
					config.update(properties);

					StandardPBEStringEncryptor enc = new StandardPBEStringEncryptor();
					EnvironmentStringPBEConfig c = new EnvironmentStringPBEConfig();
					c.setStringOutputType(CommonUtils.STRING_OUTPUT_TYPE_BASE64);
					c.setKeyObtentionIterations(1042);
					c.setAlgorithm("PBEWithHmacSHA256AndAES_256");
					c.setPassword("masterpasswordfortest");
					c.setIvGenerator(new RandomIvGenerator());
					enc.setConfig(c);

					Dictionary<String, Object> props = new Hashtable<>();
					props.put("decryptor", "my-decryptor");
					context.registerService(StringEncryptor.class, enc, props);
				});
			}
		});
	}

}
