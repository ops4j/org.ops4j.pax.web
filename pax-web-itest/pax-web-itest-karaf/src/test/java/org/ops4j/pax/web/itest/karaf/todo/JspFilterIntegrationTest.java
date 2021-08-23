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
package org.ops4j.pax.web.itest.karaf.todo;


import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.karaf.AbstractKarafTestBase;

/**
 * @author Achim Nierbeck
 */

@RunWith(PaxExam.class)
public class JspFilterIntegrationTest extends AbstractKarafTestBase {

//	private static final Logger LOG = LoggerFactory.getLogger(JspFilterIntegrationTest.class);
//
//	private Bundle installWarBundle;
//
//	@Configuration
//	public Option[] config() {
//		return jettyConfig();
//	}
//
//
//	@Before
//	public void setUp() throws BundleException, InterruptedException {
//		initWebListener();
//		final String bundlePath = "mvn:org.ops4j.pax.web.samples/jsp-filter/"
//				+ VersionUtil.getProjectVersion() + "/war";
//		installWarBundle = installAndStartBundle(bundlePath);
//		waitForWebListener();
//	}
//
//	@After
//	public void tearDown() throws BundleException {
//		if (installWarBundle != null) {
//			installWarBundle.stop();
//			installWarBundle.uninstall();
//		}
//	}
//
//
//	@Test
//	public void testSimpleJsp() throws Exception {
//	    createTestClientForKaraf()
//				.withResponseAssertion("Response must contain 'Filtered'",
//						resp -> resp.contains("Filtered"))
//				.doGETandExecuteTest("http://localhost:8181/jsp-filter/");
//	}
//
//	@Test
//	public void testExplicitTagLib() throws Exception {
//	    createTestClientForKaraf()
//				.withResponseAssertion("Response must contain 'works'",
//						resp -> resp.contains("core taglib works\n" +
//								"<body>"))
//				.doGETandExecuteTest("http://localhost:8181/jsp-filter/test-taglib.jsp");
//	}
//
//	@Test
//	@Ignore("PAXWEB-1070: Tags aren't interpreted correctly")
//	public void testAutoIncludedTagLib() throws Exception {
//	    createTestClientForKaraf()
//				.withResponseAssertion("Response must contain 'core taglib works'",
//						resp -> resp.contains("core taglib works\n" +
//								"<body>"))
//				.doGETandExecuteTest("http://localhost:8181/jsp-filter/test-taglib-inc.jsp");
//	}
}
