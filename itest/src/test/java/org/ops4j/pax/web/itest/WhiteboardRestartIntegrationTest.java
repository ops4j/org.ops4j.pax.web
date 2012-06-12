package org.ops4j.pax.web.itest;

import java.io.IOException;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(JUnit4TestRunner.class)
public class WhiteboardRestartIntegrationTest extends ITestBase {
	
	private Bundle installWarBundle;
	
    @Inject
    private BundleContext ctx;

	@Configuration
	public static Option[] configure() {
		return baseConfigure();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard/" + getProjectVersion();
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
	public void testWhiteBoardRoot() throws BundleException, InterruptedException, IOException {
		testWebPath("http://127.0.0.1:8181/root", "Hello Whiteboard Extender");
	}
	
	@Test
	public void testWhiteBoardSlash() throws BundleException, InterruptedException, IOException {
		testWebPath("http://127.0.0.1:8181/", "Welcome to the Welcome page");
	}
	
	@Test
	public void testWhiteBoardForbidden() throws BundleException, InterruptedException, IOException {
		testWebPath("http://127.0.0.1:8181/forbidden", "", 401, false);
	}
	
	@Test
	public void testWhiteBoardFiltered() throws Exception {
		testWebPath("http://127.0.0.1:8181/filtered", "Filter was there before");
	}

	@Test
	public void testWhiteBoardRootRestart() throws BundleException, InterruptedException, IOException {

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
