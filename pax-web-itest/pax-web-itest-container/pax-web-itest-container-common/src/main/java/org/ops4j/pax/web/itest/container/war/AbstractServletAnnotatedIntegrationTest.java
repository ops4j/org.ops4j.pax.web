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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.ops4j.pax.tinybundles.TinyBundles;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.AnnotatedMultipartTestServlet;
import org.ops4j.pax.web.itest.utils.web.AnnotatedTestServlet;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Constants;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;

/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
public abstract class AbstractServletAnnotatedIntegrationTest extends AbstractContainerTestBase {

	protected UrlProvisionOption theWab() {
		return streamBundle(TinyBundles.bundle()
				.addClass(AnnotatedTestServlet.class)
				.addClass(AnnotatedMultipartTestServlet.class)
				.setHeader(Constants.BUNDLE_SYMBOLICNAME, "AnnotatedServletTest")
				.setHeader(PaxWebConstants.HEADER_CONTEXT_PATH, "/annotatedTest")
				.setHeader(Constants.IMPORT_PACKAGE, "jakarta.servlet")
				.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*")
				.build()).noStart();
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeployment(() -> {
			bundle("AnnotatedServletTest").stop();
			bundle("AnnotatedServletTest").start();
		});
	}

	@Test
	public void testServlet() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'", resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/annotatedTest/test");
	}

	@Test
	public void testMultipart() throws Exception {
		Map<String, byte[]> attachments = new HashMap<>();
		attachments.put("exampleFile", "file.part".getBytes(StandardCharsets.UTF_8));
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Part of file: exampleFile'",
						resp -> resp.contains("Part of file: exampleFile"))
				.doPOST("http://127.0.0.1:8181/annotatedTest/multipartest", attachments)
				.executeTest();
	}

}
