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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.fail;

import java.io.IOException;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebContainerIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WebContainerIntegrationTest.class);

	private Bundle installWarBundle;
	
	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		final String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
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
	public void testWebContextPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
						resp -> resp.contains("Have bundle context in filter: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc");
	}

	/**
	 * The server-container must register each ServletContext as an OSGi service
	 */
	@Test
	public void testServletContextRegistration() throws Exception {
		String filter = String.format("(%s=%s)",
				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");

		if(bundleContext.getServiceReferences(ServletContext.class, filter).size() == 0){
			fail("ServletContext was not registered as Service.");
		}
	}

	/**
	 * The server-container must unregister a ServletContext if the ServletContext got destroyed
	 */
	@Test
	public void testServletContextUnregistration() throws Exception {
		installWarBundle.stop();
		String filter = String.format("(%s=%s)",
				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");

		if(bundleContext.getServiceReferences(ServletContext.class, filter).size() > 0){
			fail("ServletContext was not unregistered.");
		}
	}
	
	@Test
	public void testErrorPage() throws Exception  {
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
            .withResponseAssertion("Response must contain '<h1>Welcome</h1>'",
                resp -> resp.contains("<h1>Welcome</h1>"))
            .doGETandExecuteTest("http://127.0.0.1:8181");
    }
	
}
