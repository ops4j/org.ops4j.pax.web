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
package org.ops4j.pax.web.itest.server;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContexts;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.ops4j.pax.web.itest.server.support.SSLUtils;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.internal.ConfigurationBuilder;
import org.ops4j.pax.web.service.internal.DefaultHttpContext;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.MetaTypePropertyResolver;
import org.ops4j.pax.web.service.jetty.internal.JettyServerControllerFactory;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.tomcat.internal.TomcatServerControllerFactory;
import org.ops4j.pax.web.service.undertow.internal.UndertowServerControllerFactory;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.nio.NioXnioProvider;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ServerControllerTest {

	public static Logger LOG = LoggerFactory.getLogger(ServerControllerTest.class);

	private int port;

	@Parameter
	public Runtime runtime;

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ Runtime.JETTY },
				{ Runtime.TOMCAT },
				{ Runtime.UNDERTOW }
		});
	}

	@Before
	public void init() throws Exception {
		ServerSocket serverSocket = new ServerSocket(0);
		port = serverSocket.getLocalPort();
		serverSocket.close();
	}

	@Test
	public void justInstantiateWithoutOsgi() throws Exception {
		ServerController controller = create(properties -> {
			new File("target/ncsa").mkdirs();
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED, "true");
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR, "target/ncsa");

			if (runtime == Runtime.JETTY) {
				// this file should be used to reconfigure thread pool already set inside Pax Web version of Jetty Server
				properties.put(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES, "target/test-classes/jetty-server.xml");
			} else if (runtime == Runtime.UNDERTOW) {
				properties.put(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES, "target/test-classes/undertow-server-without-listener.xml");
			}
		});

		controller.configure();
		controller.start();

		assertThat(get(port, "/"), containsString("HTTP/1.1 404"));

		controller.stop();
	}

	@Test
	public void sslConfiguration() throws Exception {
		SSLUtils.generateKeyStores();

		ServerController controller = create(properties -> {
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
		});

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

		assertThat(gets(port, "/"), containsString("HTTP/1.1 404"));

		controller.stop();
	}

	@Test
	public void registerSingleServletUsingExplicitBatch() throws Exception {
		ServerController controller = create(properties -> {
			new File("target/ncsa").mkdirs();
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED, "true");
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR, "target/ncsa");

			if (runtime == Runtime.JETTY) {
				// this file should be used to reconfigure thread pool already set inside Pax Web version of Jetty Server
				properties.put(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES, "target/test-classes/jetty-server.xml");
			}
		});
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);

		WebContainerContext wcc = new DefaultHttpContext(bundle) {
			@Override
			public URL getResource(String name) {
				// this should be used when calling ServletContext.getResource
				try {
					return new URL("file://" + name);
				} catch (MalformedURLException ignored) {
					return null;
				}
			}

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				LOG.info("handleSecurity(" + request + ")");
				return request.getHeader("Let-Me-In") != null;
			}
		};

		Servlet servlet = new HttpServlet() {
			private ServletConfig config;

			private final Map<ServletContext, Boolean> contexts = new IdentityHashMap<>();

			@Override
			public void init(ServletConfig config) throws ServletException {
				super.init(config);
				assertThat(config.getInitParameter("p1"), equalTo("v1"));
				assertThat(super.getInitParameter("p1"), equalTo("v1"));
				contexts.put(config.getServletContext(), true);
				this.config = config;
			}

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setContentType("text/plain");

				contexts.put(getServletContext(), true);
				contexts.put(req.getServletContext(), true);
				contexts.put(req.getSession().getServletContext(), true);
				contexts.put(config.getServletContext(), true);
				contexts.put(getServletConfig().getServletContext(), true);

				assertThat(contexts.size(), equalTo(1));

				assertThat(super.getInitParameter("p1"), equalTo("v1"));

				// this should give us "file:/something"
				resp.getWriter().print(req.getServletContext().getResource("/something").toString());
			}
		};

		Batch batch = new Batch("Register Single Servlet");

		ServerModel server = new ServerModel(new SameThreadExecutor());
		ServletContextModel context = new ServletContextModel("/c");
		batch.addServletContextModel(server, context);

		OsgiContextModel osgiContext = new OsgiContextModel(wcc, bundle);
		osgiContext.setServletContextModel(context);
		batch.addOsgiContextModel(osgiContext);

		Map<String, String> initParams = new HashMap<>();
		initParams.put("p1", "v1");

		batch.addServletModel(server, new ServletModel.Builder()
				.withServletName("my-servlet")
				.withUrlPatterns(new String[] { "/s/*" })
				.withServlet(servlet)
				.withInitParams(initParams)
				.withOsgiContextModel(osgiContext)
				.withRegisteringBundle(bundle)
				.build());

		controller.sendBatch(batch);

		String response = get(port, "/c/s/1", "Let-Me-In: true");
		assertTrue(response.endsWith("file:/something"));

		response = get(port, "/c/s/1");
		assertTrue(response.contains("HTTP/1.1 403"));

		controller.stop();
	}

	@Test
	public void registerSingleServletUsingWebContainer() throws Exception {
		ServerController controller = create(null);
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		ServerModel server = new ServerModel(new SameThreadExecutor());

		WebContainer wc = new HttpServiceEnabled(bundle, controller, server, null, controller.getConfiguration());

		HttpContext context = new HttpContext() {
			@Override
			public URL getResource(String name) {
				// this should be used when calling ServletContext.getResource
				try {
					return new URL("file://" + name);
				} catch (MalformedURLException ignored) {
					return null;
				}
			}

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				LOG.info("handleSecurity(" + request + ")");
				return request.getHeader("Let-Me-In") != null;
			}

			@Override
			public String getMimeType(String name) {
				return null;
			}
		};

		Servlet servlet = new HttpServlet() {
			private ServletConfig config;

			private final Map<ServletContext, Boolean> contexts = new IdentityHashMap<>();

			@Override
			public void init(ServletConfig config) throws ServletException {
				super.init(config);
				assertThat(config.getInitParameter("p1"), equalTo("v1"));
				assertThat(super.getInitParameter("p1"), equalTo("v1"));
				contexts.put(config.getServletContext(), true);
				this.config = config;
			}

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setContentType("text/plain");

				contexts.put(getServletContext(), true);
				contexts.put(req.getServletContext(), true);
				contexts.put(req.getSession().getServletContext(), true);
				contexts.put(config.getServletContext(), true);
				contexts.put(getServletConfig().getServletContext(), true);

				assertThat(contexts.size(), equalTo(1));

				assertThat(super.getInitParameter("p1"), equalTo("v1"));

				// this should give us "file:/something"
				resp.getWriter().print(req.getServletContext().getResource("/something").toString());
			}
		};

		Dictionary<String, String> initParams = new Hashtable<>();
		initParams.put("p1", "v1");

		wc.registerServlet(servlet, "my-servlet", new String[] { "/s/*" }, initParams, context);

		String response = get(port, "/s/1", "Let-Me-In: true");
		assertTrue(response.endsWith("file:/something"));

		response = get(port, "/s/1");
		assertTrue(response.contains("HTTP/1.1 403"));

		controller.stop();
	}

	@Test
	public void registerFilterAndServletUsingExcplicitBatch() throws Exception {
		ServerController controller = create(null);
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleContext context = mock(BundleContext.class);
		when(bundle.getBundleContext()).thenReturn(context);

		WebContainerContext wcc1 = new DefaultHttpContext(bundle);
		WebContainerContext wcc2 = new DefaultHttpContext(bundle, "special");

		// when single instance is added more than once (passed in ServletModel), init(ServletConfig)
		// operates on single instance and even the Whiteboard Service specification suggests using Prototype
		// Service. Otherwise, init() would be called more than once on single instance providing different
		// ServletConfig objects (with different - and usually wrong ServletContext)
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> ref = mock(ServiceReference.class);
		when(context.getService(ref)).thenReturn(new MyHttpServlet());

		Filter filter = new HttpFilter() {
			@Override
			protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().write(getFilterName() + "1");
				if (!"/d".equals(req.getServletContext().getContextPath())) {
					// in /d we know we don't map to any servlet
					chain.doFilter(req, resp);
				}
				resp.getWriter().write(getFilterName() + "2");
				resp.getWriter().close();
			}
		};

		Batch batch = new Batch("Register Servlet and Filter");

		// two contexts. servlet will be registered to /c, filter - to /c and /d
		ServerModel server = new ServerModel(new SameThreadExecutor());

		ServletContextModel contextC = new ServletContextModel("/c");
		ServletContextModel contextD = new ServletContextModel("/d");
		ServletContextModel contextE = new ServletContextModel("/e");
		batch.addServletContextModel(server, contextC);
		batch.addServletContextModel(server, contextD);
		batch.addServletContextModel(server, contextE);

		OsgiContextModel osgiContextC = new OsgiContextModel(wcc1, bundle, contextC);
		OsgiContextModel osgiContextC2 = new OsgiContextModel(wcc2, bundle, contextC);
		OsgiContextModel osgiContextD = new OsgiContextModel(wcc1, bundle, contextD);
		OsgiContextModel osgiContextE = new OsgiContextModel(wcc1, bundle, contextE);
		batch.addOsgiContextModel(osgiContextC);
		batch.addOsgiContextModel(osgiContextC2);
		batch.addOsgiContextModel(osgiContextD);
		batch.addOsgiContextModel(osgiContextE);

		Map<String, String> initParams = new HashMap<>();
		initParams.put("p1", "v1");

		batch.addServletModel(server, new ServletModel.Builder()
				.withServletName("my-servlet1")
				.withUrlPatterns(new String[] { "/s/*" }) // responds to /*/s/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.withOsgiContextModel(osgiContextC) // responds to /c/s/*
				.withOsgiContextModel(osgiContextE) // responds to /e/s/*
				.withRegisteringBundle(bundle)
				.build());
		batch.addServletModel(server, new ServletModel.Builder()
				.withServletName("my-servlet2")
				.withUrlPatterns(new String[] { "/s2/*" }) // responds to /*/s2/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.withOsgiContextModel(osgiContextC2) // responds to /c/s2/*
				.withRegisteringBundle(bundle)
				.build());

		Map<String, Set<FilterModel>> filters = new HashMap<>();
		Set<FilterModel> set = new TreeSet<>();
		// this filter is NOT registered to osgiContextC2, so should NOT be mapped to /c/s2/*
		set.add(new FilterModel.Builder()
				.withFilterName("my-filter")
				.withUrlPatterns(new String[] { "/*" }) // maps to /*/* depending on associated contexts
				.withFilter(filter)
				.withOsgiContextModel(osgiContextC) // maps to /c/*
				.withOsgiContextModel(osgiContextD) // maps to /d/*
				.withRegisteringBundle(bundle)
				.build());
		filters.put("/c", set);
		filters.put("/d", set);
		batch.updateFilters(filters);

		controller.sendBatch(batch);

		// filter -> servlet
		String response;
		response = get(port, "/c/s/1");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-servlet1[/c]my-filter2"));

		// just one filter in the chain, without target servlet
		response = get(port, "/d/s/1");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-filter2"));

		// just servlet, because /* filter doesn't use servlet's ServletContextHelper
		response = get(port, "/c/s2/1");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet2[/c]"));

		// just servlet, because /* filter isn't associated with OsgiContext for /e
		response = get(port, "/e/s/1");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet1[/e]"));

		controller.stop();
	}

	@Test
	public void registerFilterAndServletUsingWebContainer() throws Exception {
		ServerController controller = create(null);
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleContext context = mock(BundleContext.class);
		when(bundle.getBundleContext()).thenReturn(context);

		ServerModel server = new ServerModel(new SameThreadExecutor());

		Configuration config = controller.getConfiguration();
		HttpServiceEnabled wc = new HttpServiceEnabled(bundle, controller, server, null, config);

		// 3 physical servlet context models
		Batch batch = new Batch("Initialization Batch");
		server.getOrCreateServletContextModel("/c", batch);
		server.getOrCreateServletContextModel("/d", batch);
		server.getOrCreateServletContextModel("/e", batch);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		WebContainerContext wccC1 = wc.createDefaultHttpContext("wccC1");
		WebContainerContext wccC2 = wc.createDefaultHttpContext("wccC2");
		WebContainerContext wccD1 = wc.createDefaultHttpContext("wccD1");
		WebContainerContext wccE1 = wc.createDefaultHttpContext("wccE1");

		// 4 logical OSGi context models
		batch = new Batch("Initialization Batch");
		server.associateHttpContext(wccC1, server.createNewContextModel(wccC1, "/c", bundle, batch));
		server.associateHttpContext(wccC2, server.createNewContextModel(wccC2, "/c", bundle, batch));
		server.associateHttpContext(wccD1, server.createNewContextModel(wccD1, "/d", bundle, batch));
		server.associateHttpContext(wccE1, server.createNewContextModel(wccE1, "/e", bundle, batch));
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> ref = mock(ServiceReference.class);
		when(context.getService(ref)).thenReturn(new MyHttpServlet());

		Filter filter = new HttpFilter() {
			@Override
			protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().write(getFilterName() + "1");
				if (!"/d".equals(req.getServletContext().getContextPath())) {
					// in /d we know we don't map to any servlet
					chain.doFilter(req, resp);
				}
				resp.getWriter().write(getFilterName() + "2");
				resp.getWriter().close();
			}
		};

		Map<String, String> initParams = new HashMap<>();
		initParams.put("p1", "v1");

		wc.doRegisterServlet(Arrays.asList(wccC1, wccE1), new ServletModel.Builder()
				.withServletName("my-servlet1")
				.withUrlPatterns(new String[] { "/s/*" }) // responds to /c/s/* or /e/s/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.build());
		wc.doRegisterServlet(Collections.singletonList(wccC2), new ServletModel.Builder()
				.withServletName("my-servlet2")
				.withUrlPatterns(new String[] { "/s2/*" }) // responds to /c/s2/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.build());

		// this filter is NOT registered to osgiContextC2, so should NOT be mapped to /c/s2/*
		wc.doRegisterFilter(Arrays.asList(wccC1, wccD1), new FilterModel.Builder()
				.withFilterName("my-filter")
				.withUrlPatterns(new String[] { "/*" }) // maps to /c/* or /d/* depending on associated contexts
				.withFilter(filter)
				.withRegisteringBundle(bundle)
				.build());

		// filter -> servlet
		String response;
		response = get(port, "/c/s/1");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-servlet1[/c]my-filter2"));

		// just one filter in the chain, without target servlet
		response = get(port, "/d/s/1");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-filter2"));

		// just servlet, because /* filter doesn't use my-servlet2's ServletContextHelper
		response = get(port, "/c/s2/1");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet2[/c]"));

		// just servlet, because /* filter isn't associated with OsgiContext for /e
		response = get(port, "/e/s/1");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet1[/e]"));

		controller.stop();
	}

	/**
	 * <p>Test for Whiteboard service registration of servlets to different OSGi contexts and handling name
	 * conflicts.</p>
	 *
	 * <p>Have 3 contexts (each with single OSGi context associated):<ul>
	 *     <li>/c1</li>
	 *     <li>/c2</li>
	 *     <li>/c3</li>
	 *     <li>/c4</li>
	 * </ul>
	 * Servlet registration plan (newer servlet has higher service.id):<ul>
	 *     <li>"s1"(1) with rank=0 ragistered to /c1 and /c2 - should be OK</li>
	 *     <li>"s1"(2) with rank=3 registered to /c3 - should be OK</li>
	 *     <li>"s1"(3) with rank=0 registered to /c1 - should be registered as disabled</li>
	 *     <li>"s1"(4) with rank=2 registered to /c2 and /c3 - should be registered as disabled because of "s1"(2)</li>
	 *     <li>"s1"(5) with rank=1 registered to /c2 and /c4 - should deactivate "s1"(1) from /c1 and /c2, should
	 *     reactivate "s1"(3) in /c1, which was previously disabled, should activate "s1"(4) in /c2 instead of "s1"(5),
	 *     but "s1"(4) is still shadowed in /c3 by "s1"(2), so "s1"(5) is the one active in /c2</li>
	 *     <li>"s1"(6) with rank=0 registered to /c4 - as disabled, because shadowed by "s1"(5)</li>
	 *     <li>"s1"(2) is unregistered from /c3 - should activate "s1"(4) in /c3 and even in /c2, because in /c2
	 *     "s1"(5) is active, but with lower rank - this has to change just as if "s1"(4) was newly registered. so
	 *     "s1"(5) is deactivated - in both /c2 and /c4, so leading to reactivation of "s1"(6) in /c4</li>
	 * </ul></p>
	 *
	 * @throws Exception
	 */
	@Test
	public void registerServletsConflictingByName() throws Exception {
		ServerController controller = create(null);
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleContext context = mock(BundleContext.class);
		when(bundle.getBundleContext()).thenReturn(context);

		ServerModel server = new ServerModel(new SameThreadExecutor());

		Configuration config = controller.getConfiguration();
		HttpServiceEnabled wc = new HttpServiceEnabled(bundle, controller, server, null, config);

		Batch batch = new Batch("Initialization Batch");
		server.getOrCreateServletContextModel("/c1", batch);
		server.getOrCreateServletContextModel("/c2", batch);
		server.getOrCreateServletContextModel("/c3", batch);
		server.getOrCreateServletContextModel("/c4", batch);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		WebContainerContext wcc1 = wc.createDefaultHttpContext("wcc1");
		WebContainerContext wcc2 = wc.createDefaultHttpContext("wcc2");
		WebContainerContext wcc3 = wc.createDefaultHttpContext("wcc3");
		WebContainerContext wcc4 = wc.createDefaultHttpContext("wcc4");

		// 4 logical OSGi context models
		batch = new Batch("Initialization Batch");
		OsgiContextModel cm1 = server.createNewContextModel(wcc1, "/c1", bundle, batch);
		OsgiContextModel cm2 = server.createNewContextModel(wcc2, "/c2", bundle, batch);
		OsgiContextModel cm3 = server.createNewContextModel(wcc3, "/c3", bundle, batch);
		OsgiContextModel cm4 = server.createNewContextModel(wcc3, "/c4", bundle, batch);
		server.associateHttpContext(wcc1, cm1);
		server.associateHttpContext(wcc2, cm2);
		server.associateHttpContext(wcc3, cm3);
		server.associateHttpContext(wcc4, cm4);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		Servlet s11 = new MyIdServlet("1");
		Servlet s12 = new MyIdServlet("2");
		Servlet s13 = new MyIdServlet("3");
		Servlet s14 = new MyIdServlet("4");
		Servlet s15 = new MyIdServlet("5");
		Servlet s16 = new MyIdServlet("6");

		long serviceId = 0;

		// servlet#1 registered in /c1 and /c2
		wc.doRegisterServlet(Arrays.asList(wcc1, wcc2), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServlet(s11)
				.withServiceRankAndId(0, ++serviceId)
				.build());

		assertThat(get(port, "/c1/s"), endsWith("my.id=1"));
		assertThat(get(port, "/c2/s"), endsWith("my.id=1"));
		assertThat(get(port, "/c3/s"), startsWith("HTTP/1.1 404"));

		// servlet#2 registered in /c3 - no conflict
		wc.doRegisterServlet(Collections.singletonList(wcc3), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServlet(s12)
				.withServiceRankAndId(3, ++serviceId)
				.build());

		assertThat(get(port, "/c1/s"), endsWith("my.id=1"));
		assertThat(get(port, "/c2/s"), endsWith("my.id=1"));
		assertThat(get(port, "/c3/s"), endsWith("my.id=2"));

		// servlet#3 registered to /c1, but with higher service ID - should be marked as disabled
		wc.doRegisterServlet(Collections.singletonList(wcc1), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServlet(s13)
				.withServiceRankAndId(0, ++serviceId)
				.build());

		assertThat(get(port, "/c1/s"), endsWith("my.id=1"));
		assertThat(get(port, "/c2/s"), endsWith("my.id=1"));
		assertThat(get(port, "/c3/s"), endsWith("my.id=2"));

		// servlet#4 registered to /c2 and /c3 - ranked higher than s#1 in /c2, but ranked lower than s#2 in /c3
		wc.doRegisterServlet(Arrays.asList(wcc2, wcc3), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServlet(s14)
				.withServiceRankAndId(2, ++serviceId)
				.build());

		assertThat(get(port, "/c1/s"), endsWith("my.id=1"));
		assertThat(get(port, "/c2/s"), endsWith("my.id=1"));
		assertThat(get(port, "/c3/s"), endsWith("my.id=2"));

		// servlet#5 registered to /c2 and /c4 - ranked higher than s#1 in /c2, so:
		//  - s#1 is deactivated in /c1 and /c2
		//  - s#3 is activated in /c1
		//  - s#5 MAY be activated in /c2 and /c4, but in /c2, s#4 is ranked higher than s#5
		//  - s#4 is ranked lower than s#2 in /c3, so it won't be activated ANYWHERE
		//  - s#5 will thus be activated in /c2 and /c4
		wc.doRegisterServlet(Arrays.asList(wcc2, wcc4), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServlet(s15)
				.withServiceRankAndId(1, ++serviceId)
				.build());

		assertThat(get(port, "/c1/s"), endsWith("my.id=3"));
		assertThat(get(port, "/c2/s"), endsWith("my.id=5"));
		assertThat(get(port, "/c3/s"), endsWith("my.id=2"));
		assertThat(get(port, "/c4/s"), endsWith("my.id=5"));

		// servlet#6 registered to /c4 - ranked lower than s#5 in /c4, so added as disabled
		wc.doRegisterServlet(Collections.singletonList(wcc4), new ServletModel.Builder()
				.withServletName("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServlet(s16)
				.withServiceRankAndId(0, ++serviceId)
				.build());

		assertThat(get(port, "/c1/s"), endsWith("my.id=3"));
		assertThat(get(port, "/c2/s"), endsWith("my.id=5"));
		assertThat(get(port, "/c3/s"), endsWith("my.id=2"));
		assertThat(get(port, "/c4/s"), endsWith("my.id=5"));

		// servlet#2 unregistered, s#4 can be activated in /c3 and can be activated in /c2 because s#5 in /c2 is ranked
		// lower than s#4, so s#5 disabled in /c4, so s#6 enabled in /c4
		wc.doUnregisterServlet(new ServletModel.Builder()
				.withServlet(s12)
				.withOsgiContextModel(cm3)
				.remove());

		assertTrue(get(port, "/c1/s").endsWith("my.id=3"));
		assertTrue(get(port, "/c2/s").endsWith("my.id=4"));
		assertTrue(get(port, "/c3/s").endsWith("my.id=4"));
		assertTrue(get(port, "/c4/s").endsWith("my.id=6"));

		controller.stop();
	}

	private ServerController create(Consumer<Hashtable<Object, Object>> callback) {
		Hashtable<Object, Object> properties = new Hashtable<>(System.getProperties());
		properties.put(PaxWebConfig.PID_CFG_TEMP_DIR, "target/tmp");
		properties.put(PaxWebConfig.PID_CFG_HTTP_PORT, Integer.toString(port));

		if (callback != null) {
			callback.accept(properties);
		}

		// it wouldn't work in OSGi because MetaTypePropertyResolver's package is not exported
		MetaTypePropertyResolver metatypeResolver = new MetaTypePropertyResolver();
		DictionaryPropertyResolver resolver = new DictionaryPropertyResolver(properties, metatypeResolver);
		Configuration config = ConfigurationBuilder.getConfiguration(resolver, Utils.toMap(properties));

		switch (runtime) {
			case JETTY: {
				ServerControllerFactory factory = new JettyServerControllerFactory(null, this.getClass().getClassLoader());
				return factory.createServerController(config);
			}
			case TOMCAT: {
				ServerControllerFactory factory = new TomcatServerControllerFactory(null, this.getClass().getClassLoader());
				return factory.createServerController(config);
			}
			case UNDERTOW:
				ServerControllerFactory factory = new UndertowServerControllerFactory(null, this.getClass().getClassLoader(), new NioXnioProvider());
				return factory.createServerController(config);
			default:
				throw new IllegalArgumentException("Not supported: " + runtime);
		}
	}

	private String get(int port, String request, String ... headers) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", port));

		s.getOutputStream().write((
				"GET " + request + " HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + port + "\r\n").getBytes());
		for (String header : headers) {
			s.getOutputStream().write((header + "\r\n").getBytes());
		}
		s.getOutputStream().write(("Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read = -1;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.getOutputStream().close();
		s.close();

		return sw.toString();
	}

	/**
	 * GET over HTTPS
	 * @param port
	 * @param request
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	private String gets(int port, String request, String ... headers) throws Exception {
		// Trust standard CA and those trusted by our custom strategy
		final SSLContext sslcontext = SSLContexts.custom()
				.loadTrustMaterial((chain, authType) -> {
					final X509Certificate cert = chain[0];
					return "CN=server1".equalsIgnoreCase(cert.getSubjectDN().getName());
				})
				.loadKeyMaterial(new File("target/client.jks"), "passw0rd".toCharArray(), "passw0rd".toCharArray(), (aliases, sslParameters) -> "client")
				.build();

		// Allow TLSv1.1 and TLSv1.2 protocol only
		final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
				.setSslContext(sslcontext)
				.setTlsVersions(TLS.V_1_1, TLS.V_1_2)
				.build();
		final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
				.setSSLSocketFactory(sslSocketFactory)
				.build();
		try (CloseableHttpClient httpclient = HttpClients.custom()
				.setConnectionManager(cm)
				.build()) {

			final HttpGet httpget = new HttpGet("https://127.0.0.1:" + port + request);

			final HttpClientContext clientContext = HttpClientContext.create();
			try (CloseableHttpResponse response = httpclient.execute(httpget, clientContext)) {
//				System.out.println("----------------------------------------");
//				System.out.println(response.getCode() + " " + response.getReasonPhrase());
//				System.out.println(EntityUtils.toString(response.getEntity()));

				final SSLSession sslSession = clientContext.getSSLSession();
				if (sslSession != null) {
					LOG.info("SSL protocol " + sslSession.getProtocol());
					LOG.info("SSL cipher suite " + sslSession.getCipherSuite());
					for (javax.security.cert.X509Certificate cert : sslSession.getPeerCertificateChain()) {
						LOG.info("Server cert: " + cert.getSubjectDN() + " (issuer: " + cert.getIssuerDN() + ")");
					}
				}

				StringBuilder sb = new StringBuilder();
				sb.append(response.getVersion()).append(" ")
						.append(response.getCode()).append(" ")
						.append(response.getReasonPhrase())
						.append("\r\n");
				for (Header header : response.getHeaders()) {
					sb.append(header.getName()).append(header.getValue()).append("\r\n");
				}
				sb.append("\r\n");
				sb.append(EntityUtils.toString(response.getEntity()));

				return sb.toString();
			}
		}
	}

	private static class SameThreadExecutor implements Executor {
		@Override
		public void execute(Runnable command) {
			command.run();
		}
	}

	private static class MyHttpServlet extends HttpServlet {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().print(getServletName() + "[" + getServletConfig().getServletContext().getContextPath() + "]");
		}
	}

	private static class MyIdServlet extends HttpServlet {

		private final String id;

		public MyIdServlet(String id) {
			this.id = id;
		}

		@Override
		public void destroy() {
			LOG.info("Servlet {} destroyed", this);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().print("my.id=" + id);
		}

		@Override
		public String toString() {
			return "S(" + id + ")";
		}
	}

}
