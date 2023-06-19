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
package org.ops4j.pax.web.itest.server.controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.StoppableHttpService;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class ServerControllerContainerInitializersTest extends MultiContainerTestSupport {

	@Override
	public void initAll() throws Exception {
		configurePort();
	}

	@Test
	public void singleDynamicServlet() throws Exception {
		ServerController controller = Utils.createServerController(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mockBundle("App Bundle", false);
		BundleContext bc = bundle.getBundleContext();

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());

		WebContainer wc = new HttpServiceEnabled(bundle, controller, server, null, controller.getConfiguration());
		@SuppressWarnings("unchecked")
		ServiceReference<WebContainer> wcRef = mock(ServiceReference.class);
		when(wcRef.getProperty(Constants.SERVICE_ID)).thenReturn(42L);
		when(bc.getServiceReference(WebContainer.class)).thenReturn(wcRef);
		when(bc.getService(wcRef)).thenReturn(wc);

		// this won't start the context ...
		wc.registerServletContainerInitializer((c, ctx) -> {
			ServletRegistration.Dynamic reg = ctx.addServlet("dynamic", new HttpServlet() {
				@Override
				protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
					resp.getWriter().print("OK");
				}
			});
			reg.addMapping("/s");
		}, null, null);

		// ... so we need some "active" component
		wc.registerResources("/", "", null);

		String response = httpGET(port, "/s");
		assertTrue(response.endsWith("OK"));

		((StoppableHttpService) wc).stop();
		controller.stop();

		ServerModelInternals serverModelInternals = serverModelInternals(server);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(bundle));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
