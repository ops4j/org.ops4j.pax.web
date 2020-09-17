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
package org.ops4j.pax.web.itest.container.war;

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;


/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
public abstract class AbstractServletAnnotatedIntegrationTest extends AbstractContainerTestBase {

//	@Before
//	public void setUp() throws Exception {
//		waitForServer("http://127.0.0.1:8181/");
//		initServletListener("test");
//		waitForServletListener();
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//	}
//
//
//	@Test
//	public void testBundle1() throws Exception {
//		HttpTestClientFactory.createDefaultTestClient()
//				.withResponseAssertion("Response must contain 'TEST OK'",
//						resp -> resp.contains("TEST OK"))
//				.doGETandExecuteTest("http://127.0.0.1:8181/annotatedTest/test");
//	}
//
//	@Test
//	@Ignore("Find a way for multipart-post with jetty-client")
//	public void testMultipart() throws Exception {
//		// TODO Mutipart
////		Map<String, Object> multiPartContent = new HashMap<String, Object>();
////		multiPartContent.put("exampleFile", "file.part");
////		testClient.testPostMultipart("http://127.0.0.1:8181/annotatedTest/multipartest", multiPartContent , "Part of file: exampleFile", 200);
//	}
}
