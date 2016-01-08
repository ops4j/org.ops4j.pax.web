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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class JspTCIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(JspTCIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
		return configureTomcat();
	}


	@Before
	public void setUp() throws BundleException, InterruptedException {
		waitForServer("http://127.0.0.1:8282/");
		initWebListener();
		initServletListener("jsp");
		final String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-jsp/" + VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForWebListener();
		waitForServletListener();

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
		for (final Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE) {
				fail("Bundle should be active: " + b);
			}

			final Dictionary<String,String> headers = b.getHeaders();
			final String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
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
	public void testSimpleJsp() throws Exception {

		testClient.testWebPath("http://localhost:8282/helloworld/jsp/simple.jsp", "<h1>Hello World</h1>");

	}

	@Test
	public void testTldJsp() throws Exception {

		testClient.testWebPath("http://localhost:8282/helloworld/jsp/using-tld.jsp", "Hello World");
	}

	@Test
	public void testPrecompiled() throws Exception {
		testClient.testWebPath("http://localhost:8282/helloworld/jspc/simple.jsp", "<h1>Hello World</h1>");
		testClient.testWebPath("http://localhost:8282/helloworld/jspc/using-tld.jsp", "Hello World");
	}
}
