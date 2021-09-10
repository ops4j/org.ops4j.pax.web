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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.servlet.Servlet;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardErrorPage;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardFilter;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardListener;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardResource;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardServlet;
import org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardServletWithContext;
import org.ops4j.pax.web.service.spi.model.events.ErrorPageEventData;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.spi.model.events.WelcomeFileEventData;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.web.itest.utils.assertion.Assert.assertThat;

public abstract class AbstractWhiteboardR6DtoIntegrationTest extends AbstractContainerTestBase {

	private Bundle bundle;

	@Before
	public void setUp() throws Exception {
		configureAndWait(() -> bundle = installAndStartBundle(sampleURI("whiteboard-ds")), events -> {
			boolean match = events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ErrorPageEventData
					&& usesContexts(e.getData(), "CustomHttpContextMapping"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData) e.getData()).getServletName().equals("org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping")
					&& usesContexts(e.getData(), "CustomHttpContext"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof WelcomeFileEventData
					&& usesContexts(e.getData(), "CustomHttpContext", "CustomHttpContextMapping"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData) e.getData()).isResourceServlet()
					&& usesContexts(e.getData(), "default", "CustomContext", "CustomHttpContext", "CustomHttpContextMapping"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData) e.getData()).getServletName().equals("ServletWithContext")
					&& usesContexts(e.getData(), "CustomContext"));
			return match;
		});
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle != null) {
			bundle.stop();
			bundle.uninstall();
		}
	}

	@Test
	public void testAllSamplesRegisteredAsExpected() throws Exception {
		// /simple-servlet in default context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Request changed by SimpleFilter'",
						resp -> resp.contains("Request changed by SimpleFilter") && resp.contains("Hello from SimpleServlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");

		// org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardResource has been registered into 4 contexts
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/index.html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/index.html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/index.html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context-mapping/index.html");

		// org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardResource has been registered into 4 contexts, but
		// welcome file mapping - only to 3 of them
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(403)
				.doGETandExecuteTest("http://127.0.0.1:8181/context");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context-mapping");

		// a servlet in /
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
						resp -> resp.contains("Hello from SimpleServlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
		// a servlet in /context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
						resp -> resp.contains("Hello from ServletWithContext"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
		// a servlet in /custom-http-context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping'",
						resp -> resp.contains("Hello from org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/servlet-mapping");

		// error pages
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a custom error page",
						resp -> resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a standard error page",
						resp -> !resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a custom error page",
						resp -> resp.contains("Whoops, there was a 404. This is provided via PaxWebWhiteboardErrorPageMapping"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context-mapping/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a standard error page",
						resp -> !resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/x");
	}

	@Test
	public void testRuntimeDto() throws Exception {
		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);

		// prepare ServiceIDs for comparrison
		final long servletServiceId = (long) getServiceReference(context, WhiteboardServlet.class, null).getProperty(Constants.SERVICE_ID);
		final long servletWithContextServiceId = (long) getServiceReference(context, WhiteboardServletWithContext.class, null).getProperty(Constants.SERVICE_ID);
		final long filterServiceId = (long) getServiceReference(context, WhiteboardFilter.class, null).getProperty(Constants.SERVICE_ID);
		final long listenerServiceId = (long) getServiceReference(context, WhiteboardListener.class, null).getProperty(Constants.SERVICE_ID);
		final long resourceServiceId = (long) getServiceReference(context, WhiteboardResource.class, null).getProperty(Constants.SERVICE_ID);
		final long errorPageServiceId = (long) getServiceReference(context, WhiteboardErrorPage.class, null).getProperty(Constants.SERVICE_ID);

		assertThat("Default- and CustomServletContextDTO must be available:" + runtimeDTO.servletContextDTOs.length,
				runtimeDTO.servletContextDTOs,
				servletContextDTOs -> servletContextDTOs.length >= 3);

		Optional<ServletContextDTO> defaultContext = Arrays.stream(runtimeDTO.servletContextDTOs)
				.filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "default"))
				.findFirst();
		Optional<ServletContextDTO> customContext = Arrays.stream(runtimeDTO.servletContextDTOs)
				.filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "CustomContext"))
				.findFirst();
		Optional<ServletContextDTO> customContextMapping = Arrays.stream(runtimeDTO.servletContextDTOs)
				.filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "CustomHttpContextMapping"))
				.findFirst();

		// previously the IDs were taken from service.id of related ServletContext.class registered as OSGi
		// services, but registration of ServletContext.class service is related with chapter 128 (WABs) and not
		// chapter 140 (Whiteboard) - we have to take the ID of associated ServletContextHelper (or similar
		// services handled by Pax Web 8
		final long defaultContextServiceId = defaultContext.map(contextDTO -> contextDTO.serviceId).orElse(-1L);
		final long customContextServiceId = customContext.map(contextDTO -> contextDTO.serviceId).orElse(-1L);
		final long customContextMappingServiceId = customContextMapping.map(contextDTO -> contextDTO.serviceId).orElse(-1L);

		// Test all under Default-ServletContext
		if (!defaultContext.isPresent()) {
			fail("DefaultContext not found");
		} else {
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
									&& servletDTO.servletContextId == defaultContextServiceId);
			assertThat("FilterDTO for WhiteboardFilter doesn't match!",
					defaultContext.get().filterDTOs[0],
					filterDTO -> filterDTO.serviceId == filterServiceId
							&& filterDTO.servletContextId == defaultContextServiceId
							&& Objects.equals(filterDTO.name, "SimpleFilter"));
			assertThat("ResourceDTO for WhiteboardResource doesn't match!",
					defaultContext.get().resourceDTOs[0], resourceDTO ->
							resourceDTO.serviceId == resourceServiceId
									&& Objects.equals(resourceDTO.prefix, "/www")
									&& Objects.equals(resourceDTO.patterns[0], "/")
									&& resourceDTO.servletContextId == defaultContextServiceId);
			assertThat("ErrorPageDTO for WhiteboardErrorPage doesn't match!",
					defaultContext.get().errorPageDTOs[0],
					errorPageDTO -> errorPageDTO.serviceId == errorPageServiceId
							&& Objects.equals(errorPageDTO.exceptions[0], "java.io.IOException")
							&& errorPageDTO.errorCodes[0] == 404
							&& errorPageDTO.servletContextId == defaultContextServiceId);
		}

		// Test all under Custom-ServletContext
		if (!customContext.isPresent()) {
			fail("CustomContext not found");
		} else {
			assertThat("ServletContextDTO for WhiteboardContext doesn't match!",
					customContext.get(), servletContextDTO -> Objects.equals(servletContextDTO.contextPath, "/context")
							&& servletContextDTO.serviceId == customContextServiceId);
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
									&& servletDTO.servletContextId == customContextServiceId);
			assertThat("ListenerDTO for WhiteboardListener doesn't match!",
					customContext.get().listenerDTOs[0], listenerDTO -> listenerDTO.serviceId == listenerServiceId
							&& listenerDTO.servletContextId == customContextServiceId
							&& Objects.equals(listenerDTO.types[0], ServletRequestListener.class.getName()));
		}

		// Test all under CustomHttpContextMapping
		if (!customContextMapping.isPresent()) {
			fail("CustomContext not found");
		} else {
			assertThat("ServletContextDTO for WhiteboardContext doesn't match!",
					customContextMapping.get(), servletContextDTO -> Objects.equals(servletContextDTO.contextPath, "/context-mapping")
							&& servletContextDTO.serviceId == customContextMappingServiceId);
			assertThat("There should be exactly one Resource in the CustomHttpContextMapping!",
					customContextMapping.get(),
					servletContextDTO -> servletContextDTO.resourceDTOs.length == 1);
			assertThat("ResourceDTO for WhiteboardResource doesn't match!",
					customContextMapping.get().resourceDTOs[0], resourceDTO ->
							resourceDTO.serviceId == resourceServiceId
									&& Objects.equals(resourceDTO.prefix, "/www")
									&& Objects.equals(resourceDTO.patterns[0], "/")
									&& resourceDTO.servletContextId == customContextMappingServiceId);
		}
	}

	@Test
	public void testRuntimeDtoWithFailedServices() throws Exception {
		// add a ServletContextHelper with wrong path
		Dictionary<String, String> props = new Hashtable<>(1);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "FailedContextName");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "bad/bad");
		ServiceRegistration<ServletContextHelper> failedContextReg =
				context.registerService(ServletContextHelper.class, new InvalidServletContextHelper(), props);
		long serviceIdFailedContext = (Long) failedContextReg.getReference().getProperty(Constants.SERVICE_ID);

		// add a Servlet with missing properties
		props = new Hashtable<>(1);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "FailedServletName");
		ServiceRegistration<Servlet> failedServletReg =
				context.registerService(Servlet.class, new InvalidServlet(), props);
		long serviceIdFailedServlet = (Long) failedServletReg.getReference().getProperty(Constants.SERVICE_ID);

		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);

		// Test all failed elements
		assertEquals("Incorrect number of failed ServletContext DTOs", 1, runtimeDTO.failedServletContextDTOs.length);
		assertThat("Invalid ServletContext doesn't match",
				runtimeDTO.failedServletContextDTOs[0],
				failedServletContextDTO -> failedServletContextDTO.serviceId == serviceIdFailedContext);
		assertEquals("Incorrect number of invalid ServletContexts", 1, runtimeDTO.failedServletDTOs.length);
		assertThat("Invalid ServletContext doesn't match",
				runtimeDTO.failedServletDTOs[0],
				failedServletDTO -> failedServletDTO.serviceId == serviceIdFailedServlet);
	}

	@Test
	public void testRequestInfoDto() throws Exception {
		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);
		Optional<ServletContextDTO> defaultContext = Arrays.stream(runtimeDTO.servletContextDTOs)
				.filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "default"))
				.findFirst();
		final long defaultContextServiceId = defaultContext.map(contextDTO -> contextDTO.serviceId).orElse(-1L);

		RequestInfoDTO requestInfoDTO = withService(
				httpServiceRuntime -> httpServiceRuntime.calculateRequestInfoDTO("/simple-servlet"));

		assertEquals("Path doesn't match", "/simple-servlet", requestInfoDTO.path);
		assertEquals("ServletContext-ServiceID doesn't match", requestInfoDTO.servletContextId, defaultContextServiceId);
		assertThat("ServletDTO doesn't match",
				requestInfoDTO.servletDTO, servletDTO ->
						Objects.equals(servletDTO.patterns[0], "/simple-servlet")
								&& Objects.equals(servletDTO.name, "SimpleServlet"));
		assertThat("FilterDTO doesn't match",
				requestInfoDTO.filterDTOs[0], filterDTO ->
						Objects.equals(filterDTO.patterns[0], "/simple-servlet")
								&& Objects.equals(filterDTO.name, "SimpleFilter"));
		assertNull("ResourceDTO should be null, because servlet matches the request", requestInfoDTO.resourceDTO);
	}

	@Test
	public void testRequestInfoDtoCustomContext() throws Exception {
		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);
		Optional<ServletContextDTO> customContext = Arrays.stream(runtimeDTO.servletContextDTOs)
				.filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "CustomContext"))
				.findFirst();
		final long customContextServiceId = customContext.map(contextDTO -> contextDTO.serviceId).orElse(-1L);

		RequestInfoDTO requestInfoDTO = withService(
				httpServiceRuntime -> httpServiceRuntime.calculateRequestInfoDTO("/context/servlet"));

		assertEquals("Path doesn't match", "/context/servlet", requestInfoDTO.path);
		assertEquals("ServletContext-ServiceID doesn't match", requestInfoDTO.servletContextId, customContextServiceId);
		assertThat("ServletDTO doesn't match",
				requestInfoDTO.servletDTO,
				servletDTO -> Objects.equals(servletDTO.patterns[0], "/servlet"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDTOServiceProperties() throws Exception {
		ServiceReference<HttpServiceRuntime> ref = getServiceReference(context, HttpServiceRuntime.class, null);

		assertNotNull("HttpServiceRuntime reference shall not be null", ref);

		ServiceReference<HttpService> serviceReference = getServiceReference(context, HttpService.class, null);

		assertNotNull("HttpService reference shall not be null", serviceReference);

		Long serviceId = (Long) serviceReference.getProperty("service.id");

		String[] endpoints = (String[]) ref.getProperty(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT);
		List<Long> serviceIds = (List<Long>) ref.getProperty(HttpServiceRuntimeConstants.HTTP_SERVICE_ID);

		assertTrue("HttpServiceIDs shall contain service ID from HttpContext", serviceIds.contains(serviceId));
		assertNotNull("endpoint shall be not null", endpoints);
		assertTrue("endpoint shall be not null", endpoints[0].length() > 0);
		assertTrue("endpoint should be bound to 0.0.0.0:8181", endpoints[0].contains("0.0.0.0:8181"));
	}

	private <T> T withService(Function<HttpServiceRuntime, T> function) throws InterruptedException {
		T result = null;
		ServiceReference<HttpServiceRuntime> ref = getServiceReference(context, HttpServiceRuntime.class, null);
		if (ref != null) {
			HttpServiceRuntime service = context.getService(ref);
			if (service != null) {
				result = function.apply(service);
				context.ungetService(ref);
			}
		}
		return result;
	}

	private <T> ServiceReference<T> getServiceReference(BundleContext context, Class<T> clazz, String filter) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final org.osgi.framework.Filter serviceFilter;
		try {
			serviceFilter = filter != null ? context.createFilter(filter) : null;
		} catch (InvalidSyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
		final AtomicReference<ServiceReference<T>> ref = new AtomicReference<>();
		ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(context, clazz, null) {
			@Override
			public T addingService(ServiceReference<T> reference) {
				T service = super.addingService(reference);
				if (serviceFilter == null || serviceFilter.match(reference)) {
					ref.set(reference);
					latch.countDown();
				}
				return service;
			}
		};
		tracker.open();
		try {
			if (latch.await(5, TimeUnit.SECONDS)) {
				return ref.get();
			}
			return new EmptyServiceReference<>();
		} finally {
			tracker.close();
		}
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
		private static final long serialVersionUID = 1L;
	}

	private static class EmptyServiceReference<T> implements ServiceReference<T> {
		@Override
		public Object getProperty(String key) {
			return "<no value>";
		}

		@Override
		public String[] getPropertyKeys() {
			return new String[0];
		}

		@Override
		public Bundle getBundle() {
			return null;
		}

		@Override
		public Bundle[] getUsingBundles() {
			return new Bundle[0];
		}

		@Override
		public boolean isAssignableTo(Bundle bundle, String className) {
			return false;
		}

		@Override
		public int compareTo(Object reference) {
			return 0;
		}
	}

}
