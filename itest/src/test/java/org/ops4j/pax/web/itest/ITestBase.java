package org.ops4j.pax.web.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.compendiumProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.configProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.BundleContext;

public class ITestBase {

	@Inject
	protected BundleContext bundleContext = null;

	protected static final String WEB_CONTEXT_PATH = "Web-ContextPath";
	protected static final String WEB_BUNDLE = "webbundle:";

	@Configuration
	public static Option[] configure() {
		return options(
				workingDirectory("target/paxexam/"),
				configProfile(),
				compendiumProfile(),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
						.value("DEBUG"),
				systemProperty("org.osgi.service.webcontainer.hostname").value(
						"127.0.0.1"),
				systemProperty("org.osgi.service.webcontainer.http.port")
						.value("8080"),
				systemProperty("java.protocol.handler.pkgs").value(
						"org.ops4j.pax.url"),
				systemProperty("org.ops4j.pax.url.war.importPaxLoggingPackages")
						.value("true"),
				mavenBundle().groupId("org.ops4j.pax.logging")
						.artifactId("pax-logging-api").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.logging")
						.artifactId("pax-logging-service")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-war").version(asInProject()),
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
						.artifactId("geronimo-servlet_2.5_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-mvn").version(asInProject()),
				mavenBundle("commons-codec", "commons-codec"),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpclient", "4.1")),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
								"httpcore", "4.1"))
				// enable for debugging
//				,
//				vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
//				waitForFrameworkStartup()

		);
	}

	public ITestBase() {
		super();
	}

	/**
	 * @return
	 * @throws IOException
	 * @throws HttpException
	 */
	protected void testWebPath(String path,
			String expectedContent) throws IOException {
		testWebPath(path, expectedContent, 200, false);
	}

	protected void testWebPath(String path, String expectedContent, int httpRC,
			boolean authenticate) throws IOException {
		HttpGet httpget = null;
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpHost targetHost = new HttpHost("localhost", 8080, "http"); 
		BasicHttpContext localcontext = new BasicHttpContext();
		if (authenticate) {


			((DefaultHttpClient) httpclient).getCredentialsProvider().setCredentials(
					new AuthScope(targetHost.getHostName(), targetHost.getPort()),
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
		if (!authenticate)
			response = httpclient.execute(httpget);
		else
			response = httpclient.execute(targetHost, httpget, localcontext);

		

		assertEquals("HttpResponseCode", httpRC, response
				.getStatusLine().getStatusCode());

		String responseBodyAsString = EntityUtils
				.toString(response.getEntity());
		assertTrue(responseBodyAsString.contains(expectedContent));
	}

}