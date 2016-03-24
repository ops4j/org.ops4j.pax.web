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
 package org.ops4j.pax.web.itest.tomcat;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
@Ignore("Failes: can't find the EL factory -> [PAXWEB-929] should fix this")
public class WarJSFTCIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {

		return OptionUtils
				.combine(
						configureTomcat(),
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
								.version(VersionUtil.getMyFacesVersion()));
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
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8282/war-jsf-sample/");

//		testClient.testWebPath("http://127.0.0.1:8282/war-jsf-sample/",
//				"Please enter your name");
	}

	@Test
	public void testJSF() throws Exception {

		LOG.debug("Testing JSF workflow!");

		CookieState cookieState = new CookieState();

		String response = HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.withResponseAssertion("Response must contain jsf-inputfield",
						resp -> {
							int indexOf = resp.indexOf("id=\"javax.faces.ViewState\" value=");
							String substring = resp.substring(indexOf + 34);
							indexOf = substring.indexOf("\"");
							substring = substring.substring(0, indexOf);

							Pattern pattern = Pattern.compile("(input id=\"mainForm:j_id_\\w*)");
							Matcher matcher = pattern.matcher(resp);
							if(!matcher.find()){
								return false;
							}

							String inputID = resp.substring(matcher.start(),matcher.end());
							inputID = inputID.substring(inputID.indexOf('"')+1);
							LOG.debug("Found ID: {}", inputID);
							return true;
						})
				.doGETandExecuteTest("http://127.0.0.1:8282/war-jsf-sample");

//		String response = testClient.testWebPath("http://127.0.0.1:8282/war-jsf-sample",
//				"Please enter your name");

		Pattern patternViewState = Pattern
				.compile("id=\\\"j_id_.*:javax.faces.ViewState:\\w\\\"");
		Matcher viewStateMatcher = patternViewState.matcher(response);
		if (!viewStateMatcher.find()) {
			fail("Didn't find required ViewState ID!");
		}
		String viewStateID = response.substring(viewStateMatcher.start() + 4,
				viewStateMatcher.end() - 1);

		String substring = response.substring(viewStateMatcher.end() + 8);
		int indexOf = substring.indexOf("\"");
		String viewStateValue = substring.substring(0, indexOf);

		Pattern pattern = Pattern.compile("(input id=\"mainForm:j_id_\\w*)");
		Matcher matcher = pattern.matcher(response);

		if (!matcher.find())
			fail("Didn't find required input id!");
		String inputID = response.substring(matcher.start(),matcher.end());
		inputID = inputID.substring(inputID.indexOf('"')+1);

		HttpTestClientFactory.createDefaultTestClient()
				.useCookieState(cookieState)
				.withResponseAssertion("Response from POST must contain 'Hello Dummy-User. We hope you enjoy Apache MyFaces'",
						resp -> resp.contains("Hello Dummy-User. We hope you enjoy Apache MyFaces"))
				.doPOST("http://127.0.0.1:8181/war-jsf-sample/faces/helloWorld.jsp")
				.addParameter("mainForm:name", "Dummy-User")
				.addParameter(viewStateID, viewStateValue)
				.addParameter(inputID, "Press me")
				.addParameter("javax.faces.ViewState", viewStateValue)
				.addParameter("mainForm_SUBMIT", "1")
				.executeTest();

		// TODO test POST
//		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
//		nameValuePairs
//				.add(new BasicNameValuePair("mainForm:name", "Dummy-User"));
//
//		nameValuePairs.add(new BasicNameValuePair("javax.faces.ViewState",
//				substring.trim()));
//		nameValuePairs.add(new BasicNameValuePair(inputID,
//				"Press me"));
//		nameValuePairs.add(new BasicNameValuePair("mainForm_SUBMIT", "1"));
//
//		LOG.debug("Will send the following NameValuePairs: {}", nameValuePairs);
//
//		testClient.testPost("http://127.0.0.1:8282/war-jsf-sample/faces/helloWorld.jsp",
//				nameValuePairs,
//				"Hello Dummy-User. We hope you enjoy Apache MyFaces", 200);

	}

}
