package org.ops4j.pax.web.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.CoreOptions.configProfile;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import javax.inject.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

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
				configProfile(),
				compendiumProfile(),
				junitBundles(),
				frameworkProperty("osgi.console").value("6666"),
				frameworkProperty("felix.bootdelegation.implicit").value(
						"false"),
				// frameworkProperty("felix.log.level").value("4"),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
						.value("DEBUG"),
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

				// do not include pax-logging-api, this is already provisioned
				// by Pax Exam
				mavenBundle().groupId("org.ops4j.pax.logging")
						.artifactId("pax-logging-service")
						.version(asInProject()),

				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-war").version(asInProject()),
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
	
//	@AfterClass
//	public void shutdown() throws Exception {
//		Bundle bundle = bundleContext.getBundle(16);
//		bundle.stop();
//	}
}
