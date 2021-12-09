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
package org.ops4j.pax.web.itest.container.httpservice;

import java.util.Dictionary;
import java.util.Hashtable;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWebContainerSecuredIntegrationTest extends AbstractContainerTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractWebContainerSecuredIntegrationTest.class);

	private Bundle hsBundle;

	@Inject
	private ConfigurationAdmin caService;

	@Before
	public void setUp() throws Exception {
		org.osgi.service.cm.Configuration config = caService.getConfiguration(PaxWebConstants.PID, null);

		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(PaxWebConfig.PID_CFG_LISTENING_ADDRESSES, "127.0.0.1");
		properties.put(PaxWebConfig.PID_CFG_HTTP_ENABLED, "false");
		properties.put(PaxWebConfig.PID_CFG_HTTP_SECURE_ENABLED, "true");
		properties.put(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE, "8443");

		// tweak ciphers/protocols to check proper configuration
		properties.put(PaxWebConfig.PID_CFG_PROTOCOLS_INCLUDED, "TLSv1.2");
//		properties.put(PaxWebConfig.PID_CFG_CIPHERSUITES_INCLUDED, "TLS_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_256_GCM_SHA384");

		// entire keystore
		properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE, "../../etc/security/server.jks");
		properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE_PASSWORD, "passw0rd");
		properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE_TYPE, "JKS");
//			properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE_PROVIDER, "SUN");
		properties.put(PaxWebConfig.PID_CFG_SSL_KEY_MANAGER_FACTORY_ALGORITHM, "SunX509");
		// single key entry in the keystore
		properties.put(PaxWebConfig.PID_CFG_SSL_KEY_PASSWORD, "passw0rd");
		properties.put(PaxWebConfig.PID_CFG_SSL_KEY_ALIAS, "server"); // to check if we can select one of many

		// entire truststore
		properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE, "../../etc/security/server.jks");
		properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PASSWORD, "passw0rd");
		properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_TYPE, "JKS");
//			properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PROVIDER, "SUN");
		properties.put(PaxWebConfig.PID_CFG_SSL_TRUST_MANAGER_FACTORY_ALGORITHM, "X509");

		// remaining SSL parameters
		properties.put(PaxWebConfig.PID_CFG_SSL_CLIENT_AUTH_WANTED, "false");
		properties.put(PaxWebConfig.PID_CFG_SSL_CLIENT_AUTH_NEEDED, "true");
		properties.put(PaxWebConfig.PID_CFG_SSL_PROTOCOL, "TLSv1.2");
		properties.put(PaxWebConfig.PID_CFG_SSL_SECURE_RANDOM_ALGORITHM, "SHA1PRNG");
		properties.put(PaxWebConfig.PID_CFG_SSL_RENEGOTIATION_ALLOWED, "true");
		properties.put(PaxWebConfig.PID_CFG_SSL_SESSION_ENABLED, "true");

		additionalConfiguration(properties);

		configureAndWaitForServletWithMapping("/helloworld/wc/error/create", () -> {
			hsBundle = installAndStartBundle(sampleURI("wc-helloworld"));
			Dictionary<String, Object> current = config.getProperties();
			if (current == null || !"8443".equals(current.get(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE))) {
				configureAndWaitForListener(8443, () -> {
					config.update(properties);
				});
			}
		});
	}

	protected void additionalConfiguration(Dictionary<String, Object> properties) {
	}

	@After
	public void tearDown() throws BundleException {
		if (hsBundle != null) {
			hsBundle.stop();
			hsBundle.uninstall();
		}
	}

	@Test
	public void testWebContextPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("https://127.0.0.1:8443/helloworld/wc");
	}

}
