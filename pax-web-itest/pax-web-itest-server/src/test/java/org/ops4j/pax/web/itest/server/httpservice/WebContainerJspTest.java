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
package org.ops4j.pax.web.itest.server.httpservice;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WebContainerJspTest extends MultiContainerTestSupport {

	@Override
	protected boolean enableJSP() {
		return true;
	}

	@Test
	@SuppressWarnings("deprecation")
	public void simpleJspTest() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		when(sample1.getResource("hello.jsp")).thenReturn(new File("src/test/resources/jsp/hello.jsp").toURI().toURL());

		WebContainer wc = container(sample1);

		// override "default" context, so it wins with the Whiteboard one
		HttpContext httpContext = wc.createDefaultHttpContext();
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/");
		ServiceReference<HttpContext> reference = mockReference(sample1,
				HttpContext.class, properties, () -> httpContext, 0L, 42);
		OsgiContextModel model = getHttpContextCustomizer().addingService(reference);

		// yes - that's enough ;)
		wc.registerJsps(null, null, null);

		String response = httpGET(port, "/hello.jsp?p1=v1&p2=v2");
		assertTrue(response.contains("<h1>v1</h1>")); // <h1><c:out value="${param['p1']}" /></h1>
		assertTrue(response.contains("<h2>v2</h2>")); // <h2>${param['p2']}</h2>

		stopContainer(sample1);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void jsppWithCustomTaglibsTest() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		when(sample1.getResource("hellotld.jspage")).thenReturn(new File("src/test/resources/jsp/hellotld.jsp").toURI().toURL());
		when(sample1.findEntries("WEB-INF/", null, false)).thenReturn(Collections.enumeration(Collections.singletonList(
				new URL("file:///WEB-INF/tlds/") // artificial URL for which getPath() is called
		)));
		when(sample1.findEntries("WEB-INF/tlds/", null, false)).thenReturn(Collections.enumeration(Collections.singletonList(
				new URL("file:///WEB-INF/tlds/test.tld")
		)));
		when(sample1.getResource("WEB-INF/tlds/test.tld")).thenReturn(new File("src/test/resources/tlds/test.tld").toURI().toURL());

		WebContainer wc = container(sample1);

		// override "default" context, so it wins with the Whiteboard one
		HttpContext httpContext = wc.createDefaultHttpContext();
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/");
		ServiceReference<HttpContext> reference = mockReference(sample1,
				HttpContext.class, properties, () -> httpContext, 0L, 42);
		OsgiContextModel model = getHttpContextCustomizer().addingService(reference);

		wc.registerJsps(new String[] { "*.jspage" }, null, null);

		assertThat(httpGET(port, "/hellotld.jsp"), startsWith("HTTP/1.1 404"));
		String response = httpGET(port, "/hellotld.jspage");
		assertThat(response, containsString("<span>Hello Custom Tags!</span>"));

		stopContainer(sample1);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
