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
import static org.ops4j.pax.exam.OptionUtils.combine;

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

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
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
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.web.itest.support.ServletListenerImpl;
import org.ops4j.pax.web.itest.support.WaitCondition;
import org.ops4j.pax.web.itest.support.WebListenerImpl;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExamReactorStrategy(PerClass.class)
public class ITestBase {

	protected static final String WEB_CONTEXT_PATH = "Web-ContextPath";
	protected static final String WEB_CONNECTORS = "Web-Connectors";
	protected static final String WEB_VIRTUAL_HOSTS = "Web-VirtualHosts";
	protected static final String WEB_BUNDLE = "webbundle:";

	protected static final String REALM_NAME = "realm.properties";

	private static final Logger LOG = LoggerFactory.getLogger(ITestBase.class);
	
	@Inject
	protected BundleContext bundleContext;
	
	protected DefaultHttpClient httpclient;

	protected WebListener webListener;

	protected ServletListener servletListener;

	public static Option[] baseConfigure() {
		return options(
				workingDirectory("target/paxexam/"),
				cleanCaches(true),
				junitBundles(),
				frameworkProperty("osgi.console").value("6666"),
				frameworkProperty("osgi.console.enable.builtin").value("true"),
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
				systemProperty("org.ops4j.pax.web.log.ncsa.directory").value(
						"target/logs"),
				systemProperty("ProjectVersion").value(getProjectVersion()),

				// javax.servlet may be on the system classpath so we need to
				// make sure
				// that all bundles load it from there
				systemPackages("javax.servlet;version=2.6.0",
						"javax.servlet;version=3.0.0"),

//				mavenBundle().groupId("org.apache.felix")
//						.artifactId("org.apache.felix.framework.security")
//						.version("2.0.1"),
						
				// do not include pax-logging-api, this is already provisioned
				// by Pax Exam
				mavenBundle().groupId("org.ops4j.pax.logging")
						.artifactId("pax-logging-service").version("1.6.4"),

				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-war").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-wrap").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-commons").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.swissbox")
						.artifactId("pax-swissbox-bnd").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.swissbox")
						.artifactId("pax-swissbox-property")
						.version(asInProject()),
				mavenBundle().groupId("biz.aQute").artifactId("bndlib")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.swissbox")
						.artifactId("pax-swissbox-optional-jcl")
						.version(asInProject()),
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
						.artifactId("pax-web-runtime").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jsp").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jdt.core.compiler")
						.artifactId("ecj").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-servlet_3.0_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-aether").version(asInProject()),
				mavenBundle().groupId("org.apache.xbean")
						.artifactId("xbean-finder").version(asInProject()),
				mavenBundle().groupId("org.apache.xbean")
						.artifactId("xbean-bundleutils").version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.bundles")
						.artifactId("org.apache.servicemix.bundles.asm").version(asInProject()),
				mavenBundle("commons-codec", "commons-codec").version(
						asInProject()),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpclient", "4.1")),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpcore", "4.1")));
	}

	public static Option[] configureJetty() {
		return combine(
				baseConfigure(),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jetty").version(asInProject()),
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
						.artifactId("jetty-servlet").version(asInProject()));
	}

	public static Option[] configureTomcat() {
		return combine(
				baseConfigure(),
				systemPackages("javax.xml.namespace;version=1.0.0", 
				    "javax.transaction;version=1.1.0", 
                                    "javax.servlet;version=2.6.0",
                                    "javax.servlet;version=3.0.0",
                                    "javax.servlet.descriptor;version=2.6.0",
                                    "javax.servlet.descriptor;version=3.0.0",
                                    "javax.annotation.processing;uses:=javax.tools,javax.lang.model,javax.lang.model.element,javax.lang.model.util;version=1.1", 
                                    "javax.annotation;version=1.1",
                                    "javax.annotation.security;version=1.1"
                                    ),
				systemProperty("org.osgi.service.http.hostname").value("127.0.0.1"),
				systemProperty("org.osgi.service.http.port").value("8282"),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-tomcat").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.ext.tomcat")
						.artifactId("catalina").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.ext.tomcat")
						.artifactId("shared").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.ext.tomcat")
						.artifactId("util").version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.specs")
						.artifactId("org.apache.servicemix.specs.saaj-api-1.3")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.specs")
						.artifactId("org.apache.servicemix.specs.jaxb-api-2.2")
						.version(asInProject()),

				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-jaxws_2.2_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-jaxrpc_1.1_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-servlet_3.0_spec")
						.version(asInProject()),

				mavenBundle()
						.groupId("org.apache.servicemix.specs")
						.artifactId(
								"org.apache.servicemix.specs.jsr303-api-1.0.0")
						.version(asInProject()),

//				mavenBundle().groupId("org.apache.geronimo.specs")
//						.artifactId("geronimo-annotation_1.1_spec")
//						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-activation_1.1_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-stax-api_1.2_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-ejb_3.1_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-jpa_2.0_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-javamail_1.4_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-osgi-registry")
						.version(asInProject()));
	}

	@Before
	public void setUpITestBase() throws Exception {
		httpclient = new DefaultHttpClient();
	}

	@After
	public void tearDownITestBase() throws Exception {
		httpclient.clearRequestInterceptors();
		httpclient.clearResponseInterceptors();
		httpclient = null;
	}

	public static String getProjectVersion() {
		String projectVersion = System.getProperty("ProjectVersion");
		LOG.info("*** The ProjectVersion is {} ***", projectVersion);
		return projectVersion;
	}

	public static String getMyFacesVersion() {
		String myFacesVersion = System.getProperty("MyFacesVersion");
		System.out.println("*** The MyFacesVersion is " + myFacesVersion
				+ " ***");
		return myFacesVersion;
	}

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
			assertTrue("Content: " + responseBodyAsString,responseBodyAsString.contains(expectedContent));
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

	protected void testPost(String path, List<NameValuePair> nameValuePairs,
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
			assertTrue(responseBodyAsString.contains(expectedContent));
		}
	}

	protected HttpResponse getHttpResponse(String path, boolean authenticate,
			BasicHttpContext basicHttpContext) throws IOException,
			KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
		HttpGet httpget = null;
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
		
		KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream instream = new FileInputStream(new File("src/test/resources/keystore"));
        try {
            trustStore.load(instream, "password".toCharArray());
        } finally {
            try { instream.close(); } catch (Exception ignore) {}//CHECKSTYLE:SKIP
        }

        SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore);
        Scheme sch = new Scheme("https", 443, socketFactory);
        httpclient.getConnectionManager().getSchemeRegistry().register(sch);
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		
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
		LOG.info("calling remote {} ...", path);
		HttpResponse response = null;
		if (!authenticate && basicHttpContext == null) {
			response = httpclient.execute(httpget);
		} else {
			response = httpclient.execute(targetHost, httpget, localcontext);
		}
		LOG.info("... responded with: {}", response.getStatusLine().getStatusCode());
		return response;
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

	protected boolean checkServer(String path) throws Exception {
		LOG.info("checking server path {}", path);
		HttpGet httpget = null;
		HttpClient myHttpClient = new DefaultHttpClient();
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
		
		KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream instream = new FileInputStream(new File("src/test/resources/keystore"));
        try {
            trustStore.load(instream, "password".toCharArray());
        } finally {
            try { instream.close(); } catch (Exception ignore) {}//CHECKSTYLE:SKIP
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
		}.waitForCondition(); //CHECKSTYLE:SKIP
	}
	
	protected void waitForServletListener() throws InterruptedException {
		new WaitCondition("servlet startup") {
			@Override
			protected boolean isFulfilled() {
				return ((ServletListenerImpl)servletListener).gotEvent();
			}
		}.waitForCondition(); //CHECKSTYLE:SKIP
	}
	
	protected void waitForServer(final String path) throws InterruptedException {
		new WaitCondition("server") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return checkServer(path);
			}
		}.waitForCondition(); //CHECKSTYLE:SKIP
	}
	
	protected Bundle installAndStartBundle(String bundlePath) throws BundleException, InterruptedException {
		final Bundle bundle = bundleContext.installBundle(bundlePath);
		bundle.start();
		new WaitCondition("bundle startup") {
			@Override
			protected boolean isFulfilled() {
				return bundle.getState() == Bundle.ACTIVE;
			}
		}.waitForCondition(); //CHECKSTYLE:SKIP
		return bundle;
	}
}