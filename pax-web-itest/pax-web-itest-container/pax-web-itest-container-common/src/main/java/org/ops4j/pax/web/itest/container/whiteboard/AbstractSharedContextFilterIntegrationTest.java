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
public abstract class AbstractSharedContextFilterIntegrationTest extends AbstractContainerTestBase {

//	protected static final String SERVLET_BUNDLE = "ServletBundleTest";
//	protected static final String FILTER_BUNDLE = "FilterBundleTest";
//
//	@Before
//	public void setUp() throws Exception {
//		waitForServer("http://127.0.0.1:8181/");
//	}
//
//
//	@Test
//	public void testBundle1() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'Hello Whiteboard Filter'",
//						resp -> resp.contains("Hello Whiteboard Filter"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/sharedContext/");
//	}
//
//	@Test
//	public void testStop() throws Exception {
//		for (final Bundle b : bundleContext.getBundles()) {
//			if (FILTER_BUNDLE.equalsIgnoreCase(b.getSymbolicName())) {
//				b.stop();
//			}
//		}
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'SimpleServlet: TEST OK'",
//						resp -> resp.contains("SimpleServlet: TEST OK"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/sharedContext/");
//	}
//
//	@Test
//	public void testStopServletBundle() throws Exception {
//		for (final Bundle b : bundleContext.getBundles()) {
//			if (SERVLET_BUNDLE.equalsIgnoreCase(b.getSymbolicName())) {
//				b.stop();
//			}
//		}
//
//		HttpTestClientFactory.createDefaultTestClient()
//				.withReturnCode(404)
//				.doGETandExecuteTest("http://127.0.0.1:8181/sharedContext/");
//	}
}
