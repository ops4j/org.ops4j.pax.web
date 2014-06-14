package org.ops4j.pax.web.itest.tomcat;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

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
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class WhiteboardRootFilterTCIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WhiteboardRootFilterTCIntegrationTest.class);
	
	private ServiceRegistration<Servlet> service;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureTomcat(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(VersionUtil.getProjectVersion())
						.noStart());

	}

	@Before
	public void setUp() throws Exception {
		int count = 0;
		while (!testClient.checkServer("http://127.0.0.1:8282/") && count < 100) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
		
		LOG.info("waiting for Server took {} ms", (count * 1000));
		
		initServletListener(null);
		
		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put(ExtenderConstants.PROPERTY_ALIAS, "/");
		service = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/"), initParams);

		waitForServletListener();
	}

	@After
	public void tearDown() throws Exception {
		service.unregister();
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8282/", "Hello Whiteboard Extender");
	}

	/**
	 * this test is supposed to prove that a servlet-filter is bound to the servlet. 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWhiteBoardFiltered() throws Exception {
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("urlPatterns", "*");
		ServiceRegistration<Filter> filter = bundleContext.registerService(
				Filter.class, new WhiteboardFilter(), props);

		testClient.testWebPath("http://127.0.0.1:8282/", "Filter was there before");

		filter.unregister();
	}

	/**
	 * This test should show that serlvets and filters can be added to a default http Context
	 * 
	 * @throws Exception
	 */
	@Test
//	@Ignore
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

		Thread.sleep(1000);
		
		testClient.testWebPath("http://127.0.0.1:8282/", "Filter was there before");

		testClient.testWebPath("http://127.0.0.1:8282/whiteboard", "Filter was there before");

		filter.unregister();
		whiteboard.unregister();
	}

}
