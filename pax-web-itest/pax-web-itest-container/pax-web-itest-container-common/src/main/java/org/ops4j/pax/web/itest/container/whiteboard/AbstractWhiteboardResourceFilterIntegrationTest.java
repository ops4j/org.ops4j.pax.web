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

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.SimpleFilter;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import static org.junit.Assert.assertNotNull;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardResourceFilterIntegrationTest extends AbstractContainerTestBase {

	private ServiceRegistration<Servlet> service;

	@Before
	public void setUp() throws Exception {
		context.installBundle(sampleURI("whiteboard"));
		configureAndWaitForServletWithMapping("/test-resources/*", () -> {
			Dictionary<String, String> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/test-resources/*");
			service = context.registerService(Servlet.class, new WhiteboardServlet("/test-resources"), props);
		});
	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		Dictionary<String, String> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		SimpleFilter simpleFilter = new SimpleFilter();
		ServiceRegistration<Filter> filter = context.registerService(Filter.class, simpleFilter, props);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-resources");

		URL resource = simpleFilter.getResource();
		assertNotNull(resource);

		filter.unregister();
	}

}
