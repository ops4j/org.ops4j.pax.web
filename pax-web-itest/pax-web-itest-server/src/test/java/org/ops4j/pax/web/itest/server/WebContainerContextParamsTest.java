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
package org.ops4j.pax.web.itest.server;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.StoppableHttpService;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WebContainerContextParamsTest extends MultiContainerTestSupport {

	@Test
	public void configureContextParameters() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		WebContainer wc = new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		Dictionary<String, Object> params = new Hashtable<>();
		params.put("test", "value");
		wc.setContextParams(params, null);

		// we need some "active" component to test the configuration
		wc.registerServlet("/visit", new TestServlet("1"), null, null);

		// no sessions
		String response = httpGET(port, "/visit");
		assertTrue(response.endsWith("value"));

		((StoppableHttpService) wc).stop();

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void configureContextParametersInServletContainerInitializerWithDefaultContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		WebContainer wc = new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		Dictionary<String, Object> params = new Hashtable<>();
		params.put("test", "value");
		wc.setContextParams(params, null);

		final String[] holder = new String[] { null };
		// this won't start the context ...
		wc.registerServletContainerInitializer((c, ctx) -> {
			holder[0] = ctx.getInitParameter("test");
		}, null, null);

		// we need some "active" component to test the configuration
		wc.registerServlet("/visit", new TestServlet("1"), null, null);

		// no sessions
		String response = httpGET(port, "/visit");
		assertTrue(response.endsWith("value"));
		assertNull(holder[0]);

		((StoppableHttpService) wc).stop();

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void configureContextParametersInServletContainerInitializerWithCustomContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		WebContainer wc = container(sample1);

		// normally, "null" httpContext is mapped to HttpService based OsgiContextModel, but in SCI invocation
		// the OsgiContextModel passed to SCI.onStartup() is the "default" OsgiContextModel from Whiteboard, so
		// we have to register higher ranked OsgiContextModel
		HttpContext httpContext = wc.createDefaultHttpContext();
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/");
		ServiceReference<HttpContext> reference = mockReference(sample1,
				HttpContext.class, properties, () -> httpContext, 0L, 42);
		OsgiContextModel model = getHttpContextCustomizer().addingService(reference);

		Dictionary<String, Object> params = new Hashtable<>();
		params.put("test", "value");
		wc.setContextParams(params, null);

		final String[] holder = new String[] { null };
		// this won't start the context ...
		wc.registerServletContainerInitializer((c, ctx) -> {
			holder[0] = ctx.getInitParameter("test");
		}, null, null);

		// we need some "active" component to test the configuration
		wc.registerServlet("/visit", new TestServlet("1"), null, null);

		// no sessions
		String response = httpGET(port, "/visit");
		assertTrue(response.endsWith("value"));
		assertThat(holder[0], equalTo("value"));

		stopContainer(sample1);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	private static class TestServlet extends Utils.MyIdServlet {
		TestServlet(String id) {
			super(id);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().print(getServletContext().getInitParameter("test"));
		}
	}

}
