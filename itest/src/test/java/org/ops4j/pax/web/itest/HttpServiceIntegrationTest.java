package org.ops4j.pax.web.itest;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.web.samples.helloworld.hs.internal.HelloWorldServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(JUnit4TestRunner.class)
public class HttpServiceIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

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
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			System.out.println("Bundle " + b.getBundleId() + " : "
					+ b.getSymbolicName());
		}

	}

	@Test
	public void testSubPath() throws BundleException,
			InterruptedException, IOException {

		testWebPath("http://127.0.0.1:8181/helloworld/hs", "Hello World");
		
		//test to retrive Image
		testWebPath("http://127.0.0.1:8181/images/logo.png", "", 200, false);
		
	}

	@Test
	public void testRootPath() throws BundleException,
			InterruptedException, IOException {

		testWebPath("http://127.0.0.1:8181/", "");

	}
	
	@Test
	public void testServletPath() throws BundleException,
			InterruptedException, IOException {

		testWebPath("http://127.0.0.1:8181/lall/blubb", "Servlet Path: ");
		testWebPath("http://127.0.0.1:8181/lall/blubb", "Path Info: /lall/blubb");

	}
	
	@Test
	public void testServletDeRegistration() throws BundleException, ServletException, NamespaceException {
		
		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}
	

}
