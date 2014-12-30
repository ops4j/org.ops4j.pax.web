package org.ops4j.pax.web.itest.jetty;

import static org.ops4j.pax.exam.OptionUtils.combine;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.support.AnnotatedTestFilter;
import org.ops4j.pax.web.itest.base.support.AnnotatedTestServlet;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
@RunWith(PaxExam.class)
public class WhiteboardServletAnnotatedIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return combine(configureJetty());
	}

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws BundleException {
	}

	@Test
	public void testWhiteboardServletRegistration() throws Exception {

		ServiceRegistration<Servlet> servletRegistration = bundleContext
				.registerService(Servlet.class, new AnnotatedTestServlet(),
						null);

		try {
			testClient.testWebPath("http://127.0.0.1:8181/test", "TEST OK");
		} finally {
			servletRegistration.unregister();
		}

	}

	@Test
	public void testWhiteboardFilterRegistration() throws Exception {

		ServiceRegistration<Servlet> servletRegistration = bundleContext
				.registerService(Servlet.class, new AnnotatedTestServlet(),
						null);

		ServiceRegistration<Filter> filterRegistration = bundleContext
				.registerService(Filter.class, new AnnotatedTestFilter(), null);

		try {
			testClient.testWebPath("http://127.0.0.1:8181/test", "TEST OK");

			testClient.testWebPath("http://127.0.0.1:8181/test", "FILTER-INIT: true");
		} finally {
			servletRegistration.unregister();
		}

	}
}
