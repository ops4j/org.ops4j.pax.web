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
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.samples.authentication.AuthHttpContext;
import org.ops4j.pax.web.samples.authentication.StatusServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

import static org.junit.Assert.assertNotNull;

@Ignore("Fails for unknown reason")
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class AuthenticationTCIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		String bundlePath = "mvn:org.ops4j.pax.web.samples/authentication/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = bundleContext.installBundle(bundlePath);
		
		installWarBundle.start();
		
		new WaitCondition("authentication - resolved bundle") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return installWarBundle.getState() == Bundle.ACTIVE;
			}
		}.waitForCondition(); 

		
		installWarBundle.stop();
		
		new WaitCondition("authentication - resolved bundle") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return installWarBundle.getState() == Bundle.RESOLVED;
			}
		}.waitForCondition(); 

		waitForServer("http://127.0.0.1:8282/");
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
		
		installWarBundle = null;
	}

	@Test
	public void testStatus() throws Exception {

		ServiceReference<HttpService> httpServiceRef = bundleContext
				.getServiceReference(HttpService.class);

		assertNotNull(httpServiceRef);
		HttpService httpService = bundleContext
				.getService(httpServiceRef);

		httpService.registerServlet("/status", new StatusServlet(), null, null);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'org.osgi.service.http.authentication.type : null'",
						resp -> resp.contains("org.osgi.service.http.authentication.type : null"))
				.doGETandExecuteTest("http://127.0.0.1:8282/status");

//		testClient.testWebPath("http://127.0.0.1:8282/status",
//				"org.osgi.service.http.authentication.type : null");

		httpService.unregister("/status");
		bundleContext.ungetService(httpServiceRef);
	}

	@Test
	public void testStatusAuth() throws Exception {

		initServletListener(null);

		ServiceReference<HttpService> httpServiceRef = bundleContext
				.getServiceReference(HttpService.class);
		assertNotNull(httpServiceRef);
		HttpService httpService = bundleContext
				.getService(httpServiceRef);
		httpService.registerServlet("/status-with-auth", new StatusServlet(),
				null, new AuthHttpContext());

		waitForServletListener();

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'org.osgi.service.http.authentication.type : BASIC'",
						resp -> resp.contains("org.osgi.service.http.authentication.type : BASIC"))
				.doGETandExecuteTest("http://127.0.0.1:8282/status-with-auth");

//		testClient.testWebPath("http://127.0.0.1:8282/status-with-auth",
//				"org.osgi.service.http.authentication.type : BASIC");

		httpService.unregister("/status-with-auth");
		bundleContext.ungetService(httpServiceRef);

	}

}
