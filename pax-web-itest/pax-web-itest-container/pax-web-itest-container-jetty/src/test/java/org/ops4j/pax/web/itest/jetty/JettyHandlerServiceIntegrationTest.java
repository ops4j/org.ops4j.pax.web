package org.ops4j.pax.web.itest.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class JettyHandlerServiceIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(JettyHandlerServiceIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return configureJetty();

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		final String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war/" + VersionUtil.getProjectVersion()
				+ "/war?" + WEB_CONTEXT_PATH + "=/test";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}


	@Test
	public void testWeb() throws Exception {
		testClient.testWebPath(retrieveBaseUrl()+"/test/wc/example",
				"<h1>Hello World</h1>");
	}

	@Test
	public void testStaticContent() throws Exception {
		
		/*
		  <New class="org.eclipse.jetty.server.handler.ContextHandler">
				<Set name="contextPath">/static-content</Set>
				<Set name="handler">
					<New class="org.eclipse.jetty.server.handler.ResourceHandler">
						<Set name="resourceBase">target/logs</Set>
						<Set name="directoriesListed">true</Set>
					</New>
				</Set>
		  </New>
		 */
		
		
		
		ContextHandler ctxtHandler = new ContextHandler();
		ctxtHandler.setContextPath("/static-content");
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setResourceBase("target");
		resourceHandler.setDirectoriesListed(true);
		ctxtHandler.setHandler(resourceHandler);
		
		ServiceRegistration<Handler> registerService = bundleContext.registerService(Handler.class, ctxtHandler, null);
		
		Thread.sleep(1000);
		
		waitForServer(retrieveBaseUrl()+"/");
		
		testClient.testWebPath(retrieveBaseUrl()+"/static-content/",
				"<A HREF=\"/static-content/");
		
		registerService.unregister();
	}
}
