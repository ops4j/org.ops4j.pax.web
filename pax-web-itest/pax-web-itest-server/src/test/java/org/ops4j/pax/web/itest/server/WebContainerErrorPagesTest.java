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

import java.util.Hashtable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.support.ErrorServlet;
import org.ops4j.pax.web.itest.server.support.ProblemServlet;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WebContainerErrorPagesTest extends MultiContainerTestSupport {

	@Test
	public void errorPagesAndContextSwitching() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		WebContainer wc = container(sample1);

		wc.registerServlet("/yikes", new ProblemServlet(), null, null);
		wc.registerServlet("/error", new ErrorServlet(), null, null);
		wc.registerErrorPage("461", "/error", null);

		assertThat(httpGET(port, "/yikes?result=461&msg=x461"), endsWith("null: [org.ops4j.pax.web.itest.server.support.ProblemServlet][null][null][x461][/yikes][461]"));

		HttpContext defaultContext = wc.createDefaultHttpContext();
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "default");
		properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/c");
		ServiceReference<HttpContext> reference1 = mockReference(sample1,
				HttpContext.class, properties, () -> defaultContext, 0L, 13322);
		OsgiContextModel model1 = getHttpContextCustomizer().addingService(reference1);

		assertThat(httpGET(port, "/c/yikes?result=461&msg=x461"), endsWith("null: [org.ops4j.pax.web.itest.server.support.ProblemServlet][null][null][x461][/c/yikes][461]"));
		assertThat(httpGET(port, "/yikes?result=461&msg=x461"), startsWith("HTTP/1.1 404"));

		getHttpContextCustomizer().removedService(reference1, model1);

		assertThat(httpGET(port, "/c/yikes?result=461&msg=x461"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/yikes?result=461&msg=x461"), endsWith("null: [org.ops4j.pax.web.itest.server.support.ProblemServlet][null][null][x461][/yikes][461]"));

		wc.unregister("/yikes");
		stopContainer(sample1);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
