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

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardDeregisteringContextIntegrationTest extends AbstractContainerTestBase {

//	@Rule
//	public CatchAllExceptionsRule catchAllExceptionsRule = new CatchAllExceptionsRule();
//
//	private ServiceReference<WebContainer> serviceReference;
//	private WebContainer webContainerService;
//
//	public static Option[] configureWhiteboard() {
//		return options(
//				mavenBundle().groupId("org.ops4j.pax.web.samples")
//						.artifactId("whiteboard").version(VersionUtil.getProjectVersion())
//						.noStart(),
//				mavenBundle().groupId("com.cedarsoft.commons").artifactId("test-utils").versionAsInProject(),
//				mavenBundle().groupId("com.cedarsoft.commons").artifactId("crypt").versionAsInProject(),
//				mavenBundle().groupId("com.cedarsoft.commons").artifactId("xml-commons").versionAsInProject(),
//				mavenBundle("commons-codec", "commons-codec").versionAsInProject(),
//				mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-core").version("2.9.10"),
//				mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-databind").version("2.9.10.2"),
//				mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-annotations").version("2.9.10"),
//				mavenBundle().groupId("com.google.guava").artifactId("guava").version("15.0"),
//
//				wrappedBundle(mavenBundle("org.mockito", "mockito-core").version("1.9.5")),
//				wrappedBundle(mavenBundle("joda-time", "joda-time").version("2.3")),
//				wrappedBundle(mavenBundle("org.objenesis", "objenesis").version("1.4")),
//				wrappedBundle(mavenBundle("org.easytesting", "fest-assert").version("1.4")),
//				wrappedBundle(mavenBundle("org.easytesting", "fest-reflect").version("1.4")),
//				wrappedBundle(mavenBundle("xmlunit", "xmlunit").version("1.5")),
//				wrappedBundle(mavenBundle("commons-io", "commons-io").version(asInProject())));
//	}
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException,
//			UnavailableException {
//		serviceReference = bundleContext
//				.getServiceReference(WebContainer.class);
//
//		while (serviceReference == null) {
//			serviceReference = bundleContext.getServiceReference(WebContainer.class);
//		}
//
//		webContainerService = (WebContainer) bundleContext
//				.getService(serviceReference);
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//		webContainerService = null;
//		if (bundleContext != null) {
//			bundleContext.ungetService(serviceReference);
//		}
//		serviceReference = null;
//	}
//
//	@Test
//	public void testDeregisterContext() throws Exception {
//		Hashtable<String, String> props = new Hashtable<>();
//		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "myContext");
//
//		HttpContext httpContext = webContainerService.createDefaultHttpContext("myContext");
//
//		ServiceRegistration<HttpContext> contextService = bundleContext.registerService(HttpContext.class, httpContext, props);
//
//		props = new Hashtable<>();
//		props.put(ExtenderConstants.PROPERTY_ALIAS, "/ungetServletTest");
//		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "myContext");
//
//		ServiceRegistration<Servlet> servletService = bundleContext.registerService(Servlet.class, new WhiteboardServlet("ungetServletTest"), props);
//
//		servletService.unregister();
//
//		contextService.unregister();
//
//	}

}
