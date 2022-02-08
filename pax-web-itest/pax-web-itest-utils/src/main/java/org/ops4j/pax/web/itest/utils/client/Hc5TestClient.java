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
package org.ops4j.pax.web.itest.utils.client;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.web.itest.utils.assertion.Assert.assertTrue;

/**
 * Third rewrite of Pax Web test client - after JettyClient and HttpClient 4, we'll use HttpClient 5 (with HTTP/2
 * support) in Pax Web 8. HttpClient 5 is <strong>not</strong> an OSGi bundle, so we have to embedd it.
 */
class Hc5TestClient implements HttpTestClient {

	private static final Logger LOG = LoggerFactory.getLogger(Hc5TestClient.class);

	private int[] returnCode;
	private final Map<String, String> httpHeaders = new HashMap<>();
	private boolean async;
	private final Collection<AssertionDefinition<String>> responseContentAssertion = new ArrayList<>();
	private final Collection<AssertionDefinition<Stream<Map.Entry<String, String>>>> responseHeaderAssertion = new ArrayList<>();
	private URL keystoreLocationURL;
	private BaseAuthDefinition authDefinition;
	private String urlToTest;
	private String pathToTest;
	private boolean doGET;
	private boolean doPOST;
	private boolean doOPTIONS;
	private boolean doHEAD;
	private final Map<String, String> requestParameters = new HashMap<>();
	private int timeoutInSeconds = 100;
	private CookieState httpState = null;

	private final boolean followRedirects;

	private String keystorePassword = "passw0rd";

	private String keyManagerPassword = "passw0rd";

	private Map<String, byte[]> attachments;

	Hc5TestClient(boolean followRedirects) {
		this.followRedirects = followRedirects;
	}

	@Override
	public HttpTestClient withExternalKeystore(String keystoreLocation) {
		if (keystoreLocation == null) {
			throw new IllegalArgumentException("keystoreLocation must not be null!");
		}
		String keystoreFilename;
		if (keystoreLocation.contains("${")) {
			int from = keystoreLocation.indexOf("${");
			int to = keystoreLocation.indexOf("}");
			String placeHolder = keystoreLocation.substring(from + 2, to);
			String property = System.getProperty(placeHolder);
			keystoreFilename = keystoreLocation.substring(0, from) + property + keystoreLocation.substring(to + 1);
		} else {
			keystoreFilename = keystoreLocation;
		}

		File keystore = new File(keystoreFilename);

		if (keystore.exists()) {
			try {
				keystoreLocationURL = keystore.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new IllegalArgumentException("No keystore-file found under '" + keystoreFilename + "'!");
		}

		return this;
	}

	@Override
	public HttpTestClient withBundleKeystore(String bundleSymbolicName, String keystoreLocation) {
		Bundle result = null;
		for (Bundle candidate : FrameworkUtil.getBundle(getClass()).getBundleContext().getBundles()) {
			if (candidate.getSymbolicName().equals(bundleSymbolicName)) {
				if (result == null || result.getVersion().compareTo(candidate.getVersion()) < 0) {
					result = candidate;
				}
			}
		}

		if (result == null) {
			LOG.error("Bundle with '{}' not found for keystore-lookup!", bundleSymbolicName);
			return this;
		}

		this.keystoreLocationURL = result.getEntry(keystoreLocation);

		if (this.keystoreLocationURL == null) {
			LOG.error("Keystore-Resource not found under '{}'!", keystoreLocation);
			return this;
		}

		return this;
	}

	@Override
	public HttpTestClient timeoutInSeconds(int seconds) {
		timeoutInSeconds = seconds;
		return this;
	}

	@Override
	public HttpTestClient addRequestHeader(String header, String value) {
		httpHeaders.put(header, value);
		return this;
	}

	@Override
	public HttpTestClient useCookieState(CookieState cookieState) {
		this.httpState = cookieState;
		return this;
	}

	@Override
	public HttpTestClient withReturnCode(int... returnCode) {
		this.returnCode = returnCode;
		return this;
	}

	@Override
	public HttpTestClient async() {
		async = true;
		return this;
	}

	@Override
	public HttpTestClient authenticate(String user, String password, String realm) {
		authDefinition = new BaseAuthDefinition(user, password, realm);
		return this;
	}

	@Override
	public HttpTestClient withResponseAssertion(final String message, final Predicate<String> assertion) {
		this.responseContentAssertion.add(new AssertionDefinition<>(message, assertion));
		return this;
	}

	@Override
	public HttpTestClient withResponseHeaderAssertion(String message, Predicate<Stream<Map.Entry<String, String>>> assertion) {
		this.responseHeaderAssertion.add(new AssertionDefinition<>(message, assertion));
		return this;
	}

	@Override
	public HttpTestClient doGET(String url) {
		this.doGET = true;
		pathToTest = url;
		return this;
	}

	@Override
	public HttpTestClient doHEAD(String url) {
		this.doHEAD = true;
		pathToTest = url;
		return this;
	}

	@Override
	public String doGETandExecuteTest(String url) throws Exception {
		doGET(url);
		return executeTest();
	}

	@Override
	public HttpTestClient doPOST(String url) {
		this.doPOST = true;
		pathToTest = url;
		return this;
	}

	@Override
	public HttpTestClient doOPTIONS(String url, String path) {
		this.doOPTIONS = true;
		this.urlToTest = url;
		this.pathToTest = path;
		return this;
	}

	@Override
	public HttpTestClient doPOST(String url, Map<String, byte[]> attachments) {
		this.doPOST = true;
		pathToTest = url;
		this.attachments = attachments;
		return this;
	}

	@Override
	public HttpTestClient addParameter(String name, String value) {
		if (name == null || value == null) {
			throw new IllegalArgumentException("Parameters must be set!");
		}
		requestParameters.put(name, value);
		return this;
	}

	@Override
	public String executeTest() throws Exception {
		if (async) {
			return "";//executeAsyncTest();
		} else {
			HttpClientBuilder httpClientBuilder = HttpClients.custom();
			CloseableHttpClient httpClient;

			if (keystoreLocationURL != null) {
				// Trust own CA and all self-signed certs
				SSLContext sslcontext = SSLContexts.custom()
						.loadTrustMaterial(keystoreLocationURL, keystorePassword.toCharArray(), new TrustSelfSignedStrategy())
						.loadKeyMaterial(keystoreLocationURL, keystorePassword.toCharArray(), keyManagerPassword.toCharArray())
						.build();

				PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
						.setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
								.setSslContext(sslcontext)
								.setTlsVersions(TLS.V_1_2)
								.build())
						.setDefaultSocketConfig(SocketConfig.custom()
								.setSoTimeout(Timeout.ofSeconds(5))
								.build())
						.setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
						.setConnPoolPolicy(PoolReusePolicy.LIFO)
						.setConnectionTimeToLive(TimeValue.ofMinutes(1L))
						.build();

				httpClientBuilder.setConnectionManager(connectionManager);
			}

			if (httpState != null) {
				httpClientBuilder.setDefaultCookieStore(httpState.getCookieStore());
			}

			if (!followRedirects) {
				httpClientBuilder.disableRedirectHandling();
			}

			RequestConfig requestConfig = RequestConfig.custom()
					.setResponseTimeout(Timeout.ofSeconds(timeoutInSeconds))
					.setConnectTimeout(Timeout.ofSeconds(timeoutInSeconds))
					.setConnectionRequestTimeout(Timeout.ofSeconds(timeoutInSeconds))
					.build();

			ClassicHttpRequest request = null;
			ClassicRequestBuilder requestBuilder = null;

			if (doGET && !doPOST) {
				requestBuilder = ClassicRequestBuilder.get(pathToTest);
			} else if (doPOST && !doGET) {
				requestBuilder = ClassicRequestBuilder.post(pathToTest);
			} else if (doHEAD) {
				requestBuilder = ClassicRequestBuilder.head(pathToTest);
			} else if (doOPTIONS) {
				requestBuilder = ClassicRequestBuilder.options(urlToTest);
			} else {
				throw new IllegalStateException("Test must be configured either with GET or POST!");
			}

			requestParameters.forEach(requestBuilder::addParameter);
			httpHeaders.forEach(requestBuilder::addHeader);

			if (attachments != null) {
				final MultipartEntityBuilder b = MultipartEntityBuilder.create();
				attachments.forEach(b::addBinaryBody);
				requestBuilder.setEntity(b.build());
			}

			if (doOPTIONS) {
				requestBuilder.setPath(pathToTest);
			}

			request = requestBuilder.build();
			httpClientBuilder.setDefaultRequestConfig(requestConfig);

			if (authDefinition != null) {
				BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
				credsProvider.setCredentials(
						new AuthScope(null, null, -1, authDefinition.realm, null),
						new UsernamePasswordCredentials(authDefinition.user, authDefinition.password.toCharArray()));
				httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
			}

			LOG.info("starting httpClient");
			httpClient = httpClientBuilder.build();

			final ResultWrapper resultWrapper = new ResultWrapper();
			CloseableHttpResponse response;
			try {
				LOG.info("calling synchronous");
				response = httpClient.execute(request);
				if (response.getEntity() != null) {
					resultWrapper.content = EntityUtils.toString(response.getEntity());
					String ct = response.getEntity().getContentType();
					if (ct != null) {
						resultWrapper.contentType = ContentType.parse(ct).getMimeType();
					}
				}
				resultWrapper.httpStatus = response.getCode();
				resultWrapper.headers = new LinkedHashMap<>();
				Arrays.stream(response.getHeaders())
						.forEach(h -> resultWrapper.headers.put(h.getName(), h.getValue()));
			} catch (Exception e) {
				LOG.info("caught exception from client call: ", e);
				throw e;
			} finally {
				LOG.info("stopping client");
				httpClient.close();
			}

			// only log text-content if available on INFO
			if (LOG.isInfoEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append(response.getVersion()).append(" ")
						.append(response.getCode()).append(" ")
						.append(response.getReasonPhrase())
						.append("\r\n");
				for (Header header : response.getHeaders()) {
					sb.append(header.getName()).append(": ").append(header.getValue()).append("\r\n");
				}
				sb.append("\r\n");
				LOG.info("---------------- Response headers received from '{}' ----------------\n" +
								"---------------- START Response-Headers ----------------\n" +
								"{}\n" +
								"---------------- END Response-Headers ----------------",
						request.getUri(), sb.toString());
				if (resultWrapper.contentType != null && resultWrapper.contentType.startsWith("text") && !resultWrapper.content.trim().isEmpty()) {
					LOG.info("---------------- Response content received from '{}' ----------------\n" +
									"---------------- START Response-Body ----------------\n" +
									"{}\n" +
									"---------------- END Response-Body ----------------",
							request.getUri(), resultWrapper.content);
				}
			}

			doAssertion(resultWrapper);

			return resultWrapper.content;
		}
	}

//	private String executeAsyncTest() throws Exception {
//		HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClientBuilder.create();
//		CloseableHttpAsyncClient httpClient = null;
//
//		if (keystoreLocationURL != null) {
//
//			// Trust own CA and all self-signed certs
//			SSLContext sslcontext = SSLContexts.custom()
//					.loadTrustMaterial(keystoreLocationURL, keystorePassword.toCharArray(), new TrustSelfSignedStrategy())
//					.loadKeyMaterial(keystoreLocationURL, keystorePassword.toCharArray(), keyManagerPassword.toCharArray())
//					.build();
//			// Allow TLSv1 protocol only
//			SSLIOSessionStrategy sslSessionStrategy = new SSLIOSessionStrategy(
//					sslcontext,
//					new String[] { "TLSv1.2" },
//					null,
//					new NoopHostnameVerifier());
//
//			httpClientBuilder.setSSLStrategy(sslSessionStrategy);
//		}
//
//		if (httpState != null) {
//			httpClientBuilder.setDefaultCookieStore(httpState.getCookieStore());
//		}
//
//		httpClientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
//
//		RequestConfig requestConfig = RequestConfig.custom()
//				.setSocketTimeout((int) TimeUnit.SECONDS.toMillis(timeoutInSeconds))
//				.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(timeoutInSeconds))
//				.setConnectionRequestTimeout((int) TimeUnit.SECONDS.toMillis(timeoutInSeconds))
//				.build();
//
//		HttpRequestBase request = null;
//		RequestBuilder requestBuilder = null;
//
//		if (doGET && !doPOST) {
//			requestBuilder = RequestBuilder.get(pathToTest);
//		} else if (doPOST && !doGET) {
//			requestBuilder = RequestBuilder.post(pathToTest);
//		} else {
//			throw new IllegalStateException("Test must be configured either with GET or POST!");
//		}
//
//		requestParameters.forEach(requestBuilder::addParameter);
//		httpHeaders.forEach(requestBuilder::addHeader);
//
//		request = (HttpRequestBase) requestBuilder.build();
//		request.setConfig(requestConfig);
//
//		if (authDefinition != null) {
//			CredentialsProvider credsProvider = new BasicCredentialsProvider();
//			credsProvider.setCredentials(
//					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, authDefinition.realm, AuthScope.ANY_SCHEME),
//					new UsernamePasswordCredentials("user", "passwd"));
//			httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
//		}
//
//		LOG.info("starting httpClient");
//		httpClient = httpClientBuilder.build();
//		httpClient.start();
//
//		final ResultWrapper resultWrapper = new ResultWrapper();
//		try {
//			LOG.info("calling asynchronous");
//			Future<HttpResponse> asyncResponse = httpClient.execute(request, null);
//			HttpResponse response = asyncResponse.get();
//			resultWrapper.content = EntityUtils.toString(response.getEntity());
//			resultWrapper.contentType = ContentType.get(response.getEntity()) == null ? null
//					: ContentType.get(response.getEntity()).getMimeType();
//			resultWrapper.httpStatus = response.getStatusLine().getStatusCode();
//			resultWrapper.headers = new LinkedHashMap<>();
//			Arrays.stream(response.getAllHeaders())
//					.forEach(h -> resultWrapper.headers.put(h.getName(), h.getValue()));
//		} catch (Exception e) {
//			LOG.info("caught exception from client call: ", e);
//			throw (Exception) e.getCause();
//		} finally {
//			LOG.info("stopping client");
//			httpClient.close();
//		}
//
//		// only log text-content if available on INFO
//		if (LOG.isInfoEnabled() && resultWrapper.contentType != null && resultWrapper.contentType.startsWith("text") && !resultWrapper.content.trim().isEmpty()) {
//			LOG.info(
//					"---------------- Response with content received from '{}' ----------------\n" +
//							"---------------- START Response-Body ----------------\n" +
//							"{}\n" +
//							"---------------- END Response-Body ----------------"
//					, request.getURI(), resultWrapper.content);
//		}
//
//		doAssertion(resultWrapper);
//
//		return resultWrapper.content;
//	}

	private void doAssertion(ResultWrapper result) {
		final Collection<String> assertionErrors = new ArrayList<>();

		if (returnCode != null) {
			final boolean assertionResult = assertTrue(result.httpStatus,
					status -> Arrays.stream(returnCode).anyMatch(value -> status == value));
			if (!assertionResult) {
				assertionErrors.add(
						String.format(
								"Unexpected HttpStatusCode returned! Expected '%s', but was '%s'",
								Arrays.stream(returnCode).mapToObj(String::valueOf).collect(Collectors.joining(" or ")),
								result.httpStatus));
			}
		}

		for (AssertionDefinition<String> wrapper : responseContentAssertion) {
			final boolean assertionResult = assertTrue(result.content != null ? result.content : "", wrapper.predicate);
			if (!assertionResult) {
				assertionErrors.add("Response-Content mismatch: " + wrapper.message);
			}
		}

		for (AssertionDefinition<Stream<Map.Entry<String, String>>> wrapper : responseHeaderAssertion) {
			final boolean assertionResult = assertTrue(result.headers.entrySet().stream(), wrapper.predicate);
			if (!assertionResult) {
				assertionErrors.add("Response-Header mismatch: " + wrapper.message);
			}
		}

		if (!assertionErrors.isEmpty()) {
			throw new AssertionError(
					"Result is not conforming to expected definitions!\n" + String.join("\n\t", assertionErrors));
		}
	}

	private static final class AssertionDefinition<T> {
		private final String message;
		private final Predicate<T> predicate;

		private AssertionDefinition(String message, Predicate<T> predicate) {
			this.message = message;
			this.predicate = predicate;
		}
	}

	private static final class BaseAuthDefinition {
		private final String user;
		private final String password;
		private final String realm;

		private BaseAuthDefinition(String user, String password, String realm) {
			if (user == null || password == null || realm == null) {
				throw new IllegalArgumentException("Values must be set!");
			}
			this.user = user;
			this.password = password;
			this.realm = realm;
		}
	}

	private static final class ResultWrapper {
		private int httpStatus;
		private String content;
		private String contentType;
		private Map<String, String> headers;
	}

	@Override
	public HttpTestClient withBundleKeystore(String bundleSymbolicName, String keystoreLocation,
			String keystorePassword, String keyManagerPassword) {
		this.keystorePassword = keystorePassword;
		this.keyManagerPassword = keyManagerPassword;
		return withBundleKeystore(bundleSymbolicName, keystoreLocation);
	}

}
