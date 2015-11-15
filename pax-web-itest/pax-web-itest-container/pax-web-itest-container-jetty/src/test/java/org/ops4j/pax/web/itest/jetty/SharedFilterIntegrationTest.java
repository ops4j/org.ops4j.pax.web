package org.ops4j.pax.web.itest.jetty;

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
import org.ops4j.pax.web.itest.base.support.Bundle1Activator;
import org.ops4j.pax.web.itest.base.support.Bundle1Filter;
import org.ops4j.pax.web.itest.base.support.Bundle1Servlet;
import org.ops4j.pax.web.itest.base.support.Bundle1SharedFilter;
import org.ops4j.pax.web.itest.base.support.Bundle2Activator;
import org.ops4j.pax.web.itest.base.support.Bundle2SharedFilter;
import org.ops4j.pax.web.itest.base.support.Bundle2SharedServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;


/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
@RunWith(PaxExam.class)
public class SharedFilterIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return combine(configureJetty(), 
				streamBundle(bundle()
		                .add(Bundle1Servlet.class)
		                .add(Bundle1Filter.class)
		                .add(Bundle1SharedFilter.class)
//		                .add(SharedContext.class)
		                .add(Bundle1Activator.class)
		                .set(Constants.BUNDLE_SYMBOLICNAME, "BundleTest1")
		                .set(Constants.BUNDLE_ACTIVATOR, Bundle1Activator.class.getName())
		                .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
		                .build()),
		         streamBundle(bundle()
		        		 .add(Bundle2SharedServlet.class)
		        		 .add(Bundle2SharedFilter.class)
		        		 .add(Bundle2Activator.class)
		        		 .set(Constants.BUNDLE_SYMBOLICNAME, "BundleTest2")
		        		 .set(Constants.BUNDLE_ACTIVATOR, Bundle2Activator.class.getName())
		        		 .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
		        		 .build()));
	}

	@Before
	public void setUp() throws 	Exception {
		waitForServer(retrieveBaseUrl()+"/");
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

		testClient.testWebPath(retrieveBaseUrl()+"/bundle1/", "Welcome to Bundle1");
		testClient.testWebPath(retrieveBaseUrl()+"/bundle2/", null, 404, false);
		
	}
}
