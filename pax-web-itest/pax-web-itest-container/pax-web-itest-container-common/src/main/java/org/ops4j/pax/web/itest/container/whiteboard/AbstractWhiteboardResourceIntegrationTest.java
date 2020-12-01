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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.whiteboard.ResourceMapping;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardResourceIntegrationTest extends AbstractContainerTestBase {

	private ServiceRegistration<ResourceMapping> service;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/whiteboardresources/*", () -> {
			DefaultResourceMapping resourceMapping = new DefaultResourceMapping();
			resourceMapping.setAlias("/whiteboardresources");
			resourceMapping.setPath("/images");
			service = context.registerService(ResourceMapping.class, resourceMapping, null);
		});
	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Header 'Content-Type' must be 'image/png'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboardresources/ops4j.png");
	}

}
