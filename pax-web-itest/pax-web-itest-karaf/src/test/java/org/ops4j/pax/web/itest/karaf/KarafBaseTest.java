package org.ops4j.pax.web.itest.karaf;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;
import org.ops4j.pax.exam.CoreOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

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
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.karaf.features.FeaturesService;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.karaf.options.configs.CustomProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.web.itest.base.ServletListenerImpl;
import org.ops4j.pax.web.itest.base.WebListenerImpl;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class KarafBaseTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(KarafBaseTest.class);
	public static final String RMI_SERVER_PORT = "44445";
    public static final String RMI_REG_PORT = "1100";
 protected static final String COVERAGE_COMMAND = "coverage.command";

	protected DefaultHttpClient httpclient;

	@Inject
	protected FeaturesService featuresService;

	@Inject
	protected BundleContext bundleContext;

	private org.ops4j.pax.web.itest.base.WebListenerImpl webListener;

	private org.ops4j.pax.web.itest.base.ServletListenerImpl servletListener;

	public Option[] baseConfig() {
		return new Option[] {
				karafDistributionConfiguration().frameworkUrl(mvnKarafDist())
						.unpackDirectory(new File("target/paxexam/unpack/"))
						.useDeployFolder(false),
//				debugConfiguration("5005", true),
				configureConsole().ignoreLocalConsole(),
				when(isEquinox()).useOptions(
					editConfigurationFilePut(
									CustomProperties.KARAF_FRAMEWORK, "equinox"),
					systemProperty("pax.exam.framework").value(
									System.getProperty("pax.exam.framework")),
					systemProperty("osgi.console").value("6666"),
					systemProperty("osgi.console.enable.builtin").value("true")
					),
				logLevel(LogLevel.INFO),
				keepRuntimeFolder(),
				editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", RMI_REG_PORT),
	            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", RMI_SERVER_PORT),
				KarafDistributionOption.replaceConfigurationFile("etc/keystore", new File("src/test/resources/keystore")),
				systemProperty("ProjectVersion").value(getProjectVersion()),
    addCodeCoverageOption(),
				/*features(
						maven().groupId("org.ops4j.pax.web")
								.artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-war"),*/
			

				mavenBundle().groupId("org.ops4j.pax.web.itest")
				        .artifactId("pax-web-itest-base").versionAsInProject(),
						
				//new ExamBundlesStartLevel(4),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpcore").version(asInProject())),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpclient").version(asInProject())),
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
	private static Option addCodeCoverageOption() {
		String coverageCommand = System.getProperty(COVERAGE_COMMAND);
		if (coverageCommand != null) {
			LOG.info("found coverage option {}", coverageCommand);
			return CoreOptions.vmOption(coverageCommand);
		}
		return null;
	}

	public Option[] jettyConfig() {

		return combine(baseConfig(), 
				features(
						maven().groupId("org.ops4j.pax.web")
								.artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-war")
				);
	}
	
	public Option[] tomcatConfig() {

		return combine(baseConfig(), 
				features(
						maven().groupId("org.ops4j.pax.web")
								.artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-war-tomcat")
				);
	}

	private boolean isEquinox() {
		return "equinox".equals(System.getProperty("pax.exam.framework"));
	}

	private boolean isFelix() {
		return "felix".equals(System.getProperty("pax.exam.framework"));
	}

	private MavenArtifactUrlReference mvnKarafDist() {
		return maven().groupId("org.apache.karaf").artifactId("apache-karaf")
				.type("zip").version(getKarafVersion());
	}

	/**
	 * @return
	 * @throws IOException
	 * @throws HttpException
	 */
	protected String testWebPath(String path, String expectedContent)
			throws Exception {
		return testWebPath(path, expectedContent, 200, false);
	}

	protected String testWebPath(String path, int httpRC) throws Exception {
		return testWebPath(path, null, httpRC, false);
	}

	protected String testWebPath(String path, String expectedContent,
			int httpRC, boolean authenticate) throws Exception {
		return testWebPath(path, expectedContent, httpRC, authenticate, null);
	}

	protected String testWebPath(String path, String expectedContent,
			int httpRC, boolean authenticate, BasicHttpContext basicHttpContext)
			throws Exception {

		int count = 0;
		while (!checkServer("http://127.0.0.1:8181/") && count++ < 5)
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
			assertTrue("Content: " + responseBodyAsString,
					responseBodyAsString.contains(expectedContent));
		}
	}

	protected boolean checkServer(String path) throws Exception {
		LOG.info("checking server path {}", path);
		HttpGet httpget = null;
		HttpClient myHttpClient = new DefaultHttpClient();
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
		
		KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream instream = new FileInputStream(new File("etc/keystore"));
        try {
            trustStore.load(instream, "password".toCharArray());
        } finally {
        	//CHECKSTYLE:OFF
            try { instream.close(); } catch (Exception ignore) {}
            //CHECKSTYLE:ON
        }

        SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
        Scheme sch = new Scheme("https", 443, socketFactory);
        myHttpClient.getConnectionManager().getSchemeRegistry().register(sch);
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		
		HttpHost targetHost = getHttpHost(path);

		
		// Set verifier     
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
		
		httpget = new HttpGet("/");
		LOG.info("calling remote {}://{}:{}/ ...", new Object[] { targetHost.getSchemeName(), targetHost.getHostName(), targetHost.getPort() });
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
	
	private HttpHost getHttpHost(String path) {
		int schemeSeperator = path.indexOf(":");
		String scheme = path.substring(0, schemeSeperator);
		
		int portSeperator = path.lastIndexOf(":");
		String hostname = path.substring(schemeSeperator + 3, portSeperator);
		
		int port = Integer.parseInt(path.substring(portSeperator + 1, portSeperator + 5));
		
		HttpHost targetHost = new HttpHost(hostname, port, scheme);
		return targetHost;
	}
	
	protected void initWebListener() {
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class, webListener, null);
	}
	
	protected void initServletListener() {
		servletListener = new ServletListenerImpl();
		bundleContext.registerService(ServletListener.class, servletListener, null);
	}
	
	protected void waitForWebListener() throws InterruptedException {
		new WaitCondition("webapp startup") {
			@Override
			protected boolean isFulfilled() {
				return ((WebListenerImpl)webListener).gotEvent();
			}
		}.waitForCondition();
	}
	
	protected void waitForServletListener() throws InterruptedException {
		new WaitCondition("servlet startup") {
			@Override
			protected boolean isFulfilled() {
				return ((ServletListenerImpl)servletListener).gotEvent();
			}
		}.waitForCondition();
	}
	
	protected void waitForServer(final String path) throws InterruptedException {
		new WaitCondition("server") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return checkServer(path);
			}
		}.waitForCondition();
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