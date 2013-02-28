package org.ops4j.pax.web.itest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.support.TestServlet;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpServiceIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws 	Exception {
		waitForServer("http://127.0.0.1:8181/");
		initServletListener();
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForServletListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			System.out.println("Bundle " + b.getBundleId() + " : "
					+ b.getSymbolicName());
		}

	}

	@Test
	public void testSubPath() throws Exception {

		testWebPath("http://127.0.0.1:8181/helloworld/hs", "Hello World");
		
		//test to retrive Image
		testWebPath("http://127.0.0.1:8181/images/logo.png", "", 200, false);
		
	}

	@Test
	public void testRootPath() throws Exception {

		testWebPath("http://127.0.0.1:8181/", "");

	}
	
	@Test
	public void testServletPath() throws Exception {

		testWebPath("http://127.0.0.1:8181/lall/blubb", "Servlet Path: ");
		testWebPath("http://127.0.0.1:8181/lall/blubb", "Path Info: /lall/blubb");

	}
	
	@Test
	public void testServletDeRegistration() throws BundleException, ServletException, NamespaceException {
		
		if (installWarBundle != null) {
			installWarBundle.stop();
		}
		// TODO check that deregistration worked
	}
	

	@Test
	public void testRegisterServlet() throws Exception {
		HttpService httpService = getHttpService(bundleContext);
		
		initServletListener();

		TestServlet servlet = new TestServlet();
		httpService.registerServlet("/test", servlet, null, null);
		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet.isInitCalled());
		
		waitForServletListener();
		
		testWebPath("http://127.0.0.1:8181/test", "TEST OK");
	}

	@Test
	public void testRegisterMultipleServlets() throws Exception {
		HttpService httpService = getHttpService(bundleContext);
		
		initServletListener();
		TestServlet servlet1 = new TestServlet();
		httpService.registerServlet("/test1", servlet1, null, null);
		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());
		waitForServletListener();

		initServletListener();
		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, null);
		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
		waitForServletListener();
		
		testWebPath("http://127.0.0.1:8181/test1", "TEST OK");
		testWebPath("http://127.0.0.1:8181/test2", "TEST OK");
	}

	/**
	 * This test registers a servlet using HttpService.registerServlet().
	 * It listens do the servlet-deployed event and then registers a second 
	 * servlet on the same context.
	 * It checks that Servlet.init() was called after every invocation of
	 * registerServlet() and that both servlets live in the same servlet context.
	 */
	@Test
	public void testRegisterMultipleServletsSameContext() throws Exception {
		final HttpService httpService = getHttpService(bundleContext);
		
		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<HttpContext>();
		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<HttpContext>();
		bundleContext.registerService(ServletListener.class.getName(), new ServletListener() {
			@Override
			public void servletEvent(ServletEvent servletEvent) {
				if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test1".equals(servletEvent.getAlias())) {
					httpContext1.set(servletEvent.getHttpContext());
				}
				if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
					httpContext2.set(servletEvent.getHttpContext());
				}
			}
		}, null);

		TestServlet servlet1 = new TestServlet();
		httpService.registerServlet("/test1", servlet1, null, null);
		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());

		for (int count = 0; count < 100; count++) {
			if (httpContext1.get() == null) {
				Thread.sleep(100);
			}
		}
		if (httpContext1.get() == null) {
			Assert.fail("Timout waiting for servlet event");
		}

		testWebPath("http://127.0.0.1:8181/test1", "TEST OK");
		
		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());
		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());

		for (int count = 0; count < 100; count++) {
			if (httpContext2.get() == null) {
				Thread.sleep(100);
			}
		}
		if (httpContext2.get() == null) {
			Assert.fail("Timout waiting for servlet event");
		}
		
		Assert.assertSame(httpContext1.get(), httpContext2.get());
		Assert.assertSame(servlet1.getServletContext(), servlet2.getServletContext());

		testWebPath("http://127.0.0.1:8181/test1", "TEST OK");
		testWebPath("http://127.0.0.1:8181/test2", "TEST OK");
	}

	/**
	 * This test registers a servlet to a already configured web context created
	 * by the war extender.
	 * It checks that Servlet.init() was called after the invocation of
	 * registerServlet() and that the servlet uses the same http context that
	 * the webapp uses.
	 */
	@Test
	public void testRegisterServletToWarContext() throws Exception {
		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<HttpContext>();
		bundleContext.registerService(WebListener.class.getName(), new WebListener() {
			@Override
			public void webEvent(WebEvent webEvent) {
				if (webEvent.getType() == WebEvent.DEPLOYED) {
					httpContext1.set(webEvent.getHttpContext());
				}
			}
		}, null);
		
		String bundlePath = WEB_BUNDLE 
				+ "mvn:org.ops4j.pax.web.samples/war-simple/" 
				+ getProjectVersion() 
				+ "/war?" 
				+ WEB_CONTEXT_PATH
				+ "=/war";
		Bundle installWarBundle = installAndStartBundle(bundlePath);
		
		for (int count = 0; count < 100; count++) {
			if (httpContext1.get() == null) {
				Thread.sleep(100);
			}
		}
		if (httpContext1.get() == null) {
			Assert.fail("Timout waiting for web event");
		}

		testWebPath("http://127.0.0.1:8181/war", "Hello, World, from JSP");
		
		// ---
		
		final HttpService httpService = getHttpService(installWarBundle.getBundleContext());
		
		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<HttpContext>();
		bundleContext.registerService(ServletListener.class.getName(), new ServletListener() {
			@Override
			public void servletEvent(ServletEvent servletEvent) {
				if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
					httpContext2.set(servletEvent.getHttpContext());
				}
			}
		}, null);
		
		TestServlet servlet2 = new TestServlet();
		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());
		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());

		for (int count = 0; count < 100; count++) {
			if (httpContext2.get() == null) {
				Thread.sleep(100);
			}
		}
		if (httpContext2.get() == null) {
			Assert.fail("Timout waiting for servlet event");
		}
		
		Assert.assertSame(httpContext1.get(), httpContext2.get());

		testWebPath("http://127.0.0.1:8181/war", "Hello, World, from JSP");
		testWebPath("http://127.0.0.1:8181/war/test2", "TEST OK");
	}	
	
	private HttpService getHttpService(BundleContext bundleContext) {
		ServiceReference ref = bundleContext.getServiceReference(HttpService.class.getName());
		Assert.assertNotNull("Failed to get HttpService", ref);
		HttpService httpService = (HttpService) bundleContext.getService(ref);
		Assert.assertNotNull("Failed to get HttpService", httpService);
		return httpService;
	}
	
	@Ignore
    @Test
    public void testRootFilterRegistration() throws Exception {
        ServiceTracker<WebContainer, WebContainer> tracker = new ServiceTracker<WebContainer, WebContainer>(bundleContext, WebContainer.class, null);
        tracker.open();
        WebContainer service = tracker.waitForService(TimeUnit.SECONDS.toMillis(20));
        final String fullContent = "This content is Filtered by a javax.servlet.Filter";
        Filter filter = new Filter() {

            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                PrintWriter writer = response.getWriter();
                writer.write(fullContent);
                writer.flush();
            }

            @Override
            public void destroy() {
            }
        };
        final StringWriter writer = new StringWriter();
        filter.doFilter(null, (ServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ServletResponse.class }, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("getWriter")) {
                    return new PrintWriter(writer);
                }
                return null;
            }
        }), null);
        //Check if our example filter do write the string to the writer...
        Assert.assertEquals(fullContent, writer.toString());
        //Now register the Filter under some alias...
        service.registerFilter(filter, new String[] { "*", "/*", "/", "/some/random/path" }, null, null, null);
        //If it works, always thw filter should take over and return the same string regardeless of the URL
        String expectedContent = "content is Filtered by";
        testWebPath("http://127.0.0.1:8181/some/random/path", expectedContent);
        testWebPath("http://127.0.0.1:8181/some/notregistered/random/path", expectedContent);
        testWebPath("http://127.0.0.1:8181/", expectedContent);
        //Even for existing path!
        testWebPath("http://127.0.0.1:8181/helloworld/hs", expectedContent);
        //And even for images
        testWebPath("http://127.0.0.1:8181/images/logo.png", expectedContent);
        //of course we should be able to deregister :-)
        service.unregisterFilter(filter);
        tracker.close();
    }
}
