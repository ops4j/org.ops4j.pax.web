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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.web.itest.common.ITestBase.configureJetty;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class JettyHandlerServiceIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(JettyHandlerServiceIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureJetty();

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		final String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war/" + VersionUtil.getProjectVersion()
				+ "/war?" + WEB_CONTEXT_PATH + "=/test";
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
	public void testWeb() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8181/test/wc/example");
	}

	@Test
	public void testStaticContent() throws Exception {
		
		/*
		  <New class="org.eclipse.jetty.server.handler.ContextHandler">
				<Set name="contextPath">/static-content</Set>
				<Set name="handler">
					<New class="org.eclipse.jetty.server.handler.ResourceHandler">
						<Set name="resourceBase">target/logs</Set>
						<Set name="directoriesListed">true</Set>
					</New>
				</Set>
		  </New>
		 */


		ContextHandler ctxtHandler = new ContextHandler();
		ctxtHandler.setContextPath("/static-content");
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setResourceBase("target");
		resourceHandler.setDirectoriesListed(true);
		ctxtHandler.setHandler(resourceHandler);

		ServiceRegistration<Handler> registerService = bundleContext.registerService(Handler.class, ctxtHandler, null);

		waitForServer("http://localhost:8181/");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<A HREF=\"/static-content/'",
						resp -> resp.contains("<A HREF=\"/static-content/"))
				.doGETandExecuteTest("http://localhost:8181/static-content/");

		registerService.unregister();
	}
}
