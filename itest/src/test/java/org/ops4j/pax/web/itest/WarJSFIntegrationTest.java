package org.ops4j.pax.web.itest;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarJSFIntegrationTest extends ITestBase {

	// private static final String MYFACES_VERSION = "2.1.0";

	Logger LOG = LoggerFactory.getLogger(WarJSFIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {

		return OptionUtils.combine(configureJetty(),
				mavenBundle().groupId("commons-beanutils")
						.artifactId("commons-beanutils").version(asInProject()),
				mavenBundle().groupId("commons-collections")
						.artifactId("commons-collections")
						.version(asInProject()),
				mavenBundle().groupId("commons-codec")
						.artifactId("commons-codec").version(asInProject()),
				mavenBundle()
						.groupId("org.apache.servicemix.bundles")
						.artifactId(
								"org.apache.servicemix.bundles.commons-digester")
						.version("1.8_4"),
				mavenBundle()
						.groupId("org.apache.servicemix.specs")
						.artifactId(
								"org.apache.servicemix.specs.jsr303-api-1.0.0")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.specs")
						.artifactId("org.apache.servicemix.specs.jsr250-1.0")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.bundles")
						.artifactId("commons-discovery").version("0.4_1"),
				mavenBundle().groupId("org.apache.myfaces.core")
						.artifactId("myfaces-api").version(getMyFacesVersion()),
				mavenBundle().groupId("org.apache.myfaces.core")
						.artifactId("myfaces-impl")
						.version(getMyFacesVersion()));
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			if ("org.apache.myfaces.core.api".equalsIgnoreCase(bundle
					.getSymbolicName())
					|| "org.apache.myfaces.core.impl".equalsIgnoreCase(bundle
							.getSymbolicName())) {
				bundle.stop();
				bundle.start();
			}
		}

		LOG.info("Setting up test");

		initWebListener();
		
		String bundlePath = "mvn:org.ops4j.pax.web.samples/war-jsf/"
				+ getProjectVersion() + "/war";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

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
//	@Test
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE)
				fail("Bundle should be active: " + b);

			Dictionary<?,?> headers = b.getHeaders();
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
		listBundles();
		testWebPath("http://127.0.0.1:8181/war-jsf-sample/",
				"Please enter your name");

	}

//	@Test
	public void testJSF() throws Exception {

		String response = testWebPath("http://127.0.0.1:8181/war-jsf-sample/",
				"Please enter your name");
		
		int indexOf = response.indexOf("id=\"javax.faces.ViewState\" value=");
		String substring = response.substring(indexOf+34);
		indexOf = substring.indexOf("\"");
		substring = substring.substring(0, indexOf);

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("mainForm:name", "Dummy-User"));

		nameValuePairs.add(new BasicNameValuePair("javax.faces.ViewState", substring));
		nameValuePairs.add(new BasicNameValuePair("mainForm:j_id_id20", "Press me"));
		nameValuePairs.add(new BasicNameValuePair("mainForm_SUBMIT", "1"));
		
		testPost("http://127.0.0.1:8181/war-jsf-sample/faces/helloWorld.jsp", nameValuePairs, "Hello Dummy-User. We hope you enjoy Apache MyFaces", 200);

	}

}
