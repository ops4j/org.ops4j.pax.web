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
package org.ops4j.pax.web.itest.server.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.servlet.FilterChain;
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
import org.ops4j.pax.web.itest.server.Runtime;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.internal.ConfigurationBuilder;
import org.ops4j.pax.web.service.internal.MetaTypePropertyResolver;
import org.ops4j.pax.web.service.jetty.internal.JettyServerControllerFactory;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.tomcat.internal.TomcatServerControllerFactory;
import org.ops4j.pax.web.service.undertow.internal.UndertowServerControllerFactory;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.nio.NioXnioProvider;

public class Utils {

	public static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private Utils() {
	}

	public static ServerController createServerController(Consumer<Hashtable<Object, Object>> callback, int port,
			Runtime runtime, ClassLoader classLoader) {
		Hashtable<Object, Object> properties = new Hashtable<>(System.getProperties());
		properties.put(PaxWebConfig.PID_CFG_TEMP_DIR, "target/tmp");
		properties.put(PaxWebConfig.PID_CFG_HTTP_PORT, Integer.toString(port));
		properties.put(PaxWebConfig.PID_CFG_SHOW_STACKS, "true");

		if (callback != null) {
			callback.accept(properties);
		}

		// it wouldn't work in OSGi because MetaTypePropertyResolver's package is not exported
		MetaTypePropertyResolver metatypeResolver = new MetaTypePropertyResolver();
		DictionaryPropertyResolver resolver = new DictionaryPropertyResolver(properties, metatypeResolver);
		Configuration config = ConfigurationBuilder.getConfiguration(resolver, org.ops4j.pax.web.service.spi.util.Utils.toMap(properties));

		switch (runtime) {
			case JETTY: {
				ServerControllerFactory factory = new JettyServerControllerFactory(null, classLoader);
				return factory.createServerController(config);
			}
			case TOMCAT: {
				ServerControllerFactory factory = new TomcatServerControllerFactory(null, classLoader);
				return factory.createServerController(config);
			}
			case UNDERTOW:
				ServerControllerFactory factory = new UndertowServerControllerFactory(null, classLoader, new NioXnioProvider());
				return factory.createServerController(config);
			default:
				throw new IllegalArgumentException("Not supported: " + runtime);
		}
	}

	public static String httpGET(int port, String request, String ... headers) throws IOException {
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
	public static String httpsGET(int port, String request, String ... headers) throws Exception {
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

	public static Map<String, String> extractHeaders(String response) throws IOException {
		Map<String, String> headers = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(new StringReader(response))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.trim().equals("")) {
					break;
				}
				// I know, security when parsing headers is very important...
				String[] kv = line.split(": ");
				String header = kv[0];
				String value = String.join("", Arrays.asList(kv).subList(1, kv.length));
				headers.put(header, value);
			}
		}
		return headers;
	}

	public static Object getField(Object object, String fieldName) {
		String[] names = fieldName.split("\\.");
		for (String name : names) {
			Field f = null;
			try {
				f = object.getClass().getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				try {
					f = object.getClass().getSuperclass().getDeclaredField(name);
				} catch (NoSuchFieldException ex) {
					try {
						f = object.getClass().getSuperclass().getSuperclass().getDeclaredField(name);
					} catch (NoSuchFieldException exx) {
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			}
			f.setAccessible(true);
			try {
				object = f.get(object);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		return object;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Object object, String fieldName, Class<T> clazz) {
		return (T) getField(object, fieldName);
	}

	public static class SameThreadExecutor implements Executor {
		@Override
		public void execute(Runnable command) {
			command.run();
		}
	}

	public static class MyHttpServlet extends HttpServlet {

		private final String id;

		public MyHttpServlet(String id) {
			this.id = id;
		}

		@Override
		public void init() {
			LOG.info("Servlet {} ({}) initialized in {}", this, System.identityHashCode(this), getServletContext().getContextPath());
		}

		@Override
		public void destroy() {
			LOG.info("Servlet {} ({}) destroyed in {}", this, System.identityHashCode(this), getServletContext().getContextPath());
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().print(getServletName() + "[" + getServletConfig().getServletContext().getContextPath() + "]");
		}

		@Override
		public String toString() {
			return "S(" + id + ")";
		}
	}

	public static class MyIdServlet extends HttpServlet {

		private final String id;

		public MyIdServlet(String id) {
			this.id = id;
		}

		@Override
		public void init() {
			LOG.info("Servlet {} ({}) initialized in {}", this, System.identityHashCode(this), getServletContext().getContextPath());
		}

		@Override
		public void destroy() {
			LOG.info("Servlet {} ({}) destroyed in {}", this, System.identityHashCode(this), getServletContext().getContextPath());
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().write(this.toString());
		}

		@Override
		public String toString() {
			return "S(" + id + ")";
		}
	}

	public static class MyIdFilter extends HttpFilter {

		protected final String id;

		public MyIdFilter(String id) {
			this.id = id;
		}

		@Override
		public void init() {
			LOG.info("Filter {} ({}) initialized in {}", this, System.identityHashCode(this), getServletContext().getContextPath());
		}

		@Override
		public void destroy() {
			LOG.info("Filter {} ({}) destroyed in {}", this, System.identityHashCode(this), getServletContext().getContextPath());
		}

		@Override
		protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().write(">" + this);
			if (!id.equals(req.getParameter("terminate"))) {
				chain.doFilter(req, resp);
			}
			resp.getWriter().write("<" + this);
		}

		@Override
		public String toString() {
			return "F(" + id + ")";
		}
	}

}
