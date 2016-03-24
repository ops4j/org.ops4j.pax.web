/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package org.ops4j.pax.web.itest.jetty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.CookieState;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarJSFPrimefacesIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WarJSFPrimefacesIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {

		return OptionUtils
				.combine(
						configureJetty(),
						mavenBundle().groupId("commons-beanutils")
								.artifactId("commons-beanutils")
								.version(asInProject()),
						mavenBundle().groupId("commons-collections")
								.artifactId("commons-collections")
								.version(asInProject()),
						mavenBundle().groupId("commons-codec")
								.artifactId("commons-codec")
								.version(asInProject()),
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
						mavenBundle()
								.groupId("org.apache.servicemix.specs")
								.artifactId(
										"org.apache.servicemix.specs.jsr250-1.0")
								.version(asInProject()),
						mavenBundle().groupId("org.apache.geronimo.bundles")
								.artifactId("commons-discovery")
								.version("0.4_1"),

						mavenBundle().groupId("javax.enterprise")
								.artifactId("cdi-api").versionAsInProject(),
						mavenBundle().groupId("javax.interceptor")
								.artifactId("javax.interceptor-api")
								.versionAsInProject(),

						mavenBundle().groupId("org.apache.myfaces.core")
								.artifactId("myfaces-api")
								.version(VersionUtil.getMyFacesVersion()),
						mavenBundle().groupId("org.apache.myfaces.core")
								.artifactId("myfaces-impl")
								.version(VersionUtil.getMyFacesVersion()),
						mavenBundle().groupId("org.primefaces")
								.artifactId("primefaces")
								.version(asInProject()));
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		final Bundle[] bundles = bundleContext.getBundles();
		for (final Bundle bundle : bundles) {
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

		final String bundlePath = "mvn:org.ops4j.pax.web.samples/war-jsf-primefaces/"
				+ VersionUtil.getProjectVersion() + "/war";
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


	@Test
	public void testSlash() throws Exception {
		// needed to wait for fully initializing the container
		Thread.sleep(1000);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-primefaces-sample/");

//		testClient.testWebPath("http://127.0.0.1:8181/war-jsf-primefaces-sample/",
//				"Please enter your name");
	}

	public void testJSF() throws Exception {
		// needed to wait for fully initializing the container
		Thread.sleep(1000);

		// Session must be kept during test-requests
		CookieState cookieState = new CookieState();

		final String response = HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-primefaces-sample/");

//		final String response = testClient.testWebPath(
//				"http://127.0.0.1:8181/war-jsf-primefaces-sample/",
//				"Please enter your name");

		int indexOf = response.indexOf("id=\"javax.faces.ViewState\" value=");
		String substring = response.substring(indexOf + 34);
		indexOf = substring.indexOf("\"");
		substring = substring.substring(0, indexOf);

		HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response must contain 'Hello Dummy-User. We hope you enjoy Apache MyFaces'",
						resp -> resp.contains("Hello Dummy-User. We hope you enjoy Apache MyFaces"))
				.doPOST("http://127.0.0.1:8181/war-jsf-sample")
				.addParameter("mainForm:name", "Dummy-User")
				.addParameter("mainForm:j_id_a", "Press me")
				.addParameter("javax.faces.ViewState", substring)
				.addParameter("mainForm_SUBMIT", "1")
				.executeTest();


//		final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
//				1);
//		nameValuePairs
//				.add(new BasicNameValuePair("mainForm:name", "Dummy-User"));
//
//		nameValuePairs.add(new BasicNameValuePair("javax.faces.ViewState",
//				substring));
//		nameValuePairs
//				.add(new BasicNameValuePair("mainForm:j_id_a", "Press me"));
//		nameValuePairs.add(new BasicNameValuePair("mainForm_SUBMIT", "1"));
//
//		testClient.testPost(
//				"http://127.0.0.1:8181/war-jsf-primefaces-sample/success.xhtml",
//				nameValuePairs,
//				"Hello Dummy-User. We hope you enjoy Apache MyFaces", 200);

	}

	@Test
	public void testPrimefacesTagRendering() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.withResponseAssertion("The Primefaces-tag <p:panelGrid> was not rendered correctly.",
						resp -> !resp.matches("(?s).*<p:panelGrid.*>.*</p:panelGrid>.*"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-primefaces-sample/");

//		final String response = testClient.testWebPath(
//				"http://127.0.0.1:8181/war-jsf-primefaces-sample/",
//				"Please enter your name");
//
//		/*
//		 * If the taglib does not get recognized, PrimeFaces tags will be rendered verbatim.
//		 * Check that no verbatim tags are visible.
//		 */
//		assertFalse(
//				"The Primefaces-tag <p:panelGrid> was not rendered correctly.",
//				response.matches("(?s).*<p:panelGrid.*>.*</p:panelGrid>.*"));
	}
}
