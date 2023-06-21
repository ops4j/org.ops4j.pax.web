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

import java.util.Hashtable;
import javax.inject.Inject;
import jakarta.servlet.Servlet;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.ops4j.pax.web.service.http.HttpContext;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardDeregisteringContextIntegrationTest extends AbstractContainerTestBase {

	@Inject
	private WebContainer webContainerService;

	@Test
	@SuppressWarnings({ "deprecation", "unchecked" })
	public void testDeregisterContext() throws Exception {
		Hashtable<String, String> props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "myContext");

		HttpContext httpContext = webContainerService.createDefaultHttpContext("myContext");

		ServiceRegistration<HttpContext> contextService = context.registerService(HttpContext.class, httpContext, props);

		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, "/ungetServletTest");
		props.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "myContext");

		// install, but don't start
		Bundle whiteboard = context.installBundle(sampleURI("whiteboard"));
		Class<? extends Servlet> c = (Class<? extends Servlet>) whiteboard.loadClass("org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet");
		Servlet s = c.getConstructor(String.class).newInstance("ungetServletTest");

		ServiceRegistration<Servlet> servletService = context.registerService(Servlet.class, s, props);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/ungetServletTest");

		servletService.unregister();
		contextService.unregister();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/ungetServletTest");
	}

}
