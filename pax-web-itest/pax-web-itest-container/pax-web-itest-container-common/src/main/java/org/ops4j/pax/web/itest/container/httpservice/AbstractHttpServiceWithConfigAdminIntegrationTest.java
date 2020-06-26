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
package org.ops4j.pax.web.itest.container.httpservice;

import javax.inject.Inject;

import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractHttpServiceWithConfigAdminIntegrationTest extends AbstractControlledTestBase {

	private Bundle installWarBundle;

	@Inject
	private ConfigurationAdmin caService;

//	@Before
//	public void setUp() throws BundleException, InterruptedException, IOException {
//		org.osgi.service.cm.Configuration config = caService.getConfiguration(WebContainerConstants.PID);
//
//		Dictionary<String, Object> props = new Hashtable<>();
//
//		props.put(WebContainerConstants.PROPERTY_LISTENING_ADDRESSES, "127.0.0.1");
//		props.put(WebContainerConstants.PROPERTY_HTTP_PORT, "8181");
//
//		/*
//		 * Tomcat will start a default root context. This will not hurt, but if we initialize the
//		 * ServletListener too early it will detect this startup and will start the test before the
//		 * Servlet configured here is registered. Therefore we wait for a second before we initialize
//		 * the ServletListener and register the configuration.
//		 */
//		Thread.sleep(1000);
//
//		initServletListener("/");
//
//		config.setBundleLocation(null);
//		config.update(props);
//
//		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + VersionUtil.getProjectVersion();
//		installWarBundle = installAndStartBundle(bundlePath);
//
//		waitForServletListener();
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//		if (installWarBundle != null) {
//			installWarBundle.stop();
//			installWarBundle.uninstall();
//		}
//	}
//
//
//	@Test
//	public void testSubPath() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello World'",
//						resp -> resp.contains("Hello World"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
//		// test image-serving
//		HttpTestClientFactory.createDefaultTestClient()
//				.doGETandExecuteTest("http://127.0.0.1:8181/images/logo.png");
//	}
//
//	@Test
//	public void testRootPath() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.doGETandExecuteTest("http://127.0.0.1:8181/");
//	}
//
//	@Test
//	public void testServletPath() throws Exception {
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Servlet Path: '",
//						resp -> resp.contains("Servlet Path: "))
//				.withResponseAssertion("Response must contain 'Path Info: /lall/blubb'",
//						resp -> resp.contains("Path Info: /lall/blubb"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/lall/blubb");
//	}
//
//	@Test
//	public void testServletDeRegistration() throws Exception {
//
//		if (installWarBundle != null) {
//			installWarBundle.stop();
//		}
//	}
//
//	/**
//	 * Tests reconfiguration to another port
//	 *
//	 * @throws Exception when any error occurs
//	 */
//	@Test
//	public void testReconfiguration() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Servlet Path: '",
//						resp -> resp.contains("Servlet Path: "))
//				.withResponseAssertion("Response must contain 'Path Info: /lall/blubb'",
//						resp -> resp.contains("Path Info: /lall/blubb"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/lall/blubb");
//
//		org.osgi.service.cm.Configuration config = caService.getConfiguration(WebContainerConstants.PID);
//
//		Dictionary<String, Object> props = new Hashtable<>();
//
//		props.put(WebContainerConstants.PROPERTY_LISTENING_ADDRESSES, "127.0.0.1");
//		props.put(WebContainerConstants.PROPERTY_HTTP_PORT, "9191");
//
//		config.setBundleLocation(null);
//		config.update(props);
//
//		waitForServer("http://127.0.0.1:9191/");
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Servlet Path: '",
//						resp -> resp.contains("Servlet Path: "))
//				.withResponseAssertion("Response must contain 'Path Info: /lall/blubb'",
//						resp -> resp.contains("Path Info: /lall/blubb"))
//				.doGETandExecuteTest("http://127.0.0.1:9191/lall/blubb");
//	}

}
