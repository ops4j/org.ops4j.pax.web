package org.ops4j.pax.web.itest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.web.jsp.JasperClassLoader;
import org.ops4j.pax.web.jsp.JspServletWrapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * The tests contained here will test the usage of the PAX Web Jsp directly with the HttpService, without
 * the need for a full servlet container environment. This is useful when integrating PAX Web JSP into an
 * existing servlet container using an HTTP Bridge service implementation such as the Felix Http bridge
 * service implementation.
 *
 * This test validates the correction for PAXWEB-497 as well as the new functionality from PAXWEB-498.
 *
 * @author Serge Huber
 */
@RunWith(PaxExam.class)
public class JspSelfRegistrationIntegrationTest extends ITestBase {

    @Configuration
   	public static Option[] configure() {
   		return configureJetty();
   	}

   	@Before
   	public void setUp() throws 	Exception {
   		waitForServer("http://127.0.0.1:8181/");
   		initServletListener();
   		waitForServletListener();
   	}

   	@After
   	public void tearDown() throws BundleException {
   	}

    /**
     * Test the class loader parent bug described in PAXWEB-497
     * @throws Exception
     */
   	@Test
   	public void testJSPEngineClassLoaderParent() throws Exception {
   		HttpService httpService = getHttpService(bundleContext);

   		initServletListener();

        String urlAlias = "/jsp/jspSelfRegistrationTest.jsp";
        JspServletWrapper servlet = new JspServletWrapper(bundleContext.getBundle(), urlAlias);
        HttpContext customHttpContext = httpService.createDefaultHttpContext();
   		httpService.registerServlet(urlAlias, servlet, null, customHttpContext);

   		waitForServletListener();

   		testWebPath("http://127.0.0.1:8181" + urlAlias, "TEST OK");

        Assert.assertEquals("Class loader " + servlet.getClassLoader().getParent() + " is not expected class loader parent",
                JasperClassLoader.class.getClassLoader(),
                servlet.getClassLoader().getParent());

        httpService.unregister(urlAlias);
   	}

    private class LoggingJasperClassLoader extends JasperClassLoader {

        StringBuilder logBuilder = new StringBuilder();

        public LoggingJasperClassLoader(Bundle bundle, ClassLoader parent) {
            super(bundle, parent);
        }

        @Override
        public URL getResource(String name) {
            logBuilder.append("getResource(" + name + ")\n");
            return super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            logBuilder.append("getResources(" + name + ")\n");
            return super.getResources(name);
        }

        @Override
        public Class loadClass(String name) throws ClassNotFoundException {
            logBuilder.append("loadClass(" + name + ")\n");
            return super.loadClass(name);
        }

        public StringBuilder getLogBuilder() {
            return logBuilder;
        }
    }

    /**
     * Tests the custom class loader described in PAXWEB-498
     * @throws Exception
     */
    @Test
   	public void testJSPEngineCustomClassLoader() throws Exception {
   		HttpService httpService = getHttpService(bundleContext);

   		initServletListener();

        String urlAlias = "/jsp/jspSelfRegistrationTest.jsp";
        LoggingJasperClassLoader loggingJasperClassLoader = new LoggingJasperClassLoader(bundleContext.getBundle(), JasperClassLoader.class.getClassLoader());
        JspServletWrapper servlet = new JspServletWrapper(urlAlias, loggingJasperClassLoader );
        HttpContext customHttpContext = httpService.createDefaultHttpContext();
   		httpService.registerServlet(urlAlias, servlet, null, customHttpContext);

   		waitForServletListener();

   		testWebPath("http://127.0.0.1:8181" + urlAlias, "TEST OK");

        String classLoaderLog = loggingJasperClassLoader.getLogBuilder().toString();
        System.out.println("classLoaderLog:\n" + classLoaderLog);
        Assert.assertTrue("Logging class loader didn't log anything !", classLoaderLog.length() > 0);

        httpService.unregister(urlAlias);
   	}

    private HttpService getHttpService(BundleContext bundleContext) {
   		ServiceReference ref = bundleContext.getServiceReference(HttpService.class.getName());
   		Assert.assertNotNull("Failed to get HttpService", ref);
   		HttpService httpService = (HttpService) bundleContext.getService(ref);
   		Assert.assertNotNull("Failed to get HttpService", httpService);
   		return httpService;
   	}

}
