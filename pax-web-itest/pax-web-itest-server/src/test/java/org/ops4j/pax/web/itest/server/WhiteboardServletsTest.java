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

import javax.servlet.Servlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardServletsTest extends MultiContainerTestSupport {

	@Test
	public void twoWaysToRegisterServlet() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// 1. Whiteboard registration as javax.servlet.Servlet OSGi service

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		ServletModel model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));

		getServletCustomizer().removedService(servletRef, model);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		// 2. Whiteboard registration as Pax Web specific org.ops4j.pax.web.service.whiteboard.ServletMapping
		//    OSGi service

		DefaultServletMapping sm = new DefaultServletMapping();
		sm.setServlet(new Utils.MyIdServlet("2"));
		sm.setUrlPatterns(new String[] { "/t" });
		ServiceReference<ServletMapping> servletMappingRef = mockReference(sample1, ServletMapping.class,
				null, () -> sm);
		ServletModel model2 = getServletMappingCustomizer().addingService(servletMappingRef);
		assertThat(httpGET(port, "/t"), endsWith("S(2)"));

		getServletMappingCustomizer().removedService(servletMappingRef, model2);
		assertThat(httpGET(port, "/t"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
