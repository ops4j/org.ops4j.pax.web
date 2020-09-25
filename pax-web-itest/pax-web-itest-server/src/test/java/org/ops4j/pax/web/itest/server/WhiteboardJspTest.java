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

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultJspMapping;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.elements.JspModel;
import org.ops4j.pax.web.service.whiteboard.JspMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardJspTest extends MultiContainerTestSupport {

	@Override
	protected boolean enableJSP() {
		return true;
	}

	@Test
	public void oneWayToRegisterJsps() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		// getResource for HttpContext, getEntry for ServletContextHelper...
		when(sample1.getEntry("hello.JSP")).thenReturn(new File("src/test/resources/jsp/hello.jsp").toURI().toURL());

		DefaultJspMapping mapping = new DefaultJspMapping();
		mapping.setUrlPatterns(new String[] { "*.JSP" });

		ServiceReference<JspMapping> ref = mockReference(sample1, JspMapping.class, null, () -> mapping, 0L, 0);
		JspModel model = getJspMappingCustomizer().addingService(ref);

		String response = httpGET(port, "/hello.JSP?p1=v1&p2=v2");
		assertTrue(response.contains("<h1>v1</h1>")); // <h1><c:out value="${param['p1']}" /></h1>
		assertTrue(response.contains("<h2>v2</h2>")); // <h2>${param['p2']}</h2>

		getJspMappingCustomizer().removedService(ref, model);

		WebContainer wc = container(sample1);
		stopContainer(sample1);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
