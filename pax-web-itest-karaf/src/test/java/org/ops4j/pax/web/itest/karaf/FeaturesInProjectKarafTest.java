package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.karaf.features.FeaturesService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FeaturesInProjectKarafTest {

	protected DefaultHttpClient httpclient;
	
	@Inject
	private FeaturesService featuresService;

	@Configuration
	public Option[] config() {
		StringBuffer warUrl = new StringBuffer(mavenBundle().groupId("org.apache.myfaces.commons")
				.artifactId("myfaces-commons-facelets-examples20")
				.version(asInProject())
				.type("war").getURL());
		
		warUrl.append("Web-ContextPath=/test-faces");
		
		return new Option[] {
				karafDistributionConfiguration(
						"mvn:org.apache.karaf/apache-karaf/2.2.8/zip", "karaf",
						"2.2.8"),
				scanFeatures(
						maven().groupId("org.ops4j.pax.web")
								.artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-war").start(),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpclient", "4.1")),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpcore", "4.1"))/*,
				mavenBundle().groupId("commons-beanutils")
					.artifactId("commons-beanutils").version(asInProject()),
				mavenBundle().groupId("commons-collections")
					.artifactId("commons-collections").version(asInProject()),
				mavenBundle().groupId("commons-codec")
					.artifactId("commons-codec").version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.bundles")
					.artifactId("org.apache.servicemix.bundles.commons-digester")
					.version("1.8_4"),
				mavenBundle().groupId("org.apache.geronimo.bundles")
					.artifactId("commons-discovery")
					.version("0.4_1"),
				mavenBundle().groupId("org.apache.myfaces.core")
					.artifactId("myfaces-api").version(asInProject()),
				mavenBundle().groupId("org.apache.myfaces.core")
					.artifactId("myfaces-impl").version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.specs")
					.artifactId("org.apache.servicemix.specs.jsr303-api-1.0.0")
					.version(asInProject()),
				bundle(warUrl.toString())*/
				};
	}

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-war")));
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-whiteboard")));
		
//		testWebPath("http://127.0.0.1:8181/test-faces", "Please enter your name");
	}
	
	
	/**
	 * @return
	 * @throws IOException
	 * @throws HttpException
	 */
	protected void testWebPath(String path, String expectedContent)
			throws IOException {
		testWebPath(path, expectedContent, 200, false);
	}
	
	protected void testWebPath(String path, int httpRC)
			throws IOException {
		testWebPath(path, null, httpRC, false);
	}

	protected void testWebPath(String path, String expectedContent, int httpRC,
			boolean authenticate) throws IOException {
		testWebPath(path, expectedContent, httpRC, authenticate, null);
	}

	protected void testWebPath(String path, String expectedContent, int httpRC,
			boolean authenticate, BasicHttpContext basicHttpContext)
			throws ClientProtocolException, IOException {

		int count = 0;
		while (!checkServer() && count++ < 5)
			if (count > 5)
				break;

		HttpResponse response = getHttpResponse(path, authenticate,
				basicHttpContext);

		assertEquals("HttpResponseCode", httpRC, response.getStatusLine()
				.getStatusCode());

		if (expectedContent != null) {
			String responseBodyAsString = EntityUtils
				.toString(response.getEntity());
			assertTrue(responseBodyAsString.contains(expectedContent));
		}
	}

	/**
	 * @param path
	 * @param authenticate
	 * @param basicHttpContext
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	protected HttpResponse getHttpResponse(String path, boolean authenticate,
			BasicHttpContext basicHttpContext) throws IOException,
			ClientProtocolException {
		HttpGet httpget = null;
		HttpHost targetHost = new HttpHost("localhost", 8181, "http");
		BasicHttpContext localcontext = basicHttpContext == null ? new BasicHttpContext()
				: basicHttpContext;
		if (authenticate) {

			((DefaultHttpClient) httpclient).getCredentialsProvider()
					.setCredentials(
							new AuthScope(targetHost.getHostName(),
									targetHost.getPort()),
							new UsernamePasswordCredentials("admin", "admin"));

			// Create AuthCache instance
			AuthCache authCache = new BasicAuthCache();
			// Generate BASIC scheme object and add it to the local auth cache
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(targetHost, basicAuth);

			// Add AuthCache to the execution context

			localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);

		}

		httpget = new HttpGet(path);
		HttpResponse response = null;
		if (!authenticate && basicHttpContext == null)
			response = httpclient.execute(httpget);
		else
			response = httpclient.execute(targetHost, httpget, localcontext);
		return response;
	}

	protected boolean checkServer() throws ClientProtocolException, IOException {
		HttpGet httpget = null;
		HttpHost targetHost = new HttpHost("localhost", 8181, "http");
		httpget = new HttpGet("/");
		HttpClient myHttpClient = new DefaultHttpClient();
		HttpResponse response = myHttpClient.execute(targetHost, httpget);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 404 || statusCode == 200)
			return true;
		else
			return false;
	}
	
	@Before
	public void setUpITestBase() throws Exception {
		httpclient = new DefaultHttpClient();
	}
	
	@After
	public void tearDown() throws Exception {
		httpclient.clearRequestInterceptors();
		httpclient.clearResponseInterceptors();
		httpclient = null;
	}
}