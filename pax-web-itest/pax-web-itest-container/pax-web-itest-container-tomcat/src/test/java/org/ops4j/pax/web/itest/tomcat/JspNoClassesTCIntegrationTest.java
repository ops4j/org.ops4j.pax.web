package org.ops4j.pax.web.itest.tomcat;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;


@RunWith(PaxExam.class)
public class JspNoClassesTCIntegrationTest extends ITestBase {

	private Bundle installWarBundle;
	
	@Configuration
	public static Option[] configure() {
		return configureTomcat();
	}


	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-jsp-noclasses/" + VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
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
	@Ignore
	public void testSimpleJsp() throws Exception {

		testClient.testWebPath("http://localhost:8181/jspnc/welcome.jsp", "Welcome");
			
	}
	
}