/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.container.httpservice;

import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWebContainerIntegrationTest extends AbstractContainerTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractWebContainerIntegrationTest.class);

	private Bundle hsBundle;

	@Before
	public void setUp() throws Exception {
		// if we grab a service using this class' bundle's context, there will be automatically created
		// OsgiContextModel with "default" id and pax-web-itest-container-common bundle. And because we
		// do it before installing wc-helloworld bundle, the OsgiServletContext that'll be associated with the
		// servlets/filters from this wc-helloworld bundle will be the one from pax-web-itest-container-common
		// and what's most important - after stopping wc-helloworld bundle, there'll still be OSGi service
		// for ServletContext registered, related to pax-web-itest-container-common ;)
//		WebContainer httpService = getWebContainer(context);

		configureAndWaitForServletWithMapping("/helloworld/wc/error/create",
				() -> hsBundle = installAndStartBundle(sampleURI("wc-helloworld")));
	}

	@After
	public void tearDown() throws BundleException {
		if (hsBundle != null) {
			hsBundle.stop();
			hsBundle.uninstall();
		}
	}

	@Test
	public void testWebContextPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
						resp -> resp.contains("Have bundle context in filter: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc");
	}

	@Test
	public void testWebContextPathWithServlet() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<title>Hello World (servlet name)</title>'",
						resp -> resp.contains("<title>Hello World (servlet name)</title>"))
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc/sn");
	}

	/**
	 * The server-container must register each ServletContext as an OSGi service
	 */
	@Test
	public void testServletContextRegistration() throws Exception {
		// according to javax.servlet.ServletContext.getContextPath() javadoc, root context has
		// "" context path, not "/", but we'll unify it
		String filter = String.format("(%s=%s)",
				PaxWebConstants.SERVICE_PROPERTY_WEB_SERVLETCONTEXT_PATH, "/");

		if (context.getServiceReferences(ServletContext.class, filter).size() == 0) {
			fail("ServletContext was not registered as Service.");
		}
	}

	/**
	 * The server-container must unregister a ServletContext if the ServletContext got destroyed
	 */
	@Test
	public void testServletContextUnregistration() throws Exception {
		hsBundle.stop();
		String filter = String.format("(%s=%s)",
				PaxWebConstants.SERVICE_PROPERTY_WEB_SERVLETCONTEXT_PATH, "/");

		if (context.getServiceReferences(ServletContext.class, filter).size() > 0) {
			fail("ServletContext was not unregistered.");
		}
	}

	@Test
	public void testErrorPage() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World Error Page</h1>'",
						resp -> resp.contains("<h1>Hello World Error Page</h1>"))
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld");
	}

	@Test
	public void testWelcomFiles() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
						resp -> resp.contains("Have bundle context in filter: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Images say Hi</h1>'",
						resp -> resp.contains("<h1>Images say Hi</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/images");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Welcome</h1>'",
						resp -> resp.contains("<h1>Welcome</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Nested Hello</h1>'",
						resp -> resp.contains("<h1>Nested Hello</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/html/nested");

		// should be an immediately-handled redirect
		// welcome files are examined, but because there's no html/nested-without-welcome/index.html, according
		// to Servlet API specification, 10.10 "Welcome files", an attempt must be made to find a web component
		// (a servlet) mapped to resulting path - and there IS such web component, which is default 404 servlet
		// mapped to "/"
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/html/nested-without-welcome");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/html/nested-without-welcome/");
	}

}
