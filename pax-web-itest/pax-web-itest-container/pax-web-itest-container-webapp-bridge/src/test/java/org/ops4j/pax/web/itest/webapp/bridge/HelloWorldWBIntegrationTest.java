package org.ops4j.pax.web.itest.webapp.bridge;

import org.junit.*;
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

import java.util.Dictionary;

/**
 * Created by loom on 24.01.16.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class HelloWorldWBIntegrationTest extends ITestBase {

    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldWBIntegrationTest.class);

    private Bundle installWarBundle;

    @Configuration
    public Option[] configure() {
        System.out.println("Configuring Test Bridge");
        return configureBridge();
    }

    @Before
    public void setUp() throws BundleException, InterruptedException {
        if (installWarBundle == null) {
            String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
                    + VersionUtil.getProjectVersion() + "/jar";
            installWarBundle = installAndStartBundle(bundlePath);
            System.out.println("Waiting for deployment to finish...");
            Thread.sleep(6000); // let the web.xml parser finish his job
        }
    }

    @After
    public void tearDown() throws BundleException, InterruptedException {
        /*
        if (installWarBundle != null) {
            installWarBundle.stop();
            installWarBundle.uninstall();
            Thread.sleep(6000); // let the web.xml parser finish his job
            installWarBundle = null;
        }
        */
    }

    /**
     * You will get a list of bundles installed by default plus your testcase,
     * wrapped into a bundle called pax-exam-probe
     */
    @Test
    public void listBundles() {
        for (Bundle b : getBundleContext().getBundles()) {
            Dictionary<String,String> headers = b.getHeaders();

            String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
            if (ctxtPath != null) {
                System.out.println("Bundle " + b.getBundleId() + " : "
                        + b.getSymbolicName() + " : " + ctxtPath + " ("+b.getState()+")");
            } else {
                System.out.println("Bundle " + b.getBundleId() + " : "
                        + b.getSymbolicName() + " ("+b.getState()+")");
            }
        }

    }

    @Test
    public void testHelloWorldServletAndFilterByUrlPattern() throws Exception {


        String result = testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/helloworld/wc",
                "Hello World");
        Assert.assertTrue("Missing filter output", result.contains("<title>Hello World (url pattern)</title>"));
    }

    @Test
    public void testHelloWorldServletAndFilterByName() throws Exception {

        String result = testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/helloworld/wc/sn",
                "Hello World");
        Assert.assertTrue("Missing filter output", result.contains("<title>Hello World (servlet name)</title>"));
    }

    @Test
    public void testGeneratedError() throws Exception {

        String result = testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/helloworld/wc/error/create?type=java.lang.IllegalArgumentException",
                "Hello World Error Page", 200, false);

    }

    @Test
    public void testNotExistingPage() throws Exception {

        String result = testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/helloworld/a.page.that.not.exis",
                "Hello World Error Page", 404, false);

    }

    @Test
    public void testHelloWorldWelcomeFile() throws Exception {

        String result = testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/html",
                "Welcome");

    }

}
