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
package org.ops4j.pax.web.itest.container.whiteboard;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.WaitCondition;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.spi.model.events.ErrorPageEventData;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WelcomeFileEventData;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractWhiteboardDSRestartIntegrationTest extends AbstractContainerTestBase {

	private Bundle bundle;

	@Override
	protected Option[] baseConfigure() {
		return combine(super.baseConfigure(), combine(configAdmin(), scr()));
	}

	@Before
	public void setUp() throws Exception {
		configureAndWait(() -> bundle = installAndStartBundle(sampleURI("whiteboard-ds")), events -> {
			// we want to be sure that all the elements are registered - and with full set of associated contexts,
			// because in SCR it is possible that a web element that's associated with several contexts may first
			// be registered with only a subset of them and then it may be registered again with full set

			boolean match = true;

			// we're looking for example for something like:
			// 53 = {org.ops4j.pax.web.service.spi.model.events.ElementEvent@6183} "DEPLOYED (org.ops4j.pax.web.samples.whiteboard-ds/8.0.0.SNAPSHOT): org.ops4j.pax.web.service.spi.model.events.ServletEventData@3cb8c8ce"
			// type: org.ops4j.pax.web.service.spi.model.events.ElementEvent$State  = {@6353} "DEPLOYED"
			// bundleName: java.lang.String  = "org.ops4j.pax.web.samples.whiteboard-ds"
			// ...
			// data: org.ops4j.pax.web.service.spi.model.events.ElementEventData  = {org.ops4j.pax.web.service.spi.model.events.ServletEventData@6392}
			//  urlPatterns: java.lang.String[]  = {java.lang.String[1]@6395} ["/resources/*"]
			//  servlet: jakarta.servlet.Servlet  = {org.ops4j.pax.web.service.jetty.internal.web.JettyResourceServlet@6396}
			//  ...
			//  contextNames: java.util.List  = {java.util.LinkedList@6398}  size = 4
			//   0 = "default"
			//   1 = "CustomContext"
			//   2 = "CustomHttpContext"
			//   3 = "CustomHttpContextMapping"

			// contexts we're registering (but don't have to wait for related events):
			//  - PaxWebWhiteboardHttpContext: "CustomHttpContext"
			//  - PaxWebWhiteboardHttpContextMapping: "CustomHttpContextMapping"
			//  - WhiteboardContext: "CustomContext"
			// elements which are registered to "default" context only - we don't care about their events:
			//  - WhiteboardErrorPage
			//  - WhiteboardFilter
			//  - WhiteboardServlet
			// elements registered to named context(s):
			//  - PaxWebWhiteboardErrorPageMapping: "CustomHttpContextMapping"
			match = events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ErrorPageEventData
					&& usesContexts(e.getData(), "CustomHttpContextMapping"));
			//  - PaxWebWhiteboardServletMapping: "CustomHttpContext"
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping")
					&& usesContexts(e.getData(), "CustomHttpContext"));
			//  - PaxWebWhiteboardWelcomeFiles: "CustomHttpContext" and "CustomHttpContextMapping"
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof WelcomeFileEventData
					&& usesContexts(e.getData(), "CustomHttpContext", "CustomHttpContextMapping"));
			//  - WhiteboardListener: "CustomContext" - don't care
			//  - WhiteboardResource: "default", "CustomContext", "CustomHttpContext" and "CustomHttpContextMapping"
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).isResourceServlet()
					&& usesContexts(e.getData(), "default", "CustomContext", "CustomHttpContext", "CustomHttpContextMapping"));
			//  - WhiteboardServletWithContext: "CustomContext"
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("ServletWithContext")
					&& usesContexts(e.getData(), "CustomContext"));
			return match;
		});
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle != null) {
			bundle.stop();
			bundle.uninstall();
		}
	}

	@Test
	public void testWhiteBoardSimpleServlet() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
						resp -> resp.contains("Hello from SimpleServlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
	}

	@Test
	public void testWhiteBoardServletWithContext() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from ServletWithContext'",
						resp -> resp.contains("Hello from ServletWithContext"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Request changed by SimpleFilter'",
						resp -> resp.contains("Request changed by SimpleFilter"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
	}

	@Test
	public void testWhiteboardBundleRestart() throws Exception {
		// /simple-servlet in default context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Request changed by SimpleFilter'",
						resp -> resp.contains("Request changed by SimpleFilter") && resp.contains("Hello from SimpleServlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");

		// org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardResource has been registered into 4 contexts
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/index.html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/index.html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/index.html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context-mapping/index.html");

		// org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardResource has been registered into 4 contexts, but
		// welcome file mapping - only to 3 of them
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(403)
				.doGETandExecuteTest("http://127.0.0.1:8181/context");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context-mapping");

		// a servlet in /
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
						resp -> resp.contains("Hello from SimpleServlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
		// a servlet in /context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
						resp -> resp.contains("Hello from ServletWithContext"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
		// a servlet in /custom-http-context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping'",
						resp -> resp.contains("Hello from org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/servlet-mapping");

		// error pages
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a custom error page",
						resp -> resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a standard error page",
						resp -> !resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a custom error page",
						resp -> resp.contains("Whoops, there was a 404. This is provided via PaxWebWhiteboardErrorPageMapping"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context-mapping/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a standard error page",
						resp -> !resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/x");

		// find Whiteboard-bundle
		final Bundle whiteBoardBundle = Arrays.stream(context.getBundles())
				.filter(bundle -> "org.ops4j.pax.web.pax-web-extender-whiteboard".equalsIgnoreCase(bundle.getSymbolicName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no Whiteboard bundle found"));

		// stop Whiteboard bundle
		whiteBoardBundle.stop();

		new WaitCondition("Check if Whiteboard bundle gets stopped") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return whiteBoardBundle.getState() == Bundle.RESOLVED;
			}
		}.waitForCondition();

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");

		// start Whiteboard bundle again
		configureAndWait(() -> {
			try {
				whiteBoardBundle.start();
			} catch (BundleException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}, events -> {
			boolean match = true;
			match = events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ErrorPageEventData
					&& usesContexts(e.getData(), "CustomHttpContextMapping"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping")
					&& usesContexts(e.getData(), "CustomHttpContext"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof WelcomeFileEventData
					&& usesContexts(e.getData(), "CustomHttpContext", "CustomHttpContextMapping"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).isResourceServlet()
					&& usesContexts(e.getData(), "default", "CustomContext", "CustomHttpContext", "CustomHttpContextMapping"));
			match &= events.stream().anyMatch(e -> e.getType() == WebElementEvent.State.DEPLOYED
					&& e.getData() instanceof ServletEventData
					&& ((ServletEventData)e.getData()).getServletName().equals("ServletWithContext")
					&& usesContexts(e.getData(), "CustomContext"));
			return match;
		});

		// /simple-servlet in default context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Request changed by SimpleFilter'",
						resp -> resp.contains("Request changed by SimpleFilter") && resp.contains("Hello from SimpleServlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");

		// org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardResource has been registered into 4 contexts
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/index.html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/index.html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/index.html");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context-mapping/index.html");

		// org.ops4j.pax.web.samples.whiteboard.ds.WhiteboardResource has been registered into 4 contexts, but
		// welcome file mapping - only to 3 of them
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(403)
				.doGETandExecuteTest("http://127.0.0.1:8181/context");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is a welcome file provided by PaxWebWhiteboardWelcomeFiles'",
						resp -> resp.contains("This is a welcome file provided by PaxWebWhiteboardWelcomeFiles"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context-mapping");

		// a servlet in /
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
						resp -> resp.contains("Hello from SimpleServlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");
		// a servlet in /context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
						resp -> resp.contains("Hello from ServletWithContext"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
		// a servlet in /custom-http-context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping'",
						resp -> resp.contains("Hello from org.ops4j.pax.web.samples.whiteboard.ds.extended.PaxWebWhiteboardServletMapping"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/servlet-mapping");

		// error pages
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a custom error page",
						resp -> resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a standard error page",
						resp -> !resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a custom error page",
						resp -> resp.contains("Whoops, there was a 404. This is provided via PaxWebWhiteboardErrorPageMapping"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context-mapping/x");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.withResponseAssertion("Response must be a standard error page",
						resp -> !resp.contains("Error Servlet, we do have a 404"))
				.doGETandExecuteTest("http://127.0.0.1:8181/custom-http-context/x");
	}

	@Test
	public void testWhiteboardSampleBundleRestart() throws Exception {
		// Test
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from ServletWithContext'",
						resp -> resp.contains("Hello from ServletWithContext"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");

		// find Whiteboard-bundle
		final Bundle whiteBoardSampleBundle = Arrays.stream(context.getBundles())
				.filter(bundle -> "org.ops4j.pax.web.samples.whiteboard-ds".equalsIgnoreCase(bundle.getSymbolicName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no Whiteboard Sample bundle found"));

		// stop Whiteboard bundle
		whiteBoardSampleBundle.stop();

		new WaitCondition("Check if Whiteboard Sample bundle gets stopped") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return whiteBoardSampleBundle.getState() == Bundle.RESOLVED;
			}
		}.waitForCondition();

		// start Whiteboard bundle again
		whiteBoardSampleBundle.start();

		new WaitCondition("Check if Whiteboard Sample bundle gets activated") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return whiteBoardSampleBundle.getState() == Bundle.ACTIVE;
			}
		}.waitForCondition();

		// Test
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from SimpleServlet'",
						resp -> resp.contains("Hello from SimpleServlet"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple-servlet");

		// Test
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from ServletWithContext'",
						resp -> resp.contains("Hello from ServletWithContext"))
				.doGETandExecuteTest("http://127.0.0.1:8181/context/servlet");
	}

}
