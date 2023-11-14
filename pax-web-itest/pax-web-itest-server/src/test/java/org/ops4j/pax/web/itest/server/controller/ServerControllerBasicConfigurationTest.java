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
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.Runtime;
import org.ops4j.pax.web.itest.server.support.SSLUtils;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.context.DefaultHttpContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.osgi.framework.Bundle;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
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

		System.setProperty("javax.net.debug", "ssl");

		ServerController controller = Utils.createServerController(properties -> {
			new File("target/ncsa").mkdirs();
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED, "true");
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR, "target/ncsa");
			if (runtime == Runtime.TOMCAT) {
				properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGFILE, "request");
			} else if (runtime == Runtime.UNDERTOW) {
				// tweak ciphers/protocols to check proper configuration
				// see https://datatracker.ietf.org/doc/html/rfc6460
				// see sun.security.ssl.CipherSuite
			}
			properties.put(PaxWebConfig.PID_CFG_SSL_PROTOCOL, "TLSv1.3");
			properties.put(PaxWebConfig.PID_CFG_PROTOCOLS_INCLUDED, "TLSv1.3");
			properties.put(PaxWebConfig.PID_CFG_CIPHERSUITES_INCLUDED, "TLS_AES_256_GCM_SHA384");

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
			properties.put(PaxWebConfig.PID_CFG_SSL_SECURE_RANDOM_ALGORITHM, "SHA1PRNG");
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
		 * $ curl --cacert ca.cer.pem --cert-type DER --cert client1.cer --key-type DER --key client1-private.key -v https://127.0.0.1:33003
		 * *   Trying 127.0.0.1:33003...
		 * * Connected to 127.0.0.1 (127.0.0.1) port 33003 (#0)
		 * * ALPN, offering h2
		 * * ALPN, offering http/1.1
		 * * successfully set certificate verify locations:
		 * *   CAfile: ca.cer.pem
		 *   CApath: none
		 * * TLSv1.3 (OUT), TLS handshake, Client hello (1):
		 * * TLSv1.3 (IN), TLS handshake, Server hello (2):
		 * * TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
		 * * TLSv1.3 (OUT), TLS handshake, Client hello (1):
		 * * TLSv1.3 (IN), TLS handshake, Server hello (2):
		 * * TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
		 * * TLSv1.3 (IN), TLS handshake, Request CERT (13):
		 * * TLSv1.3 (IN), TLS handshake, Certificate (11):
		 * * TLSv1.3 (IN), TLS handshake, CERT verify (15):
		 * * TLSv1.3 (IN), TLS handshake, Finished (20):
		 * * TLSv1.3 (OUT), TLS handshake, Certificate (11):
		 * * TLSv1.3 (OUT), TLS handshake, CERT verify (15):
		 * * TLSv1.3 (OUT), TLS handshake, Finished (20):
		 * * SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
		 * * ALPN, server did not agree to a protocol
		 * * Server certificate:
		 * *  subject: CN=server1
		 * *  start date: Dec 31 23:00:00 2018 GMT
		 * *  expire date: Dec 31 23:00:00 2038 GMT
		 * *  subjectAltName: host "127.0.0.1" matched cert's IP address!
		 * *  issuer: CN=CA
		 * *  SSL certificate verify ok.
		 * > GET / HTTP/1.1
		 * > Host: 127.0.0.1:33003
		 * > User-Agent: curl/7.71.1
		 * > Accept: * slash *
		 * >
		 * * TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
		 * * Mark bundle as not supporting multiuse
		 * < HTTP/1.1 404 Not Found
		 * < Cache-Control: must-revalidate,no-cache,no-store
		 * < Content-Type: text/html;charset=iso-8859-1
		 * < Content-Length: 352
		 * <
		 * <html>
		 * <head>
		 * <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
		 * <title>Error 404 Not Found</title>
		 * </head>
		 * <body><h2>HTTP ERROR 404 Not Found</h2>
		 * <table>
		 * <tr><th>URI:</th><td>/</td></tr>
		 * <tr><th>STATUS:</th><td>404</td></tr>
		 * <tr><th>MESSAGE:</th><td>Not Found</td></tr>
		 * <tr><th>SERVLET:</th><td>-</td></tr>
		 * </table>
		 *
		 * </body>
		 * </html>
		 * * Connection #0 to host 127.0.0.1 left intact
		 *
		 * Or openssl:
		 * $ openssl s_client -connect 127.0.0.1:38615 -tls1_3 -CAfile ca.cer.pem -cert client1.cer -certform der -key client1-private.key -keyform der
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
		 * MIIDMTCCAhmgAwIBAgIBATANBgkqhkiG9w0BAQUFADANMQswCQYDVQQDDAJDQTAe
		 * Fw0xODEyMzEyMzAwMDBaFw0zODEyMzEyMzAwMDBaMBIxEDAOBgNVBAMMB3NlcnZl
		 * cjEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCXlw/uSyg7KO41ok10
		 * tellidbaZq58GiDyP2JjmQS8IeAZ8OpFfQLcKhrEQbcCalwaNbl1qpFTmhsNtrGw
		 * Fv7G3AoEHfYvySHw0hnQA/6aciQz7rIWnR1e/kT4Pr6F1ZqEJ70Vc0XdrP7wOhiN
		 * PCmeS4NdAoJc1WoMe3Am1s/Fh/KZa6BYxf5TDfEiFgqcIMjZf78MwWt4PMg0Q4ok
		 * bI4gOLudq00jmu6Jch03sLkY0mxg3HJriD6VHGx0C8w1qeiFd23xYrVk/iy9Wday
		 * u2WmuWgTXUvRHKIrvQkugHCJEeBmMcShq+7nflmM5IcLhIxEkI4ivpgRefOaxu8N
		 * nw6DAgMBAAGjgZYwgZMwCQYDVR0TBAIwADALBgNVHQ8EBAMCBeAwHwYDVR0jBBgw
		 * FoAUuZRaF3E7rCYUHn26YkQYKmkeD9kwHQYDVR0OBBYEFF6e2ws8JmTOxgohstFd
		 * K9NtTFO/MB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAaBgNVHREEEzAR
		 * hwR/AAABgglsb2NhbGhvc3QwDQYJKoZIhvcNAQEFBQADggEBAF8UQFWyftbhOTkQ
		 * N/WEA2w4U+fi0IEMLvT6ixsvxY3JeSPRTpthB/fwR4T0KjXJ9NSy/QFWeoiKFMNu
		 * IM7zAqgX6HWQ1tJNivUqeYTGDfS+hjYAiTB0/nUXf2wAg2z+R33tVg2e8HGeiF6Q
		 * ihIQR+Bwp3jI3x+alu3vvfQwzUWIPdlDWWO34qyI5TEg03fzUqiugjQxuAn8osyE
		 * qgye0cI9Q2ZHzwU81xZn3pnFUpSuFVvyltnbDVwfnlnaLp9CSVirw2QGnfAJfSGy
		 * KTbKYPZQCgMRqKT8EZeDZGRojf1pZ8BB8iH9efleByONMAESRO0Z90n/XTVLSGX2
		 * +LEnIUA=
		 * -----END CERTIFICATE-----
		 * subject=CN = server1
		 *
		 * issuer=CN = CA
		 *
		 * ---
		 * Acceptable client certificate CA names
		 * CN = server1
		 * CN = server2
		 * CN = CA
		 * Requested Signature Algorithms: ECDSA+SHA256:ECDSA+SHA384:ECDSA+SHA512:RSA-PSS+SHA256:RSA-PSS+SHA384:RSA-PSS+SHA512:RSA-PSS+SHA256:RSA-PSS+SHA384:RSA-PSS+SHA512:RSA+SHA256:RSA+SHA384:RSA+SHA512:ECDSA+SHA1:RSA+SHA1
		 * Shared Requested Signature Algorithms: ECDSA+SHA256:ECDSA+SHA384:ECDSA+SHA512:RSA-PSS+SHA256:RSA-PSS+SHA384:RSA-PSS+SHA512:RSA-PSS+SHA256:RSA-PSS+SHA384:RSA-PSS+SHA512:RSA+SHA256:RSA+SHA384:RSA+SHA512
		 * Peer signing digest: SHA256
		 * Peer signature type: RSA-PSS
		 * Server Temp Key: ECDH, P-256, 256 bits
		 * ---
		 * SSL handshake has read 1722 bytes and written 2485 bytes
		 * Verification: OK
		 * ---
		 * New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384
		 * Server public key is 2048 bit
		 * Secure Renegotiation IS NOT supported
		 * Compression: NONE
		 * Expansion: NONE
		 * No ALPN negotiated
		 * Early data was not sent
		 * Verify return code: 0 (ok)
		 * ---
		 * ---
		 * Post-Handshake New Session Ticket arrived:
		 * SSL-Session:
		 *     Protocol  : TLSv1.3
		 *     Cipher    : TLS_AES_256_GCM_SHA384
		 *     Session-ID: 535C3D0B6565AA2B797AFAFDDDF42B87253E0452F9F566EBC50D2DFBFE0F31CE
		 *     Session-ID-ctx:
		 *     Resumption PSK: 6DBB4873A5BA4EA18AC71FF39D164163840D073CFF5BA34E7011FDB1654D92187A5BCAC76BEC5C2E83371DD7C22900A5
		 *     PSK identity: None
		 *     PSK identity hint: None
		 *     SRP username: None
		 *     TLS session ticket lifetime hint: 86400 (seconds)
		 *     TLS session ticket:
		 *     0000 - b9 e4 ed 5c 10 a9 b0 e0-4f eb 53 13 35 68 fd 41   ...\....O.S.5h.A
		 *     0010 - c8 33 c1 63 b3 e8 fc 7d-08 df 16 03 54 c4 8d 34   .3.c...}....T..4
		 *
		 *     Start Time: 1626857245
		 *     Timeout   : 7200 (sec)
		 *     Verify return code: 0 (ok)
		 *     Extended master secret: no
		 *     Max Early Data: 0
		 * ---
		 * read R BLOCK
		 * GET / HTTP/1.1
		 * Host: 127.0.0.1
		 * Connection: close
		 *
		 * HTTP/1.1 404 Not Found
		 * Connection: close
		 * Cache-Control: must-revalidate,no-cache,no-store
		 * Content-Type: text/html;charset=iso-8859-1
		 * Content-Length: 352
		 *
		 * <html>
		 * <head>
		 * <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
		 * <title>Error 404 Not Found</title>
		 * </head>
		 * <body><h2>HTTP ERROR 404 Not Found</h2>
		 * <table>
		 * <tr><th>URI:</th><td>/</td></tr>
		 * <tr><th>STATUS:</th><td>404</td></tr>
		 * <tr><th>MESSAGE:</th><td>Not Found</td></tr>
		 * <tr><th>SERVLET:</th><td>-</td></tr>
		 * </table>
		 *
		 * </body>
		 * </html>
		 * closed
		 */

		final List<X509Certificate[]> certificates = new ArrayList<>();

		Servlet servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");

				certificates.add((X509Certificate[]) req.getAttribute("jakarta.servlet.request.X509Certificate"));

				resp.getWriter().print("OK");
				resp.getWriter().close();
			}
		};

		Batch batch = new Batch("Register Single Servlet");

		ServletContextModel scm = new ServletContextModel("/c");
		batch.addServletContextModel(scm);

		Bundle bundle = mockBundle("sample", false);

		WebContainerContext wcc = new DefaultHttpContext(bundle);

		OsgiContextModel osgiContext = new OsgiContextModel(wcc, bundle, "/", false);
		batch.addOsgiContextModel(osgiContext, scm);

		batch.addServletModel(new ServletModel.Builder()
				.withServletName("my-servlet")
				.withUrlPatterns(new String[] { "/s/*" })
				.withServlet(servlet)
				.withOsgiContextModel(osgiContext)
				.withRegisteringBundle(bundle)
				.build());

		controller.sendBatch(batch);

		assertThat(httpsGET(port, "/s/test"), endsWith("OK"));
		assertThat(certificates.size(), equalTo(1));
		assertNotNull(certificates.get(0));

		controller.stop();
	}

}
