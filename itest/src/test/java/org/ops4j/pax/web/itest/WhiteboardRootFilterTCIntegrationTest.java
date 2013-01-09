package org.ops4j.pax.web.itest;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardRootFilterTCIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WhiteboardRootFilterTCIntegrationTest.class);
	
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
	public void setUp() throws Exception {
		int count = 0;
		while (!checkServer("http://127.0.0.1:8282/") && count < 100) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		
		LOG.info("waiting for Server took {} ms", (count * 1000));
		
		initServletListener();
		
		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", "/");
		service = bundleContext.registerService(Servlet.class.getName(),
				new WhiteboardServlet("/"), initParams);

		waitForServletListener();
	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();

	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		testWebPath("http://127.0.0.1:8282/", "Hello Whiteboard Extender");
	}

	@Test
	@Ignore
	public void testWhiteBoardFiltered() throws Exception {
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("urlPatterns", "*");
		ServiceRegistration filter = bundleContext.registerService(
				Filter.class.getName(), new WhiteboardFilter(), props);

		testWebPath("http://127.0.0.1:8282/", "Filter was there before");

		filter.unregister();
	}

	@Test
	@Ignore
	public void testWhiteBoardNotFiltered() throws Exception {
		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", "/whiteboard");
		ServiceRegistration whiteboard = bundleContext.registerService(
				Servlet.class.getName(), new WhiteboardServlet("/whiteboard"),
				initParams);

		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("urlPatterns", "/*");
		ServiceRegistration filter = bundleContext.registerService(
				Filter.class.getName(), new WhiteboardFilter(), props);

		testWebPath("http://127.0.0.1:8282/", "Filter was there before");

		testWebPath("http://127.0.0.1:8282/whiteboard",
				"Filter was there before");

		filter.unregister();
		whiteboard.unregister();
	}

}
