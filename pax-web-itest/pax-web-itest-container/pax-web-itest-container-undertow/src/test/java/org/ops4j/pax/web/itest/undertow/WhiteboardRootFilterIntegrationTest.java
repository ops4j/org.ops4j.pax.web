package org.ops4j.pax.web.itest.undertow;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardRootFilterIntegrationTest extends ITestBase {

	private ServiceRegistration<Servlet> service;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureUndertow(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(VersionUtil.getProjectVersion())
						.noStart());

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {

		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", "/");
		service = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/"), initParams);

	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();

	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/", "Hello Whiteboard Extender");
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("urlPatterns", "*");
		ServiceRegistration<Filter> filter = bundleContext.registerService(
				Filter.class, new WhiteboardFilter(), props);

		testClient.testWebPath("http://127.0.0.1:8181/", "Filter was there before");

		filter.unregister();
	}

	@Test
	public void testWhiteBoardNotFiltered() throws Exception {
		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", "/whiteboard");
		ServiceRegistration<Servlet> whiteboard = bundleContext.registerService(
				Servlet.class, new WhiteboardServlet("/whiteboard"),
				initParams);

		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("urlPatterns", "/*");
		ServiceRegistration<Filter> filter = bundleContext.registerService(
				Filter.class, new WhiteboardFilter(), props);

		testClient.testWebPath("http://127.0.0.1:8181/", "Filter was there before");

		testClient.testWebPath("http://127.0.0.1:8181/whiteboard",
				"Filter was there before");

		filter.unregister();
		whiteboard.unregister();
	}

}
