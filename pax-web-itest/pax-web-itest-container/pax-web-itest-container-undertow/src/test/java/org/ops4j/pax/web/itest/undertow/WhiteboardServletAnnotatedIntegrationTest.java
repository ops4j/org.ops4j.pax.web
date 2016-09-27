package org.ops4j.pax.web.itest.undertow;

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

import javax.servlet.Filter;
import javax.servlet.Servlet;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
@RunWith(PaxExam.class)
public class WhiteboardServletAnnotatedIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return combine(configureUndertow());
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
	public void testWhiteboardServletRegistrationDestroyCalled() throws Exception {

		AnnotatedTestServlet annotatedTestServlet = new AnnotatedTestServlet();
		
		ServiceRegistration<Servlet> servletRegistration = bundleContext
				.registerService(Servlet.class, annotatedTestServlet,
						null);

		try {
			testClient.testWebPath("http://127.0.0.1:8181/test", "TEST OK");
		} finally {
			servletRegistration.unregister();
		}
		
		assertThat(annotatedTestServlet.isInitCalled(), is(true));
		assertThat(annotatedTestServlet.isDestroyCalled(), is(true));
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
