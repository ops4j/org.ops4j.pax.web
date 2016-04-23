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
 /**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author achim
 * 
 */
@RunWith(PaxExam.class)
@Ignore("Ignored for unknown reason")
public class WebJSFKarafTest extends KarafBaseTest {

	Logger LOG = LoggerFactory.getLogger(WebJSFKarafTest.class);

	private Bundle facesApiBundle;

	private Bundle facesImplBundle;

	private Bundle warBundle;

	@Configuration
	public Option[] config() {
		return combine(jettyConfig(), new VMOption("-DMyFacesVersion="
				+ getMyFacesVersion()));
	}

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-war")));
		assertTrue(featuresService.isInstalled(featuresService
				.getFeature("pax-http-whiteboard")));
	}

	@Test
	public void testSlash() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain expected message",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-sample");

	}

	@Test
	public void testJSF() throws Exception {

		LOG.info("Testing JSF workflow!");

		createTestClientForKaraf()
				.withResponseAssertion("Response must contain expected message",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-sample");

//		String response = testClient.testWebPath("http://127.0.0.1:8181/war-jsf-sample",
//				"Please enter your name");
		String response = "";

		// TODO check if this test is still necessary, if so, provide new method to new TestClient
		LOG.info("Found JSF starting page: {}",response);
		int indexOf = response.indexOf("id=\"javax.faces.ViewState\" value=");
		String substring = response.substring(indexOf + 34);
		indexOf = substring.indexOf("\"");
		substring = substring.substring(0, indexOf);
		
		Pattern pattern = Pattern.compile("(input id=\"mainForm:j_id_\\w*)");
		Matcher matcher = pattern.matcher(response);
		if (!matcher.find())
			fail("Didn't find required input id!");
		
		String inputID = response.substring(matcher.start(),matcher.end());
		inputID = inputID.substring(inputID.indexOf('"')+1);
		LOG.info("Found ID: {}", inputID);

		// TODO POST

//		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
//		nameValuePairs
//				.add(new BasicNameValuePair("mainForm:name", "Dummy-User"));
//
//		nameValuePairs.add(new BasicNameValuePair("javax.faces.ViewState",
//				substring.trim()));
//		nameValuePairs.add(new BasicNameValuePair(inputID,
//				"Press me"));
//		nameValuePairs.add(new BasicNameValuePair("mainForm_SUBMIT", "1"));

//		LOG.info("Will send the following NameValuePairs: {}", nameValuePairs);

		// FIXME add HTTP-POST to new TestClient
//		testClient.testPost("http://127.0.0.1:8181/war-jsf-sample/faces/helloWorld.jsp",
//				nameValuePairs,
//				"Hello Dummy-User. We hope you enjoy Apache MyFaces", 200);

	}

	@Before
	public void setUp() throws Exception {
		facesApiBundle = bundleContext
				.installBundle("mvn:org.apache.myfaces.core/myfaces-api/"
						+ getMyFacesVersion());
		facesImplBundle = bundleContext
				.installBundle("mvn:org.apache.myfaces.core/myfaces-impl/"
						+ getMyFacesVersion());

		facesApiBundle.start();
		facesImplBundle.start();

		initWebListener();

		int failCount = 0;
		while (facesApiBundle.getState() != Bundle.ACTIVE
				&& facesImplBundle.getState() != Bundle.ACTIVE) {
			Thread.sleep(500);
			if (failCount > 500)
				throw new RuntimeException(
						"Required myfaces bundles where never active");
			failCount++;
		}
		

		String warUrl = "mvn:org.ops4j.pax.web.samples/war-jsf/"
				+ getProjectVersion() + "/war";
		warBundle = bundleContext.installBundle(warUrl);
		warBundle.start();

		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (facesApiBundle != null) {
			facesApiBundle.stop();
			facesApiBundle.uninstall();
		}

		if (facesImplBundle != null) {
			facesImplBundle.stop();
			facesImplBundle.uninstall();
		}

		if (warBundle != null) {
			warBundle.stop();
			warBundle.uninstall();
		}
	}

}
