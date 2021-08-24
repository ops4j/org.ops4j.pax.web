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
package org.ops4j.pax.web.itest.karaf;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Achim Nierbeck
 */

@RunWith(PaxExam.class)
public abstract class JspFilterBaseKarafTest extends AbstractKarafTestBase {

	private Bundle wab;

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("jsp-filter", () -> {
			installAndStartBundle(sampleWarURI("jsp-filter"));
		});
	}

	@After
	public void tearDown() throws BundleException {
		if (wab != null) {
			wab.stop();
			wab.uninstall();
		}
	}

	@Test
	public void testSimpleJsp() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain 'Filtered'",
						resp -> resp.contains("Filtered"))
				.doGETandExecuteTest("http://localhost:8181/jsp-filter/");
	}

	@Test
	public void testExplicitTagLib() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain 'works'",
						resp -> resp.contains("<span>core taglib works explicitly</span>"))
				.doGETandExecuteTest("http://localhost:8181/jsp-filter/test-taglib.jsp");
	}

	@Test
	public void testAutoIncludedTagLib() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain 'core taglib works'",
						resp -> resp.contains("<span>core taglib works implicitly</span>"))
				.doGETandExecuteTest("http://localhost:8181/jsp-filter/test-taglib-inc.jsp");
	}

}
