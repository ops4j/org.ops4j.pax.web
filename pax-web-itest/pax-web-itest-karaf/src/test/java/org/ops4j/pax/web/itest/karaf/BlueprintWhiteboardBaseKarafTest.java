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

import jakarta.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)
public abstract class BlueprintWhiteboardBaseKarafTest extends AbstractKarafTestBase {

	private Bundle bundle;

	@Before
	public void setUp() throws Exception {
		configureAndWait(() -> bundle = installAndStartBundle(sampleURI("whiteboard-blueprint")), events -> {
			boolean match = events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).isResourceServlet()
					&& usesContexts(e.getData(), "default"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("forbidden-servlet")
					&& usesContexts(e.getData(), "forbidden"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("whiteboard-servlet")
					&& usesContexts(e.getData(), "default"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("root-servlet")
					&& usesContexts(e.getData(), "default"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("filtered-servlet")
					&& usesContexts(e.getData(), "default"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof FilterEventData
					&& ((FilterEventData)e.getData()).getFilterName().equals("filter1")
					&& usesContexts(e.getData(), "default"));

			return match;
		});
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
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf using Whiteboard-Extender!",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");
	}

	@Test
	public void testWhiteboardSlash() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must be served from welcome-page!",
						resp -> resp.contains("Welcome to the Welcome page"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

	@Test
	public void testWhiteboardForbidden() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(401)
				.doGETandExecuteTest("http://127.0.0.1:8181/forbidden");
	}

	@Test
	public void testWhiteboardFiltered() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must be served from with message from Whiteboard-Filter!",
						resp -> resp.contains("Filter was there before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/filtered");
	}

	@Test
	public void testImage() throws Exception {
		createTestClientForKaraf()
				.withResponseHeaderAssertion("ContentType for image must be 'image/png'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/images/ops4j.png");
	}

	@Test
	public void test404() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Respone must be served from Default 404 page",
						resp -> resp.contains("<title>Default 404 page</title>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/doesNotExist");
	}

	@Test
	public void testResourceMapping() throws Exception {
		createTestClientForKaraf()
				.withResponseHeaderAssertion("ContentType for image must be 'image/png'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboardresources/ops4j.png");
	}

	@Test
	public void testJspMapping() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/jsp/simple.jsp");
	}

	@Test
	public void testTldJsp() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
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
			@SuppressWarnings("unchecked")
			final ServiceRegistration<ServletMapping>[] servletRegistration = new ServiceRegistration[1];
			configureAndWaitForServletWithMapping("/alias/*", () -> {
				servletRegistration[0] = bundleContext.registerService(ServletMapping.class, servletMapping, null);
			});
			try {
				createTestClientForKaraf()
						.withResponseAssertion("Response must contain text served by Karaf using Whiteboard-Extender Alias!",
								resp -> resp.contains("Hello Whiteboard Extender"))
						.doGETandExecuteTest("http://127.0.0.1:8181/alternative/alias");
			} finally {
				servletRegistration[0].unregister();
			}
		} finally {
			httpContextMappingRegistration.unregister();
		}
	}

}
