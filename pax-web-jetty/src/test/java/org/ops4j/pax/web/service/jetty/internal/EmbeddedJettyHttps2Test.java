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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;

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
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.io.CloseMode;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class EmbeddedJettyHttps2Test {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedJettyHttps2Test.class);

	// https://httpwg.org/specs/rfc7540.html
	// https://sookocheff.com/post/networking/how-does-http-2-work/

	@Test
	public void https2NioExchange() throws Exception {
		QueuedThreadPool qtp = new QueuedThreadPool(10);
		qtp.setName("jetty-qtp");

		Server server = new Server(qtp);

		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);

		sslContextFactory.setKeyStorePath("../pax-web-itest/etc/security/server.jks");
		sslContextFactory.setKeyStorePassword("passw0rd");
		sslContextFactory.setKeyStoreType("JKS");
		sslContextFactory.setKeyStoreProvider("SUN");
		sslContextFactory.setKeyManagerFactoryAlgorithm("SunX509");
		sslContextFactory.setKeyManagerPassword("passw0rd");
		sslContextFactory.setCertAlias("server");

		sslContextFactory.setTrustStorePath("../pax-web-itest/etc/security/server.jks");
		sslContextFactory.setTrustStorePassword("passw0rd");
		sslContextFactory.setTrustStoreType("JKS");
		sslContextFactory.setTrustStoreProvider("SUN");
		sslContextFactory.setTrustManagerFactoryAlgorithm("SunX509");

		sslContextFactory.setWantClientAuth(false);
		sslContextFactory.setNeedClientAuth(false);
		sslContextFactory.setHostnameVerifier(null);

		sslContextFactory.setIncludeProtocols("TLSv1.3");
		// only TWO ciphers are supported on JDK8 for TLSv1.3
		//  - sun.security.ssl.ProtocolVersion.PROTOCOLS_OF_13 is used only in TLS_AES_256_GCM_SHA384 and
		//    TLS_AES_128_GCM_SHA256 in sun.security.ssl.CipherSuite
		//  - sun.security.ssl.ProtocolVersion.PROTOCOLS_12_13 is not used at all
		//  - sun.security.ssl.ProtocolVersion.PROTOCOLS_TO_13 is not used at all
		sslContextFactory.setIncludeCipherSuites("TLS_AES_256_GCM_SHA384");
		sslContextFactory.setProtocol("TLSv1.3");
		sslContextFactory.setSecureRandomAlgorithm("SHA1PRNG");
		sslContextFactory.setUseCipherSuitesOrder(true);

		ServerConnector connector = new ServerConnector(server, null, null, null, -1, -1);
//		connector.setPort(0);
		connector.setPort(8123);
		LOG.info("Local port before start: {}", connector.getLocalPort());
		connector.clearConnectionFactories();
		HttpConfiguration config = new HttpConfiguration();
		config.addCustomizer(new SecureRequestCustomizer());

		// org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory needs one service (java.util.ServiceLoader)
		// implementing org.eclipse.jetty.io.ssl.ALPNProcessor.Server. Jetty provides two:
		//  - org.eclipse.jetty.alpn.openjdk8.server.OpenJDK8ServerALPNProcessor (org.eclipse.jetty:jetty-alpn-openjdk8-server)
		//  - org.eclipse.jetty.alpn.conscrypt.server.ConscryptServerALPNProcessor (org.eclipse.jetty:jetty-alpn-conscrypt-server)
		// Before JDK 8u252, special boot-classpath library must be loaded:
		//  - org.eclipse.jetty.alpn.ALPN from org.eclipse.jetty.alpn:alpn-api:1.1.3.v20160715
		// JDK 8u252 adds javax.net.ssl.SSLEngine.setHandshakeApplicationProtocolSelector() method used by
		// org.eclipse.jetty.alpn.openjdk8.server.OpenJDK8ServerALPNProcessor, so no need to add any boot-classpath entries

		connector.addConnectionFactory(new SslConnectionFactory(sslContextFactory, "ALPN"));
		ALPNServerConnectionFactory alpnConnectionFactory = new ALPNServerConnectionFactory();
		// if no protocol can be negotiated, we'll force HTTP/1.1
		alpnConnectionFactory.setDefaultProtocol(HttpVersion.HTTP_1_1.asString());
		connector.addConnectionFactory(alpnConnectionFactory);
		connector.addConnectionFactory(new HTTP2ServerConnectionFactory(config));
		connector.addConnectionFactory(new HttpConnectionFactory(config));
		connector.setIdleTimeout(3600000);

		server.addConnector(connector);

		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(new ServletHolder("servlet", new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				LOG.info("Handling request {} from {}:{}", req.getRequestURI(), req.getRemoteAddr(), req.getRemotePort());
				if (!"/index.html".equals(req.getPathInfo())) {
					// normal request
					resp.setCharacterEncoding("UTF-8");
					if (req.getPathInfo() != null && req.getPathInfo().endsWith("css")) {
						resp.setContentType("text/css");
						resp.addHeader("X-Request-CSS", req.getHeader("X-Request-P1"));
						resp.getWriter().write("body { margin: 0 }\n");
					} else if (req.getPathInfo() != null && req.getPathInfo().endsWith("js")) {
						resp.setContentType("text/javascript");
						resp.addHeader("X-Request-JS", req.getHeader("X-Request-P2"));
						resp.getWriter().write("window.alert(\"hello world\");\n");
					}
					resp.getWriter().close();
				} else {
					// first - normal data
					resp.setContentType("text/plain");
					resp.setCharacterEncoding("UTF-8");
					resp.addHeader("X-Request", req.getRequestURI());
					resp.getWriter().write("OK\n");
					// request with PUSH_PROMISE: https://httpwg.org/specs/rfc7540.html#PUSH_PROMISE
					PushBuilder pushBuilder = req.newPushBuilder();
					if (pushBuilder != null) {
						pushBuilder.path("test/default.css").addHeader("X-Request-P1", req.getRequestURI()).push();
						pushBuilder.path("test/app.js").removeHeader("X-Request-P1").addHeader("X-Request-P2", req.getRequestURI()).push();
					}
					resp.getWriter().close();
				}
			}
		}), "/test/*");
		server.setHandler(handler);

		server.start();

		LOG.info("Local port after start: {}", connector.getLocalPort());

		final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
				.setCiphers("TLS_AES_256_GCM_SHA384")
				.setTlsVersions(TLS.V_1_3)
				.setSslContext(sslContextFactory.getSslContext())
				.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
		final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
				.setDefaultTlsConfig(TlsConfig.custom().setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_2).build())
				.setTlsStrategy(tlsStrategy).build();

		final CountDownLatch latch = new CountDownLatch(3);

		try (CloseableHttpAsyncClient client = HttpAsyncClients.custom()
				.setH2Config(H2Config.custom().setPushEnabled(true).build())
				.setConnectionManager(cm).build()) {

			client.register("*", () -> new AsyncPushConsumer() {
				@Override
				public void consumePromise(HttpRequest promise, HttpResponse response, EntityDetails entityDetails, HttpContext context) throws HttpException {
					LOG.info("{} -> {}", promise, new StatusLine(response));
					LOG.info("Received promised response: {} (Content-Length: {})", response, response.getHeader("Content-Length").getValue());
					latch.countDown();
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

			client.start();

			final HttpHost target = new HttpHost("https", "127.0.0.1", connector.getLocalPort());
			final HttpClientContext clientContext = HttpClientContext.create();

			final SimpleHttpRequest request = SimpleRequestBuilder.get().setHttpHost(target).setPath("/test/index.html").build();

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
		server.join();
	}

}
