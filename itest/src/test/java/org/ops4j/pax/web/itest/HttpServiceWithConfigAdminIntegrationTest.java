package org.ops4j.pax.web.itest;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class HttpServiceWithConfigAdminIntegrationTest extends ITestBase {

	private Bundle installWarBundle;
	
	@Inject
	private ConfigurationAdmin caService;
	

	@Configuration
	public static Option[] configure() {
		return combine(configureJetty(),
	            mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.4.0"));
	}

	@Before
	public void setUp() throws BundleException, InterruptedException, IOException {

		org.osgi.service.cm.Configuration config = caService.getConfiguration(WebContainerConstants.PID);
        @SuppressWarnings("rawtypes")
		Dictionary props = new Properties();

        props.put(WebContainerConstants.PROPERTY_LISTENING_ADDRESSES,"127.0.0.1");
		props.put(WebContainerConstants.PROPERTY_HTTP_PORT,"8181");
        
		config.setBundleLocation(null);
        config.update(props);
		
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + getProjectVersion();
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
	public void testSubPath() throws Exception {

		testWebPath("http://127.0.0.1:8181/helloworld/hs", "Hello World");
		
		//test to retrive Image
		testWebPath("http://127.0.0.1:8181/images/logo.png", "", 200, false);
		
	}

	@Test
	public void testRootPath() throws Exception {

		testWebPath("http://127.0.0.1:8181/", "");

	}
	
	@Test
	public void testServletPath() throws Exception {

		testWebPath("http://127.0.0.1:8181/lall/blubb", "Servlet Path: ");
		testWebPath("http://127.0.0.1:8181/lall/blubb", "Path Info: /lall/blubb");

	}
	
	@Test
	public void testServletDeRegistration() throws Exception {
		
		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}
	

}
