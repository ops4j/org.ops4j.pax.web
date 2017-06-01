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
package org.ops4j.pax.web.itest.jetty;

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
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(PaxExam.class)
public class WhiteboardR6IntegrationTest extends ITestBase {

	@Inject
	@Filter(timeout = 20000)
	private WebContainer webcontainer;

	@Inject
	private BundleContext bundleContext;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Test
	public void testWhiteBoardServlet() throws Exception {
		ServiceRegistration<Servlet> registerService = registerServlet();

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet name: value'",
						resp -> resp.contains("Servlet name: value"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myservlet");

		registerService.unregister();
	}

	@Test
	public void testWhiteBoardServletWithContext() throws Exception {
		Dictionary<String, String> contextProps = new Hashtable<>();
		contextProps.put("osgi.http.whiteboard.context.name", "my-context");
		contextProps.put("osgi.http.whiteboard.context.path", "/myapp");

		CDNServletContextHelper context = new CDNServletContextHelper();
		ServiceRegistration<ServletContextHelper> contextHelperService = bundleContext
				.registerService(ServletContextHelper.class, context, contextProps);

		Dictionary<String, String> extProps = new Hashtable<>();
		extProps.put("osgi.http.whiteboard.context.select", "(osgi.http.whiteboard.context.name=my-context)");
		ServiceRegistration<Servlet> registerServlet = registerServlet(extProps);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet name: value'",
						resp -> resp.contains("Servlet name: value"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myapp/myservlet");

		assertEquals(1, context.handleSecurityCalls.get());

		registerServlet.unregister();
		contextHelperService.unregister();

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

		final String message1 = String.format("%d|null|%s|null|%s|default", 404, "Not Found", "/error");
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

	@Test
	public void testAsyncServlet() throws Exception {
		Dictionary<String, String> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/as");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "true");

		ServiceRegistration<Servlet> registerService = bundleContext.registerService(Servlet.class, new AsyncServlet(),
				properties);

		HttpTestClientFactory.createDefaultTestClient()
				.async()
				.withResponseAssertion("Response must contain 'Servlet executed async in:'",
						resp -> resp.contains("Servlet executed async in:"))
				.doGETandExecuteTest("http://127.0.0.1:8181/as");

		registerService.unregister();
	}

	@Test
	public void testFilterServlet() throws Exception {
		ServiceRegistration<Servlet> registerService = registerServlet();

		Dictionary<String, String> properties = new Hashtable<>();
		properties.put("osgi.http.whiteboard.filter.pattern", "/*");
		ServiceRegistration<javax.servlet.Filter> registerFilter = bundleContext
				.registerService(javax.servlet.Filter.class, new MyFilter(), properties);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'before'",
						resp -> resp.contains("before"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myservlet");

		registerFilter.unregister();
		registerService.unregister();
	}

	@Test
	public void testListeners() throws Exception {
		ServiceRegistration<Servlet> registerService = registerServlet();

		MyServletRequestListener listener = new MyServletRequestListener();

		Dictionary<String, String> properties = new Hashtable<>();
		properties.put("osgi.http.whiteboard.listener", "true");

		ServiceRegistration<ServletRequestListener> listenerService = bundleContext.registerService(ServletRequestListener.class, listener, properties);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet name: value'",
						resp -> resp.contains("Servlet name: value"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myservlet");

		assertThat(listener.gotEvent(), is(true));

		listenerService.unregister();
		registerService.unregister();
	}

	/**
	 * Registering a Listener-Service without the property osgi.http.whiteboard.listener=true
	 * marks the listener as disabled
	 * @throws Exception propagate to JUnit
	 */
	@Test
	public void testListenersDisabled() throws Exception {
		ServiceRegistration<Servlet> registerService = registerServlet();

		MyServletRequestListener listener = new MyServletRequestListener();

		ServiceRegistration<ServletRequestListener> listenerService = bundleContext.registerService(ServletRequestListener.class, listener, null);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet name: value'",
						resp -> resp.contains("Servlet name: value"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myservlet");

		assertThat(listener.gotEvent(), is(false));

		listenerService.unregister();
		registerService.unregister();
	}

	/**
	 * Tests if a listener which is mapped on a HttpContext is called only in case of the right context.
	 * @throws Exception propagate to JUnit
	 */
	@Test
	public void testListenersWithHttpContext() throws Exception {
		// register context
		Dictionary<String, String> contextProps = new Hashtable<>();
		contextProps.put("osgi.http.whiteboard.context.name", "my-context");
		contextProps.put("osgi.http.whiteboard.context.path", "/myapp");

		CDNServletContextHelper context = new CDNServletContextHelper();
		ServiceRegistration<ServletContextHelper> contextHelperService = bundleContext
				.registerService(ServletContextHelper.class, context, contextProps);

		// register servlet
		Dictionary<String, String> extProps = new Hashtable<>();
		extProps.put("osgi.http.whiteboard.context.select", "(osgi.http.whiteboard.context.name=my-context)");
		ServiceRegistration<Servlet> registerServlet = registerServlet(extProps);

		// register listener with context
		MyServletRequestListener listenerWithContext = new MyServletRequestListener();
		Dictionary<String, String> properties = new Hashtable<>();
		properties.put("osgi.http.whiteboard.listener", "true");
		properties.put("osgi.http.whiteboard.context.select", "(osgi.http.whiteboard.context.name=my-context)");
		ServiceRegistration<ServletRequestListener> listenerWithContextServiceReg =
				bundleContext.registerService(ServletRequestListener.class, listenerWithContext, properties);

		// register listener without context
		MyServletRequestListener listenerNoContext = new MyServletRequestListener();
		properties = new Hashtable<>();
		properties.put("osgi.http.whiteboard.listener", "true");
		ServiceRegistration<ServletRequestListener> listenerNoContextServiceReg =
		bundleContext.registerService(ServletRequestListener.class, listenerNoContext, properties);

		// test servlet
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet name: value'",
						resp -> resp.contains("Servlet name: value"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myapp/myservlet");

		// context must have been called
		assertEquals(1, context.handleSecurityCalls.get());
		// listener attached to context must have been called
		assertThat(listenerWithContext.gotEvent(), is(true));
		// listener not attached to context must not been called
		assertThat(listenerNoContext.gotEvent(), is(false));

		listenerWithContextServiceReg.unregister();
		listenerNoContextServiceReg.unregister();
		registerServlet.unregister();
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

	private ServiceRegistration<Servlet> registerServlet() {
		return registerServlet(null);
	}

	private ServiceRegistration<Servlet> registerServlet(Dictionary<String, String> extendedProps) {
		Dictionary<String, String> properties = new Hashtable<>();
		properties.put("osgi.http.whiteboard.servlet.pattern", "/myservlet");
		properties.put("servlet.init.myname", "value");

		if (extendedProps != null) {
			Enumeration<String> keys = extendedProps.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				properties.put(key, extendedProps.get(key));
			}
		}

		return bundleContext.registerService(Servlet.class, new MyServlet(),
				properties);
	}

	private static class CDNServletContextHelper extends ServletContextHelper {
		final AtomicInteger handleSecurityCalls = new AtomicInteger();

		@Override
		public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
			handleSecurityCalls.incrementAndGet();
			return super.handleSecurity(request, response);
		}

		public URL getResource(String name) {
			try {
				return new URL("http://acmecdn.com/myapp/" + name);
			} catch (MalformedURLException e) {
				return null;
			}
		}
	}

	private static class MyServlet extends HttpServlet {

		private static final long serialVersionUID = 1L;

		private String name = "<not set>";

		public void init(ServletConfig config) {
			name = config.getInitParameter("myname");
		}

		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			resp.setContentType("text/plain");
			resp.getWriter().println("Servlet name: " + name);
		}
	}

	private static class AsyncServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;

		ExecutorService executor = Executors.newCachedThreadPool(r -> new Thread(r, "Pooled Thread"));

		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			doGetAsync(req.startAsync());
		}

		private void doGetAsync(AsyncContext asyncContext) {
			executor.submit(() -> {
				try {
					PrintWriter writer = asyncContext.getResponse().getWriter();
					writer.print("Servlet executed async in: " + Thread.currentThread()); // writes
					// 'Pooled
					// Thread'
				} finally {
					asyncContext.complete();
				}
				return null;
			});
		}
	}

	private static class MyFilter implements javax.servlet.Filter {
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			response.getWriter().write("before");
			chain.doFilter(request, response);
			response.getWriter().write("after");
		}

		public void destroy() {
		}
	}

	private static class MyServletRequestListener implements ServletRequestListener {

		private boolean event = false;

		public void requestInitialized(ServletRequestEvent sre) {
			event = true;
			System.out.println("Request initialized for client: " + sre.getServletRequest().getRemoteAddr());
		}

		public void requestDestroyed(ServletRequestEvent sre) {
			System.out.println("Request destroyed for client: " + sre.getServletRequest().getRemoteAddr());
		}

		boolean gotEvent() {
			return event;
		}
	}

	private static class MyResourceService {
	}
}
