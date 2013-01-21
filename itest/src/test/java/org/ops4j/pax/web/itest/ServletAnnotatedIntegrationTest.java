package org.ops4j.pax.web.itest;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.support.AnnotatedTestServlet;
import org.ops4j.pax.web.itest.support.Bundle1Activator;
import org.ops4j.pax.web.itest.support.Bundle1Filter;
import org.ops4j.pax.web.itest.support.Bundle1Servlet;
import org.ops4j.pax.web.itest.support.Bundle1SharedFilter;
import org.ops4j.pax.web.itest.support.Bundle2Activator;
import org.ops4j.pax.web.itest.support.Bundle2SharedFilter;
import org.ops4j.pax.web.itest.support.Bundle2SharedServlet;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;


/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
@RunWith(PaxExam.class)
public class ServletAnnotatedIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return combine(configureJetty(), 
				streamBundle(bundle()
		                .add(AnnotatedTestServlet.class)
		                .set(Constants.BUNDLE_SYMBOLICNAME, "AnnotatedServletTest")
		                .set(WebContainerConstants.CONTEXT_PATH_KEY, "/annotatedTest")
		                .set(Constants.IMPORT_PACKAGE, "javax.servlet")
		                .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
		                .build()));
	}

	@Before
	public void setUp() throws 	Exception {
		waitForServer("http://127.0.0.1:8181/");
	}

	@After
	public void tearDown() throws BundleException {
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
	public void testBundle1() throws Exception {

		testWebPath("http://127.0.0.1:8181/annotatedTest/test", "TEST OK");
		
	}
}
