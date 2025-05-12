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
package org.ops4j.pax.web.itest.jetty.config;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

import java.io.File;

import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class JettyHandlerServiceIntegrationTest extends AbstractContainerTestBase {

	private Bundle war;

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		Option[] jspOptions = combine(serverOptions, paxWebJsp());
		return combine(jspOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
		war = configureAndWaitForDeploymentUnlessInstalled("war", () -> {
			installAndStartWebBundle("war", "/test");
		});
	}

	@After
	public void tearDown() throws BundleException {
		if (war != null) {
			war.stop();
			war.uninstall();
		}
	}

	@Test
	public void testWeb() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8181/test/wc/example");
	}

	@Test
	public void testStaticContent() throws Exception {
		ContextHandler ctxtHandler = new ContextHandler();
		ctxtHandler.setContextPath("/static-content");
		ctxtHandler.setBaseResourceAsString(new File("target").getCanonicalPath());
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setBaseResourceAsString(new File("target").getCanonicalPath());
		resourceHandler.setDirAllowed(true);
		ctxtHandler.setHandler(resourceHandler);

		// if lambda, Pax Exam has problems scanning this class
		@SuppressWarnings("Convert2Lambda")
		HttpConfiguration.Customizer customizer = new HttpConfiguration.Customizer() {
			@Override
			public Request customize(Request request, HttpFields.Mutable responseHeaders) {
				responseHeaders.add("X-Y-Z", "x-y-z");
				return request;
			}
		};

		@SuppressWarnings("unchecked")
		final ServiceRegistration<Handler>[] registerHandlerService = new ServiceRegistration[1];

		ServiceRegistration<HttpConfiguration.Customizer> registerCustomizerService  = context.registerService(HttpConfiguration.Customizer.class, customizer, null);
		configureAndWaitForDeployment(() -> {
			registerHandlerService[0] = context.registerService(Handler.class, ctxtHandler, null);
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<a href=\"/static-content/'",
						resp -> resp.contains("<a href=\"/static-content/"))
				.withResponseHeaderAssertion("Response should contain \"X-Y-Z\" header", headers
						-> headers.anyMatch(e -> e.getKey().equals("X-Y-Z") && e.getValue().equals("x-y-z")))
				.doGETandExecuteTest("http://localhost:8181/static-content/");

		registerCustomizerService.unregister();
		registerHandlerService[0].unregister();
	}

}
