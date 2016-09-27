package org.ops4j.pax.web.itest.undertow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class JspIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(JspIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return combine(configureUndertow(),
				systemProperty("javax.servlet.context.tempdir").value("target/jsp-compile"));
//		return configureUndertow();
	}


	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-jsp/" + VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		// TODO this is not a war bundle. web listener is never called
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
			if (b.getState() != Bundle.ACTIVE) {
				fail("Bundle should be active: " + b);
			}

			Dictionary<String,String> headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

	}

	@Test
	public void testSimpleJsp() throws Exception {

		testClient.testWebPath("http://localhost:8181/helloworld/jsp/simple.jsp", "<h1>Hello World</h1>");
			
	}

	@Test
	public void testTldJsp() throws Exception {

		testClient.testWebPath("http://localhost:8181/helloworld/jsp/using-tld.jsp", "Hello World");
	}

	@Test
	public void testPrecompiled() throws Exception {
	    testClient.testWebPath("http://localhost:8181/helloworld/jspc/simple.jsp", "<h1>Hello World</h1>");
	    testClient.testWebPath("http://localhost:8181/helloworld/jspc/using-tld.jsp", "Hello World");
	}
}
