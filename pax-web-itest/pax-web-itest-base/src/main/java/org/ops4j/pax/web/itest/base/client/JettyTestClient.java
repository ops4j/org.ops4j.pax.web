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
package org.ops4j.pax.web.itest.base.client;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.ops4j.pax.web.itest.base.assertion.Assert.assertTrue;

class JettyTestClient implements HttpTestClient {

	private static Logger LOG = LoggerFactory.getLogger(JettyTestClient.class);

	private int[] returnCode;
	private final Map<String, String> httpHeaders = new HashMap<>();
	private boolean async;
	private final Collection<AssertionDefinition<String>> responseContentAssertion = new ArrayList<>();
	private final Collection<AssertionDefinition<Stream<Map.Entry<String, String>>>> responseHeaderAssertion = new ArrayList<>();
	private URL keystoreLocationURL;
	private BaseAuthDefinition authDefinition;
	private String pathToTest = null;
	private boolean doGET;
	private boolean doPOST;
	private Map<String, String> requestParameters = new HashMap<>();
	private int timeoutInSeconds = 100;
	private Optional<CookieState> httpState = Optional.empty();


	JettyTestClient() {
	}


	@Override
	public HttpTestClient withExternalKeystore(String keystoreLocation) {
		if(keystoreLocation == null){
			throw new IllegalArgumentException("keystoreLocation must not be null!");
		}
		if (keystoreLocation.startsWith("${")) {
			int indexOfPlaceHolder = keystoreLocation.indexOf("}");
			String placeHolder = keystoreLocation.substring(0, indexOfPlaceHolder);
			placeHolder = placeHolder.substring(2, placeHolder.length());
			String property = System.getProperty(placeHolder);
			keystoreLocation = property + keystoreLocation.substring(indexOfPlaceHolder + 1);
		}

		File keystore = new File(keystoreLocation);

		if(keystore.exists()){
			try {
				keystoreLocationURL = keystore.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}else{
			throw new IllegalArgumentException("No keystore-file found under '" + keystoreLocation + "'!");
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

		if(result == null){
			LOG.error("Bundle with '{}' not found for keystore-lookup!", bundleSymbolicName);
			return this;
		}

		this.keystoreLocationURL = result.getEntry(keystoreLocation);

		if(this.keystoreLocationURL == null){
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
		this.httpState = Optional.of(cookieState);
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
	public HttpTestClient addParameter(String name, String value) {
		if(name == null || value == null){
			throw new IllegalArgumentException("Parameters must be set!");
		}
		requestParameters.put(name, value);
		return this;
	}

	@Override
	public String executeTest() throws Exception {
		final HttpClient httpClient;
		if(keystoreLocationURL != null){
			SslContextFactory sslContextFactory = new SslContextFactory(true);
			sslContextFactory.setKeyStorePath(keystoreLocationURL.toString());
			sslContextFactory.setKeyStorePassword("password");
			sslContextFactory.setKeyManagerPassword("password");
			sslContextFactory.setKeyStoreType(KeyStore.getDefaultType());
			httpClient = new HttpClient(sslContextFactory);
		}else{
			httpClient = new HttpClient();
		}

		Request request;
		if(doGET && !doPOST){
			request = httpClient.newRequest(pathToTest);
			requestParameters.entrySet().stream().forEach(entry -> request.param(entry.getKey(), entry.getValue()));
		}else if(doPOST && !doGET){
			final Fields fields = new Fields();
			requestParameters.entrySet().stream().forEach(entry -> fields.add(entry.getKey(), entry.getValue()));
			request = httpClient.POST(pathToTest);
			request.content(new FormContentProvider(fields));

		}else {
			throw new IllegalStateException("Test must be configured either with GET or POST!");
		}

		request.timeout(timeoutInSeconds, TimeUnit.SECONDS);


		if(httpState.isPresent()){
			httpState.get().getStateValues().forEach(entry -> request.cookie(new HttpCookie(entry.getKey(), entry.getValue())));
		}

		for(Map.Entry<String, String> headerEntry : httpHeaders.entrySet()){
			request.header(headerEntry.getKey(), headerEntry.getValue());
		}

		if(authDefinition != null){
			httpClient.getAuthenticationStore().addAuthentication(
					new BasicAuthentication(
							request.getURI(),
							authDefinition.realm,
							authDefinition.user,
							authDefinition.password));
		}

		httpClient.start();
		final ResultWrapper resultWrapper = new ResultWrapper();
		try {

			if (async) {
				CompletableFuture<Result> future = new CompletableFuture<>();
				request.send(new BufferingResponseListener() {
					@Override
					public void onComplete(Result result) {
						resultWrapper.content = new String(getContent());
						resultWrapper.contentType = getMediaType() != null ? getMediaType() : "";
						future.complete(result);
					}
				});

				Result result = future.get();
				resultWrapper.httpStatus = result.getResponse().getStatus();
				resultWrapper.headers = extractHeadersFromResponse(result.getResponse());
				if(httpState.isPresent()) {
					Map<String, String> cookies = extractCockiesFromResponse(result.getResponse());
					httpState.get().putAll(cookies);
				}


			} else {
				ContentResponse contentResponse = request.send();
				resultWrapper.content = new String(contentResponse.getContent());
				resultWrapper.contentType = contentResponse.getMediaType() != null ? contentResponse.getMediaType() : "";
				resultWrapper.httpStatus = contentResponse.getStatus();
				resultWrapper.headers = extractHeadersFromResponse(contentResponse);
				if(httpState.isPresent()) {
					Map<String, String> cookies = extractCockiesFromResponse(contentResponse);
					httpState.get().putAll(cookies);
				}

			}
		} finally {
			httpClient.stop();
		}

		// only log text-content if available on INFO
		if(LOG.isInfoEnabled() && resultWrapper.contentType.startsWith("text") && !resultWrapper.content.trim().isEmpty()){
			LOG.info(
					"---------------- Response with content received from '{}' ----------------\n" +
					"---------------- START Response-Body ----------------\n" +
					"{}\n" +
					"---------------- END Response-Body ----------------"
					, request.getURI(), resultWrapper.content);
		}

		doAssertion(resultWrapper);

		return resultWrapper.content;
	}


	private Map<String, String> extractHeadersFromResponse(final Response response){
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
						response.getHeaders().iterator(), Spliterator.ORDERED), false)
				.filter(httpField -> !httpField.getName().equals("Set-Cookie"))
				.collect(Collectors.toMap(HttpField::getName, HttpField::getValue,
						(key1, key2) -> {
							LOG.warn("Dupplicate key '{}' found! Using first occurece.", key1);
							return key1;
						}));
	}

	private Map<String, String> extractCockiesFromResponse(final Response response){
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
						response.getHeaders().iterator(), Spliterator.ORDERED), false)
				.filter(httpField -> httpField.getName().equals("Set-Cookie"))
				.collect(Collectors.toMap(
						httpField -> httpField.getValue().substring(0, httpField.getValue().indexOf('=')),
						httpField -> httpField.getValue().substring(httpField.getValue().indexOf('=') + 1, httpField.getValue().length())));
	}

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


	private final class AssertionDefinition<T> {
		private final String message;
		private final Predicate<T> predicate;

		private AssertionDefinition(String message, Predicate<T> predicate) {
			this.message = message;
			this.predicate = predicate;
		}
	}

	private final class BaseAuthDefinition {
		private final String user;
		private final String password;
		private final String realm;

		private BaseAuthDefinition(String user, String password, String realm) {
			if(user == null || password == null || realm == null){
				throw new IllegalArgumentException("Values must be set!");
			}
			this.user = user;
			this.password = password;
			this.realm = realm;
		}
	}

	private final class ResultWrapper {
		private int httpStatus;
		private String content;
		private String contentType;
		private Map<String, String> headers;
	}
}
