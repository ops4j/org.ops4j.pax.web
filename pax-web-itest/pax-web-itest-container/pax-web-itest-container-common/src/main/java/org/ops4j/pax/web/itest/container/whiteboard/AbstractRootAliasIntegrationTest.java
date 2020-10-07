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
package org.ops4j.pax.web.itest.container.whiteboard;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractRootAliasIntegrationTest extends AbstractContainerTestBase {

	private ServiceRegistration<Servlet> servletRoot;
	private ServiceRegistration<Servlet> servletSecond;
	private HttpService httpService;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/secondRoot/third/*", () -> {
			servletRoot = registerServletWhiteBoard("/myRoot");
			servletSecond = registerServletWhiteBoard("/myRoot/second");

			httpService = getHttpService(context);

			registerServlet("/secondRoot");
			registerServlet("/secondRoot/third");
		});
	}

	@After
	public void tearDown() throws BundleException {
		servletRoot.unregister();
		servletSecond.unregister();

		unregisterServlet("/secondRoot");
		unregisterServlet("/secondRoot/third");
	}

	private ServiceRegistration<Servlet> registerServletWhiteBoard(final String path) throws ServletException {
		Dictionary<String, String> initParams = new Hashtable<>();
		initParams.put(PaxWebConstants.INIT_PARAM_SERVLET_NAME, path);
		initParams.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, path);

		return context.registerService(Servlet.class,
				new HttpServlet() {
					@Override
					protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
						resp.getOutputStream().write(path.getBytes());
					}
				},
				initParams);
	}

	private void registerServlet(final String path) throws ServletException, NamespaceException {
		Dictionary<String, String> initParams = new Hashtable<>();
		initParams.put(PaxWebConstants.INIT_PARAM_SERVLET_NAME, path);
		httpService.registerServlet(path, new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getOutputStream().write(path.getBytes());
			}
		}, initParams, null);
		System.out.println("registered: " + path);
	}

	private void unregisterServlet(String path) {
		httpService.unregister(path);
		System.out.println("unregistered: " + path);
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'myRoot'",
						resp -> resp.contains("myRoot"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myRoot");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'myRoot/second'",
						resp -> resp.contains("myRoot/second"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myRoot/second");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'myRoot'",
						resp -> resp.contains("myRoot"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myRoot/wrong");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'secondRoot'",
						resp -> resp.contains("secondRoot"))
				.doGETandExecuteTest("http://127.0.0.1:8181/secondRoot");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'secondRoot/third'",
						resp -> resp.contains("secondRoot/third"))
				.doGETandExecuteTest("http://127.0.0.1:8181/secondRoot/third");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'secondRoot'",
						resp -> resp.contains("secondRoot"))
				.doGETandExecuteTest("http://127.0.0.1:8181/secondRoot/wrong");
	}

}
