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
package org.ops4j.pax.web.itest.server.controller;

import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityParametersTest {

	public static final Logger LOG = LoggerFactory.getLogger(SecurityParametersTest.class);

	@Test
	public void testSecurityProtocolsAndCiphers() throws Exception {
		// These algorithms are provided by JDK 8u302:
		//  - SSLContext
		//    - Default : sun.security.ssl.SSLContextImpl$DefaultSSLContext
		//    - TLS : sun.security.ssl.SSLContextImpl$TLSContext, aliases: [SSL]
		//    - TLSv1 : sun.security.ssl.SSLContextImpl$TLS10Context, aliases: [SSLv3]
		//    - TLSv1.1 : sun.security.ssl.SSLContextImpl$TLS11Context
		//    - TLSv1.2 : sun.security.ssl.SSLContextImpl$TLS12Context
		//    - TLSv1.3 : sun.security.ssl.SSLContextImpl$TLS13Context
		SSLContext tls = SSLContext.getInstance("TLS");
		SSLContext tls1 = SSLContext.getInstance("TLSv1");
		SSLContext tls11 = SSLContext.getInstance("TLSv1.1");
		SSLContext tls12 = SSLContext.getInstance("TLSv1.2");
		SSLContext tls13 = SSLContext.getInstance("TLSv1.3");

		{
			LOG.info("TLS:");
			tls.init(new KeyManager[0], new TrustManager[0], SecureRandom.getInstance("SHA1PRNG"));
			SSLParameters enabled = tls.getDefaultSSLParameters();
			SSLParameters supported = tls.getSupportedSSLParameters();
			LOG.info("    Enabled/Default protocols: {}", String.join(", ", enabled.getProtocols()));
			LOG.info("    Supported protocols: {}", String.join(", ", supported.getProtocols()));
			LOG.info("    Enabled/Default cipher suites: {}", String.join(", ", enabled.getCipherSuites()));
			LOG.info("    Supported cipher suites: {}", String.join(", ", supported.getCipherSuites()));
		}

		{
			LOG.info("TLS 1.3:");
			tls13.init(new KeyManager[0], new TrustManager[0], SecureRandom.getInstance("SHA1PRNG"));
			SSLParameters enabled = tls13.getDefaultSSLParameters();
			SSLParameters supported = tls13.getSupportedSSLParameters();
			LOG.info("    Enabled/Default protocols: {}", String.join(", ", enabled.getProtocols()));
			LOG.info("    Supported protocols: {}", String.join(", ", supported.getProtocols()));
			LOG.info("    Enabled/Default cipher suites: {}", String.join(", ", enabled.getCipherSuites()));
			LOG.info("    Supported cipher suites: {}", String.join(", ", supported.getCipherSuites()));
		}

		// according to sun.security.ssl.CipherSuite, the preference is:
		// 1. Prefer Suite B compliant cipher suites, see RFC6460 (To be changed later, see below).
		// 2. Prefer the stronger bulk cipher, in the order of AES_256(GCM), AES_128(GCM), AES_256, AES_128, 3DES-EDE.
		// 3. Prefer the stronger MAC algorithm, in the order of SHA384, SHA256, SHA, MD5.
		// 4. Prefer the better performance of key exchange and digital signature algorithm, in the order of
		//    ECDHE-ECDSA, ECDHE-RSA, RSA, ECDH-ECDSA, ECDH-RSA, DHE-RSA, DHE-DSS.
		//
		// (GCM - Galois Counter Mode)

		// see https://www.iana.org/assignments/tls-parameters/tls-parameters.xhtml

		// according to "3.1. Minimum Levels of Security (minLOS) for Suite B TLS" of RFC6460, there are:
		//  - Suite B Combination 1 (minimum 128 bit)
		//     - AES with 128-bit key in GCM mode
		//     - ECDH using the 256-bit prime modulus curve P-256
		//     - TLS PRF with SHA-256
		//  - Suite B Combination 2 (minimum 192 bit)
		//     - AES with 256-bit key in GCM mode
		//     - ECDH using the 384-bit prime modulus curve P-384
		//     - TLS PRF with SHA-384

		// TLS 1.3 gives (manually ordered by me according to the above) these (default/enabled and supported):
		//  - TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
		//  - TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384
		//  - TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA
		//  - TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
		//  - TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256
		//  - TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA
		//  - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
		//  - TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384
		//  - TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA
		//  - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
		//  - TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256
		//  - TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA
		//  - TLS_RSA_WITH_AES_256_GCM_SHA384
		//  - TLS_RSA_WITH_AES_256_CBC_SHA256
		//  - TLS_RSA_WITH_AES_256_CBC_SHA
		//  - TLS_RSA_WITH_AES_128_GCM_SHA256
		//  - TLS_RSA_WITH_AES_128_CBC_SHA256
		//  - TLS_RSA_WITH_AES_128_CBC_SHA
		//  - TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384
		//  - TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384
		//  - TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA
		//  - TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256
		//  - TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256
		//  - TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA
		//  - TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384
		//  - TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384
		//  - TLS_ECDH_RSA_WITH_AES_256_CBC_SHA
		//  - TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256
		//  - TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256
		//  - TLS_ECDH_RSA_WITH_AES_128_CBC_SHA
		//  - TLS_DHE_RSA_WITH_AES_256_GCM_SHA384
		//  - TLS_DHE_RSA_WITH_AES_256_CBC_SHA256
		//  - TLS_DHE_RSA_WITH_AES_256_CBC_SHA
		//  - TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
		//  - TLS_DHE_RSA_WITH_AES_128_CBC_SHA256
		//  - TLS_DHE_RSA_WITH_AES_128_CBC_SHA
		//  - TLS_DHE_DSS_WITH_AES_256_GCM_SHA384
		//  - TLS_DHE_DSS_WITH_AES_256_CBC_SHA256
		//  - TLS_DHE_DSS_WITH_AES_256_CBC_SHA
		//  - TLS_DHE_DSS_WITH_AES_128_GCM_SHA256
		//  - TLS_DHE_DSS_WITH_AES_128_CBC_SHA256
		//  - TLS_DHE_DSS_WITH_AES_128_CBC_SHA
		//  - TLS_AES_256_GCM_SHA384
		//  - TLS_AES_128_GCM_SHA256
		//  - TLS_EMPTY_RENEGOTIATION_INFO_SCSV

		// however sun.security.ssl.CipherSuite contains suite-protocol mapping...
		// TLS 1.3 (https://www.rfc-editor.org/rfc/rfc8446.html#appendix-B.4):
		// - TLS_AES_256_GCM_SHA384
		// - TLS_AES_128_GCM_SHA256
		// - TLS_CHACHA20_POLY1305_SHA256 (unsupported)
		// - TLS_AES_128_CCM_SHA256 (unsupported)
		// - TLS_AES_128_CCM_8_SHA256 (unsupported)
		// TLS 1.2 (in order of appearance in sun.security.ssl.CipherSuite
		// - TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
		// - TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
		// - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
		// - TLS_RSA_WITH_AES_256_GCM_SHA384
		// - TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384
		// - TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384
		// - TLS_DHE_RSA_WITH_AES_256_GCM_SHA384
		// - TLS_DHE_DSS_WITH_AES_256_GCM_SHA384
		// - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
		// - TLS_RSA_WITH_AES_128_GCM_SHA256
		// - TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256
		// - TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256
		// - TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
		// - TLS_DHE_DSS_WITH_AES_128_GCM_SHA256
		// - TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384
		// - TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384
		// - TLS_RSA_WITH_AES_256_CBC_SHA256
		// - TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384
		// - TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384
		// - TLS_DHE_RSA_WITH_AES_256_CBC_SHA256
		// - TLS_DHE_DSS_WITH_AES_256_CBC_SHA256
		// - TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA
		// - TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA
		// - TLS_RSA_WITH_AES_256_CBC_SHA
		// - TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA
		// - TLS_ECDH_RSA_WITH_AES_256_CBC_SHA
		// - TLS_DHE_RSA_WITH_AES_256_CBC_SHA
		// - TLS_DHE_DSS_WITH_AES_256_CBC_SHA
		// - TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256
		// - TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256
		// - TLS_RSA_WITH_AES_128_CBC_SHA256
		// - TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256
		// - TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256
		// - TLS_DHE_RSA_WITH_AES_128_CBC_SHA256
		// - TLS_DHE_DSS_WITH_AES_128_CBC_SHA256
		// - TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA
		// - TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA
		// - TLS_RSA_WITH_AES_128_CBC_SHA
		// - TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA
		// - TLS_ECDH_RSA_WITH_AES_128_CBC_SHA
		// - TLS_DHE_RSA_WITH_AES_128_CBC_SHA
		// - TLS_DHE_DSS_WITH_AES_128_CBC_SHA
		// - TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA
		// - TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA
		// - SSL_RSA_WITH_3DES_EDE_CBC_SHA/TLS_RSA_WITH_3DES_EDE_CBC_SHA
		// - TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA
		// - TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA
		// - SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA/TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA
		// - SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA/TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA
		// - TLS_EMPTY_RENEGOTIATION_INFO_SCSV
		// - TLS_DH_anon_WITH_AES_256_GCM_SHA384
		// - TLS_DH_anon_WITH_AES_128_GCM_SHA256
		// - TLS_DH_anon_WITH_AES_256_CBC_SHA256
		// - TLS_ECDH_anon_WITH_AES_256_CBC_SHA
		// - TLS_DH_anon_WITH_AES_256_CBC_SHA
		// - TLS_DH_anon_WITH_AES_128_CBC_SHA256
		// - TLS_ECDH_anon_WITH_AES_128_CBC_SHA
		// - TLS_DH_anon_WITH_AES_128_CBC_SHA
		// - TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA
		// - SSL_DH_anon_WITH_3DES_EDE_CBC_SHA/TLS_DH_anon_WITH_3DES_EDE_CBC_SHA
		// - TLS_ECDHE_ECDSA_WITH_RC4_128_SHA
		// - TLS_ECDHE_RSA_WITH_RC4_128_SHA
		// - SSL_RSA_WITH_RC4_128_SHA/TLS_RSA_WITH_RC4_128_SHA
		// - TLS_ECDH_ECDSA_WITH_RC4_128_SHA
		// - TLS_ECDH_RSA_WITH_RC4_128_SHA
		// - SSL_RSA_WITH_RC4_128_MD5/TLS_RSA_WITH_RC4_128_MD5
		// - TLS_ECDH_anon_WITH_RC4_128_SHA
		// - SSL_DH_anon_WITH_RC4_128_MD5/TLS_DH_anon_WITH_RC4_128_MD5
		// - TLS_RSA_WITH_NULL_SHA256
		// - TLS_ECDHE_ECDSA_WITH_NULL_SHA
		// - TLS_ECDHE_RSA_WITH_NULL_SHA
		// - SSL_RSA_WITH_NULL_SHA/TLS_RSA_WITH_NULL_SHA
		// - TLS_ECDH_ECDSA_WITH_NULL_SHA
		// - TLS_ECDH_RSA_WITH_NULL_SHA
		// - TLS_ECDH_anon_WITH_NULL_SHA
		// - SSL_RSA_WITH_NULL_MD5/TLS_RSA_WITH_NULL_MD5
		// - TLS_KRB5_WITH_3DES_EDE_CBC_SHA
		// - TLS_KRB5_WITH_3DES_EDE_CBC_MD5
		// - TLS_KRB5_WITH_RC4_128_SHA
		// - TLS_KRB5_WITH_RC4_128_MD5
	}

}
