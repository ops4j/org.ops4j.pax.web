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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class HttpComponentsWrapper {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpComponentsWrapper.class);

	protected CloseableHttpClient httpclient;

	protected CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients.createDefault();

	private HttpClientContext context = HttpClientContext.create();

	private final Map<String, String> httpHeaders;

	private String user;

	private String password;

	private String keyStore;

	HttpComponentsWrapper(Map<String, String> httpHeaders, String user, String password, String keyStore) throws Exception {
		if(httpHeaders != null){
			this.httpHeaders = Collections.unmodifiableMap(httpHeaders);
		}else{
			this.httpHeaders = Collections.unmodifiableMap(Collections.emptyMap());
		}
		this.user = user;
		this.password = password;

		if (keyStore.startsWith("${")) {
			int indexOfPlaceHolder = keyStore.indexOf("}");
			String placeHolder = keyStore.substring(0, indexOfPlaceHolder);
			placeHolder = placeHolder.substring(2, placeHolder.length());
			String property = System.getProperty(placeHolder);
			this.keyStore = property + keyStore.substring(indexOfPlaceHolder + 1);
		} else {
			this.keyStore = keyStore;
		}

		httpclient = createHttpClient();
		httpAsyncClient.start();
	}

	private CloseableHttpClient createHttpClient() throws KeyStoreException, IOException, NoSuchAlgorithmException,
			CertificateException, KeyManagementException {
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		SSLConnectionSocketFactory sslsf = null;
		try {
			FileInputStream instream = new FileInputStream(new File(keyStore));
			try {
				trustStore.load(instream, "password".toCharArray());
			} finally {
				// CHECKSTYLE:OFF
				try {
					instream.close();
				} catch (Exception ignore) {
				}
				// CHECKSTYLE:ON
			}

			SSLContext sslContext = SSLContexts.custom().useTLS().loadTrustMaterial(trustStore).build();
			sslsf = new SSLConnectionSocketFactory(sslContext, (X509HostnameVerifier) hostnameVerifier);
		} catch (FileNotFoundException e) {
			LOG.error("Error preparing SSL for testing. Https will not be available.", e);
		}

		PlainConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();

		RegistryBuilder<ConnectionSocketFactory> rb = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("http", plainsf);
		if (sslsf != null) {
			rb.register("https", sslsf);
		}

		Registry<ConnectionSocketFactory> registry = rb.build();

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);

		return HttpClients.custom().setConnectionManager(cm).build();

	}
	

	public HttpResponse testWebPath(String path) throws Exception {
		return testWebPath(path, false, null);
	}

	public HttpResponse testWebPath(String path, boolean authenticate) throws Exception {
		return testWebPath(path, authenticate, null);
	}

	private HttpResponse testWebPath(String path, boolean authenticate, BasicHttpContext basicHttpContext)
			throws Exception{
		return getHttpResponse(path, authenticate, basicHttpContext, false);
	}

	private HttpResponse getHttpResponse(String path, boolean authenticate, BasicHttpContext basicHttpContext,
			boolean async) throws IOException, KeyManagementException, UnrecoverableKeyException,
					NoSuchAlgorithmException, KeyStoreException, CertificateException, AuthenticationException,
					InterruptedException, ExecutionException {

		HttpHost targetHost = getHttpHost(path);

		BasicHttpContext localcontext = basicHttpContext == null ? new BasicHttpContext() : basicHttpContext;

		HttpGet httpget = new HttpGet(path);
		for(Map.Entry<String, String> entry : httpHeaders.entrySet()){
			LOG.info("adding request-header: {}={}", entry.getKey(), entry.getValue());
			httpget.addHeader(entry.getKey(), entry.getValue());
		}

		LOG.info("calling remote {} ...", path);
		HttpResponse response = null;
		if (!authenticate && basicHttpContext == null) {
			if (localcontext.getAttribute(ClientContext.AUTH_CACHE) != null) {
				localcontext.removeAttribute(ClientContext.AUTH_CACHE);
			}
			if (!async) {
				response = httpclient.execute(httpget, context);
			} else {
				Future<HttpResponse> future = httpAsyncClient.execute(httpget, context, null);
				response = future.get();
			}
		} else {
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, password);

			// Create AuthCache instance
			AuthCache authCache = new BasicAuthCache();
			// Generate BASIC scheme object and add it to the local auth
			// cache
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(targetHost, basicAuth);

			localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
			httpget.addHeader(basicAuth.authenticate(creds, httpget, localcontext));
			if (!async) {
				response = httpclient.execute(targetHost, httpget, localcontext);
			} else {
				Future<HttpResponse> future = httpAsyncClient.execute(targetHost, httpget, localcontext, null);
				response = future.get();
			}
		}

		LOG.info("... responded with: {}", response.getStatusLine().getStatusCode());
		return response;
	}
	
	HttpHost getHttpHost(String path) {
		int schemeSeperator = path.indexOf(":");
		String scheme = path.substring(0, schemeSeperator);

		int portSeperator = path.lastIndexOf(":");
		String hostname = path.substring(schemeSeperator + 3, portSeperator);

		int port = Integer.parseInt(path.substring(portSeperator + 1,
				portSeperator + 5));

		HttpHost targetHost = new HttpHost(hostname, port, scheme);
		return targetHost;
	}
}
