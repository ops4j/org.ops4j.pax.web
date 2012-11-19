package org.ops4j.pax.web.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

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

import javax.inject.Inject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;

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
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class ITestBase {

	@Inject
	protected BundleContext bundleContext;

	protected static final String WEB_CONTEXT_PATH = "Web-ContextPath";
	protected static final String WEB_CONNECTORS = "Web-Connectors";
	protected static final String WEB_VIRTUAL_HOSTS = "Web-VirtualHosts";
	protected static final String WEB_BUNDLE = "webbundle:";

	protected static final String REALM_NAME = "realm.properties";

	protected DefaultHttpClient httpclient;

	public static Option[] baseConfigure() {
		return options(
				workingDirectory("target/paxexam/"),
				cleanCaches(true),
				junitBundles(),
				frameworkProperty("osgi.console").value("6666"),
				frameworkProperty("felix.bootdelegation.implicit").value(
						"false"),
				// frameworkProperty("felix.log.level").value("4"),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
						.value("INFO"),
				systemProperty("org.osgi.service.http.hostname").value(
						"127.0.0.1"),
				systemProperty("org.osgi.service.http.port").value("8181"),
				systemProperty("java.protocol.handler.pkgs").value(
						"org.ops4j.pax.url"),
				systemProperty("org.ops4j.pax.url.war.importPaxLoggingPackages")
						.value("true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.enabled").value(
						"true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.directory").value("target/logs"),
                systemProperty("ProjectVersion").value(getProjectVersion()),
                
                // javax.servlet may be on the system classpath so we need to make sure
                // that all bundles load it from there
                systemPackages("javax.servlet;version=2.6.0", "javax.servlet;version=3.0.0"),
                
				// do not include pax-logging-api, this is already provisioned
				// by Pax Exam
				mavenBundle().groupId("org.ops4j.pax.logging")
						.artifactId("pax-logging-service")
						.version("1.6.4"),

		        mavenBundle().groupId("org.ops4j.pax.url")
                        .artifactId("pax-url-war").version(asInProject()),
                mavenBundle().groupId("org.ops4j.pax.url")
                        .artifactId("pax-url-wrap").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-commons").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.swissbox")
						.artifactId("pax-swissbox-bnd").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.swissbox")
						.artifactId("pax-swissbox-property").version(asInProject()),
				mavenBundle().groupId("biz.aQute")
						.artifactId("bndlib").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-spi").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-api").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-extender-war")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-extender-whiteboard")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jetty").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-runtime").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jsp").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jdt.core.compiler")
						.artifactId("ecj").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-util").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-io").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-http").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-continuation")
						.version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-server").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-security").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-xml").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-servlet").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-servlet_3.0_spec").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-aether").version(asInProject()),
				mavenBundle("commons-codec", "commons-codec").version(asInProject()),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpclient", "4.1")),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpcore", "4.1"))
		);
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
	
	protected static String getProjectVersion() {
		String projectVersion = System.getProperty("ProjectVersion");
		System.out.println("*** The ProjectVersion is " + projectVersion
				+ " ***");
		return projectVersion;
	}

	protected static String getMyFacesVersion() {
		String myFacesVersion = System.getProperty("MyFacesVersion");
		System.out.println("*** The MyFacesVersion is " + myFacesVersion
				+ " ***");
		return myFacesVersion;
	}

	/**
	 * @return 
	 * @return
	 * @throws IOException
	 * @throws CertificateException 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws UnrecoverableKeyException 
	 * @throws KeyManagementException 
	 * @throws HttpException
	 */
	protected String testWebPath(String path, String expectedContent)
			throws IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
		return testWebPath(path, expectedContent, 200, false);
	}
	
	protected String testSecureWebPath(String path, String expectedContent) throws IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
		return testSecureWebPath(path, expectedContent, 200, false);
	}
	
	protected String testWebPath(String path, int httpRC)
			throws IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
		return testWebPath(path, null, httpRC, false);
	}

	protected String testSecureWebPath(String path, int httpRC)
			throws IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
		return testSecureWebPath(path, null, httpRC, false);
	}
	
	protected String testWebPath(String path, String expectedContent, int httpRC,
			boolean authenticate) throws IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
		return testWebPath(path, expectedContent, httpRC, authenticate, null, false);
	}
	
	protected String testSecureWebPath(String path, String expectedContent, int httpRC,
			boolean authenticate) throws IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
		return testWebPath(path, expectedContent, httpRC, authenticate, null, true);
	}

	protected String testWebPath(String path, String expectedContent, int httpRC,
			boolean authenticate, BasicHttpContext basicHttpContext, boolean securedConnection)
			throws ClientProtocolException, IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {

        int count=0;
        while(!checkServer() && count++<5) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw (IOException) new InterruptedIOException().initCause(e);
            }
            if (count > 5)
                break;
        }

		HttpResponse response = null;
		if (!securedConnection) {
			response = getHttpResponse(path, authenticate, basicHttpContext);
		} else {
			response = getHttpSecureResponse(path, authenticate, basicHttpContext);
		}

		assertEquals("HttpResponseCode", httpRC, response.getStatusLine()
				.getStatusCode());

		String responseBodyAsString = null;
		if (expectedContent != null) {
			responseBodyAsString = EntityUtils
				.toString(response.getEntity());
			assertTrue(responseBodyAsString.contains(expectedContent));
		}

		return responseBodyAsString;
	}
	
	protected void testPost(String path, List<NameValuePair> nameValuePairs, String expectedContent, int httpRC) throws ClientProtocolException, IOException {
		
		
		HttpPost post = new HttpPost(path);
		post.setEntity(new UrlEncodedFormEntity((List<NameValuePair>) nameValuePairs));
		
		
		HttpResponse response = httpclient.execute(post);
		assertEquals("HttpResponseCode", httpRC, response.getStatusLine()
				.getStatusCode());

		if (expectedContent != null) {
			String responseBodyAsString = EntityUtils
				.toString(response.getEntity());
			assertTrue(responseBodyAsString.contains(expectedContent));
		}
	}

	/**
	 * 
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
	
	protected HttpResponse getHttpSecureResponse(String path, boolean authenticate,
			BasicHttpContext basicHttpContext) throws IOException,
			ClientProtocolException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
		HttpGet httpget = null;
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
		
		KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream instream = new FileInputStream(new File("src/test/resources/keystore"));
        try {
            trustStore.load(instream, "password".toCharArray());
        } finally {
            try { instream.close(); } catch (Exception ignore) {}
        }

        SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
        Scheme sch = new Scheme("https", 443, socketFactory);
        httpclient.getConnectionManager().getSchemeRegistry().register(sch);
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		
		HttpHost targetHost = new HttpHost("localhost", 8443, "https");

		
		// Set verifier     
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

		
		
		
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

	protected boolean checkServer() throws InterruptedIOException {
		ServiceTracker tracker = new ServiceTracker(bundleContext, HttpService.class.getName(), null);
		tracker.open();
		try {
			Object svc = tracker.waitForService(5000);
			return svc != null;
		} catch (InterruptedException e) {
			return false;
		} finally {
			tracker.close();
		}
	}

//	@AfterClass
//	public void shutdown() throws Exception {
//		Bundle bundle = bundleContext.getBundle(16);
//		bundle.stop();
//	}
}
