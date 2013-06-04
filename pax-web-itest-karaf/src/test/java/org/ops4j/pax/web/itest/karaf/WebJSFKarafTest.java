/**
 * 
 */
package org.ops4j.pax.web.itest.karaf;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 * 
 */
@RunWith(PaxExam.class)
public class WebJSFKarafTest extends KarafBaseTest {

	Logger LOG = LoggerFactory.getLogger(WebJSFKarafTest.class);

	private org.ops4j.pax.web.itest.karaf.WebJSFKarafTest.WebListenerImpl webListener;

	private Bundle facesApiBundle;

	private Bundle facesImplBundle;

	private Bundle warBundle;

	@Configuration
	public Option[] config() {
		return combine(baseConfig(), new VMOption("-DMyFacesVersion="
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

		testWebPath("http://127.0.0.1:8181/war-jsf-sample",
				"Please enter your name");

	}

	@Test
	public void testJSF() throws Exception {

		LOG.info("Testing JSF workflow!");
		String response = testWebPath("http://127.0.0.1:8181/war-jsf-sample",
				"Please enter your name");

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

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs
				.add(new BasicNameValuePair("mainForm:name", "Dummy-User"));

		nameValuePairs.add(new BasicNameValuePair("javax.faces.ViewState",
				substring.trim()));
		nameValuePairs.add(new BasicNameValuePair(inputID,
				"Press me"));
		nameValuePairs.add(new BasicNameValuePair("mainForm_SUBMIT", "1"));

		LOG.info("Will send the following NameValuePairs: {}", nameValuePairs);
		
		testPost("http://127.0.0.1:8181/war-jsf-sample/faces/helloWorld.jsp",
				nameValuePairs,
				"Hello Dummy-User. We hope you enjoy Apache MyFaces", 200);

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
		webListener = new WebListenerImpl();

		int failCount = 0;
		while (facesApiBundle.getState() != Bundle.ACTIVE
				&& facesImplBundle.getState() != Bundle.ACTIVE) {
			Thread.sleep(500);
			if (failCount > 500)
				throw new RuntimeException(
						"Required myfaces bundles are never active");
			failCount++;
		}

		String warUrl = "mvn:org.ops4j.pax.web.samples/war-jsf/"
				+ getProjectVersion() + "/war";
		warBundle = bundleContext.installBundle(warUrl);
		warBundle.start();

		failCount = 0;
		while (warBundle.getState() != Bundle.ACTIVE) {
			Thread.sleep(500);
			if (failCount > 500)
				throw new RuntimeException(
						"Required war-bundles is never active");
			failCount++;
		}

		int count = 0;
		while (!((WebListenerImpl) webListener).gotEvent() && count < 100) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
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
