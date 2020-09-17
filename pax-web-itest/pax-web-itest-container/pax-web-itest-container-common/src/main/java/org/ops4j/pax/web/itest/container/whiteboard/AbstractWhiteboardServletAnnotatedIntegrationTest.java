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
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
public abstract class AbstractWhiteboardServletAnnotatedIntegrationTest extends AbstractContainerTestBase {

//	@Before
//	public void setUp() throws Exception {
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//	}
//
//	@Test
//	public void testWhiteboardServletRegistration() throws Exception {
//		initServletListener();
//		ServiceRegistration<Servlet> servletRegistration = bundleContext
//				.registerService(Servlet.class, new AnnotatedTestServlet(),
//						null);
//		waitForServletListener();
//
//		try {
//			HttpTestClientFactory.createDefaultTestClient()
//					.withResponseAssertion("Response must contain 'TEST OK'",
//							resp -> resp.contains("TEST OK"))
//					.doGETandExecuteTest("http://127.0.0.1:8181/test");
//		} finally {
//			servletRegistration.unregister();
//		}
//
//	}
//
//	@Test
//	public void testWhiteboardServletRegistrationDestroyCalled() throws Exception {
//
//		AnnotatedTestServlet annotatedTestServlet = new AnnotatedTestServlet();
//
//		initServletListener();
//		ServiceRegistration<Servlet> servletRegistration = bundleContext
//				.registerService(Servlet.class, annotatedTestServlet,
//						null);
//		waitForServletListener();
//
//		try {
//			HttpTestClientFactory.createDefaultTestClient()
//					.withResponseAssertion("Response must contain 'TEST OK'",
//							resp -> resp.contains("TEST OK"))
//					.doGETandExecuteTest("http://127.0.0.1:8181/test");
//		} finally {
//			servletRegistration.unregister();
//		}
//
//		assertThat(annotatedTestServlet.isInitCalled(), is(true));
//		assertThat(annotatedTestServlet.isDestroyCalled(), is(true));
//	}
//
//	@Test
//	public void testWhiteboardFilterRegistration() throws Exception {
//
//		initServletListener();
//		ServiceRegistration<Servlet> servletRegistration = bundleContext
//				.registerService(Servlet.class, new AnnotatedTestServlet(),
//						null);
//
//		ServiceRegistration<Filter> filterRegistration = bundleContext
//				.registerService(Filter.class, new AnnotatedTestFilter(), null);
//		waitForServletListener();
//
//		try {
//			HttpTestClientFactory.createDefaultTestClient()
//					.withResponseAssertion("Response must contain 'TEST OK'",
//							resp -> resp.contains("TEST OK"))
//					.withResponseAssertion("Response must contain 'FILTER-INIT: true'",
//							resp -> resp.contains("FILTER-INIT: true"))
//					.doGETandExecuteTest("http://127.0.0.1:8181/test");
//		} finally {
//			servletRegistration.unregister();
//			filterRegistration.unregister();
//		}
//	}

}
