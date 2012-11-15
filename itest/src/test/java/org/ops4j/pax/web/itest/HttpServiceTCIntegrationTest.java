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
	public void setUp() throws Exception {
		
		int count = 0;
		while (!checkServer("http://127.0.0.1:8282/") && count < 100) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		
		LOG.info("waiting for Server took {} ms", (count * 1000));
		
		initServletListener();
		
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + getProjectVersion();
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		while (installWarBundle.getState() != Bundle.ACTIVE) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		
		waitForServletListener();
	}

	@After
	public void tearDown() throws BundleException {
		LOG.info("tear down ... ");
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
		LOG.info(" ... good bye ... ");
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
		String path = "http://127.0.0.1:8282/helloworld/hs";
		LOG.info("testSubPath - call path {}", path);
		testWebPath(path, "Hello World");
		
		//test to retrive Image
		path = "http://127.0.0.1:8282/images/logo.png";
		LOG.info("testSubPath - call path {}", path);
		testWebPath(path, "", 200, false);
		
	}

	@Test
	@Ignore
	public void testRootPath() throws Exception {

		String path = "http://127.0.0.1:8282/";
		LOG.info("testSubPath - call path {}", path);
		testWebPath(path, "");

	}
	
	@Test
	@Ignore
	public void testServletPath() throws Exception {

		testWebPath("http://127.0.0.1:8282/lall/blubb", "Servlet Path: ");
		testWebPath("http://127.0.0.1:8282/lall/blubb", "Path Info: /lall/blubb");

	}
	
	@Test
	@Ignore
	public void testServletDeRegistration() throws Exception {
		
		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}
	

}
