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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Hashtable;

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
	public void testResources() throws Exception {

		Dictionary<String, String> properties = new Hashtable<>();
		properties.put("osgi.http.whiteboard.resource.pattern", "/files");
		properties.put("osgi.http.whiteboard.resource.prefix", "/images");

		ServiceRegistration<MyResourceService> registerService = bundleContext.registerService(MyResourceService.class, new MyResourceService(), properties);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Header 'Content-Type' must be 'image/png'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/files/ops4j.png");

		registerService.unregister();
	}

	/**
	 * Test error handling. In Undertow, error servlet is invoked like this:<pre>
	 * "default task-18@10211" prio=5 tid=0xac nid=NA runnable
	 *   java.lang.Thread.State: RUNNABLE
	 * 	  at grgr.test.MyErrorServlet.doGet(MyErrorServlet.java:33)
	 * 	  at javax.servlet.http.HttpServlet.service(HttpServlet.java:687)
	 * 	  at javax.servlet.http.HttpServlet.service(HttpServlet.java:790)
	 * 	  at io.undertow.servlet.handlers.ServletHandler.handleRequest(ServletHandler.java:85)
	 * 	  at io.undertow.servlet.handlers.security.ServletSecurityRoleHandler.handleRequest(ServletSecurityRoleHandler.java:62)
	 * 	  at io.undertow.servlet.handlers.ServletDispatchingHandler.handleRequest(ServletDispatchingHandler.java:36)
	 * 	  at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
	 * 	  at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
	 * 	  at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
	 * 	  at io.undertow.servlet.handlers.ServletInitialHandler.dispatchRequest(ServletInitialHandler.java:274)
	 * 	  at io.undertow.servlet.handlers.ServletInitialHandler.dispatchToPath(ServletInitialHandler.java:209)
	 * 	  at io.undertow.servlet.spec.RequestDispatcherImpl.error(RequestDispatcherImpl.java:480)
	 * 	  at io.undertow.servlet.spec.RequestDispatcherImpl.error(RequestDispatcherImpl.java:405)
	 * 	  at io.undertow.servlet.spec.HttpServletResponseImpl.doErrorDispatch(HttpServletResponseImpl.java:156)
	 * 	  at io.undertow.servlet.handlers.ServletInitialHandler.handleFirstRequest(ServletInitialHandler.java:295)
	 * 	  at io.undertow.servlet.handlers.ServletInitialHandler.access$100(ServletInitialHandler.java:81)
	 * 	  at io.undertow.servlet.handlers.ServletInitialHandler$2.call(ServletInitialHandler.java:138)
	 * 	  at io.undertow.servlet.handlers.ServletInitialHandler$2.call(ServletInitialHandler.java:135)
	 * 	  at io.undertow.servlet.core.ServletRequestContextThreadSetupAction$1.call(ServletRequestContextThreadSetupAction.java:48)
	 * 	  at io.undertow.servlet.core.ContextClassLoaderSetupAction$1.call(ContextClassLoaderSetupAction.java:43)
	 * 	  at io.undertow.servlet.api.LegacyThreadSetupActionWrapper$1.call(LegacyThreadSetupActionWrapper.java:44)
	 * 	  at io.undertow.servlet.api.LegacyThreadSetupActionWrapper$1.call(LegacyThreadSetupActionWrapper.java:44)
	 * 	  at io.undertow.servlet.api.LegacyThreadSetupActionWrapper$1.call(LegacyThreadSetupActionWrapper.java:44)
	 * 	  at io.undertow.servlet.api.LegacyThreadSetupActionWrapper$1.call(LegacyThreadSetupActionWrapper.java:44)
	 * 	  at io.undertow.servlet.api.LegacyThreadSetupActionWrapper$1.call(LegacyThreadSetupActionWrapper.java:44)
	 * 	  at io.undertow.servlet.handlers.ServletInitialHandler.dispatchRequest(ServletInitialHandler.java:272)
	 * 	  at io.undertow.servlet.handlers.ServletInitialHandler.access$000(ServletInitialHandler.java:81)
	 * 	  at io.undertow.servlet.handlers.ServletInitialHandler$1.handleRequest(ServletInitialHandler.java:104)
	 * 	  at io.undertow.server.Connectors.executeRootHandler(Connectors.java:202)
	 * 	  at io.undertow.server.HttpServerExchange$1.run(HttpServerExchange.java:805)
	 * 	  at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
	 * 	  at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	 * 	  at java.lang.Thread.run(Thread.java:748)
	 * </pre>
	 * @throws Exception
	 */
	@Test
	public void testErrorServlet() throws Exception {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, new String[] {
				"404", "442", "5xx",
				"java.io.IOException"
		});

		ServiceRegistration<Servlet> registerService = bundleContext.registerService(Servlet.class,
				new MyErrorServlet(), properties);
		ServiceRegistration<Servlet> brokenServlet = registerBrokenServlet();

		final String message1 = String.format("%d|null|%s|null|%s|default", 404, "Not Found", "/error");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.timeoutInSeconds(7200)
				.withResponseAssertion("Response must contain '" + message1 + "'",
						resp -> resp.contains(message1))
				.doGETandExecuteTest("http://127.0.0.1:8181/error");

		final String message2 = String.format("%d|null|%s|null|%s|broken-servlet", 442, "Unknown", "/broken");
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
				500, exception, "somethingwronghashappened", exception, "/broken");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(500)
				.timeoutInSeconds(7200)
				.withResponseAssertion("Response must contain '" + message4 + "'",
						resp -> resp.contains(message4))
				.doGETandExecuteTest("http://127.0.0.1:8181/broken?what=throw&ex=" + exception + "&message=somethingwronghashappened");

		registerService.unregister();
		brokenServlet.unregister();
	}

	private ServiceRegistration<Servlet> registerBrokenServlet() {
		Dictionary<String, String> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "broken-servlet");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/broken");

		return bundleContext.registerService(Servlet.class, new MyBrokenServlet(),
				properties);
	}

	private static class MyResourceService {}

	private static class MyBrokenServlet extends HttpServlet {

		private static final long serialVersionUID = 1L;

		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");
			String what = req.getParameter("what");
			if ("throw".equals(what)) {
				String exceptionClass = req.getParameter("ex");
				String exceptionMessage = req.getParameter("message");
				try {
					Class<?> tc = Class.forName(exceptionClass);
					Constructor<?> ct = tc.getConstructor(String.class);
					if (RuntimeException.class.isAssignableFrom(tc)) {
						throw (RuntimeException) ct.newInstance(exceptionMessage);
					} else if (IOException.class.isAssignableFrom(tc)) {
						throw (IOException) ct.newInstance(exceptionMessage);
					} else {
						throw new ServletException((Throwable)ct.newInstance(exceptionMessage));
					}
				} catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					throw new RuntimeException("unexpected");
				}
			} else if ("return".equals(what)) {
				Integer code = Integer.parseInt(req.getParameter("code"));
				resp.sendError(code);
			} else {
				resp.getWriter().println("OK");
			}
		}
	}

	private static class MyErrorServlet extends HttpServlet {

		private static final long serialVersionUID = 1L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType("text/plain");

			// Servlets 3.1 spec, 10.9.1 "Request Attributes"
			Integer status_code = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
			Class<?> exception_type = (Class<?>) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
			String message = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
			Throwable exception = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
			String request_uri = (String) req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			String servlet_name = (String) req.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
			resp.getWriter().println(String.format("%d|%s|%s|%s|%s|%s",
					status_code == null ? 0 : status_code,
					exception_type == null ? "null" : exception_type.getName(),
					message,
					exception == null ? "null" : exception.getClass().getName(),
					request_uri,
					servlet_name));
		}
	}

}
