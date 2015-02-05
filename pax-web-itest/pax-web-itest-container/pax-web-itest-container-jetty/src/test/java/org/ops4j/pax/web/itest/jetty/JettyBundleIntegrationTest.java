package org.ops4j.pax.web.itest.jetty;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.OptionUtils.combine;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JettyBundleIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(JettyBundleIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return options(combine(
				baseConfigure(),
				systemPackages("javax.xml.namespace;version=1.0.0", 
						"javax.transaction;version=1.1.0"
	                                    ),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jetty-bundle")
						.version(VersionUtil.getProjectVersion())

		));

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
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

	@Test
	public void testSubPath() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/helloworld/hs", "Hello World");

		// test to retrive Image
		testClient.testWebPath("http://127.0.0.1:8181/images/logo.png", "", 200, false);

	}

	@Test
	public void testRootPath() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/", "");

	}

	@Test
	public void testServletPath() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/lall/blubb", "Servlet Path: ");
		testClient.testWebPath("http://127.0.0.1:8181/lall/blubb",
				"Path Info: /lall/blubb");

	}

	@Test
	public void testServletDeRegistration() throws BundleException,
			ServletException, NamespaceException {

		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}

}
