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

import static org.junit.Assert.fail;

import java.util.Dictionary;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarTCIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WarTCIntegrationTest.class);

	private Bundle installWarBundle;
	
	@Inject
	private WebContainer webContainer;

	@Configuration
	public Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = WEB_BUNDLE + "mvn:org.ops4j.pax.web.samples/war/"
				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH + "=/war";
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
	@Test
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE) {
				fail("Bundle should be active: " + b);
			}

			Dictionary<String, String> headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

	}

	@Test
	public void testWC() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/wc", "<h1>Hello World</h1>");

	}

	@Test
	public void testWebContainerExample() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/wc/example",
				"<h1>Hello World</h1>");

		testClient.testWebPath("http://127.0.0.1:8282/war/images/logo.png", "", 200, false);

	}

	@Test
	public void testWebContainerSN() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/wc/sn", "<h1>Hello World</h1>");

	}

	@Test
	public void testSlash() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/", "<h1>Error Page</h1>", 404, false);

	}

	@Test
	public void testSubJSP() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8282/war/wc/subjsp",
				"<h2>Hello World!</h2>");

	}

	@Test
	public void testErrorJSPCall() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/war/wc/error.jsp", "<h1>Error Page</h1>",  404, false);
	}

	@Test
	public void testWrongServlet() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/war/wrong/", "<h1>Error Page</h1>",
				404, false);
	}
	
	@Test
	@Ignore("Occasionally fails to unknown reason")
	public void testAdditionalWar() throws Exception {
		initWebListener();

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-dispatch-jsp/"
				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH
				+ "=/war-dispatch-jsp";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		waitForWebListener();
		
		testClient.testWebPath("http://127.0.0.1:8282/war/wc", "<h1>Hello World</h1>");
		testClient.testWebPath("http://127.0.0.1:8282/war-dispatch-jsp/wc/dispatch/jsp", "<h2>Hello World!</h2>");
	}
	
	@Test
    public void testStartStopBundle() throws Exception {
        LOG.debug("start/stopping bundle");
        initWebListener();
        
        initServletListener(null);
        
        installWarBundle.stop();
        
        installWarBundle.start();

        waitForWebListener();
        waitForServletListener();
        LOG.debug("Update done, testing bundle");

        testClient.testWebPath("http://127.0.0.1:8282/war/wc", "<h1>Hello World</h1>");
            
    }
	
}