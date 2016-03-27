package org.ops4j.pax.web.itest.webapp.bridge;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class HttpServiceIntegrationTest extends ITestBase {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
		System.out.println("Configuring Test Bridge");
		return configureBridge();
	}

    @Before
    public void setUp() throws BundleException, InterruptedException {
        LOG.info("Setting up test");
        String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + VersionUtil.getProjectVersion();
        installWarBundle = installAndStartBundle(bundlePath);
        System.out.println("Waiting for deployment to finish...");
        Thread.sleep(10000); // let the web.xml parser finish his job
    }

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
        for (Bundle b : getBundleContext().getBundles()) {
            Dictionary<String,String> headers = b.getHeaders();

            String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
            if (ctxtPath != null) {
                System.out.println("Bundle " + b.getBundleId() + " : "
                        + b.getSymbolicName() + " : " + ctxtPath + " ("+b.getState()+")");
            } else {
                System.out.println("Bundle " + b.getBundleId() + " : "
                        + b.getSymbolicName() + " ("+b.getState()+")");
            }
        }
	}

	@Test
	public void testSubPath() throws Exception {

		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/helloworld/hs", "Hello World");
		
		//test to retrive Image
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/images/logo.png", "", 200, false);
		
	}

	@Test
	public void testRootPath() throws Exception {

		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/", "");

	}
	
	@Test
	public void testServletPath() throws Exception {

		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/lall/blubb", "Servlet Path: ");
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/lall/blubb", "Path Info: /lall/blubb");

	}
	
	@Test
	public void testServletDeRegistration() throws BundleException {

		if (installWarBundle != null) {
			installWarBundle.stop();
		}
		// TODO check that deregistration worked
	}


//	@Test
//	public void testRegisterServlet() throws Exception {
//		HttpService httpService = getHttpService(getBundleContext());
//
//		TestServlet servlet = new TestServlet();
//		httpService.registerServlet("/test", servlet, null, null);
//		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet.isInitCalled());
//
//		testClient.testWebPath("http://127.0.0.1:8181/test", "TEST OK");
//	}
//
//	@Test
//	public void testRegisterMultipleServlets() throws Exception {
//		HttpService httpService = getHttpService(getBundleContext());
//
//		TestServlet servlet1 = new TestServlet();
//		httpService.registerServlet("/test1", servlet1, null, null);
//		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());
//
//		TestServlet servlet2 = new TestServlet();
//		httpService.registerServlet("/test2", servlet2, null, null);
//		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
//
//		testClient.testWebPath("http://127.0.0.1:8181/test1", "TEST OK");
//		testClient.testWebPath("http://127.0.0.1:8181/test2", "TEST OK");
//	}
//
//	/**
//	 * This test registers a servlet using HttpService.registerServlet().
//	 * It listens do the servlet-deployed event and then registers a second
//	 * servlet on the same context.
//	 * It checks that Servlet.init() was called after every invocation of
//	 * registerServlet() and that both servlets live in the same servlet context.
//	 */
//	@Test
//	public void testRegisterMultipleServletsSameContext() throws Exception {
//		final HttpService httpService = getHttpService(getBundleContext());
//
//		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<HttpContext>();
//		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<HttpContext>();
//		getBundleContext().registerService(ServletListener.class, new ServletListener() {
//			@Override
//			public void servletEvent(ServletEvent servletEvent) {
//				if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test1".equals(servletEvent.getAlias())) {
//					httpContext1.set(servletEvent.getHttpContext());
//				}
//				if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
//					httpContext2.set(servletEvent.getHttpContext());
//				}
//			}
//		}, null);
//
//		TestServlet servlet1 = new TestServlet();
//		httpService.registerServlet("/test1", servlet1, null, null);
//		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet1.isInitCalled());
//
//		for (int count = 0; count < 100; count++) {
//			if (httpContext1.get() == null) {
//				Thread.sleep(100);
//			}
//		}
//		if (httpContext1.get() == null) {
//			Assert.fail("Timout waiting for servlet event");
//		}
//
//		testClient.testWebPath("http://127.0.0.1:8181/test1", "TEST OK");
//
//		TestServlet servlet2 = new TestServlet();
//		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());
//		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
//
//		for (int count = 0; count < 100; count++) {
//			if (httpContext2.get() == null) {
//				Thread.sleep(100);
//			}
//		}
//		if (httpContext2.get() == null) {
//			Assert.fail("Timout waiting for servlet event");
//		}
//
//		Assert.assertSame(httpContext1.get(), httpContext2.get());
//		Assert.assertSame(servlet1.getServletContext(), servlet2.getServletContext());
//
//		testClient.testWebPath("http://127.0.0.1:8181/test1", "TEST OK");
//		testClient.testWebPath("http://127.0.0.1:8181/test2", "TEST OK");
//	}

	/**
	 * This test registers a servlet to a already configured web context created
	 * by the war extender.
	 * It checks that Servlet.init() was called after the invocation of
	 * registerServlet() and that the servlet uses the same http context that
	 * the webapp uses.
	 */
//	@Test
//	public void testRegisterServletToWarContext() throws Exception {
//		final AtomicReference<HttpContext> httpContext1 = new AtomicReference<HttpContext>();
//		bundleContext.registerService(WebListener.class, new WebListener() {
//			@Override
//			public void webEvent(WebEvent webEvent) {
//				if (webEvent.getType() == WebEvent.DEPLOYED) {
//					httpContext1.set(webEvent.getHttpContext());
//				}
//			}
//		}, null);
//
//		LOG.debug("installing war-simple war");
//
//		String bundlePath = WEB_BUNDLE
//				+ "mvn:org.ops4j.pax.web.samples/war-simple/"
//				+ VersionUtil.getProjectVersion()
//				+ "/war?"
//				+ WEB_CONTEXT_PATH
//				+ "=/war";
//		Bundle installWarBundle = installAndStartBundle(bundlePath);
//
//		for (int count = 0; count < 100; count++) {
//			if (httpContext1.get() == null) {
//				Thread.sleep(100);
//			}
//		}
//		if (httpContext1.get() == null) {
//			Assert.fail("Timout waiting for web event");
//		}
//
//		LOG.debug("context registered, calling web request ...");
//
//		testClient.testWebPath("http://127.0.0.1:8181/war", "Hello, World, from JSP");
//
//		// ---
//
//		final HttpService httpService = getHttpService(installWarBundle.getBundleContext());
//
//		LOG.debug("... adding additional content to war");
//
//		final AtomicReference<HttpContext> httpContext2 = new AtomicReference<HttpContext>();
//		bundleContext.registerService(ServletListener.class, new ServletListener() {
//			@Override
//			public void servletEvent(ServletEvent servletEvent) {
//				if (servletEvent.getType() == ServletEvent.DEPLOYED && "/test2".equals(servletEvent.getAlias())) {
//					httpContext2.set(servletEvent.getHttpContext());
//				}
//			}
//		}, null);
//
//		TestServlet servlet2 = new TestServlet();
//		httpService.registerServlet("/test2", servlet2, null, httpContext1.get());
//		Assert.assertTrue("Servlet.init(ServletConfig) was not called", servlet2.isInitCalled());
//
//		for (int count = 0; count < 100; count++) {
//			if (httpContext2.get() == null) {
//				Thread.sleep(100);
//			}
//		}
//		if (httpContext2.get() == null) {
//			Assert.fail("Timout waiting for servlet event");
//		}
//
//		Assert.assertSame(httpContext1.get(), httpContext2.get());
//
//		testClient.testWebPath("http://127.0.0.1:8181/war", "Hello, World, from JSP");
//		testClient.testWebPath("http://127.0.0.1:8181/war/test2", "TEST OK");
//	}
	
//	private HttpService getHttpService(BundleContext bundleContext) {
//		ServiceReference<HttpService> ref = bundleContext.getServiceReference(HttpService.class);
//		Assert.assertNotNull("Failed to get HttpService", ref);
//		HttpService httpService = (HttpService) bundleContext.getService(ref);
//		Assert.assertNotNull("Failed to get HttpService", httpService);
//		return httpService;
//	}
	
}
