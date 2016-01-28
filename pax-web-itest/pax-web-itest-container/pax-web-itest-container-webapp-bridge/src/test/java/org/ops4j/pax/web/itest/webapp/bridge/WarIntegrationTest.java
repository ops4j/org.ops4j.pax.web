package org.ops4j.pax.web.itest.webapp.bridge;

import static org.junit.Assert.fail;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class WarIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WarIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
	    System.out.println("Configuring Test Bridge");
        return configureBridge();
	}


	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");
		
		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war/"
				+ VersionUtil.getProjectVersion() + "/war?"
				+ WEB_CONTEXT_PATH + "=/war";
		installWarBundle = getBundleContext().installBundle(bundlePath);
		installWarBundle.start();
		System.out.println("Waiting for deployment to finish...");
        Thread.sleep(10000); // let the web.xml parser finish his job
	}

	@After
	public void tearDown() throws BundleException {
	}

	@Test
	public void testWC() throws Exception {

		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wc", "<h1>Hello World</h1>");
			
	}

	@Test
	public void testImage() throws Exception {

		testClient
				.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/images/logo.png",
				200);

	}

	@Test
	public void testFilterInit() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wc", "Have bundle context in filter: true");
	}
	
	@Test
	public void testStartStopBundle() throws Exception {
		LOG.debug("start/stopping bundle");

		installWarBundle.stop();
		
		installWarBundle.start();

        System.out.println("Waiting for deployment to finish...");
        Thread.sleep(10000); // let the web.xml parser finish his job
		
		LOG.debug("Update done, testing bundle");

		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wc", "<h1>Hello World</h1>");
			
	}

	
	@Test
	public void testUpdateBundle() throws Exception {
		LOG.debug("updating bundle");


		installWarBundle.update();
		
		LOG.info("Update done, testing bundle");

		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wc", "<h1>Hello World</h1>");
			
	}
	
	@Test
	public void testWebContainerExample() throws Exception {
			
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wc/example", "<h1>Hello World</h1>");

		
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/images/logo.png", "", 200, false);
		
	}
	
	@Test
	public void testWebContainerSN() throws Exception {

		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wc/sn", "<h1>Hello World</h1>");

	}
	
	@Test
	public void testSlash() throws Exception {
			
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/", "<h1>Error Page</h1>", 404, false);

	}
	
	
	@Test
	public void testSubJSP() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wc/subjsp", "<h2>Hello World!</h2>");
	}
	
	@Test
	public void testErrorJSPCall() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wc/error.jsp", "<h1>Error Page</h1>", 404, false);
	}
	
	@Test
	public void testWrongServlet() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wrong/", "<h1>Error Page</h1>", 404, false);
	}

	@Test
	public void testTalkativeServlet() throws Exception {
		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/war/wc/talkative", "<h1>Silent Servlet activated</h1>");
	}

}

