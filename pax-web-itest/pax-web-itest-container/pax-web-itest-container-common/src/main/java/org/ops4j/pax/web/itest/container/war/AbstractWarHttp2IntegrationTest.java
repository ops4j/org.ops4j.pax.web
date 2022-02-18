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

package org.ops4j.pax.web.itest.container.war;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServletResponse;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.io.CloseMode;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @author Grzegorz Grzybek
 */
public abstract class AbstractWarHttp2IntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarHttp2IntegrationTest.class);

	@Inject
	private ConfigurationAdmin caService;

	@Before
	public void setUp() throws Exception {
		org.osgi.service.cm.Configuration config = caService.getConfiguration(PaxWebConstants.PID, null);

		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(PaxWebConfig.PID_CFG_LISTENING_ADDRESSES, "127.0.0.1");
		properties.put(PaxWebConfig.PID_CFG_HTTP_ENABLED, "true");
		properties.put(PaxWebConfig.PID_CFG_HTTP_PORT, "8181");
		properties.put(PaxWebConfig.PID_CFG_HTTP_SECURE_ENABLED, "true");
		properties.put(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE, "8443");

		// tweak ciphers/protocols to check proper configuration
		properties.put(PaxWebConfig.PID_CFG_PROTOCOLS_INCLUDED, "TLSv1.2");
		properties.put(PaxWebConfig.PID_CFG_CIPHERSUITES_INCLUDED, "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");

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
		properties.put(PaxWebConfig.PID_CFG_SSL_CLIENT_AUTH_NEEDED, "false");
		properties.put(PaxWebConfig.PID_CFG_SSL_PROTOCOL, "TLSv1.2");
		properties.put(PaxWebConfig.PID_CFG_SSL_SECURE_RANDOM_ALGORITHM, "SHA1PRNG");
		properties.put(PaxWebConfig.PID_CFG_SSL_RENEGOTIATION_ALLOWED, "true");
		properties.put(PaxWebConfig.PID_CFG_SSL_SESSION_ENABLED, "true");

		configureAndWaitForDeploymentUnlessInstalled("war-http2", () -> {
			installAndStartBundle(sampleWarURI("war-http2"));
			Dictionary<String, Object> current = config.getProperties();
			if (current == null || !"8443".equals(current.get(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE))) {
				configureAndWaitForListener(8443, () -> {
					config.update(properties);
				});
			}
		});
	}

	protected boolean supportsHttp2Push() {
		return true;
	}

	@Test
	public void testHttp2ClearText() throws Exception {
		final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create().build();

		final CountDownLatch latch = new CountDownLatch(supportsHttp2Push() ? 3 : 1);

		try (CloseableHttpAsyncClient client = HttpAsyncClients.custom()
				.setH2Config(H2Config.custom().setPushEnabled(true).build())
				.setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_2).setConnectionManager(cm).build()) {

			if (supportsHttp2Push()) {
				client.register("*", () -> new AsyncPushConsumer() {
					@Override
					public void consumePromise(HttpRequest promise, HttpResponse response, EntityDetails entityDetails, HttpContext context) throws HttpException {
						LOG.info("{} -> {}", promise, new StatusLine(response));
						if (response.getVersion() == HttpVersion.HTTP_2 && response.getCode() == HttpServletResponse.SC_OK) {
							latch.countDown();
						}
					}

					@Override
					public void failed(Exception cause) {
						System.out.println();
					}

					@Override
					public void updateCapacity(CapacityChannel capacityChannel) {
						System.out.println();
					}

					@Override
					public void consume(ByteBuffer src) {
						System.out.println();
					}

					@Override
					public void streamEnd(List<? extends Header> trailers) {
						System.out.println();
					}

					@Override
					public void releaseResources() {
						System.out.println();
					}
				});
			}

			client.start();

			final HttpHost target = new HttpHost("http", "127.0.0.1", 8181);
			final HttpClientContext clientContext = HttpClientContext.create();

			final SimpleHttpRequest request = SimpleRequestBuilder.get().setHttpHost(target).setPath("/http2/servlet/index.html").build();

			LOG.info("Sending request: {}", request);
			final Future<SimpleHttpResponse> future = client.execute(
					SimpleRequestProducer.create(request),
					SimpleResponseConsumer.create(),
					clientContext,
					new FutureCallback<SimpleHttpResponse>() {
						@Override
						public void completed(final SimpleHttpResponse response) {
							LOG.info("{} -> {}", request, new StatusLine(response));
							final SSLSession sslSession = clientContext.getSSLSession();
							if (sslSession != null) {
								LOG.info("SSL Protocol: {}", sslSession.getProtocol());
								LOG.info("SSL Cipher Suite: {}", sslSession.getCipherSuite());
							}
							LOG.info("Received response: {}", response.getBody());
							latch.countDown();
						}

						@Override
						public void failed(final Exception ex) {
							LOG.info("Failure: {} -> {}", request, ex.getMessage(), ex);
						}

						@Override
						public void cancelled() {
							LOG.info("Request cancelled");
						}

					});
			future.get();
			client.close(CloseMode.GRACEFUL);
		}

		assertTrue(latch.await(5, TimeUnit.SECONDS));
	}

	@Test
	public void testHttp2Secure() throws Exception {
		KeyStore ts = KeyStore.getInstance("JKS");
		try (InputStream is = new FileInputStream("../../etc/security/server.jks")) {
			ts.load(is, "passw0rd".toCharArray());
		}
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		trustManagerFactory.init(ts);
		keyManagerFactory.init(ts, "passw0rd".toCharArray());
		SSLContext context = SSLContext.getInstance("TLSv1.2");
		context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

		final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
				.setCiphers("TLS_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")
				.setTlsVersions(TLS.V_1_2)
				.setSslContext(context)
				.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
		final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
				.setTlsStrategy(tlsStrategy).build();

		final CountDownLatch latch = new CountDownLatch(supportsHttp2Push() ? 3 : 1);

		try (CloseableHttpAsyncClient client = HttpAsyncClients.custom()
				.setH2Config(H2Config.custom().setPushEnabled(true).build())
				.setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_2).setConnectionManager(cm).build()) {

			if (supportsHttp2Push()) {
				client.register("*", () -> new AsyncPushConsumer() {
					@Override
					public void consumePromise(HttpRequest promise, HttpResponse response, EntityDetails entityDetails, HttpContext context) throws HttpException {
						LOG.info("{} -> {}", promise, new StatusLine(response));
						if (response.getVersion() == HttpVersion.HTTP_2 && response.getCode() == HttpServletResponse.SC_OK) {
							latch.countDown();
						}
					}

					@Override
					public void failed(Exception cause) {
						System.out.println();
					}

					@Override
					public void updateCapacity(CapacityChannel capacityChannel) {
						System.out.println();
					}

					@Override
					public void consume(ByteBuffer src) {
						System.out.println();
					}

					@Override
					public void streamEnd(List<? extends Header> trailers) {
						System.out.println();
					}

					@Override
					public void releaseResources() {
						System.out.println();
					}
				});
			}

			client.start();

			final HttpHost target = new HttpHost("https", "127.0.0.1", 8443);
			final HttpClientContext clientContext = HttpClientContext.create();

			final SimpleHttpRequest request = SimpleRequestBuilder.get().setHttpHost(target).setPath("/http2/servlet/index.html").build();

			LOG.info("Sending request: {}", request);
			final Future<SimpleHttpResponse> future = client.execute(
					SimpleRequestProducer.create(request),
					SimpleResponseConsumer.create(),
					clientContext,
					new FutureCallback<SimpleHttpResponse>() {
						@Override
						public void completed(final SimpleHttpResponse response) {
							LOG.info("{} -> {}", request, new StatusLine(response));
							final SSLSession sslSession = clientContext.getSSLSession();
							if (sslSession != null) {
								LOG.info("SSL Protocol: {}", sslSession.getProtocol());
								LOG.info("SSL Cipher Suite: {}", sslSession.getCipherSuite());
							}
							LOG.info("Received response: {}", response.getBody());
							latch.countDown();
						}

						@Override
						public void failed(final Exception ex) {
							LOG.info("Failure: {} -> {}", request, ex.getMessage(), ex);
						}

						@Override
						public void cancelled() {
							LOG.info("Request cancelled");
						}

					});
			future.get();
			client.close(CloseMode.GRACEFUL);
		}

		assertTrue(latch.await(5, TimeUnit.SECONDS));
	}

}
