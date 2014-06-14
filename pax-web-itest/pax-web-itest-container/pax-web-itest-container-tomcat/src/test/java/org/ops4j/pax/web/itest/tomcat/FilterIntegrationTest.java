package org.ops4j.pax.web.itest.tomcat;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.service.WebContainer;
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
		return configureTomcat();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
	}

	@After
	public void tearDown() throws BundleException {
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
        
        HttpContext defaultHttpContext = service.createDefaultHttpContext();
        service.begin(defaultHttpContext);
        service.registerResources("/", "default", defaultHttpContext);
        
        service.registerFilter(filter, new String[] { "/testFilter/*", }, new String[] {"default",}, initParams, defaultHttpContext);
        
        service.end(defaultHttpContext);
        
        Thread.sleep(200);
        
        testClient.testWebPath("http://127.0.0.1:8282/testFilter/filter.me",
				"This content is Filtered by a javax.servlet.Filter");
        
        service.unregisterFilter(filter);
	}
	
}
