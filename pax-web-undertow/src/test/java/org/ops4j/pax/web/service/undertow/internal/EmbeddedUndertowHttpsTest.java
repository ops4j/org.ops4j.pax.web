/*
 * Copyright 2025 OPS4J.
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
package org.ops4j.pax.web.service.undertow.internal;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.io.CloseMode;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Options;
import org.xnio.Sequence;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbeddedUndertowHttpsTest {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedUndertowHttpsTest.class);

	// https://httpwg.org/specs/rfc7540.html
	// https://sookocheff.com/post/networking/how-does-http-2-work/

	@Test
	public void httpsNioExchange() throws Exception {
//		System.setProperty("javax.net.debug", "all");
		KeyStore ts = KeyStore.getInstance("JKS");
		try (InputStream is = new FileInputStream("../pax-web-itest/etc/security/server.jks")) {
			ts.load(is, "passw0rd".toCharArray());
		}
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		trustManagerFactory.init(ts);
		keyManagerFactory.init(ts, "passw0rd".toCharArray());
		SSLContext context = SSLContext.getInstance("TLSv1.3");
		context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpsListener(0, "0.0.0.0", context)
				.setServerOption(UndertowOptions.ENABLE_HTTP2, true)
				.setServerOption(Options.SSL_ENABLED, true)
				// see https://issues.redhat.com/browse/UNDERTOW-2030
				.setServerOption(Options.SSL_PROTOCOL, "TLSv1.3")
				.setServerOption(Options.SSL_ENABLED_PROTOCOLS, Sequence.of("TLSv1.3"))
				.setServerOption(Options.SSL_ENABLED_CIPHER_SUITES, Sequence.of("TLS_AES_128_GCM_SHA256"))
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				LOG.info("Handling request {} from {}:{}", req.getRequestURI(), req.getRemoteAddr(), req.getRemotePort());
				LOG.info("Content-Length: {}", req.getHeader("Content-Length"));
				resp.setContentType("text/plain");
				resp.getWriter().write("OK");
				resp.getWriter().close();
			}
		};

		ServletInfo servlet = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
		servlet.addMapping("/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
				.setCiphers("TLS_AES_128_GCM_SHA256")
				.setTlsVersions(TLS.V_1_3)
				.setSslContext(context)
				.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
		final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
				.setDefaultTlsConfig(TlsConfig.custom().build())
				.setTlsStrategy(tlsStrategy).build();

		final CountDownLatch latch = new CountDownLatch(1);

		try (CloseableHttpAsyncClient client = HttpAsyncClients.custom()
				.setConnectionManager(cm).build()) {

			client.start();

			final HttpHost target = new HttpHost("https", "127.0.0.1", port);
			final HttpClientContext clientContext = HttpClientContext.create();

			byte[] body = new byte[32768];
			Arrays.fill(body, (byte) 0x42);
			final SimpleHttpRequest request = SimpleRequestBuilder.post().setBody(body, ContentType.APPLICATION_OCTET_STREAM)
					.setHttpHost(target).setPath("/test").build();

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

		server.stop();
	}

}
