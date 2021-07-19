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

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.Principal;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

public class XmlSslNoAuthManyAliases2IntegrationTest extends XmlITestBase {

	@Configuration
	public Option[] configure() {
		return combine(
				configure("src/test/resources/xml/undertow-ssl3b.xml"),
				systemProperty("org.osgi.service.http.port.secure.special").value("8485"));
	}

	@Test
	public void testServerIdentity() throws Exception {
		KeyStore ts = KeyStore.getInstance("JKS");
		try (InputStream is = getClass().getResourceAsStream("/certs/server-bad.keystore")) {
			ts.load(is, "secret1".toCharArray());
		}
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
		trustManagerFactory.init(ts);

		TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, trustManagers, null);

		HttpsURLConnection connection = (HttpsURLConnection) new URL("https://127.0.0.1:8485").openConnection();
		connection.setSSLSocketFactory(context.getSocketFactory());
		connection.setHostnameVerifier((hostname, session) -> true);
		connection.connect();
		Principal server = connection.getPeerPrincipal();
		assertEquals("CN=server-bad,O=OPS4J,C=PL", server.getName());
		connection.disconnect();
	}

}
