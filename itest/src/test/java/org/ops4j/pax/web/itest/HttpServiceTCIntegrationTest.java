package org.ops4j.pax.web.itest;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class HttpServiceTCIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceTCIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws Exception {
		waitForServer("http://127.0.0.1:8282/");
		initServletListener();
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForServletListener();
	}

	@After
	public void tearDown() throws BundleException {
		LOG.info("tear down ... ");
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
		
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle b : bundles) {
//			if (b.getState() != Bundle.ACTIVE)
//				fail("Bundle should be active: " + b);

			Dictionary<?,?> headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null)
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			else
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
		}
		
		LOG.info(" ... good bye ... ");
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
		String path = "http://127.0.0.1:8282/helloworld/hs";
		LOG.info("testSubPath - call path {}", path);
		testWebPath(path, "Hello World");
		
		//test to retrive Image
		path = "http://127.0.0.1:8282/images/logo.png";
		LOG.info("testSubPath - call path {}", path);
		testWebPath(path, "", 200, false);
		
	}

	@Test
	public void testRootPath() throws Exception {

		String path = "http://127.0.0.1:8282/";
		LOG.info("testSubPath - call path {}", path);
		testWebPath(path, "");

	}
	
	@Test
	public void testServletPath() throws Exception {

		testWebPath("http://127.0.0.1:8282/lall/blubb", "Servlet Path: ");
		testWebPath("http://127.0.0.1:8282/lall/blubb", "Path Info: /lall/blubb");

	}
	
	@Test
	public void testServletDeRegistration() throws Exception {
		
		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}
}