/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.itest.container.whiteboard;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class AbstractWhiteboardAliasIntegrationTest extends AbstractContainerTestBase {

	private ServiceRegistration<Servlet> service;

	@Before
	@SuppressWarnings("deprecation")
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/*", () -> {
			Dictionary<String, Object> initParams = new Hashtable<>();
			initParams.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, "/");
			initParams.put(Constants.SERVICE_RANKING, 100);
			HttpServlet documentServlet = new HttpServlet() {
				@Override
				protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
					resp.getWriter().println("I won with ResourceMapping!");
				}
			};
			service = context.registerService(Servlet.class, documentServlet, initParams);
		});
	}

	@After
	public void tearDown() {
		if (service != null) {
			service.unregister();
		}
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'I won with ResourceMapping!'",
						resp -> resp.contains("I won with ResourceMapping!"))
				.doGETandExecuteTest("http://127.0.0.1:8181");
	}

}
