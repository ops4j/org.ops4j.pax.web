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
package org.ops4j.pax.web.itest.undertow;

import java.util.Dictionary;
import java.util.Hashtable;
import javax.inject.Inject;
import javax.servlet.Servlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.BrokenServlet;
import org.ops4j.pax.web.itest.base.support.ErrorServlet;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@RunWith(PaxExam.class)
public class WhiteboardR6IntegrationTest extends ITestBase {

	@Inject
	@Filter(timeout = 20000)
	private WebContainer webcontainer;

	@Inject
	private BundleContext bundleContext;

	@Configuration
	public static Option[] configure() {
		return configureUndertow();
	}

	@Test
	public void testErrorServlet() throws Exception {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, new String[] {
				"404", "442", "5xx",
				"java.io.IOException"
		});

		ServiceRegistration<Servlet> errorServletReg = ErrorServlet.register(bundleContext, properties);
		ServiceRegistration<Servlet> brokenServletReg = BrokenServlet.register(bundleContext);

		final String message1 = String.format("%d|null|%s|null|%s|null", 404, "Not Found", "/error");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.timeoutInSeconds(7200)
				.withResponseAssertion("Response must contain '" + message1 + "'",
						resp -> resp.contains(message1))
				.doGETandExecuteTest("http://127.0.0.1:8181/error");

		final String message2 = String.format("%d|null|%s|null|%s|broken-servlet", 442, "442", "/broken");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(442)
				.timeoutInSeconds(7200)
				.withResponseAssertion("Response must contain '" + message2 + "'",
						resp -> resp.contains(message2))
				.doGETandExecuteTest("http://127.0.0.1:8181/broken?what=return&code=442");

		final String message3 = String.format("%d|null|%s|null|%s|broken-servlet", 502, "Bad Gateway", "/broken");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(502)
				.timeoutInSeconds(7200)
				.withResponseAssertion("Response must contain '" + message3 + "'",
						resp -> resp.contains(message3))
				.doGETandExecuteTest("http://127.0.0.1:8181/broken?what=return&code=502");

		String exception = "java.io.IOException";
		final String message4 = String.format("%d|%s|%s|%s|%s|broken-servlet",
				500, exception, "java.io.IOException: somethingwronghashappened", exception, "/broken");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(500)
				.timeoutInSeconds(7200)
				.withResponseAssertion("Response must contain '" + message4 + "'",
						resp -> resp.contains(message4))
				.doGETandExecuteTest("http://127.0.0.1:8181/broken?what=throw&ex=" + exception + "&message=somethingwronghashappened");

		errorServletReg.unregister();
		brokenServletReg.unregister();
	}

}
