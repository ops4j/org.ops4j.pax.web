package org.ops4j.pax.web.itest;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardRestartIntegrationTest extends ITestBase {
	
	private Bundle installWarBundle;
	
    @Inject
    private BundleContext ctx;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard/" + getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
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
	public void testWhiteBoardRoot() throws Exception {
		testWebPath("http://127.0.0.1:8181/root", "Hello Whiteboard Extender");
	}
	
	@Test
	public void testWhiteBoardSlash() throws Exception {
		testWebPath("http://127.0.0.1:8181/", "Welcome to the Welcome page");
	}
	
	@Test
	public void testWhiteBoardForbidden() throws Exception {
		testWebPath("http://127.0.0.1:8181/forbidden", "", 401, false);
	}
	
	@Test
	public void testWhiteBoardFiltered() throws Exception {
		testWebPath("http://127.0.0.1:8181/filtered", "Filter was there before");
	}

	@Test
	public void testWhiteBoardRootRestart() throws Exception {

		Bundle whiteBoardBundle = null;
		
		for (Bundle bundle : ctx.getBundles()) {
			String symbolicName = bundle.getSymbolicName();
			if ("org.ops4j.pax.web.pax-web-extender-whiteboard".equalsIgnoreCase(symbolicName)) {
				whiteBoardBundle = bundle;
				break;
			}
		}
		
		if(whiteBoardBundle == null)
			Assert.fail("no Whiteboard Bundle found");
		
		whiteBoardBundle.stop();
		
		Thread.sleep(2500);//workaround for buildserver issue
		
		int maxCount = 500;
		while(whiteBoardBundle.getState() != Bundle.RESOLVED && maxCount > 0) {
			Thread.sleep(500);
			maxCount--;
		}
		if (maxCount == 0)
			Assert.fail("maxcount reached, Whiteboard bundle never reached ACTIVE state again!");
		
		whiteBoardBundle.start();
		while(whiteBoardBundle.getState() != Bundle.ACTIVE && maxCount > 0) {
			Thread.sleep(500);
			maxCount--;
		}
		if (maxCount == 0)
			Assert.fail("maxcount reached, Whiteboard bundle never reached ACTIVE state again!");
		
		testWebPath("http://127.0.0.1:8181/root", "Hello Whiteboard Extender");
	}
}
