/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.container.whiteboard;

import java.util.Arrays;
import javax.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardIntegrationTest extends AbstractContainerTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractWhiteboardIntegrationTest.class);

	private Bundle bundle;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/",
				() -> bundle = installAndStartBundle(sampleURI("whiteboard")));
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle != null) {
			bundle.stop();
			bundle.uninstall();
		}
	}

	@Test
	public void testWhiteboardRoot() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");
	}

	@Test
	public void testWhiteboardSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to the Welcome page'",
						resp -> resp.contains("Welcome to the Welcome page"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

	@Test
	public void testWhiteboardForbidden() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.doGETandExecuteTest("http://127.0.0.1:8181/forbidden");
	}

	@Test
	public void testWhiteboardFiltered() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter was there before'",
						resp -> resp.contains("Filter was there before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/filtered");
	}

	@Test
	public void testWhiteboardSecondFilter() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter was there before'",
						resp -> resp.contains("Filter was there before"))
				.withResponseAssertion("Response must contain 'SecondFilter - filtered'",
						resp -> resp.contains("SecondFilter - filtered"))
				.doGETandExecuteTest("http://127.0.0.1:8181/second");
	}

	@Test
	public void testWhiteboardFilteredInitialized() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Have bundle context in filter: true'",
						resp -> resp.contains("Have bundle context in filter: true"))
				.doGETandExecuteTest("http://127.0.0.1:8181/filtered");
	}

	@Test
	public void testImage() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Header 'Content-Type' must be 'image/png'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/images/ops4j.png");
	}

	@Test
	public void test404() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain '<title>Default 404 page</title>'",
						resp -> resp.contains("<title>Default 404 page</title>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/doesNotExist");
	}

	@Test
	public void testResourceMapping() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Header 'Content-Type' must be 'image/png'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboardresources/ops4j.png");
	}

	@Test
	public void testJspMapping() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jsp/simple.jsp");
	}

	@Test
	public void testTldJsp() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jsp/using-tld.jsp");
	}

	@Test
	public void testMultipleContextMappings() throws Exception {
		BundleContext bundleContext = bundle.getBundleContext();
		DefaultHttpContextMapping httpContextMapping = new DefaultHttpContextMapping();
		httpContextMapping.setContextId("alternative");
		httpContextMapping.setContextPath("/alternative");
		ServiceRegistration<HttpContextMapping> httpContextMappingRegistration
				= bundleContext.registerService(HttpContextMapping.class, httpContextMapping, null);
		try {
			Servlet servlet = new WhiteboardServlet("/alias");
			DefaultServletMapping servletMapping = new DefaultServletMapping();
			servletMapping.setServlet(servlet);
			servletMapping.setAlias("/alias");
			servletMapping.setContextId(httpContextMapping.getContextId());
			ServiceRegistration<ServletMapping> servletRegistration
					= bundleContext.registerService(ServletMapping.class, servletMapping, null);
			try {
				HttpTestClientFactory.createDefaultTestClient()
						.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
								resp -> resp.contains("Hello Whiteboard Extender"))
						.doGETandExecuteTest("http://127.0.0.1:8181/alternative/alias");
			} finally {
				servletRegistration.unregister();
			}
		} finally {
			httpContextMappingRegistration.unregister();
		}
	}

	@Test
	@Ignore("DTOs do not work yet in Pax Web 8")
	public void testMultipleContextMappingsWithDTOsCheck() throws Exception {
		BundleContext bundleContext = bundle.getBundleContext();
		DefaultHttpContextMapping httpContextMapping = new DefaultHttpContextMapping();
		httpContextMapping.setContextId("dtoCheck");
		httpContextMapping.setContextPath("dtocheck");
		ServiceRegistration<HttpContextMapping> httpContextMappingRegistration = bundleContext
				.registerService(HttpContextMapping.class,
						httpContextMapping, null);
		try {
			Servlet servlet = new WhiteboardServlet("/dtocheck");
			DefaultServletMapping servletMapping = new DefaultServletMapping();
			servletMapping.setServlet(servlet);
			servletMapping.setAlias("/dtocheck");
			String httpContextId = httpContextMapping.getContextId();
			servletMapping.setContextId(httpContextId);
			ServiceRegistration<ServletMapping> servletRegistration = bundleContext
					.registerService(ServletMapping.class,
							servletMapping, null);

			ServiceReference<HttpServiceRuntime> serviceReference = bundleContext.getServiceReference(HttpServiceRuntime.class);
			try {
				HttpTestClientFactory.createDefaultTestClient()
						.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
								resp -> resp.contains("Hello Whiteboard Extender"))
						.doGETandExecuteTest("http://127.0.0.1:8181/dtocheck/dtocheck");

				HttpServiceRuntime httpServiceRuntime = bundleContext.getService(serviceReference);

				RuntimeDTO runtimeDTO = httpServiceRuntime.getRuntimeDTO();

				assertEquals(0, runtimeDTO.failedServletContextDTOs.length);

				assertEquals(2, runtimeDTO.servletContextDTOs.length);

				long count = Arrays.stream(runtimeDTO.servletContextDTOs).filter(servletContext -> servletContext.name.equalsIgnoreCase("dtoCheck")).count();

				assertEquals(1, count);
			} finally {
				bundleContext.ungetService(serviceReference);
				servletRegistration.unregister();
			}
		} finally {
			httpContextMappingRegistration.unregister();
		}
	}

}
