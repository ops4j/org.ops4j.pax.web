package org.ops4j.pax.web.itest.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.spi.Probes.builder;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.extender.whiteboard.ResourceMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.support.SimpleFilter;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardResourceIntegrationTest extends ITestBase {

	private ServiceRegistration<ResourceMapping> service;
	private ServiceRegistration<Servlet> servlet;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
				.artifactId("whiteboard").version(VersionUtil.getProjectVersion())
				.noStart());

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {

		DefaultResourceMapping resourceMapping = new DefaultResourceMapping();
		resourceMapping.setAlias("/whiteboardresources");
		resourceMapping.setPath("/images");
		service = bundleContext.registerService(ResourceMapping.class,
				resourceMapping, null);
		
//		Dictionary<String, String> initParams = new Hashtable<String, String>();
//		initParams.put("alias", "/test-resources");
//		servlet = bundleContext.registerService(Servlet.class,
//				new WhiteboardServlet("/test-resources"), initParams);

	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();
//		servlet.unregister();
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		
		HttpResponse httpResponse = getHttpResponse(
				"http://127.0.0.1:8181/whiteboardresources/ops4j.png", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
		
	}

}
