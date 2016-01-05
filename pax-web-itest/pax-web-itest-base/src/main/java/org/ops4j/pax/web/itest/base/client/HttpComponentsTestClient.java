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

import static org.ops4j.pax.web.itest.base.assertion.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpComponentsTestClient implements HttpTestClient {

	private static final Logger LOG = LoggerFactory.getLogger(HttpComponentsTestClient.class);

	private KeystoreConfiguration keystoreConfig;
	private Integer returnCode;
	private Map<String, String> httpHeaders = new HashMap<>();
	private Collection<AssertionWrapper<String>> responseAssertion = new ArrayList<>();

	HttpComponentsTestClient() {
	}

	@Override
	public HttpTestClient withKeystore(String keystoreLocation, String username, String password) {
		this.keystoreConfig = new KeystoreConfiguration(keystoreLocation, username, password);
		return this;
	}

	@Override
	public HttpTestClient addRequestHeader(String header, String value) {
		httpHeaders.put(header, value);
		return this;
	}

	@Override
	public HttpTestClient withReturnCode(int returnCode) {
		this.returnCode = Integer.valueOf(returnCode);
		return this;
	}

	@Override
	public HttpTestClient prepareResponseAssertion(final String message, final Predicate<String> assertion) {
		this.responseAssertion.add(new AssertionWrapper<>(message, assertion));
		return this;
	}

	@Override
	public void executeTest(String path) throws Exception {

		HttpComponentsWrapper wrapper = new HttpComponentsWrapper(
				httpHeaders,
				keystoreConfig.getUsername(), 
				keystoreConfig.getPassword(), 
				keystoreConfig.getKeystoreLocation());

		HttpResponse httpResponse = wrapper.testWebPath(path);

		doAssertion(httpResponse);
	}

	@SuppressWarnings("unchecked")
	private void doAssertion(HttpResponse httpResponse) throws Exception {

		final Collection<String> assertionErrors = new ArrayList<>();

		if (returnCode != null) {
			final boolean assertionResult = assertTrue(httpResponse.getStatusLine().getStatusCode(),
					status -> returnCode.equals(status));
			if (!assertionResult) {
				assertionErrors.add(
						String.format(
								"Unexpected HttpStatusCode returned! Expected '%s', but was '%s'",
								returnCode, 
								httpResponse.getStatusLine().getStatusCode()));
			}
		}

		final String responseContent;
		// handle No-Content-Responses
		if(httpResponse.getEntity() == null){
			responseContent = "";
		}else{
			responseContent = EntityUtils.toString(httpResponse.getEntity());
		}

		for (@SuppressWarnings("rawtypes")
		AssertionWrapper wrapper : responseAssertion) {
			final boolean assertionResult = assertTrue(responseContent, wrapper.predicate);
			if (!assertionResult) {
				assertionErrors.add("Response mismatch: " + wrapper.message);
			}
		}

		if (!assertionErrors.isEmpty()) {
			throw new AssertionError(
					"Result is not conforming to expected definitions!\n" + String.join("\n\t", assertionErrors));
		}

	}

	private final class AssertionWrapper<T> {
		private final String message;
		private final Predicate<T> predicate;

		private AssertionWrapper(String message, Predicate<T> predicate) {
			this.message = message;
			this.predicate = predicate;
		}
	}

	private final class KeystoreConfiguration {
		private final String keystoreLocation;
		private final String username;
		private final String password;

		private KeystoreConfiguration(String keystoreLocation, String username, String password) {
			super();
			this.keystoreLocation = keystoreLocation;
			this.username = username;
			this.password = password;
		}

		private String getKeystoreLocation() {
			return keystoreLocation;
		}

		private String getUsername() {
			return username;
		}

		private String getPassword() {
			return password;
		}

	}

}
