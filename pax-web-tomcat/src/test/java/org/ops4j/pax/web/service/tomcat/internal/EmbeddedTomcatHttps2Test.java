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
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.CatalinaBaseConfigurationSource;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.coyote.http2.Http2Protocol;
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
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.nio.AsyncPushConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.tomcat.util.file.ConfigFileLoader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class EmbeddedTomcatHttps2Test {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedTomcatHttps2Test.class);

	// https://httpwg.org/specs/rfc7540.html
	// https://sookocheff.com/post/networking/how-does-http-2-work/

	@Test
	public void https2NioExchange() throws Exception {
		// important when running together with EmbeddedTomcatTests (`mvn test`)
		ConfigFileLoader.setSource(new CatalinaBaseConfigurationSource(new File("target"), null));

		//		System.setProperty("javax.net.debug", "all");
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
		connector.setScheme("https");
		connector.setSecure(true);
		connector.setProperty("SSLEnabled", "true");
		connector.setPort(0);
//		connector.setPort(8123);
		AbstractHttp11JsseProtocol<?> protocol = (AbstractHttp11JsseProtocol<?>) connector.getProtocolHandler();
		protocol.setSslImplementationName("org.apache.tomcat.util.net.jsse.JSSEImplementation");

		protocol.setKeystoreFile("../../pax-web-itest/etc/security/server.jks");
		protocol.setKeystorePass("passw0rd");
		protocol.setKeystoreType("JKS");
		protocol.setKeystoreProvider("SUN");
		protocol.setKeyPass("passw0rd");
		protocol.setKeyAlias("server");

		protocol.setTruststoreFile("../../pax-web-itest/etc/security/server.jks");
		protocol.setTruststorePass("passw0rd");
		protocol.setTruststoreType("JKS");
		protocol.setTruststoreProvider("SUN");
		protocol.setTruststoreAlgorithm("SunX509");

		protocol.setSslEnabledProtocols("TLSv1.3");
		// only TWO ciphers are supported on JDK8 for TLSv1.3
		//  - sun.security.ssl.ProtocolVersion.PROTOCOLS_OF_13 is used only in TLS_AES_256_GCM_SHA384 and
		//    TLS_AES_128_GCM_SHA256 in sun.security.ssl.CipherSuite
		//  - sun.security.ssl.ProtocolVersion.PROTOCOLS_12_13 is not used at all
		//  - sun.security.ssl.ProtocolVersion.PROTOCOLS_TO_13 is not used at all
		protocol.setSSLCipherSuite("TLS_AES_256_GCM_SHA384");
		protocol.setSSLProtocol("TLSv1.3");
		protocol.setUseServerCipherSuitesOrder(true);

		connector.addUpgradeProtocol(new Http2Protocol());
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Servlet servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				if (!"/index.html".equals(req.getPathInfo())) {
					// normal request
					LOG.info("Handling request {} from {}:{}", req.getRequestURI(), req.getRemoteAddr(), req.getRemotePort());
					resp.setContentType("text/plain");
					resp.setCharacterEncoding("UTF-8");
					if (req.getPathInfo() != null && req.getPathInfo().endsWith("css")) {
						resp.getWriter().write("body { margin: 0 }\n");
					} else if (req.getPathInfo() != null && req.getPathInfo().endsWith("js")) {
						resp.getWriter().write("window.alert(\"hello world\");\n");
					} else {
						resp.getWriter().write("OK\n");
					}
					resp.getWriter().close();
				} else {
					// request with PUSH_PROMISE: https://httpwg.org/specs/rfc7540.html#PUSH_PROMISE
					javax.servlet.http.PushBuilder pushBuilder = req.newPushBuilder();
					if (pushBuilder != null) {
						pushBuilder.path("test/default.css").push();
						pushBuilder.path("test/app.js").push();
					}
				}
			}
		};

		Context c1 = new StandardContext();
		c1.setName("c1");
		c1.setPath("");
		c1.setMapperContextRootRedirectEnabled(false);
		c1.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				c1.setConfigured(true);
			}
		});
		host.addChild(c1);

		Wrapper wrapper1 = new StandardWrapper();
		wrapper1.setServlet(servlet);
		wrapper1.setName("servlet");

		c1.addChild(wrapper1);
		c1.addServletMappingDecoded("/test/*", wrapper1.getName(), false);

		server.start();

		LOG.info("Local port after start: {}", connector.getLocalPort());

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

		final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
				.setCiphers("TLS_AES_256_GCM_SHA384")
				.setTlsVersions(TLS.V_1_3)
				.setSslContext(context)
				.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
		final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
				.setTlsStrategy(tlsStrategy).build();

		final CountDownLatch latch = new CountDownLatch(3);

		try (CloseableHttpAsyncClient client = HttpAsyncClients.custom()
				.setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_2).setConnectionManager(cm).build()) {

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
		server.destroy();
	}

}
