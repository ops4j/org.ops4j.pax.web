package org.ops4j.pax.web.itest.undertow;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.itest.base.support.TestActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class SimultaneousWhiteboardIntegrationTest extends ITestBase {
	private static final Logger LOG = LoggerFactory.getLogger(SimultaneousWhiteboardIntegrationTest.class);

	@Configuration
	public static Option[] configure() {
		return combine(
				configureUndertow(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(asInProject()).noStart(),
				streamBundle(
						bundle().add(TestActivator.class)
								.add(WhiteboardFilter.class)
								.set(Constants.BUNDLE_ACTIVATOR, TestActivator.class.getName())
								.set(Constants.BUNDLE_SYMBOLICNAME,
										"org.ops4j.pax.web.itest.SimultaneousTest")
								.set(Constants.DYNAMICIMPORT_PACKAGE, "*")
								.build()).noStart());
	}
	
	@Before
	public void setUp() throws Exception {
		//org.ops4j.pax.web.extender.samples.whiteboard
		
		Bundle whiteBoardBundle = null;
		Bundle simultaneousTestBundle = null;
		
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			String symbolicName = bundle.getSymbolicName();
			if ("org.ops4j.pax.web.extender.samples.whiteboard".equals(symbolicName)) {
				whiteBoardBundle = bundle;
			} else if ("org.ops4j.pax.web.itest.SimultaneousTest".equals(symbolicName)) {
				simultaneousTestBundle = bundle;
			}
		}
		
		assertNotNull(simultaneousTestBundle);
		assertNotNull(whiteBoardBundle);
		
		simultaneousTestBundle.start();
		whiteBoardBundle.start();
		
		//org.ops4j.pax.web.itest.SimultaneousTest
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
			assertTrue(b.getState() == Bundle.ACTIVE);
		}

	}

	@Test
	public void testWhiteBoardRoot() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/root", "Hello Whiteboard Extender");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/", "Welcome to the Welcome page");
	}

}
