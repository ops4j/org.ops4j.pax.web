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
package org.ops4j.pax.web.itest.common;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RuntimeDTO;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Before
	public void setUp() throws BundleException, InterruptedException {
		// wait for the whiteboard extender runtime and the HTTP service to come up
		waitForService(bundleContext, HttpServiceRuntime.class);
		getHttpService(bundleContext);
		initServletListener();
		String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForServletListener();
		// wait for the services to come up
		Thread.sleep(2000);
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}


	@Test
	public void testWhiteBoardRoot() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to the Welcome page'",
						resp -> resp.contains("Welcome to the Welcome page"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

	@Test
	public void testWhiteBoardForbidden() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.doGETandExecuteTest("http://127.0.0.1:8181/forbidden");
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter was there before'",
						resp -> resp.contains("Filter was there before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/filtered");
	}

	@Test
	public void testWhiteBoardSecondFilter() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Filter was there before'",
						resp -> resp.contains("Filter was there before"))
				.withResponseAssertion("Response must contain 'SecondFilter - filtered'",
						resp -> resp.contains("SecondFilter - filtered"))
				.doGETandExecuteTest("http://127.0.0.1:8181/second");
	}

	@Test
	public void testWhiteBoardFilteredInitialized() throws Exception {
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
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jsp/using-tld.jsp");
	}

	@Test
	public void testMultipleContextMappings() throws Exception {
		BundleContext bundleContext = installWarBundle.getBundleContext();
		DefaultHttpContextMapping httpContextMapping = new DefaultHttpContextMapping();
		httpContextMapping.setHttpContextId("alternative");
		httpContextMapping.setPath("alternative");
		ServiceRegistration<HttpContextMapping> httpContextMappingRegistration = bundleContext
				.registerService(HttpContextMapping.class,
						httpContextMapping, null);
		try {
			Servlet servlet = new WhiteboardServlet("/alias");
			DefaultServletMapping servletMapping = new DefaultServletMapping();
			servletMapping.setServlet(servlet);
			servletMapping.setAlias("/alias");
			String httpContextId = httpContextMapping.getHttpContextId();
			servletMapping.setHttpContextId(httpContextId);
			ServiceRegistration<ServletMapping> servletRegistration = bundleContext
					.registerService(ServletMapping.class,
							servletMapping, null);
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
    public void testMultipleContextMappingsWithDTOsCheck() throws Exception {
        BundleContext bundleContext = installWarBundle.getBundleContext();
        DefaultHttpContextMapping httpContextMapping = new DefaultHttpContextMapping();
        httpContextMapping.setHttpContextId("dtoCheck");
        httpContextMapping.setPath("dtocheck");
        ServiceRegistration<HttpContextMapping> httpContextMappingRegistration = bundleContext
                .registerService(HttpContextMapping.class,
                        httpContextMapping, null);
        try {
            Servlet servlet = new WhiteboardServlet("/dtocheck");
            DefaultServletMapping servletMapping = new DefaultServletMapping();
            servletMapping.setServlet(servlet);
            servletMapping.setAlias("/dtocheck");
            String httpContextId = httpContextMapping.getHttpContextId();
            servletMapping.setHttpContextId(httpContextId);
            ServiceRegistration<ServletMapping> servletRegistration = bundleContext
                    .registerService(ServletMapping.class,
                            servletMapping, null);
            
            try {
                HttpTestClientFactory.createDefaultTestClient()
                        .withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
                                resp -> resp.contains("Hello Whiteboard Extender"))
                        .doGETandExecuteTest("http://127.0.0.1:8181/dtocheck/dtocheck");
                
                HttpServiceRuntime httpServiceRuntime = waitForService(bundleContext, HttpServiceRuntime.class);
                
                RuntimeDTO runtimeDTO = httpServiceRuntime.getRuntimeDTO();
                
                assertTrue(0 == runtimeDTO.failedServletContextDTOs.length);
                
                assertTrue(2 == runtimeDTO.servletContextDTOs.length);
                
                long count = Arrays.stream(runtimeDTO.servletContextDTOs).filter(servletContext -> servletContext.name.equalsIgnoreCase("dtoCheck")).count();
                
                assertTrue(1 == count);
                
            } finally {
                servletRegistration.unregister();
            }
        } finally {
            httpContextMappingRegistration.unregister();
        }
    }

}
