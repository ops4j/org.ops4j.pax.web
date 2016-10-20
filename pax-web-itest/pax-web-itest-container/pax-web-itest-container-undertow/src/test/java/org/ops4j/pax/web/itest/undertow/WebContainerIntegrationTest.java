package org.ops4j.pax.web.itest.undertow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.util.Dictionary;

import static org.junit.Assert.fail;

import javax.servlet.ServletContext;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebContainerIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureUndertow();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		final String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
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
		for (final Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE) {
				fail("Bundle should be active: " + b);
			}

			final Dictionary<String, String> headers = b.getHeaders();
			final String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
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
	public void testWebContextPath() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/helloworld/wc",
				"<h1>Hello World</h1>");

	}

	@Test
	public void testFilterInitWebContextPath() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/helloworld/wc", "Have bundle context in filter: true");
	}


	/**
	 * The server-container must register each ServletContext as an OSGi service
	 */
	@Test
	public void testServletContextRegistration() throws Exception {
		// It's necessary to execute a request, because there might be currently no Undertow-RequestHandler
		// (which is tied to the availability of the ServletContext)
		testClient.testWebPath("http://127.0.0.1:8181/helloworld/wc", "Have bundle context in filter: true");

		String filter = String.format("(%s=%s)",
				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");

		if(bundleContext.getServiceReferences(ServletContext.class, filter).size() == 0){
			fail("ServletContext was not registered as Service.");
		}
	}


	/**
	 * The server-container must unregister a ServletContext if the ServletContext got destroyed
	 */
	@Test
	public void testServletContextUnregistration() throws Exception {
		// It's necessary to execute a request, because there might be currently no Undertow-RequestHandler
		// (which is tied to the availability of the ServletContext)
		testClient.testWebPath("http://127.0.0.1:8181/helloworld/wc", "Have bundle context in filter: true");

		installWarBundle.stop();
		String filter = String.format("(%s=%s)",
				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");

		if(bundleContext.getServiceReferences(ServletContext.class, filter).size() > 0){
			fail("ServletContext was not unregistered.");
		}
	}

}
