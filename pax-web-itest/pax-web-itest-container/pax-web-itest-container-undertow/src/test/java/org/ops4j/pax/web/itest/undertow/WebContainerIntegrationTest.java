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

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.Optional;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebContainerIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureUndertow();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initServletListener();
		final String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
				+ VersionUtil.getProjectVersion();
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


	@Test
	public void testWebContextPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc");
	}

	@Test
	public void testFilterInitWebContextPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
						resp -> resp.contains("Have bundle context in filter: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc");
	}

	@Test
	public void testImageResources() throws Exception {
	    HttpTestClientFactory.createDefaultTestClient()
	        .withResponseAssertion("Response must contain an image", resp -> {
	            byte[] img = resp.getBytes();
	            return img.length > 0;
	        })
	        .doGETandExecuteTest("http://localhost:8181/images/logo.png");
	}
	
    @Test
    public void testErrorPage() throws Exception  {
        HttpTestClientFactory.createDefaultTestClient()
        .withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
                resp -> resp.contains("<h1>Hello World</h1>"))
        .withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
                resp -> resp.contains("Have bundle context in filter: true"))
        .doGETandExecuteTest("http://127.0.0.1:8181/helloworld/wc");
        
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
            .doGETandExecuteTest("http://127.0.0.1:8181/");

        HttpTestClientFactory.createDefaultTestClient()
            .withResponseAssertion("Response must contain '<h1>Welcome</h1>'",
                resp -> resp.contains("<h1>Welcome</h1>"))
            .doGETandExecuteTest("http://127.0.0.1:8181");
    }

	/**
	 * The server-container must register each ServletContext as an OSGi service
	 */
	@Test
	public void testServletContextRegistration() throws Exception {
		String filter = String.format("(%s=%s)",
				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");

		Collection<ServiceReference<ServletContext>> serviceReferences = bundleContext.getServiceReferences(
				ServletContext.class,
				filter);
		if(serviceReferences.isEmpty()){
			fail("ServletContext was not registered as Service.");
		}

		Optional<ServiceReference<ServletContext>> contextRef = serviceReferences.stream().findFirst();

		assertThat("Proxy must initialize ServletContext and provide correct name",
				bundleContext.getService(contextRef.orElseThrow(AssertionError::new)).getServletContextName(),
				CoreMatchers.equalTo("default"));

	}


	/**
	 * The server-container must unregister a ServletContext if the ServletContext got destroyed
	 */
	@Test
	public void testServletContextUnregistration() throws Exception {
		installWarBundle.stop();
		String filter = String.format("(%s=%s)",
				WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, "/");

		if(!bundleContext.getServiceReferences(ServletContext.class, filter).isEmpty()){
			fail("ServletContext was not unregistered.");
		}
	}

}
