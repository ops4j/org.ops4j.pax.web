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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.web.itest.base.assertion.Assert.assertThat;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServlet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardErrorPage;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardFilter;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardListener;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardResource;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardServlet;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardServletWithContext;
import org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

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
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"));
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

		// Pax-Web specific whiteboard features...not relevant for DTOs

		// test servlet on HttpContext-service
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from " + PaxWebWhiteboardServletMapping.class.getName() + "'",
						resp -> resp.contains("Hello from " + PaxWebWhiteboardServletMapping.class.getName()))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/servlet-mapping");

		//The registration of the HttpContext Mapping and the combination of WelcomeFile+ErrorPage doesn't work right now
		//The context though is available. 
		
		// test welcome-page on HttpContextMapping-service
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
//						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context-mapping/");

		// test error-page on HttpContextMapping-service
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(404)
//				.withResponseAssertion("Response must contain 'Whoops, there was a 404.'",
//						resp -> resp.contains("Whoops, there was a 404."))
//				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context-mapping/not-available");		//this can't work, because of error servlet handler ... as no context will match, default error will take care.
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
		final long defaultServletContextServiceId = (long)bundleContext.getServiceReferences(ServletContext.class, "(" + WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME + "=default)").stream().findFirst().orElseThrow(() -> new AssertionError("Default ServletContext not found")).getProperty(Constants.SERVICE_ID);
		final long customServletContextServiceId = (long)bundleContext.getServiceReferences(ServletContext.class, "(" + WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME + "=CustomContext)").stream().findFirst().orElseThrow(() -> new AssertionError("CustomContext ServletContext not found")).getProperty(Constants.SERVICE_ID);
		final long filterServiceId = (long)bundleContext.getServiceReference(WhiteboardFilter.class).getProperty(Constants.SERVICE_ID);
		final long listenerServiceId = (long)bundleContext.getServiceReference(WhiteboardListener.class).getProperty(Constants.SERVICE_ID);
		final long resourceServiceId = (long)bundleContext.getServiceReference(WhiteboardResource.class).getProperty(Constants.SERVICE_ID);
		final long errorPageServiceId = (long)bundleContext.getServiceReference(WhiteboardErrorPage.class).getProperty(Constants.SERVICE_ID);


		assertThat("Default- and CustomServletContextDTO must be available:" + runtimeDTO.servletContextDTOs.length,
				runtimeDTO.servletContextDTOs,
				servletContextDTOs -> servletContextDTOs.length == 3);

		Optional<ServletContextDTO> defaultContext = Arrays.stream(runtimeDTO.servletContextDTOs)
				.filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "default"))
				.findFirst();
        Optional<ServletContextDTO> customContext = Arrays.stream(runtimeDTO.servletContextDTOs)
                .filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "CustomContext"))
                .findFirst();
//        Optional<ServletContextDTO> customContextMapping = Arrays.stream(runtimeDTO.servletContextDTOs)
//                .filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "CustomHttpContextMapping"))
//                .findFirst();

		// Test all under Default-ServletContext
		if(!defaultContext.isPresent()){
			fail("DefaultContext not found");
		}else{
			assertThat("ServletContextDTO for DefaultServletContext doesn't match!",
					defaultContext.get(),
					servletContextDTO -> Objects.equals(servletContextDTO.contextPath, "/"));
			assertThat("There should be exactly one Servlet in the DefaultServletContext!",
					defaultContext.get(),
					servletContextDTO -> servletContextDTO.servletDTOs.length == 1);
			assertThat("There should be exactly one Filter in the DefaultServletContext!",
					defaultContext.get(),
					servletContextDTO -> servletContextDTO.filterDTOs.length == 1);
			assertThat("There should be exactly one Resource in the DefaultServletContext!",
					defaultContext.get(),
					servletContextDTO -> servletContextDTO.resourceDTOs.length == 1);
			assertThat("There should be exactly one ErrorPage in the DefaultServletContext!",
					defaultContext.get(),
					servletContextDTO -> servletContextDTO.errorPageDTOs.length == 1);
			assertThat("ServletDTO for WhiteboardServlet doesn't match!",
					defaultContext.get().servletDTOs[0], servletDTO ->
							Objects.equals(servletDTO.name, "SimpleServlet")
									&& Objects.equals(servletDTO.patterns[0], "/simple-servlet")
									&& servletDTO.serviceId == servletServiceId
									&& servletDTO.servletContextId == defaultServletContextServiceId);
			assertThat("FilterDTO for WhiteboardFilter doesn't match!",
					defaultContext.get().filterDTOs[0],
					filterDTO -> filterDTO.serviceId == filterServiceId
							&& filterDTO.servletContextId == defaultServletContextServiceId);
			assertThat("ResourceDTO for WhiteboardResource doesn't match!",
					defaultContext.get().resourceDTOs[0], resourceDTO ->
							resourceDTO.serviceId == resourceServiceId
									&& Objects.equals(resourceDTO.prefix, "/www")
									&& Objects.equals(resourceDTO.patterns[0], "/resources")
									&& resourceDTO.servletContextId == defaultServletContextServiceId);
			assertThat("ErrorPageDTO for WhiteboardErrorPage doesn't match!",
					defaultContext.get().errorPageDTOs[0],
					errorPageDTO -> errorPageDTO.serviceId == errorPageServiceId
							&& Objects.equals(errorPageDTO.exceptions[0], "java.io.IOException")
//							&& errorPageDTO.errorCodes[0] == 404 FIXME errorCodes currently not mapped
							&& errorPageDTO.servletContextId == defaultServletContextServiceId);
		}
		// Test all under Custom-ServletContext
		if(!customContext.isPresent()){
			fail("CustomContext not found");
		}else{
			assertThat("ServletContextDTO for WhiteboardContext doesn't match!",
					customContext.get(), servletContextDTO -> Objects.equals(servletContextDTO.contextPath, "/context")
							&& servletContextDTO.serviceId == customServletContextServiceId);
			assertThat("There should be exactly one Servlet in the CustomServletContext!",
					customContext.get(),
					servletContextDTO -> servletContextDTO.servletDTOs.length == 1);
			assertThat("There should be exactly one Listener in the CustomServletContext!",
					customContext.get(),
					servletContextDTO -> servletContextDTO.listenerDTOs.length == 1);
			assertThat("ServletDTO for WhiteboardServletWithContext doesn't match!",
					customContext.get().servletDTOs[0], servletDTO ->
							Objects.equals(servletDTO.name, "ServletWithContext")
									&& Objects.equals(servletDTO.patterns[0], "/servlet")
									&& servletDTO.serviceId == servletWithContextServiceId
									&& servletDTO.servletContextId == customServletContextServiceId);
			assertThat("ListenerDTO for WhiteboardListener doesn't match!",
					customContext.get().listenerDTOs[0], listenerDTO -> listenerDTO.serviceId == listenerServiceId
							&& listenerDTO.servletContextId == customServletContextServiceId
							&& Objects.equals(listenerDTO.types[0], ServletRequestListener.class.getName()));
		}
		
		//TODO: check CustomHttpContextMapping
	}


	@Test
	public void testRuntimeDtoWithFailedServices() throws Exception {

		// add a ServletContextHelper with missing path
		Dictionary<String, String> props = new Hashtable<>(1);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "FailedContextName");
		ServiceRegistration<ServletContextHelper> failedContextReg =
				bundleContext.registerService(ServletContextHelper.class, new InvalidServletContextHelper(), props);
		long serviceIdFailedContext = (Long) failedContextReg.getReference().getProperty(Constants.SERVICE_ID);

		// add a Servlet with missing properties
		props = new Hashtable<>(1);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "FailedServletName");
		ServiceRegistration<Servlet> failedServletReg =
				bundleContext.registerService(Servlet.class, new InvalidServlet(), props);
		long serviceIdFailedServlet = (Long) failedServletReg.getReference().getProperty(Constants.SERVICE_ID);

		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);

		// Test all failed elements
		
		System.err.println(runtimeDTO.failedServletContextDTOs.length);
		
		assertTrue("Incorrect number of failed ServletContext DTOs", 1 == runtimeDTO.failedServletContextDTOs.length);
		assertThat("Invalid ServletContext doesn't match",
				runtimeDTO.failedServletContextDTOs[0],
				failedServletContextDTO -> failedServletContextDTO.serviceId == serviceIdFailedContext);
		assertTrue("Incorrect number of invalid ServletContexts", 1 == runtimeDTO.failedServletDTOs.length);
		assertThat("Invalid ServletContext doesn't match",
				runtimeDTO.failedServletDTOs[0],
				failedServletDTO -> failedServletDTO.serviceId == serviceIdFailedServlet);
	}

	@Test
	public void testRequestInfoDto() throws Exception {
		RequestInfoDTO requestInfoDTO = withService(
				httpServiceRuntime -> httpServiceRuntime.calculateRequestInfoDTO("/custom/servlet"));
		// FIXME evaluate
	}


	/**
	 * This ServletContextHelper is supposed to be registered with missing properties
	 */
	private static final class InvalidServletContextHelper extends ServletContextHelper {

	}


	/**
	 * This Servlet is supposed to be registered with missing properties
	 */
	private static final class InvalidServlet extends HttpServlet {

	}

}
