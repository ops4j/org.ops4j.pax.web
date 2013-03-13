package org.ops4j.pax.web.itest.karaf;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.tooling.exam.options.ExamBundlesStartLevel;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.apache.karaf.tooling.exam.options.LogLevelOption.LogLevel;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class KarafBaseTest {

	protected DefaultHttpClient httpclient;

	@Inject
	protected FeaturesService featuresService;

	@Inject
	protected BundleContext bundleContext;

	public Option[] baseConfig() {
		return new Option[] {
				karafDistributionConfiguration(
						"mvn:org.apache.karaf/apache-karaf/"
								+ getKarafVersion() + "/zip", "karaf",
						getKarafVersion()).useDeployFolder(false)
						.unpackDirectory(new File("target/paxexam/unpack/")),
				logLevel(LogLevel.INFO),
				keepRuntimeFolder(),
				KarafDistributionOption.editConfigurationFilePut(
						"etc/org.ops4j.pax.url.mvn.cfg",
						"org.ops4j.pax.url.mvn.repositories",
						"http://repo1.maven.org/maven2"),
				new VMOption("-DProjectVersion=" + getProjectVersion()),
				scanFeatures(
						maven().groupId("org.ops4j.pax.web")
								.artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-war").start(),
				new ExamBundlesStartLevel(4),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpclient", "4.1")),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpcore", "4.1")),
				mavenBundle().groupId("commons-beanutils")
						.artifactId("commons-beanutils").version(asInProject()),
				mavenBundle().groupId("commons-collections")
						.artifactId("commons-collections")
						.version(asInProject()),
				mavenBundle().groupId("commons-codec")
						.artifactId("commons-codec").version(asInProject()),
				mavenBundle()
						.groupId("org.apache.servicemix.bundles")
						.artifactId(
								"org.apache.servicemix.bundles.commons-digester")
						.version("1.8_4"),
				mavenBundle().groupId("org.apache.geronimo.bundles")
						.artifactId("commons-discovery").version("0.4_1"),
				mavenBundle()
						.groupId("org.apache.servicemix.specs")
						.artifactId(
								"org.apache.servicemix.specs.jsr303-api-1.0.0")
						.version(asInProject()) };
	}

	/**
	 * @return
	 * @throws IOException
	 * @throws HttpException
	 */
	protected String testWebPath(String path, String expectedContent)
			throws IOException {
		return testWebPath(path, expectedContent, 200, false);
	}

	protected String testWebPath(String path, int httpRC) throws IOException {
		return testWebPath(path, null, httpRC, false);
	}

	protected String testWebPath(String path, String expectedContent,
			int httpRC, boolean authenticate) throws IOException {
		return testWebPath(path, expectedContent, httpRC, authenticate, null);
	}

	protected String testWebPath(String path, String expectedContent,
			int httpRC, boolean authenticate, BasicHttpContext basicHttpContext)
			throws ClientProtocolException, IOException {

		int count = 0;
		while (!checkServer() && count++ < 5)
			if (count > 5)
				break;

		HttpResponse response = getHttpResponse(path, authenticate,
				basicHttpContext);

		assertEquals("HttpResponseCode", httpRC, response.getStatusLine()
				.getStatusCode());

		String responseBodyAsString = EntityUtils
				.toString(response.getEntity());

		if (expectedContent != null) {
			assertTrue(responseBodyAsString.contains(expectedContent));
		}

		return responseBodyAsString;
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

	protected void testPost(String path, List<NameValuePair> nameValuePairs,
			String expectedContent, int httpRC) throws ClientProtocolException,
			IOException {

		HttpPost post = new HttpPost(path);
		post.setEntity(new UrlEncodedFormEntity(
				(List<NameValuePair>) nameValuePairs));

		HttpResponse response = httpclient.execute(post);
		assertEquals("HttpResponseCode", httpRC, response.getStatusLine()
				.getStatusCode());

		if (expectedContent != null) {
			String responseBodyAsString = EntityUtils.toString(response
					.getEntity());
			assertTrue(responseBodyAsString.contains(expectedContent));
		}
	}

	protected boolean checkServer() throws ClientProtocolException, IOException {
		HttpGet httpget = null;
		HttpHost targetHost = new HttpHost("localhost", 8181, "http");
		httpget = new HttpGet("/");
		HttpClient myHttpClient = new DefaultHttpClient();
		HttpResponse response = null;
		try {
			response = myHttpClient.execute(targetHost, httpget);
		} catch (IOException ioe) {
			return false;
		}
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

	protected static String getMyFacesVersion() {
		String myFacesVersion = System.getProperty("MyFacesVersion");
		System.out.println("*** The MyFacesVersion is " + myFacesVersion
				+ " ***");
		return myFacesVersion;
	}

	protected static String getProjectVersion() {
		String projectVersion = System.getProperty("ProjectVersion");
		System.out.println("*** The ProjectVersion is " + projectVersion
				+ " ***");
		return projectVersion;
	}

	protected static String getKarafVersion() {
		String karafVersion = System.getProperty("KarafVersion");
		System.out.println("*** The KarafVersion is " + karafVersion + " ***");
		return karafVersion;
	}

}