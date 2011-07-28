package org.ops4j.pax.web.itest;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.http.NamespaceException;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(JUnit4TestRunner.class)
public class WebConsoleIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return options(
				workingDirectory("target/paxexam/"),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("TRACE"),
				systemProperty("org.osgi.service.http.hostname").value("127.0.0.1"),
				systemProperty("org.osgi.service.http.port").value("8181"),
				systemProperty("java.protocol.handler.pkgs").value("org.ops4j.pax.url"),
				systemProperty("org.ops4j.pax.url.war.importPaxLoggingPackages").value("true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.enabled").value("true"),
				felix(),
				mavenBundle().groupId("org.apache.felix")
					.artifactId("org.apache.felix.bundlerepository").version("1.6.2"),
				mavenBundle().groupId("org.apache.felix")
					.artifactId("org.apache.felix.configadmin").version("1.2.8"),
				mavenBundle().groupId("org.apache.felix")
					.artifactId("org.apache.felix.shell").version("1.4.2"),
				mavenBundle().groupId("org.apache.felix")
					.artifactId("org.apache.felix.shell.tui").version("1.4.1"),
				mavenBundle().groupId("org.apache.felix")
					.artifactId("org.apache.felix.webconsole").version("3.1.6"),
					
				mavenBundle().groupId("org.ops4j.pax.web")
					.artifactId("pax-web-extender-war")
					.version("1.1.1-SNAPSHOT"),
				mavenBundle().groupId("org.ops4j.pax.web")
					.artifactId("pax-web-jetty-bundle").version("1.1.1-SNAPSHOT"),
				mavenBundle().groupId("org.ops4j.pax.web")
					.artifactId("pax-web-jsp").version("1.1.1-SNAPSHOT"),

				mavenBundle().groupId("org.ops4j.pax.logging")
					.artifactId("pax-logging-api").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.logging")
					.artifactId("pax-logging-service")
					.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
					.artifactId("geronimo-servlet_2.5_spec")
					.version(asInProject()),
				//HTTP Client needed for UnitTesting
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

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			System.out.println("Bundle " + b.getBundleId() + " : "
					+ b.getSymbolicName());
		}

	}

	@Ignore
	@Test
	public void testSubPath() throws BundleException, InterruptedException,
			IOException {

		testWebPath("http://127.0.0.1:8181/helloworld/hs", "Hello World");

		// test to retrive Image
		testWebPath("http://127.0.0.1:8181/images/logo.png", "", 200, false);

	}

	@Ignore
	@Test
	public void testRootPath() throws BundleException, InterruptedException,
			IOException {

		testWebPath("http://127.0.0.1:8181/", "");

	}

	@Ignore
	@Test
	public void testServletPath() throws BundleException, InterruptedException,
			IOException {

		testWebPath("http://127.0.0.1:8181/lall/blubb", "Servlet Path: ");
		testWebPath("http://127.0.0.1:8181/lall/blubb",
				"Path Info: /lall/blubb");

	}

	@Ignore
	@Test
	public void testServletDeRegistration() throws BundleException,
			ServletException, NamespaceException {

		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}

}