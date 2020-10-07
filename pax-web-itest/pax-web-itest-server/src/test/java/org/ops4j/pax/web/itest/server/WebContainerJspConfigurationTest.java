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
import javax.servlet.ServletException;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.StoppableHttpService;
import org.ops4j.pax.web.service.spi.servlet.DefaultJspPropertyGroupDescriptor;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WebContainerJspConfigurationTest extends MultiContainerTestSupport {

	@Test
	public void jspTaglibsAndConfiguration() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		WebContainer wc = new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		// this itself won't start the context
		wc.registerJspConfigTagLibs("file:/dev/null", "urn:x", null);
		DefaultJspPropertyGroupDescriptor descriptor = new DefaultJspPropertyGroupDescriptor();
		descriptor.setBuffer("1042");
		wc.registerJspConfigPropertyGroup(descriptor, null);

		// we need some "active" component to test the configuration
		wc.registerServlet("/jspinfo", new TestServlet("1"), null, null);

		String response = httpGET(port, "/jspinfo");
		assertTrue(response.endsWith("1|1042|1|file:/dev/null"));

		((StoppableHttpService) wc).stop();

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
			JspConfigDescriptor jspConfig = req.getServletContext().getJspConfigDescriptor();
			resp.getWriter().print(String.format("%d|%s|%d|%s",
					jspConfig.getJspPropertyGroups().size(),
					jspConfig.getJspPropertyGroups().iterator().next().getBuffer(),
					jspConfig.getTaglibs().size(),
					jspConfig.getTaglibs().iterator().next().getTaglibLocation()));
		}
	}

}
