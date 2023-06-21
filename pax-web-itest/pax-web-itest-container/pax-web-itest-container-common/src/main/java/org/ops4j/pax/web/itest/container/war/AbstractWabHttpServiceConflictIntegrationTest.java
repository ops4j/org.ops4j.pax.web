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
package org.ops4j.pax.web.itest.container.war;

import java.io.IOException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;

@RunWith(PaxExam.class)
public abstract class AbstractWabHttpServiceConflictIntegrationTest extends AbstractContainerTestBase {

	@Test
	public void whiteboardAndWab() throws Exception {
		WebContainer container = getWebContainer(context);

		// simulation of the servlet registered by CXF's cxf-rt-transports-http
		HttpServlet cxf = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().print("CXF");
				resp.getWriter().close();
			}
		};
		container.registerServlet(cxf, new String[] { "/cxf/*" }, null, null);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'CXF'",
						resp -> resp.contains("CXF"))
				.doGETandExecuteTest("http://127.0.0.1:8181/cxf/hello");

		// now install a WAB mapped to "/" context

		final Bundle[] b = new Bundle[1];
		configureAndWaitForDeploymentUnlessInstalled("war-simplest-javaee", () -> {
			b[0] = installAndStartWebBundle("war-simplest-javaee", "/");
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello (WAR)'",
						resp -> resp.contains("Hello (WAR)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/servlet");

		// HttpService registered servlet should still work
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'CXF'",
						resp -> resp.contains("CXF"))
				.doGETandExecuteTest("http://127.0.0.1:8181/cxf/hello");

		// WAR should work after the CXF servlet is unregistered
		container.unregisterServlet(cxf);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello (WAR)'",
						resp -> resp.contains("Hello (WAR)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/servlet");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/cxf/hello");

		// register the servlet again
		container.registerServlet(cxf, new String[] { "/cxf/*" }, null, null);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello (WAR)'",
						resp -> resp.contains("Hello (WAR)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/servlet");

		// HttpService registered servlet should still work
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'CXF'",
						resp -> resp.contains("CXF"))
				.doGETandExecuteTest("http://127.0.0.1:8181/cxf/hello");

		// but this time uninstall the WAB
		b[0].uninstall();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/servlet");

		// HttpService registered servlet should still work
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'CXF'",
						resp -> resp.contains("CXF"))
				.doGETandExecuteTest("http://127.0.0.1:8181/cxf/hello");
	}

}
