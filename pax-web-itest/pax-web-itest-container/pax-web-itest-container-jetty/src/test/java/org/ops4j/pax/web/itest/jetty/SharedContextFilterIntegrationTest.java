package org.ops4j.pax.web.itest.jetty;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.support.FilterBundleActivator;
import org.ops4j.pax.web.itest.base.support.ServletBundleActivator;
import org.ops4j.pax.web.itest.base.support.SimpleOnlyFilter;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
@RunWith(PaxExam.class)
public class SharedContextFilterIntegrationTest extends ITestBase {

	private static final String SERVLET_BUNDLE = "ServletBundleTest";
	private static final String FILTER_BUNDLE = "FilterBundleTest";

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				streamBundle(bundle()
						.add(TestServlet.class)
						.add(ServletBundleActivator.class)
						.set(Constants.BUNDLE_SYMBOLICNAME, SERVLET_BUNDLE)
						.set(Constants.BUNDLE_ACTIVATOR,
								ServletBundleActivator.class.getName())
						.set(Constants.DYNAMICIMPORT_PACKAGE, "*").build()),
				streamBundle(bundle()
						.add(SimpleOnlyFilter.class)
						.add(FilterBundleActivator.class)
						.set(Constants.BUNDLE_SYMBOLICNAME, FILTER_BUNDLE)
						.set(Constants.BUNDLE_ACTIVATOR,
								FilterBundleActivator.class.getName())
						.set(Constants.DYNAMICIMPORT_PACKAGE, "*").build()));
	}

	@Before
	public void setUp() throws Exception {
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

		testClient.testWebPath("http://127.0.0.1:8181/sharedContext/", "Hello Whiteboard Filter");

	}
	
	@Test
	public void testStop() throws Exception {
		for (final Bundle b : bundleContext.getBundles()) {
			if (FILTER_BUNDLE.equalsIgnoreCase(b.getSymbolicName())) {
				b.stop();
			}
		}
		
		testClient.testWebPath("http://127.0.0.1:8181/sharedContext/", "SimpleServlet: TEST OK");
	}
}
