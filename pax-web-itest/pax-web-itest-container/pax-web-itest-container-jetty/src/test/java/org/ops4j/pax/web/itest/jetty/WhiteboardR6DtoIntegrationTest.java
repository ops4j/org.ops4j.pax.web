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

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;


import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@RunWith(PaxExam.class)
public class WhiteboardR6DtoIntegrationTest extends ITestBase {

	@Inject
	@Filter(timeout = 20000)
	private WebContainer webcontainer;

	@Inject
	private BundleContext bundleContext;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("whiteboard-ds").versionAsInProject(),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"));
	}

	@Before
	public void setUp() throws Exception {
		initServletListener();
		waitForServletListener();
	}

	@Test
	public void testAllSamplesRegisteredAsExpected() throws Exception {
		// test simple-servlet without additional context, but with filter
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
						resp -> resp.contains("Hello from SimpleServlet"))
				.withResponseAssertion("Response must contain 'Request changed by SimpleFilter'",
						resp -> resp.contains("Request changed by SimpleFilter"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");

		// test custom-servlet with additional context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from ServletWithContext'",
						resp -> resp.contains("Hello from ServletWithContext"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");

		// test welcome-file
		// FIXME welcome file not working???
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'This is a welcome file provided by WhiteboardWelcomeFiles'",
//						resp -> resp.contains("This is a welcome file provided by WhiteboardWelcomeFiles"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/");

		// test error-page
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain 'Error Servlet, we do have a 404'",
						resp -> resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/error");

		// test resource
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Header 'Content-Type' must be 'plain/text'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("text/plain")))
				.doGETandExecuteTest("http://127.0.0.1:8181/resources/file.txt");
	}


	private <T> T withService(Function<HttpServiceRuntime, T> function){
		T result = null;
		ServiceReference<HttpServiceRuntime> ref = bundleContext.getServiceReference(HttpServiceRuntime.class);
		if(ref != null){
			HttpServiceRuntime service = bundleContext.getService(ref);
			if(service != null){
				result = function.apply(service);
				bundleContext.ungetService(ref);
			}
		}
		return result;
	}


	@Test
	public void testRuntimeDto() throws Exception {
		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);
		// FIXME evaluate

	}


	@Test
	public void testRuntimeDtoWithFailedServices() throws Exception {
		// TODO add invalid services
		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);
	}

	@Test
	public void testRequestInfoDto() throws Exception {
		RequestInfoDTO requestInfoDTO = withService(
				httpServiceRuntime -> httpServiceRuntime.calculateRequestInfoDTO("/custom/servlet"));
		// FIXME evaluate
	}


}
