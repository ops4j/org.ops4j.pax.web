package org.ops4j.pax.web.itest;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
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
public class HttpServiceTCIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + getProjectVersion();
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		while (installWarBundle.getState() != Bundle.ACTIVE) {
			this.wait(100);
		}
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
	@Ignore
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			System.out.println("Bundle " + b.getBundleId() + " : "
					+ b.getSymbolicName());
		}

	}

	@Test
	public void testSubPath() throws Exception {

		testWebPath("http://127.0.0.1:8080/helloworld/hs", "Hello World");
		
		//test to retrive Image
		testWebPath("http://127.0.0.1:8080/images/logo.png", "", 200, false);
		
	}

	@Test
	public void testRootPath() throws Exception {

		testWebPath("http://127.0.0.1:8080/", "");

	}
	
	@Test
	public void testServletPath() throws Exception {

		testWebPath("http://127.0.0.1:8080/lall/blubb", "Servlet Path: ");
		testWebPath("http://127.0.0.1:8080/lall/blubb", "Path Info: /lall/blubb");

	}
	
	@Test
	@Ignore
	public void testServletDeRegistration() throws Exception {
		
		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}
	

}
