package org.ops4j.pax.web.itest;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.UnavailableException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.support.DocumentServlet;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardAliasIntegrationTest extends ITestBase {

	private ServiceRegistration service;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(getProjectVersion())
						.noStart());

	}

	@Before
	public void setUp() throws BundleException, InterruptedException,
			UnavailableException {

		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", "/");
		DocumentServlet documentServlet = new DocumentServlet();
		documentServlet.activate();
		service = bundleContext.registerService(Servlet.class.getName(),
				documentServlet, initParams);

	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();

	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		testWebPath("http://127.0.0.1:8181/", "<H1>Directory: /</H1>");
	}

}
