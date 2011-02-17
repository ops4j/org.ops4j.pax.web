package org.ops4j.pax.web.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.compendiumProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.configProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.logProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
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
					logProfile(),
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
							.artifactId("pax-web-runtime")
							.version(asInProject()),
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
					wrappedBundle(mavenBundle("commons-httpclient",
							"commons-httpclient", "3.1"))
//	 enable for debugging
					,
					vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
					waitForFrameworkStartup()
	
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
	protected void testWebPath(String path, String expectedContent) throws IOException, HttpException {
		testWebPath(path, expectedContent, 200);
	}
	
	protected void testWebPath(String path, String expectedContent, int httpRC) throws IOException, HttpException {
		GetMethod get = null;
		try {
			HttpClient client = new HttpClient();
			get = new GetMethod(path);
			int executeMethod = client.executeMethod(get);
			assertEquals("HttpResponseCode", httpRC, executeMethod);
			String responseBodyAsString = get.getResponseBodyAsString();
			assertTrue(responseBodyAsString.contains(expectedContent));
		} finally {
			if (get != null)
				get.releaseConnection();
		}
	}



}