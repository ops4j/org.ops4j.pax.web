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

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;

public abstract class AbstractWhiteboardR6DtoIntegrationTest extends AbstractContainerTestBase {

//	@Inject
//	@Filter(timeout = 20000)
//	private WebContainer webcontainer;
//
//	@Inject
//	private BundleContext bundleContext;
//
//	@Before
//	public void setUp() throws Exception {
//		initServletListener();
//		waitForServletListener();
//		waitForServer("http://127.0.0.1:8181/");
//	}
//
//	/**
//	 * This test just makes sure that the sample is working as expected for the other tests in this class.
//	 */
//	@Test
//	public void testAllSamplesRegisteredAsExpected() throws Exception {
//		// test simple-servlet without additional context, but with filter
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
//						resp -> resp.contains("Hello from SimpleServlet"))
//				.withResponseAssertion("Response must contain 'Request changed by SimpleFilter'",
//						resp -> resp.contains("Request changed by SimpleFilter"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
//
//		// test custom-servlet with additional context
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from ServletWithContext'",
//						resp -> resp.contains("Hello from ServletWithContext"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
//
//		// test error-page
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(404)
//				.withResponseAssertion("Response must contain 'Error Servlet, we do have a 404'",
//						resp -> resp.contains("Error Servlet, we do have a 404"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/error");
//
//		// test resource
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseHeaderAssertion("Header 'Content-Type' must be 'plain/text'",
//						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
//								&& header.getValue().equals("text/plain")))
//				.doGETandExecuteTest("http://127.0.0.1:8181/resources/file.txt");
//
//		// Pax-Web specific whiteboard features...not relevant for DTOs
//
//		// test servlet on HttpContext-service
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello from " + PaxWebWhiteboardServletMapping.class.getName() + "'",
//						resp -> resp.contains("Hello from " + PaxWebWhiteboardServletMapping.class.getName()))
//				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/servlet-mapping");
//
//		//The registration of the HttpContext Mapping and the combination of WelcomeFile+ErrorPage doesn't work right now
//		//The context though is available.
//
//		// test welcome-page on HttpContextMapping-service
////		HttpTestClientFactory.createDefaultTestClient()
////				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
////						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
////				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context-mapping/");
//
//		// test error-page on HttpContextMapping-service
////		HttpTestClientFactory.createDefaultTestClient()
////				.withReturnCode(404)
////				.withResponseAssertion("Response must contain 'Whoops, there was a 404.'",
////						resp -> resp.contains("Whoops, there was a 404."))
////				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context-mapping/not-available");		//this can't work, because of error servlet handler ... as no context will match, default error will take care.
//	}
//
//
//	private <T> T withService(Function<HttpServiceRuntime, T> function) throws InterruptedException {
//		T result = null;
//		ServiceReference<HttpServiceRuntime> ref = getServiceReference(bundleContext, HttpServiceRuntime.class, null);
//		if (ref != null) {
//			HttpServiceRuntime service = bundleContext.getService(ref);
//			if (service != null) {
//				result = function.apply(service);
//				bundleContext.ungetService(ref);
//			}
//		}
//		return result;
//	}
//
//	/**
//	 * Tests the DTO structure for the Whiteboard-Services registered by sample whiteboard-ds
//	 */
//	@Test
//	public void testRuntimeDto() throws Exception {
//		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);
//
//		// prepare ServiceIDs for comparrison
//		final long servletServiceId = (long)getServiceReference(bundleContext, WhiteboardServlet.class, null).getProperty(Constants.SERVICE_ID);
//		final long servletWithContextServiceId = (long)getServiceReference(bundleContext, WhiteboardServletWithContext.class, null).getProperty(Constants.SERVICE_ID);
//		final long defaultServletContextServiceId = (long)getServiceReference(bundleContext, ServletContext.class, "(" + WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME + "=default)").getProperty(Constants.SERVICE_ID);
//		final long customServletContextServiceId = (long)getServiceReference(bundleContext, ServletContext.class, "(" + WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME + "=CustomContext)").getProperty(Constants.SERVICE_ID);
//		final long filterServiceId = (long)getServiceReference(bundleContext, WhiteboardFilter.class, null).getProperty(Constants.SERVICE_ID);
//		final long listenerServiceId = (long)getServiceReference(bundleContext, WhiteboardListener.class, null).getProperty(Constants.SERVICE_ID);
//		final long resourceServiceId = (long)getServiceReference(bundleContext, WhiteboardResource.class, null).getProperty(Constants.SERVICE_ID);
//		final long errorPageServiceId = (long)getServiceReference(bundleContext, WhiteboardErrorPage.class, null).getProperty(Constants.SERVICE_ID);
//
//		assertThat("Default- and CustomServletContextDTO must be available:" + runtimeDTO.servletContextDTOs.length,
//				runtimeDTO.servletContextDTOs,
//				servletContextDTOs -> servletContextDTOs.length >= 3);
//
//		Optional<ServletContextDTO> defaultContext = Arrays.stream(runtimeDTO.servletContextDTOs)
//				.filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "default"))
//				.findFirst();
//        Optional<ServletContextDTO> customContext = Arrays.stream(runtimeDTO.servletContextDTOs)
//                .filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "CustomContext"))
//                .findFirst();
////        Optional<ServletContextDTO> customContextMapping = Arrays.stream(runtimeDTO.servletContextDTOs)
////                .filter(servletContextDTO -> Objects.equals(servletContextDTO.name, "CustomHttpContextMapping"))
////                .findFirst();
//
//		// Test all under Default-ServletContext
//		if (!defaultContext.isPresent()) {
//			fail("DefaultContext not found");
//		} else {
//			assertThat("ServletContextDTO for DefaultServletContext doesn't match!",
//					defaultContext.get(),
//					servletContextDTO -> Objects.equals(servletContextDTO.contextPath, "/"));
//			assertThat("There should be exactly one Servlet in the DefaultServletContext!",
//					defaultContext.get(),
//					servletContextDTO -> servletContextDTO.servletDTOs.length == 1);
//			assertThat("There should be exactly one Filter in the DefaultServletContext!",
//					defaultContext.get(),
//					servletContextDTO -> servletContextDTO.filterDTOs.length == 1);
//			assertThat("There should be exactly one Resource in the DefaultServletContext!",
//					defaultContext.get(),
//					servletContextDTO -> servletContextDTO.resourceDTOs.length == 1);
//			assertThat("There should be exactly one ErrorPage in the DefaultServletContext!",
//					defaultContext.get(),
//					servletContextDTO -> servletContextDTO.errorPageDTOs.length == 1);
//			assertThat("ServletDTO for WhiteboardServlet doesn't match!",
//					defaultContext.get().servletDTOs[0], servletDTO ->
//							Objects.equals(servletDTO.name, "SimpleServlet")
//									&& Objects.equals(servletDTO.patterns[0], "/simple-servlet")
//									&& servletDTO.serviceId == servletServiceId
//									&& servletDTO.servletContextId == defaultServletContextServiceId);
//			assertThat("FilterDTO for WhiteboardFilter doesn't match!",
//					defaultContext.get().filterDTOs[0],
//					filterDTO -> filterDTO.serviceId == filterServiceId
//							&& filterDTO.servletContextId == defaultServletContextServiceId
//							&& Objects.equals(filterDTO.name, "SimpleFilter"));
//			assertThat("ResourceDTO for WhiteboardResource doesn't match!",
//					defaultContext.get().resourceDTOs[0], resourceDTO ->
//							resourceDTO.serviceId == resourceServiceId
//									&& Objects.equals(resourceDTO.prefix, "/www")
//									&& Objects.equals(resourceDTO.patterns[0], "/resources")
//									&& resourceDTO.servletContextId == defaultServletContextServiceId);
//			assertThat("ErrorPageDTO for WhiteboardErrorPage doesn't match!",
//					defaultContext.get().errorPageDTOs[0],
//					errorPageDTO -> errorPageDTO.serviceId == errorPageServiceId
//							&& Objects.equals(errorPageDTO.exceptions[0], "java.io.IOException")
////							&& errorPageDTO.errorCodes[0] == 404 FIXME errorCodes currently not mapped
//							&& errorPageDTO.servletContextId == defaultServletContextServiceId);
//		}
//		// Test all under Custom-ServletContext
//		if (!customContext.isPresent()) {
//			fail("CustomContext not found");
//		} else {
//			assertThat("ServletContextDTO for WhiteboardContext doesn't match!",
//					customContext.get(), servletContextDTO -> Objects.equals(servletContextDTO.contextPath, "/context")
//							&& servletContextDTO.serviceId == customServletContextServiceId);
//			assertThat("There should be exactly one Servlet in the CustomServletContext!",
//					customContext.get(),
//					servletContextDTO -> servletContextDTO.servletDTOs.length == 1);
//			assertThat("There should be exactly one Listener in the CustomServletContext!",
//					customContext.get(),
//					servletContextDTO -> servletContextDTO.listenerDTOs.length == 1);
//			assertThat("ServletDTO for WhiteboardServletWithContext doesn't match!",
//					customContext.get().servletDTOs[0], servletDTO ->
//							Objects.equals(servletDTO.name, "ServletWithContext")
//									&& Objects.equals(servletDTO.patterns[0], "/servlet")
//									&& servletDTO.serviceId == servletWithContextServiceId
//									&& servletDTO.servletContextId == customServletContextServiceId);
//			assertThat("ListenerDTO for WhiteboardListener doesn't match!",
//					customContext.get().listenerDTOs[0], listenerDTO -> listenerDTO.serviceId == listenerServiceId
//							&& listenerDTO.servletContextId == customServletContextServiceId
//							&& Objects.equals(listenerDTO.types[0], ServletRequestListener.class.getName()));
//		}
//
//		//TODO: check CustomHttpContextMapping
//	}
//
//	@Test
//	public void testRuntimeDtoWithFailedServices() throws Exception {
//		// add a ServletContextHelper with missing path
//		Dictionary<String, String> props = new Hashtable<>(1);
//		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "FailedContextName");
//		ServiceRegistration<ServletContextHelper> failedContextReg =
//				bundleContext.registerService(ServletContextHelper.class, new InvalidServletContextHelper(), props);
//		long serviceIdFailedContext = (Long) failedContextReg.getReference().getProperty(Constants.SERVICE_ID);
//
//		// add a Servlet with missing properties
//		props = new Hashtable<>(1);
//		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "FailedServletName");
//		ServiceRegistration<Servlet> failedServletReg =
//				bundleContext.registerService(Servlet.class, new InvalidServlet(), props);
//		long serviceIdFailedServlet = (Long) failedServletReg.getReference().getProperty(Constants.SERVICE_ID);
//
//		RuntimeDTO runtimeDTO = withService(HttpServiceRuntime::getRuntimeDTO);
//
//		// Test all failed elements
//		assertTrue("Incorrect number of failed ServletContext DTOs",
//				1 == runtimeDTO.failedServletContextDTOs.length);
//		assertThat("Invalid ServletContext doesn't match",
//				runtimeDTO.failedServletContextDTOs[0],
//				failedServletContextDTO -> failedServletContextDTO.serviceId == serviceIdFailedContext);
//		assertTrue("Incorrect number of invalid ServletContexts",
//				1 == runtimeDTO.failedServletDTOs.length);
//		assertThat("Invalid ServletContext doesn't match",
//				runtimeDTO.failedServletDTOs[0],
//				failedServletDTO -> failedServletDTO.serviceId == serviceIdFailedServlet);
//	}
//
//	@Test
//	public void testRequestInfoDto() throws Exception {
//		final long defaultServletContextServiceId = (long)getServiceReference(bundleContext, ServletContext.class, "(" + WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME + "=default)").getProperty(Constants.SERVICE_ID);
//
//		RequestInfoDTO requestInfoDTO = withService(
//				httpServiceRuntime -> httpServiceRuntime.calculateRequestInfoDTO("/simple-servlet"));
//
//		assertTrue("Path doesn't match",
//				Objects.equals(requestInfoDTO.path, "/simple-servlet"));
//		assertTrue("ServletContext-ServiceID doesn't match",
//				requestInfoDTO.servletContextId == defaultServletContextServiceId);
//		assertThat("ServletDTO doesn't match",
//				requestInfoDTO.servletDTO, servletDTO ->
//						Objects.equals(servletDTO.patterns[0], "/simple-servlet")
//								&& Objects.equals(servletDTO.name, "SimpleServlet"));
//		assertThat("FilterDTO doesn't match",
//				requestInfoDTO.filterDTOs[0], filterDTO ->
//						Objects.equals(filterDTO.patterns[0], "/simple-servlet")
//								&& Objects.equals(filterDTO.name, "SimpleFilter"));
//		assertThat("ResourceDTO doesn't match",
//				requestInfoDTO.resourceDTO, resourceDTO ->
//						Objects.equals(resourceDTO.patterns[0], "/resources"));
//	}
//
//
//	@Test
//	public void testRequestInfoDto_CustomContext() throws Exception {
//		final long customServletContextServiceId = (long)bundleContext.getServiceReferences(ServletContext.class, "(" + WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME + "=CustomContext)").stream().findFirst().orElseThrow(() -> new AssertionError("CustomContext ServletContext not found")).getProperty(Constants.SERVICE_ID);
//
//		RequestInfoDTO requestInfoDTO = withService(
//				httpServiceRuntime -> httpServiceRuntime.calculateRequestInfoDTO("/context/servlet"));
//
//		assertTrue("Path doesn't match",
//				Objects.equals(requestInfoDTO.path, "/context/servlet"));
//		assertTrue("ServletContext-ServiceID doesn't match",
//				requestInfoDTO.servletContextId == customServletContextServiceId);
//		assertThat("ServletDTO doesn't match",
//				requestInfoDTO.servletDTO,
//				servletDTO -> Objects.equals(servletDTO.patterns[0], "/servlet"));
//	}
//
//	@Test
//	@SuppressWarnings("unchecked")
//	public void testDTOServiceProperties() throws Exception {
//		ServiceReference<HttpServiceRuntime> ref = getServiceReference(bundleContext, HttpServiceRuntime.class, null);
//
//		assertTrue("HttpServiceRuntime reference shall not be null", ref != null);
//
//		ServiceReference<HttpService> serviceReference = getServiceReference(bundleContext, HttpService.class, null);
//
//		assertTrue("HttpService reference shall not be null", serviceReference != null);
//
//		Long serviceId = (Long) serviceReference.getProperty("service.id");
//
//		String endpoint = (String) ref.getProperty(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT);
//		List<Long> serviceIds = (List<Long>) ref.getProperty(HttpServiceRuntimeConstants.HTTP_SERVICE_ID);
//
//		assertTrue("HttpServiceIDs shall contain service ID from HttpContext", serviceIds.contains(serviceId));
//		assertTrue("endpoint shall be not null", endpoint != null);
//		assertTrue("endpoint shall be not null", endpoint.length() > 0);
//		assertTrue("endpoint should be bound to 0.0.0.0:8181", endpoint.contentEquals("0.0.0.0:8181"));
//	}
//
//	private <T> ServiceReference<T> getServiceReference(BundleContext bundleContext, Class<T> clazz, String filter) throws InterruptedException {
//		final CountDownLatch latch = new CountDownLatch(1);
//		final org.osgi.framework.Filter serviceFilter;
//		try {
//			serviceFilter = filter != null ? bundleContext.createFilter(filter) : null;
//		} catch (InvalidSyntaxException ex) {
//			throw new IllegalArgumentException(ex);
//		}
//		final AtomicReference<ServiceReference<T>> ref = new AtomicReference<>();
//		ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(bundleContext, clazz, null) {
//			@Override
//			public T addingService(ServiceReference<T> reference) {
//				T service = super.addingService(reference);
//				if (serviceFilter == null || serviceFilter.match(reference)) {
//					ref.set(reference);
//					latch.countDown();
//				}
//				return service;
//			}
//		};
//		tracker.open();
//		try {
//			if (latch.await(5, TimeUnit.SECONDS)) {
//				return ref.get();
//			}
//			return new EmptyServiceReference<>();
//		} finally {
//			tracker.close();
//		}
//	}
//
//	/**
//	 * This ServletContextHelper is supposed to be registered with missing properties
//	 */
//	private static final class InvalidServletContextHelper extends ServletContextHelper {
//
//	}
//
//
//	/**
//	 * This Servlet is supposed to be registered with missing properties
//	 */
//	private static final class InvalidServlet extends HttpServlet {
//		private static final long serialVersionUID = 1L;
//	}
//
//	private static class EmptyServiceReference<T> implements ServiceReference<T> {
//		@Override
//		public Object getProperty(String key) {
//			return "<no value>";
//		}
//
//		@Override
//		public String[] getPropertyKeys() {
//			return new String[0];
//		}
//
//		@Override
//		public Bundle getBundle() {
//			return null;
//		}
//
//		@Override
//		public Bundle[] getUsingBundles() {
//			return new Bundle[0];
//		}
//
//		@Override
//		public boolean isAssignableTo(Bundle bundle, String className) {
//			return false;
//		}
//
//		@Override
//		public int compareTo(Object reference) {
//			return 0;
//		}
//	}

}
