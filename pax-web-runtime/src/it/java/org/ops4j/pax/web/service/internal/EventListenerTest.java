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
package org.ops4j.pax.web.service.internal;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

import static org.easymock.EasyMock.*;

public class EventListenerTest
		extends IntegrationTests {

	@Test
	public void listenerIsCalled()
			throws IOException, NamespaceException, ServletException, InterruptedException {
		HttpSessionListener listener = createMock(HttpSessionListener.class);
		listener.sessionCreated((HttpSessionEvent) notNull());
		listener.sessionDestroyed((HttpSessionEvent) notNull());

		replay(listener);

		HttpContext context = m_httpService.createDefaultHttpContext();
		m_httpService.registerServlet("/test", new TestServlet(), null, context);
		m_httpService.registerEventListener(listener, context);

		HttpMethod method = new GetMethod("http://localhost:8080/test");
		m_client.executeMethod(method);
		System.out.println("Waiting the session to expire for two minutes...");
		method.releaseConnection();
		Thread.sleep(2 * 60 * 1000);

		verify(listener);

		((StoppableHttpService) m_httpService).stop();
	}

	private static class TestServlet
			extends HttpServlet

	{

		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			// create the session
			req.getSession();
		}

	}

}
