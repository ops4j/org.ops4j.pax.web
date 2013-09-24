package org.ops4j.pax.web.itest.jetty;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class FilterIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(FilterIntegrationTest.class);

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
	}

	@After
	public void tearDown() throws BundleException {
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (final Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE) {
				fail("Bundle should be active: " + b);
			}

			final Dictionary<String,String> headers = b.getHeaders();
			final String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

	}

	@Test
	public void testSimpleFilter() throws Exception {
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
        
        Dictionary<String, String> initParams = new Hashtable<String, String>();
        initParams.put(WebContainerConstants.FILTER_MAPPING_DISPATCHER, "include");
		
        HttpContext defaultHttpContext = service.createDefaultHttpContext();
        service.begin(defaultHttpContext);
        service.registerResources("/", "default", defaultHttpContext);
        
        service.registerFilter(filter, new String[] { "/testFilter/*", }, null, initParams, defaultHttpContext);
        
        service.end(defaultHttpContext);
        
        Thread.sleep(200);
        
        testWebPath("http://127.0.0.1:8181/testFilter/filter.me",
				"This content is Filtered by a javax.servlet.Filter");
        
        service.unregisterFilter(filter);
	}
	
	@Test
	@Ignore
	public void testFilterWar() throws Exception{
		String bundlePath = WEB_BUNDLE 
				+ "mvn:org.ops4j.pax.web.samples/simple-filter/" 
				+ VersionUtil.getProjectVersion() 
				+ "/war?" 
				+ WEB_CONTEXT_PATH
				+ "=/web-filter";
		Bundle installWarBundle = installAndStartBundle(bundlePath);
		
		testWebPath("http://127.0.0.1:8181/web-filter/me.filter",
				"Filtered");
		
		installWarBundle.uninstall();
        
	}
}
