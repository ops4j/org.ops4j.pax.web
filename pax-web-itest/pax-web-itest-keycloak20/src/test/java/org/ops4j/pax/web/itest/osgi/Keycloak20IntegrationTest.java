/*
 * Copyright 2019 OPS4J.
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
package org.ops4j.pax.web.itest.osgi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class Keycloak20IntegrationTest extends AbstractOsgiTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(Keycloak20IntegrationTest.class);

	@Configuration
	public Option[] configure() {
		Option[] keycloakBundles = new Option[]{
				mavenBundle("org.bouncycastle", "bcprov-jdk18on")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.bouncycastle", "bcpkix-jdk18on")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.bouncycastle", "bcutil-jdk18on")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),

				mavenBundle("org.apache.httpcomponents", "httpcore-osgi")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.httpcomponents", "httpclient-osgi")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),

				mavenBundle("com.fasterxml.jackson.core", "jackson-annotations")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("com.fasterxml.jackson.core", "jackson-core")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("com.fasterxml.jackson.core", "jackson-databind")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),

				mavenBundle("org.ops4j.pax.web", "keycloak20-common")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "keycloak20-osgi-adapter")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
		Option[] cm = combine(keycloakBundles, configAdmin());
		return combine(cm, combine(baseConfigure(), paxWebCore()));
	}

	@Test
	public void justRun() {
		Set<Bundle> bundles = new TreeSet<>((b1, b2) -> (int) (b1.getBundleId() - b2.getBundleId()));
		bundles.addAll(Arrays.asList(context.getBundles()));
		for (Bundle b : bundles) {
			String info = String.format("#%02d: %s/%s (%s)",
					b.getBundleId(), b.getSymbolicName(), b.getVersion(), b.getLocation());
			LOG.info(info);
		}

		Bundle api = bundle("org.ops4j.pax.web.pax-web-api");
		Bundle spi = bundle("org.ops4j.pax.web.pax-web-spi");
		assertThat(api.getState(), equalTo(Bundle.ACTIVE));
		assertThat(spi.getState(), equalTo(Bundle.ACTIVE));
	}

	@Test
	public void checkServices() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException {
		String json = "{" +
				"  \"realm\":\"paxweb\"," +
				"  \"auth-server-url\":\"http://127.0.0.1:8180/auth/\"," +
				"  \"resource\":\"paxweb-samples\"" +
				"}";
		KeycloakDeploymentBuilder.build(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
		// should find bundle://9.0:1/META-INF/services/org.keycloak.common.crypto.CryptoProvider
		// where #9 is mvn:org.ops4j.pax.web/keycloak20-common/8.0.15
		CryptoProvider provider = CryptoIntegration.getProvider();
		assertThat(provider.getClass().getName(), equalTo("org.keycloak.crypto.def.DefaultCryptoProvider"));
		LOG.info("CryptoProvider: {}", provider);

		assertNotNull(provider.getAesCbcCipher());
		assertNotNull(provider.getAesGcmCipher());
		assertNotNull(provider.getBouncyCastleProvider());
		assertNotNull(provider.getEcdsaCryptoProvider());
		assertNotNull(provider.getCertificateUtils());
		assertNotNull(provider.getCertPathBuilder());
		assertNotNull(provider.getPemUtils());
		assertNotNull(provider.getX509CertFactory());
	}

}
