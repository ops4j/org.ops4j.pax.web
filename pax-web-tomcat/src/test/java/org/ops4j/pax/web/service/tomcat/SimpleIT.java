package org.ops4j.pax.web.service.tomcat;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

/**
 * @author Romaim Gilles
 */
@RunWith(JUnit4TestRunner.class)
public class SimpleIT extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return baseConfigure();
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	// @Ignore
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			System.out.println("Bundle " + b.getBundleId() + " : "
					+ b.getSymbolicName());
		}

	}

	@Test
	public void testServerUp() throws Exception {
		Assert.assertTrue(multiCheckServer(5));
	}
}
