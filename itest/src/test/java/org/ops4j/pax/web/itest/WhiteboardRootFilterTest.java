package org.ops4j.pax.web.itest;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(JUnit4TestRunner.class)
public class WhiteboardRootFilterTest extends ITestBase {

	private ServiceRegistration service;

	@Configuration
	public static Option[] configure() {
		Option[] options = baseConfigure();

		Option[] options2 = options(mavenBundle()
				.groupId("org.ops4j.pax.web.samples")
				.artifactId("whiteboard")
				.version(getProjectVersion()));

		List<Option> list = new ArrayList<Option>(Arrays.asList(options));
		list.addAll(Arrays.asList(options2));

		return (Option[]) list.toArray(new Option[list.size()]);
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		
		
		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", "/");
		service = bundleContext.registerService(Servlet.class.getName(),
				new WhiteboardServlet("/"), initParams);

		

	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();

	}

	@Test
	public void testWhiteBoardSlash() throws BundleException,
			InterruptedException, IOException {
		testWebPath("http://127.0.0.1:8181/", "Hello Whiteboard Extender");
	}

	@Test
	public void testWhiteBoardFiltered() throws BundleException,
			InterruptedException, IOException {
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("urlPatterns", "*");
		ServiceRegistration filter = bundleContext.registerService(Filter.class.getName(),
				new WhiteboardFilter(), props);
		
		testWebPath("http://127.0.0.1:8181/", "Filter was there before");
		
		filter.unregister();
	}

}
