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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Tests virtual hosts and connectors configured externally and through service registration properties headers.
 *
 * @author Gareth Collins
 */
public class AbstractWhiteboardVirtualHostsIntegrationTest extends AbstractContainerTestBase {

	private Bundle bundle;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForNamedServlet("extended4", () -> {
			bundle = installAndStartBundle(sampleURI("whiteboard-extended"));
		});
	}

	@After
	public void cleanUp() throws BundleException {
		if (bundle != null) {
			bundle.stop();
			bundle.uninstall();
			bundle = null;
		}
	}

	// contexts  | path | virtual host | connectors
	// ----------+------+--------------+--------------
	// extended1 | /foo | localhost    | jettyConn1(*)
	// extended2 | /bar | <all>        | default
	// extended3 | /    | 127.0.0.1    | jettyConn1(*)
	// extended4 | /baz | <all>        | jettyConn1(*)
	//
	// (*) means that this config is externally specified

	// org.eclipse.jetty.server.handler.ContextHandler.checkVirtualHost() is the canonical implementation
	// of virtual host + connector name handling.
	//
	// If Jetty has "localhost" host and "@default" connector configured, connector is checked in two cases:
	// 1) host@connnector - both must match
	// 2) @connector - only a connector matching is enough - even if "Host" header doesn't match ANY context's host
	//

	@Test
	public void testWhiteBoardContextFound() throws Exception {
		// vhost and connector matches
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://localhost:8282/foo/whiteboard");
		// only connector matches
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8282/foo/whiteboard");
		// only vhost matches
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://localhost:8181/foo/whiteboard");
		// connector matches with one vhost
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://localhost:8181/bar/whiteboard");
		// connector matches with the other vhost
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/bar/whiteboard");
		// vhost and connector matches
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8282/whiteboard");
		// only connector matches
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://localhost:8282/whiteboard");
		// only vhost matches
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");
		// connector matches with one vhost
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://localhost:8282/baz/whiteboard");
		// connector matches with the other vhost
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8282/baz/whiteboard");
	}

	@Test
	public void testWhiteBoardContextWrongServlet() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/foo/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/foo/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/foo/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/foo/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/bar/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/bar/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/bar/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/bar/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/baz/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/baz/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/baz/whiteboard2");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/baz/whiteboard2");
	}

	@Test
	public void testWhiteBoardContextNotFoundWrongVirtualHostAndConnector() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/foo/whiteboard");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8282/bar/whiteboard");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8282/bar/whiteboard");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8181/whiteboard");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://localhost:8181/baz/whiteboard");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/baz/whiteboard");
	}

}
