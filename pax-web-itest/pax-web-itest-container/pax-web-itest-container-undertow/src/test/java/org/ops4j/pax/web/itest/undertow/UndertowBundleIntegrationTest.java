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
 package org.ops4j.pax.web.itest.undertow;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@Ignore
public class UndertowBundleIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(UndertowBundleIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return options(combine(
				baseConfigure(),
				systemPackages("javax.xml.namespace;version=1.0.0", 
						"javax.transaction;version=1.1.0"
	                                    ),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-undertow-bundle")
						.version(VersionUtil.getProjectVersion())

		));

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
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
	public void testSubPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8181/images/logo.png");

//		testClient.testWebPath("http://127.0.0.1:8181/helloworld/hs", "Hello World");
//		// test to retrive Image
//		testClient.testWebPath("http://127.0.0.1:8181/images/logo.png", "", 200, false);

	}

	@Test
	public void testRootPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8181/");

//		testClient.testWebPath("http://127.0.0.1:8181/", "");
	}

	@Test
	public void testServletPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet Path: '",
						resp -> resp.contains("Servlet Path: "))
				.withResponseAssertion("Response must contain 'Path Info: /lall/blubb'",
						resp -> resp.contains("Path Info: /lall/blubb"))
				.doGETandExecuteTest("http://127.0.0.1:8181/lall/blubb");

//		testClient.testWebPath("http://127.0.0.1:8181/lall/blubb", "Servlet Path: ");
//		testClient.testWebPath("http://127.0.0.1:8181/lall/blubb", "Path Info: /lall/blubb");

	}

	@Test
	public void testServletDeRegistration() throws BundleException,
			ServletException, NamespaceException {

		if (installWarBundle != null) {
			installWarBundle.stop();
		}
	}

}
