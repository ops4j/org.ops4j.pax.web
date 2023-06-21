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

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.ops4j.pax.tinybundles.core.InnerClassStrategy;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.TestServlet;
import org.ops4j.pax.web.itest.utils.web.TestServletContainerInitializer;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Constants;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;

/**
 * @author Marc Schlegel
 */
public abstract class AbstractWarContainerInitializerIntegrationTest extends AbstractContainerTestBase {

	protected UrlProvisionOption theWab() {
		return streamBundle(TinyBundles.bundle()
				.add(TestServlet.class, InnerClassStrategy.NONE)
				.add(TestServletContainerInitializer.class, InnerClassStrategy.NONE)
				.set(Constants.BUNDLE_SYMBOLICNAME, "war-bundle")
				.set(PaxWebConstants.HEADER_CONTEXT_PATH, "/contextroot")
				.set(Constants.IMPORT_PACKAGE, "jakarta.servlet, jakarta.servlet.annotation, jakarta.servlet.http")
				.set(Constants.DYNAMICIMPORT_PACKAGE, "*")
				.add("WEB-INF/web.xml",
						AbstractWarContainerInitializerIntegrationTest.class.getClassLoader().getResourceAsStream("web-3.0.xml"))
				.add("META-INF/services/jakarta.servlet.ServletContainerInitializer",
						AbstractWarContainerInitializerIntegrationTest.class.getClassLoader().getResourceAsStream("META-INF/services/jakarta.servlet.ServletContainerInitializer"))
				.build()).noStart();
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeployment(() -> {
			bundle("war-bundle").start();
		});
	}

	@Test
	public void testServlet30() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'!", resp -> resp.contains("TEST OK"))
				.withResponseAssertion("Filter is registered in Service-Locator for ContainerInitializer. Response must contain 'FILTER-INIT'", resp -> resp.contains("FILTER-INIT"))
				.doGETandExecuteTest("http://127.0.0.1:8181/contextroot/servlet");
	}

}
