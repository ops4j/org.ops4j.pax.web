package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.SystemPackageOption;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Achim Nierbeck
 */
@RunWith(JUnit4TestRunner.class)
public class WarJSFIntegrationTest extends ITestBase {

 Logger LOG = LoggerFactory.getLogger(WarJSFIntegrationTest.class);

	private Bundle installWarBundle;

	private WebListener webListener;
	
	@Configuration
	public static Option[] configureExtra() {
		return options(
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
				.value("DEBUG"),
				systemPackages("javax.activation;version=1.0.0",
							   "javax.validation;version=1.0.0", 
							   "javax.validation.groups;version=1.0.0",
							   "javax.validation.metadata;version=1.0.0",
							   "javax.annotation;version=1.0.0"),
				mavenBundle().groupId("commons-beanutils")
				.artifactId("commons-beanutils").version(asInProject()),
				mavenBundle().groupId("commons-collections")
				.artifactId("commons-collections").version(asInProject()),
				mavenBundle().groupId("commons-codec")
				.artifactId("commons-codec").version(asInProject()),
				mavenBundle().groupId("org.apache.commons")
				.artifactId("com.springsource.org.apache.commons.digester")
				.version("1.8.0"),
				mavenBundle().groupId("org.apache.commons")
				.artifactId("com.springsource.org.apache.commons.discovery")
				.version("0.4.0"),
				mavenBundle().groupId("org.apache.myfaces.core")
				.artifactId("myfaces-api").version(asInProject()),
				mavenBundle().groupId("org.apache.myfaces.core")
				.artifactId("myfaces-impl").version(asInProject())
		);
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			if ("org.apache.myfaces.core.api".equalsIgnoreCase(bundle.getSymbolicName())
				|| "org.apache.myfaces.core.impl".equalsIgnoreCase(bundle.getSymbolicName())) {
				bundle.stop();
				bundle.start();
			}
		}
		
		LOG.info("Setting up test");
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class.getName(), webListener,
				null);
		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-jsf/"
				+ getProjectVersion() + "/war";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		int count = 0;
		while (!((WebListenerImpl) webListener).gotEvent() && count < 50) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
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
			if (b.getState() != Bundle.ACTIVE)
				fail("Bundle should be active: " + b);

			Dictionary headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null)
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			else
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
		}

	}

	@Test
	public void testSlash() throws Exception {

		testWebPath("http://127.0.0.1:8181/war-jsf-sample/", "Please enter your name");

	}
	
	@Test
	public void testJSF() throws Exception {
		
		testWebPath("http://127.0.0.1:8181/war-jsf-sample/", "Please enter your name");

		//TODO improve JSF test
		
	}

	
	private class WebListenerImpl implements WebListener {

		private boolean event = false;

		public void webEvent(WebEvent event) {
			LOG.info("Got event: " + event);
			if (event.getType() == 2)
				this.event = true;
		}

		public boolean gotEvent() {
			return event;
		}

	}

}
