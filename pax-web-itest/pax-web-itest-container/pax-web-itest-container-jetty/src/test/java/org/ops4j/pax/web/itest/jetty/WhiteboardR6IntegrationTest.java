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

//        testClient.testWebPath("http://127.0.0.1:8181/myservlet", "Servlet name: value");

        registerService.unregister();
    }

    @Test
//    @Ignore("Registration of ServletContextHelper isn't functional right now")
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

//        testClient.testWebPath("http://127.0.0.1:8181/myapp/myservlet", "Servlet name: value");

        assertEquals(1, context.handleSecurityCalls.get());

        registerServlet.unregister();
        contextHelperService.unregister();

    }

    @Test
    public void testErrorServlet() throws Exception {
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "java.io.IOException");
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "404");

        ServiceRegistration<Servlet> registerService = bundleContext.registerService(Servlet.class,
                new MyErrorServlet(), properties);

        HttpTestClientFactory.createDefaultTestClient()
                .withReturnCode(404)
                .withResponseAssertion("Response must contain 'Error Servlet, we do have a 404'",
                        resp -> resp.contains("Error Servlet, we do have a 404"))
                .doGETandExecuteTest("http://127.0.0.1:8181/error");

//        testClient.testWebPath("http://127.0.0.1:8181/error", "Error Servlet, we do have a 404", 404, false);

        registerService.unregister();
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

//        testClient.testAsyncWebPath("http://127.0.0.1:8181/as", "Servlet executed async in:", 200, false, null);

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

//        testClient.testWebPath("http://127.0.0.1:8181/myservlet", "before");

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

//        testClient.testWebPath("http://127.0.0.1:8181/myservlet", "Servlet name: value");
        
        assertThat(listener.gotEvent(), is(true));
        
        listenerService.unregister();
        registerService.unregister();
    }
    
    @Test
    public void testResources() throws Exception {
        
        Dictionary<String, String>properties = new Hashtable<>();
        properties.put("osgi.http.whiteboard.resource.pattern", "/files");
        properties.put("osgi.http.whiteboard.resource.prefix", "/images");
        
        ServiceRegistration<Object> registerService = bundleContext.registerService(Object.class, new MyResourceService(), properties);

        HttpTestClientFactory.createDefaultTestClient()
                .withResponseHeaderAssertion("Header 'Content-Type' must be 'image/png'",
                        headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
                                && header.getValue().equals("image/png")))
                .doGETandExecuteTest("http://127.0.0.1:8181/files/ops4j.png");

//        HttpResponse httpResponse = testClient.getHttpResponse(
//                "http://127.0.0.1:8181/files/ops4j.png", false, null, false);
//        Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
//        assertEquals("image/png", header.getValue());

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

    public class CDNServletContextHelper extends ServletContextHelper {
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

    public class MyServlet extends HttpServlet {

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

    public class MyErrorServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            resp.getWriter().println("Error Servlet, we do have a 404");
        }
    }

    public class AsyncServlet extends HttpServlet {
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

    public class MyFilter implements javax.servlet.Filter {
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

    public class MyServletRequestListener implements ServletRequestListener {
        
        private boolean event = false;
        
        public void requestInitialized(ServletRequestEvent sre) {
            event = true;
            System.out.println("Request initialized for client: " + sre.getServletRequest().getRemoteAddr());
        }

        public void requestDestroyed(ServletRequestEvent sre) {
            System.out.println("Request destroyed for client: " + sre.getServletRequest().getRemoteAddr());
        }
        
        public boolean gotEvent() {
            return event;
        }
    }
    
    public class MyResourceService {}
}
