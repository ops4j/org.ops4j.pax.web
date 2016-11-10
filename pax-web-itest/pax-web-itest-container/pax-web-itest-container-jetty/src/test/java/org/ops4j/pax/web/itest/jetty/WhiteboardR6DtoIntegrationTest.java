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

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.web.itest.base.assertion.Assert.*;
import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME;


import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;

import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.samples.whiteboard.ds.*;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.*;

import java.util.Arrays;
import java.util.Objects;
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

	/**
	 * Tests the DTO structure for the Whiteboard-Services registered by sample whiteboard-ds
	 */
	@Test
	public void testRuntimeDto() throws Exception {
		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);

		// prepare ServiceIDs for comparrison
		final long servletServiceId = (long)bundleContext.getServiceReference(WhiteboardServlet.class).getProperty(Constants.SERVICE_ID);
		final long servletWithContextServiceId = (long)bundleContext.getServiceReference(WhiteboardServletWithContext.class).getProperty(Constants.SERVICE_ID);
		final long defaultContextServiceId = (long)bundleContext.getServiceReferences(ServletContext.class, "(" + WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME + "=default)").stream().findFirst().orElseThrow(() -> new AssertionError("Default ServletContext not found")).getProperty(Constants.SERVICE_ID);
		final long customContextServiceId = (long)bundleContext.getServiceReference(WhiteboardContext.class).getProperty(Constants.SERVICE_ID);
		final long filterServiceId = (long)bundleContext.getServiceReference(WhiteboardFilter.class).getProperty(Constants.SERVICE_ID);
		final long listenerServiceId = (long)bundleContext.getServiceReference(WhiteboardListener.class).getProperty(Constants.SERVICE_ID);
		final long resourceServiceId = (long)bundleContext.getServiceReference(WhiteboardResource.class).getProperty(Constants.SERVICE_ID);
		final long errorPageServiceId = (long)bundleContext.getServiceReference(WhiteboardErrorPage.class).getProperty(Constants.SERVICE_ID);


		assertThat("ServletContextDTOs must be available", runtimeDTO.servletContextDTOs, servletContextDTOs -> servletContextDTOs.length == 2);

		Optional<ServletContextDTO> defaultContext = Arrays.stream(runtimeDTO.servletContextDTOs)
				.filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "default"))
				.findFirst();
		Optional<ServletContextDTO> customContext = Arrays.stream(runtimeDTO.servletContextDTOs)
				.filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "CustomContext"))
				.findFirst();

		// Test all under Default-ServletContext
		if(!defaultContext.isPresent()){
			fail("DefaultContext not found");
		}else{
			assertThat("ServletContextDTO for DefaultServletContext doesn't match!", defaultContext.get(), servletContextDTO -> Objects.equals(servletContextDTO.contextPath, "/"));
			assertThat("ServletDTO for WhiteboardServlet doesn't match!", defaultContext.get().servletDTOs[0], servletDTO ->
					Objects.equals(servletDTO.name, "SimpleServlet")
							&& Objects.equals(servletDTO.patterns[0], "/simple-servlet")
							&& servletDTO.serviceId == servletServiceId
							&& servletDTO.servletContextId == defaultContextServiceId);
			assertThat("FilterDTO for WhiteboardFilter doesn't match!", defaultContext.get().filterDTOs[0], filterDTO ->
					filterDTO.serviceId == filterServiceId
							&& filterDTO.servletContextId == defaultContextServiceId);
			assertThat("ResourceDTO for WhiteboardResource doesn't match!", defaultContext.get().resourceDTOs[0], resourceDTO ->
					resourceDTO.serviceId == resourceServiceId
							&& Objects.equals(resourceDTO.prefix, "/www")
							&& Objects.equals(resourceDTO.patterns[0], "/resources")
							&& resourceDTO.servletContextId == defaultContextServiceId);
			assertThat("ErrorPageDTO for WhiteboardErrorPage doesn't match!", defaultContext.get().errorPageDTOs[0], errorPageDTO ->
					errorPageDTO.serviceId == errorPageServiceId
							&& Objects.equals(errorPageDTO.exceptions[0], "java.io.IOException")
							&& errorPageDTO.errorCodes[0] == 404
							&& errorPageDTO.servletContextId == defaultContextServiceId);
		}
		// Test all under Custom-ServletContext
		if(!customContext.isPresent()){
			fail("CustomContext not found");
		}else{
			assertThat("ServletContextDTO for WhiteboardContext doesn't match!", customContext.get(), servletContextDTO ->
					Objects.equals(servletContextDTO.contextPath, "/custom")
							&& servletContextDTO.serviceId == customContextServiceId);
			assertThat("ServletDTO for WhiteboardServletWithContext doesn't match!", customContext.get().servletDTOs[0], servletDTO ->
					Objects.equals(servletDTO.name, "ServletWithContext")
							&& Objects.equals(servletDTO.patterns[0], "/servlet")
							&& servletDTO.serviceId == servletWithContextServiceId
							&& servletDTO.servletContextId == customContextServiceId);
			assertThat("ListenerDTO for WhiteboardListener doesn't match!", customContext.get().listenerDTOs[0], listenerDTO ->
					listenerDTO.serviceId == listenerServiceId
							&& listenerDTO.servletContextId == customContextServiceId);
		}
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
