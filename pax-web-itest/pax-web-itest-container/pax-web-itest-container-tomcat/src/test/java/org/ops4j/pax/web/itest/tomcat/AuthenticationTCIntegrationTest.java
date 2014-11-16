package org.ops4j.pax.web.itest.tomcat;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.samples.authentication.AuthHttpContext;
import org.ops4j.pax.web.samples.authentication.StatusServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@Ignore("Fails for unknown reason")
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class AuthenticationTCIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		String bundlePath = "mvn:org.ops4j.pax.web.samples/authentication/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = bundleContext.installBundle(bundlePath);
		
		installWarBundle.start();
		
		new WaitCondition("authentication - resolved bundle") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return installWarBundle.getState() == Bundle.ACTIVE;
			}
		}.waitForCondition(); 

		
		installWarBundle.stop();
		
		new WaitCondition("authentication - resolved bundle") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return installWarBundle.getState() == Bundle.RESOLVED;
			}
		}.waitForCondition(); 

		waitForServer("http://127.0.0.1:8282/");
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
		
		installWarBundle = null;
	}

	@Test
	public void testStatus() throws Exception {

		ServiceReference<HttpService> httpServiceRef = bundleContext
				.getServiceReference(HttpService.class);

		assertNotNull(httpServiceRef);
		HttpService httpService = (HttpService) bundleContext
				.getService(httpServiceRef);

		httpService.registerServlet("/status", new StatusServlet(), null, null);

		testClient.testWebPath("http://127.0.0.1:8282/status",
				"org.osgi.service.http.authentication.type : null");

		httpService.unregister("/status");
		bundleContext.ungetService(httpServiceRef);
	}

	@Test
	public void testStatusAuth() throws Exception {

		initServletListener(null);

		ServiceReference<HttpService> httpServiceRef = bundleContext
				.getServiceReference(HttpService.class);
		assertNotNull(httpServiceRef);
		HttpService httpService = (HttpService) bundleContext
				.getService(httpServiceRef);
		httpService.registerServlet("/status-with-auth", new StatusServlet(),
				null, new AuthHttpContext());

		waitForServletListener();

		testClient.testWebPath("http://127.0.0.1:8282/status-with-auth",
				"org.osgi.service.http.authentication.type : BASIC");

		httpService.unregister("/status-with-auth");
		bundleContext.ungetService(httpServiceRef);

	}

}
