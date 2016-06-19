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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;

/**
 */
@RunWith(PaxExam.class)
public class WhiteboardRestartTCIntegrationTest extends ITestBase {
	
	private static final Logger LOG = LoggerFactory.getLogger(WhiteboardRestartTCIntegrationTest.class);
	
	private Bundle installWarBundle;
	
    @Inject
    private BundleContext ctx;

	@Configuration
	public Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws Exception {
		int count = 0;
		while (!testClient.checkServer("http://127.0.0.1:8282/") && count < 100) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		
		LOG.info("waiting for Server took {} ms", (count * 1000));
		
		initServletListener("jsp");
		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard/" + VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
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
		for (Bundle b : bundleContext.getBundles()) {
			System.out.println("Bundle " + b.getBundleId() + " : "
					+ b.getSymbolicName());
		}

	}

	@Test
	public void testWhiteBoardRoot() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/root", "Hello Whiteboard Extender");
	}
	
	@Test
	@Ignore("Failing for duplicate Context - PAXWEB-597")
	public void testWhiteBoardSlash() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/", "Welcome to the Welcome page");
	}
	
	@Test
	@Ignore("Failing for duplicate context - PAXWEB-597")
	public void testWhiteBoardForbidden() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/forbidden", "", 401, false);
	}
	
	@Test
	public void testWhiteBoardFiltered() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/filtered", "Filter was there before");
	}

	@Test
	public void testWhiteBoardRootRestart() throws Exception {

		Bundle whiteBoardBundle = null;
		
		for (Bundle bundle : ctx.getBundles()) {
			String symbolicName = bundle.getSymbolicName();
			if ("org.ops4j.pax.web.pax-web-extender-whiteboard".equalsIgnoreCase(symbolicName)) {
				whiteBoardBundle = bundle;
				break;
			}
		}
		
		if (whiteBoardBundle == null) {
			Assert.fail("no Whiteboard Bundle found");
		}
		
		whiteBoardBundle.stop();
		
		Thread.sleep(3000);// workaround for buildserver issue
		
		int maxCount = 500;
		while (whiteBoardBundle.getState() != Bundle.RESOLVED && maxCount > 0) {
			Thread.sleep(500);
			maxCount--;
		}
		if (maxCount == 0) {
			Assert.fail("maxcount reached, Whiteboard bundle never reached ACTIVE state again!");
		}
		
		whiteBoardBundle.start();
		while (whiteBoardBundle.getState() != Bundle.ACTIVE && maxCount > 0) {
			Thread.sleep(500);
			maxCount--;
		}
		if (maxCount == 0) {
			Assert.fail("maxcount reached, Whiteboard bundle never reached ACTIVE state again!");
		}
		
		testClient.testWebPath("http://127.0.0.1:8282/root", "Hello Whiteboard Extender");
	}
}
