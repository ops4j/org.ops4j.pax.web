package org.ops4j.pax.web.itest.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.anything;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.omg.CORBA.NamedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTestClient {

	private static final Logger LOG = LoggerFactory.getLogger(HttpTestClient.class);

	protected DefaultHttpClient httpclient;

	private String user;

	private String password;
	
	private String keyStore;
	
	public HttpTestClient() {
		this("admin", "admin", "src/test/resources/keystore");
	}


	public HttpTestClient(String user, String password, String keyStore) {
		this.user = user;
		this.password = password;
		this.keyStore = keyStore;
		httpclient = new DefaultHttpClient();
	}


	public void close() {
		httpclient.clearRequestInterceptors();
		httpclient.clearResponseInterceptors();
		httpclient = null;
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

	public String testWebPath(String path, String expectedContent)
			throws Exception {
		return testWebPath(path, expectedContent, 200, false);
	}

	public String testWebPath(String path, int httpRC) throws Exception {
		return testWebPath(path, null, httpRC, false);
	}

	public String testWebPath(String path, String expectedContent,
			int httpRC, boolean authenticate) throws Exception {
		return testWebPath(path, expectedContent, httpRC, authenticate, null);
	}

	public String testWebPath(String path, String expectedContent,
			int httpRC, boolean authenticate, BasicHttpContext basicHttpContext)
			throws Exception {

		int count = 0;
		while (!checkServer(path) && count++ < 5) {
			if (count > 5) {
				break;
			}
		}

		HttpResponse response = null;
		response = getHttpResponse(path, authenticate, basicHttpContext);

		assertEquals("HttpResponseCode", httpRC, response.getStatusLine()
				.getStatusCode());

		String responseBodyAsString = null;
		if (expectedContent != null) {
			responseBodyAsString = EntityUtils.toString(response.getEntity());
			assertTrue("Content: " + responseBodyAsString,
					responseBodyAsString.contains(expectedContent));
//			assertThat(responseBodyAsString, anything(expectedContent));
		}

		return responseBodyAsString;
	}

	private boolean isSecuredConnection(String path) {
		int schemeSeperator = path.indexOf(":");
		String scheme = path.substring(0, schemeSeperator);

		if ("https".equalsIgnoreCase(scheme)) {
			return true;
		}

		return false;
	}

	public void testPost(String path, List<NameValuePair> nameValuePairs,
			String expectedContent, int httpRC) throws IOException {

		HttpPost post = new HttpPost(path);
		post.setEntity(new UrlEncodedFormEntity(
				(List<NameValuePair>) nameValuePairs));

		HttpResponse response = httpclient.execute(post);
		assertEquals("HttpResponseCode", httpRC, response.getStatusLine()
				.getStatusCode());

		if (expectedContent != null) {
			String responseBodyAsString = EntityUtils.toString(response
					.getEntity());
			assertTrue("Content: " + responseBodyAsString,
					responseBodyAsString.contains(expectedContent));
		}
	}
	
	public void testPostMultipart(String path, Map<String, Object> multipartContent,
			String expectedContent, int httpRC) throws IOException {
		HttpPost httppost = new HttpPost(path);

        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (Entry<String, Object> content : multipartContent.entrySet()) {
        	if (content.getValue() instanceof String) {
        		multipartEntityBuilder.addPart(content.getKey(), new StringBody((String) content.getValue(), ContentType.TEXT_PLAIN));
        	} 
		}
        		
        httppost.setEntity(multipartEntityBuilder.build());

        CloseableHttpResponse response = httpclient.execute(httppost);
        
        assertEquals("HttpResponseCode", httpRC, response.getStatusLine()
				.getStatusCode());

		if (expectedContent != null) {
			String responseBodyAsString = EntityUtils.toString(response
					.getEntity());
			assertTrue("Content: " + responseBodyAsString,
					responseBodyAsString.contains(expectedContent));
		}
	}

	public HttpResponse getHttpResponse(String path, boolean authenticate,
			BasicHttpContext basicHttpContext) throws IOException,
			KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException, CertificateException {
		HttpGet httpget = null;
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		FileInputStream instream = new FileInputStream(new File(
				keyStore));
		try {
			trustStore.load(instream, "password".toCharArray());
		} finally {
			//CHECKSTYLE:OFF
			try {
				instream.close();
			} catch (Exception ignore) {
			}
			//CHECKSTYLE:ON
		}

		SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
		Scheme sch = new Scheme("https", 443, socketFactory);
		httpclient.getConnectionManager().getSchemeRegistry().register(sch);
		socketFactory
				.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);

		HttpHost targetHost = getHttpHost(path);

		// Set verifier
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

		BasicHttpContext localcontext = basicHttpContext == null ? new BasicHttpContext()
				: basicHttpContext;
		if (authenticate) {

			((DefaultHttpClient) httpclient).getCredentialsProvider()
					.setCredentials(
							new AuthScope(targetHost.getHostName(),
									targetHost.getPort()),
							new UsernamePasswordCredentials(user, password));

			// Create AuthCache instance
			AuthCache authCache = new BasicAuthCache();
			// Generate BASIC scheme object and add it to the local auth cache
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(targetHost, basicAuth);

			// Add AuthCache to the execution context

			localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);

		} else {
			if (localcontext.getAttribute(ClientContext.AUTH_CACHE) != null)
				localcontext.removeAttribute(ClientContext.AUTH_CACHE);
		}

		httpget = new HttpGet(path);
		LOG.info("calling remote {} ...", path);
		HttpResponse response = null;
		if (!authenticate && basicHttpContext == null) {
			response = httpclient.execute(httpget);
		} else {
			response = httpclient.execute(targetHost, httpget, localcontext);
		}

		if (response.getStatusLine().getStatusCode() == 403) {
			EntityUtils.consumeQuietly(response.getEntity());
		}

		LOG.info("... responded with: {}", response.getStatusLine()
				.getStatusCode());
		return response;
	}
	

	public boolean checkServer(String path) throws Exception {
		LOG.info("checking server path {}", path);
		HttpGet httpget = null;
		HttpClient myHttpClient = new DefaultHttpClient();
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
	
		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		FileInputStream instream = new FileInputStream(new File(
				keyStore));
		try {
			trustStore.load(instream, "password".toCharArray());
		} finally {
			//CHECKSTYLE:OFF
			try {
				instream.close();
			} catch (Exception ignore) {
			}
			//CHECKSTYLE:ON
		}
	
		SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
		Scheme sch = new Scheme("https", 443, socketFactory);
		myHttpClient.getConnectionManager().getSchemeRegistry().register(sch);
		socketFactory
				.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
	
		HttpHost targetHost = getHttpHost(path);
	
		// Set verifier
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
	
		httpget = new HttpGet("/");
		LOG.info(
				"calling remote {}://{}:{}/ ...",
				new Object[] { targetHost.getSchemeName(),
						targetHost.getHostName(), targetHost.getPort() });
		HttpResponse response = null;
		try {
			response = myHttpClient.execute(targetHost, httpget);
		} catch (IOException ioe) {
			LOG.info("... caught IOException");
			return false;
		}
		int statusCode = response.getStatusLine().getStatusCode();
		LOG.info("... responded with: {}", statusCode);
		return statusCode == 404 || statusCode == 200;
	}

}
