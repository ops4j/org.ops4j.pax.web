package org.ops4j.pax.web.itest;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarPostIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WarPostIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = "mvn:org.ops4j.pax.web.samples/war-extended-post/"
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

	@Test
	public void testWC() throws Exception {

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs
				.add(new BasicNameValuePair("data", createData()));
	

		LOG.info("Sending Post");
		testPost("http://127.0.0.1:8181/posttest/upload-check", nameValuePairs, "POST data size is: 3000000", 200);
		
	}

	private String createData() {
		StringBuffer buff = new StringBuffer();
		
		int i = 0;
		while(i < 3000000) {
			buff.append("A");
			i++;
		}
		
		return buff.toString();
	}
	
}
