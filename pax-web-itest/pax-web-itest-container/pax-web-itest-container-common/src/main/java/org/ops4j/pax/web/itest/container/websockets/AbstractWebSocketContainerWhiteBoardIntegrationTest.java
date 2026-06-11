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
package org.ops4j.pax.web.itest.container.websockets;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.events.EventListenerEventData;
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public abstract class AbstractWebSocketContainerWhiteBoardIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWebSocketContainerWhiteBoardIntegrationTest.class);

	@Before
	public void setUp() throws Exception {
		configureAndWaitForListener(8181);
	}

	@Test
	public void testServerContainer() throws Exception {
		List<ServerContainer> containers = new ArrayList<>();

		Servlet myServlet1 = new MyHttpServlet(containers);
		Dictionary<String, Object> props1 = new Hashtable<>();
		props1.put(PaxWebConstants.SERVICE_PROPERTY_WEBSOCKET, "true");
		props1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1");
		configureAndWait(() -> {
			context.registerService(Servlet.class.getName(), myServlet1, props1);
		}, events -> events.stream()
				// Tomcat and Undertow - a filter, Jetty - a listener
				.anyMatch(ev -> ev.getType() == WebElementEvent.State.DEPLOYED
						&& (ev.getData() instanceof FilterEventData || ev.getData() instanceof EventListenerEventData)));

		Servlet myServlet2 = new MyHttpServlet(containers);
		Dictionary<String, Object> props2 = new  Hashtable<>();
		props2.put(PaxWebConstants.SERVICE_PROPERTY_WEBSOCKET, "true");
		props2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2");
		configureAndWaitForNamedServlet("s2", () -> {
			context.registerService(Servlet.class.getName(), myServlet2, props2);
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain servlet's name",
						resp -> resp.contains("s1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/s1");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain servlet's name",
						resp -> resp.contains("s2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/s2");

		assertEquals(2, containers.size());
		assertNotNull(containers.get(0));
		assertNotNull(containers.get(1));
		assertSame(containers.get(0), containers.get(1));
	}

	private static class MyHttpServlet extends HttpServlet {

		private final List<ServerContainer> containers;

		MyHttpServlet(List<ServerContainer> containers) {
			this.containers = containers;
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			// see org.atmosphere.container.JSR356AsyncSupport.JSR356AsyncSupport()
			ServerContainer container = (ServerContainer) req.getServletContext().getAttribute(ServerContainer.class.getName());
			containers.add(container);
			resp.getWriter().write(getServletConfig().getServletName());
		}
	}

}
