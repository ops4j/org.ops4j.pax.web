package org.ops4j.pax.web.itest.jetty;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.ops4j.pax.web.itest.jetty.support.AsyncServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * @author Grzegorz Grzybek
 * @since Apr 1, 2016
 */
@RunWith(PaxExam.class)
public class AsyncServletTest extends ITestBase {

    @Configuration
    public static Option[] configure() {
        return configureJetty();

    }

    private HttpService getHttpService(BundleContext bundleContext) {
        ServiceReference<HttpService> ref = bundleContext.getServiceReference(HttpService.class);
        return bundleContext.getService(ref);
    }

    @Before
    public void setUp() throws Exception {
        waitForServer("http://127.0.0.1:8181/");
        HttpService httpService = getHttpService(bundleContext);

        initServletListener(null);

        // servlets in different contexts
        httpService.registerServlet("/test", new TestServlet(), null, new CustomHttpContext(httpService.createDefaultHttpContext()));
        httpService.registerServlet("/async", new AsyncServlet(), null, new CustomHttpContext(httpService.createDefaultHttpContext()));
        waitForServletListener();
    }

    @Test
    public void testAsyncResponse() throws Exception {
        byte[] bytes = new byte[AsyncServlet.SIZE];
        Arrays.fill(bytes, (byte)0x42);
        String expected = new String(bytes);
        testClient.testWebPath("http://127.0.0.1:8181/async", expected);
    }

    public static class CustomHttpContext implements HttpContext {

        private HttpContext delegate;

        public CustomHttpContext(HttpContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
            return delegate.handleSecurity(request, response);
        }

        @Override
        public URL getResource(String name) {
            return delegate.getResource(name);
        }

        @Override
        public String getMimeType(String name) {
            return delegate.getMimeType(name);
        }

    }

}
