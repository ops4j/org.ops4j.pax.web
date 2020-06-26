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

import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;
import org.osgi.framework.Bundle;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWebContainerIntegrationTest extends AbstractControlledTestBase {

	private Bundle installWarBundle;
	
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		initWebListener();
//		final String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
//				+ VersionUtil.getProjectVersion();
//		installWarBundle = installAndStartBundle(bundlePath);
//		waitForWebListener();
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
//	@Test
//	public void testWebContextPath() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
//						resp -> resp.contains("Have bundle context in filter: true"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc");
//	}
//
//	@Test
//	public void testWebContextPathWithServlet() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<title>Hello World (servlet name)</title>'",
//						resp -> resp.contains("<title>Hello World (servlet name)</title>"))
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc/sn");
//	}
//
//	/**
//	 * The server-container must register each ServletContext as an OSGi service
//	 */
//	@Test
//	public void testServletContextRegistration() throws Exception {
//		String filter = String.format("(%s=%s)",
//				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");
//
//		if (bundleContext.getServiceReferences(ServletContext.class, filter).size() == 0) {
//			fail("ServletContext was not registered as Service.");
//		}
//	}
//
//	/**
//	 * The server-container must unregister a ServletContext if the ServletContext got destroyed
//	 */
//	@Test
//	public void testServletContextUnregistration() throws Exception {
//		installWarBundle.stop();
//		String filter = String.format("(%s=%s)",
//				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");
//
//		if (bundleContext.getServiceReferences(ServletContext.class, filter).size() > 0) {
//			fail("ServletContext was not unregistered.");
//		}
//	}
//
//	@Test
//	public void testErrorPage() throws Exception  {
//	    HttpTestClientFactory.createDefaultTestClient()
//            .withResponseAssertion("Response must contain '<h1>Hello World Error Page</h1>'",
//                    resp -> resp.contains("<h1>Hello World Error Page</h1>"))
//            .withReturnCode(404)
//            .doGETandExecuteTest("http://127.0.0.1:8181/helloworld");
//	}
//
//	@Test
//	public void testWelcomFiles() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
//						resp -> resp.contains("<h1>Hello World</h1>"))
//				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
//						resp -> resp.contains("Have bundle context in filter: true"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc");
//
//		if (containerAlwaysInstallsDefaultServlet()) {
//			HttpTestClientFactory.createDefaultTestClient()
//					.withResponseAssertion("Response must contain '<h1>ROOT says Hi</h1>'",
//							resp -> resp.contains("<h1>ROOT says Hi</h1>"))
//					.doGETandExecuteTest("http://127.0.0.1:8181");
//		}
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Images say Hi</h1>'",
//						resp -> resp.contains("<h1>Images say Hi</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/images");
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Welcome</h1>'",
//						resp -> resp.contains("<h1>Welcome</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/html");
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain '<h1>Nested Hello</h1>'",
//						resp -> resp.contains("<h1>Nested Hello</h1>"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/html/nested");
//		// OSGi problem with org.osgi.framework.Bundle#getEntry() not detecting directories...
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(200)
//				.doGETandExecuteTest("http://127.0.0.1:8181/html/nested-without-welcome");
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(403)
//				.doGETandExecuteTest("http://127.0.0.1:8181/html/nested-without-welcome/");
//	}
//
//	/**
//	 * Derived classes may inform the test case if given container installs implicit "default" servlet
//	 * if one is not provided.
//	 * TODO: provide such servlet for Tomcat. Jetty and Undertow have better lifecycle and it's easier there
//	 * @return
//	 */
//	protected boolean containerAlwaysInstallsDefaultServlet() {
//		return true;
//	}

}
