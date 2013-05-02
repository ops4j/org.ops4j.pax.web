package org.ops4j.pax.web.itest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(JUnit4TestRunner.class)
public class HttpServiceIntegrationTest extends ITestBase {

	private Bundle installWarBundle;
	
	private static Logger LOG = LoggerFactory.getLogger(HttpServiceIntegrationTest.class);

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
	public void testNCSALogger() throws Exception {
		testSubPath();
		
		SimpleDateFormat formater = new SimpleDateFormat("yyyy_MM_dd");
		String date = formater.format(new Date());
		
		File logFile = new File("logs/"+date+".request.log");
		
		LOG.info("Log-File: {}", logFile.getAbsoluteFile());
		
		assertNotNull(logFile);
		
		boolean exists = logFile.getAbsoluteFile().exists();
		
		assertTrue(exists);
		
		FileInputStream fstream = new FileInputStream(logFile.getAbsoluteFile());
		DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = br.readLine();
		assertNotNull(strLine);
		in.close();
		fstream.close();
//		while ((strLine = br.readLine()) != null) 	{
//			assertNotNull(strLine);
//		}
	}
	
	@Test
	public void testServletDeRegistration() throws BundleException, ServletException, NamespaceException {
		
		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}
	

}
