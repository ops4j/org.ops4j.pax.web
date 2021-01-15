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
package org.ops4j.pax.web.itest.server.controller;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.Runtime;
import org.ops4j.pax.web.itest.server.support.SSLUtils;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.spi.ServerController;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;
import static org.ops4j.pax.web.itest.server.support.Utils.httpsGET;

/**
 * These tests show how to configure target runtime through {@link ServerController} interface.
 */
@RunWith(Parameterized.class)
public class ServerControllerBasicConfigurationTest extends MultiContainerTestSupport {

	@Override
	public void initAll() throws Exception {
		configurePort();
	}

	@Test
	public void justInstantiateWithoutOsgi() throws Exception {
		ServerController controller = Utils.createServerController(properties -> {
			new File("target/ncsa").mkdirs();
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED, "true");
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR, "target/ncsa");

			if (runtime == Runtime.JETTY) {
				// this file should be used to reconfigure thread pool already set inside Pax Web version of Jetty Server
				properties.put(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES, "target/test-classes/jetty-server.xml");
			} else if (runtime == Runtime.UNDERTOW) {
				properties.put(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES, "target/test-classes/undertow-server-without-listener.xml");
			}
		}, port, runtime, getClass().getClassLoader());

		controller.configure();
		controller.start();

		assertThat(httpGET(port, "/"), containsString("HTTP/1.1 404"));

		controller.stop();
	}

	@Test
	public void sslConfiguration() throws Exception {
		SSLUtils.generateKeyStores();

		ServerController controller = Utils.createServerController(properties -> {
			new File("target/ncsa").mkdirs();
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED, "true");
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR, "target/ncsa");
			if (runtime == Runtime.TOMCAT) {
				properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGFILE, "request");
			} else if (runtime == Runtime.UNDERTOW) {
				// tweak ciphers/protocols to check proper configuration
				properties.put(PaxWebConfig.PID_CFG_PROTOCOLS_INCLUDED, "TLSv1.1");
				properties.put(PaxWebConfig.PID_CFG_CIPHERSUITES_INCLUDED, "TLS_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_256_GCM_SHA384");
			}

			properties.put(PaxWebConfig.PID_CFG_HTTP_ENABLED, "false");
			properties.remove(PaxWebConfig.PID_CFG_HTTP_PORT);
			properties.put(PaxWebConfig.PID_CFG_HTTP_SECURE_ENABLED, "true");
			properties.put(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE, Integer.toString(port));

//			properties.put(PaxWebConfig.PID_CFG_SSL_PROVIDER, "");

			// entire keystore
			properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE, "target/server.jks");
			properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE_PASSWORD, "passw0rd");
			properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE_TYPE, "JKS");
//			properties.put(PaxWebConfig.PID_CFG_SSL_KEYSTORE_PROVIDER, "SUN");
			properties.put(PaxWebConfig.PID_CFG_SSL_KEY_MANAGER_FACTORY_ALGORITHM, "SunX509");
			// single key entry in the keystore
			properties.put(PaxWebConfig.PID_CFG_SSL_KEY_PASSWORD, "passw0rd");
			properties.put(PaxWebConfig.PID_CFG_SSL_KEY_ALIAS, "server"); // to check if we can select one of many

			// entire truststore
			properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE, "target/server.jks");
			properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PASSWORD, "passw0rd");
			properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_TYPE, "JKS");
//			properties.put(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PROVIDER, "SUN");
			properties.put(PaxWebConfig.PID_CFG_SSL_TRUST_MANAGER_FACTORY_ALGORITHM, "X509");

			// remaining SSL parameters
			properties.put(PaxWebConfig.PID_CFG_SSL_CLIENT_AUTH_WANTED, "false");
			properties.put(PaxWebConfig.PID_CFG_SSL_CLIENT_AUTH_NEEDED, "true");
			properties.put(PaxWebConfig.PID_CFG_SSL_PROTOCOL, "TLSv1.2");
			properties.put(PaxWebConfig.PID_CFG_SSL_SECURE_RANDOM_ALGORITHM, "NativePRNGNonBlocking");
			properties.put(PaxWebConfig.PID_CFG_SSL_RENEGOTIATION_ALLOWED, "true");
//			properties.put(PaxWebConfig.PID_CFG_SSL_RENEGOTIATION_LIMIT, "");
			properties.put(PaxWebConfig.PID_CFG_SSL_SESSION_ENABLED, "true");
//			properties.put(PaxWebConfig.PID_CFG_SSL_SESSION_CACHE_SIZE, "");
//			properties.put(PaxWebConfig.PID_CFG_SSL_SESSION_TIMEOUT, "");
		}, port, runtime, getClass().getClassLoader());

		controller.configure();
		controller.start();

		/*
		 * With the above configuration (client auth required), we'll be able to access the server using curl:
		 *
		 * $ curl --cacert ca.cer.pem --cert-type DER --cert client1.cer --key-type DER --key client1-private.key -v https://127.0.0.1:42203/
		 * *   Trying 127.0.0.1:42203...
		 * * TCP_NODELAY set
		 * * Connected to 127.0.0.1 (127.0.0.1) port 42203 (#0)
		 * * ALPN, offering h2
		 * * ALPN, offering http/1.1
		 * * successfully set certificate verify locations:
		 * *   CAfile: ca.cer.pem
		 *   CApath: none
		 * * TLSv1.3 (OUT), TLS handshake, Client hello (1):
		 * * TLSv1.3 (IN), TLS handshake, Server hello (2):
		 * * TLSv1.2 (IN), TLS handshake, Certificate (11):
		 * * TLSv1.2 (IN), TLS handshake, Server key exchange (12):
		 * * TLSv1.2 (IN), TLS handshake, Request CERT (13):
		 * * TLSv1.2 (IN), TLS handshake, Server finished (14):
		 * * TLSv1.2 (OUT), TLS handshake, Certificate (11):
		 * * TLSv1.2 (OUT), TLS handshake, Client key exchange (16):
		 * * TLSv1.2 (OUT), TLS handshake, CERT verify (15):
		 * * TLSv1.2 (OUT), TLS change cipher, Change cipher spec (1):
		 * * TLSv1.2 (OUT), TLS handshake, Finished (20):
		 * * TLSv1.2 (IN), TLS handshake, Finished (20):
		 * * SSL connection using TLSv1.2 / ECDHE-RSA-AES256-GCM-SHA384
		 * * ALPN, server did not agree to a protocol
		 * * Server certificate:
		 * *  subject: CN=server1
		 * *  start date: Dec 31 23:00:00 2018 GMT
		 * *  expire date: Dec 31 23:00:00 2038 GMT
		 * *  subjectAltName: host "127.0.0.1" matched cert's IP address!
		 * *  issuer: CN=CA
		 * *  SSL certificate verify ok.
		 * > GET / HTTP/1.1
		 * > Host: 127.0.0.1:42203
		 * > User-Agent: curl/7.66.0
		 * > Accept: * /*
		 * >
		 * * Mark bundle as not supporting multiuse
		 * < HTTP/1.1 404 Not Found
		 * < Cache-Control: must-revalidate,no-cache,no-store
		 * < Content-Type: text/html;charset=iso-8859-1
		 * < Content-Length: 352
		 *
		 * Or openssl:
		 * $ openssl s_client -connect 127.0.0.1:42203 -CAfile ca.cer.pem -keyform der -key client1-private.key -certform der -cert client1.cer
		 * CONNECTED(00000003)
		 * Can't use SSL_get_servername
		 * depth=1 CN = CA
		 * verify return:1
		 * depth=0 CN = server1
		 * verify return:1
		 * ---
		 * Certificate chain
		 *  0 s:CN = server1
		 *    i:CN = CA
		 * ---
		 * Server certificate
		 * -----BEGIN CERTIFICATE-----
		 * MIIDGzCCAgOgAwIBAgIBATANBgkqhkiG9w0BAQUFADANMQswCQYDVQQDDAJDQTAe
		 * Fw0xODEyMzEyMzAwMDBaFw0zODEyMzEyMzAwMDBaMBIxEDAOBgNVBAMMB3NlcnZl
		 * cjEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCT8n+ZzGpvPZGb0jl2
		 * iInvKbeVDnDl5r887KAsQFwLm87/m3ooovyjRd+d2YfEgtIPvCdmiZobQb5YbMAo
		 * +NhBLUJzXUGFvpoCKuNyLZ/g4XPlhiuw/nqgBRWDOEzc5OxuxcCJ21blhfPpAo4l
		 * PMzOmkSuaLr14qvnS1Y1pjOMYrjiBxADqWAB3M5slTNkd2wmoOqMHE3AS/Fd5kWt
		 * gSF8In9UHdr7dO1/d1OGgx409uaNAKU6y4eunRxgQDYuscxZ0NhQrZ7fOVU7lzt2
		 * tGExb6YOcNrmciWwtHXOLqADXex0xRXm2Xkz4ltHha6N9hmKUnlZzEZws00aYRk1
		 * Ry7/AgMBAAGjgYAwfjAJBgNVHRMEAjAAMAsGA1UdDwQEAwIF4DAfBgNVHSMEGDAW
		 * gBSDj+8/EcYxjP7zN2CYnr4WCLZWdzAdBgNVHQ4EFgQUzoPWz4/KDg7nJgoY0AbZ
		 * 4koQTdcwEwYDVR0lBAwwCgYIKwYBBQUHAwEwDwYDVR0RBAgwBocEfwAAATANBgkq
		 * hkiG9w0BAQUFAAOCAQEAh92LnzsnKMMI39Xs8j13/H+oVCJh4bUhKR6D3oVqJ+8o
		 * 7aG8FchYhwqDUtVnzKKvuL5uIUHf0Bs8X8jmhEoZeP6GtNGoA570aXsdWG/pI61Z
		 * l5Cul6XObuFWrwQXjCDlXKcGqVDiAEnfv0cO/ymWmo3o8Cr/R5h5Ztcyp/47W16w
		 * QUouyI1vqgbI9wf/Ombzt9Ju5jKsQunPzS5UWVIJYgbHt7H9spM1tAmsbBjufkIc
		 * PTmjR5ZkOrgwzN/V04cBOeiZKjYxBrR2m0xv5YPNxEAJXL0Qji2vwcN8nipXe2ix
		 * +4okmXBoHoMe9XNd3GNNRVbSw28CW+IiTvKNTEPvqQ==
		 * -----END CERTIFICATE-----
		 * subject=CN = server1
		 *
		 * issuer=CN = CA
		 *
		 * ---
		 * Acceptable client certificate CA names
		 * CN = server2
		 * CN = server1
		 * CN = CA
		 * Client Certificate Types: RSA sign, DSA sign, ECDSA sign
		 * Requested Signature Algorithms: ECDSA+SHA512:RSA+SHA512:ECDSA+SHA384:RSA+SHA384:ECDSA+SHA256:RSA+SHA256:DSA+SHA256:ECDSA+SHA224:RSA+SHA224:DSA+SHA224:ECDSA+SHA1:RSA+SHA1:DSA+SHA1
		 * Shared Requested Signature Algorithms: ECDSA+SHA512:RSA+SHA512:ECDSA+SHA384:RSA+SHA384:ECDSA+SHA256:RSA+SHA256:DSA+SHA256:ECDSA+SHA224:RSA+SHA224:DSA+SHA224:ECDSA+SHA1:RSA+SHA1:DSA+SHA1
		 * Peer signing digest: SHA256
		 * Peer signature type: RSA
		 * Server Temp Key: ECDH, P-256, 256 bits
		 * ---
		 * SSL handshake has read 1386 bytes and written 2225 bytes
		 * Verification: OK
		 * ---
		 * New, TLSv1.2, Cipher is ECDHE-RSA-AES256-GCM-SHA384
		 * Server public key is 2048 bit
		 * Secure Renegotiation IS supported
		 * Compression: NONE
		 * Expansion: NONE
		 * No ALPN negotiated
		 * SSL-Session:
		 *     Protocol  : TLSv1.2
		 *     Cipher    : ECDHE-RSA-AES256-GCM-SHA384
		 *     Session-ID: 5E831344113312A768A0B9F6ACA412F285718A28DECC66BB286023054CD49569
		 *     Session-ID-ctx:
		 *     Master-Key: 722813FE30E27C1068C80E123F395B943015EB9036652C40130CBAE73636EBFBF41D5311446D227DB40AB9CDE2D7EC4A
		 *     PSK identity: None
		 *     PSK identity hint: None
		 *     SRP username: None
		 *     Start Time: 1585648452
		 *     Timeout   : 7200 (sec)
		 *     Verify return code: 0 (ok)
		 *     Extended master secret: yes
		 * ---
		 * GET / HTTP/1.1
		 * Host: 127.0.0.1
		 * Connection: close
		 *
		 * HTTP/1.1 404 Not Found
		 * Connection: close
		 * Cache-Control: must-revalidate,no-cache,no-store
		 * Content-Type: text/html;charset=iso-8859-1
		 * Content-Length: 352
		 */

		assertThat(httpsGET(port, "/"), containsString("HTTP/1.1 404"));

		controller.stop();
	}

}
