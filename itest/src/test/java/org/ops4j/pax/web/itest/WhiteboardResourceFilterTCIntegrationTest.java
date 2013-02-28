package org.ops4j.pax.web.itest;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.itest.support.SimpleFilter;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardResourceFilterTCIntegrationTest extends ITestBase {

	private ServiceRegistration service;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureTomcat(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(getProjectVersion())
						.noStart());

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {

		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", "/test-resources");
		service = bundleContext.registerService(Servlet.class.getName(),
				new WhiteboardServlet("/test-resources"), initParams);

	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();

	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("urlPatterns", "*");
		SimpleFilter simpleFilter = new SimpleFilter();
		ServiceRegistration filter = bundleContext.registerService(
				Filter.class.getName(), simpleFilter, props);

		testWebPath("http://127.0.0.1:8282/test-resources",
				"Hello Whiteboard Extender");

		URL resource = simpleFilter.getResource();
		assertNotNull(resource);

		filter.unregister();

	}

}
