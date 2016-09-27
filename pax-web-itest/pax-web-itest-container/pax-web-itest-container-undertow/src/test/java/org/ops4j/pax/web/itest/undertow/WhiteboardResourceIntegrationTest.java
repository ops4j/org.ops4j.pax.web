package org.ops4j.pax.web.itest.undertow;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.whiteboard.ResourceMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

import javax.servlet.Servlet;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

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
				configureUndertow(),
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
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		
		HttpResponse httpResponse = testClient.getHttpResponse(
				"http://127.0.0.1:8181/whiteboardresources/ops4j.png", false, null);
		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		assertEquals("image/png", header.getValue());
		
	}

}
