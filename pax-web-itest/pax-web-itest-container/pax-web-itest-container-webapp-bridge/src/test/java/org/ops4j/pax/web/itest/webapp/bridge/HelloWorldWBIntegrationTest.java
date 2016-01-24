package org.ops4j.pax.web.itest.webapp.bridge;

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
        return configureBridge();
    }

    @Before
    public void setUp() throws BundleException, InterruptedException {
        String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
                + VersionUtil.getProjectVersion() + "/jar";
        installWarBundle = installAndStartBundle(bundlePath);

    }

    @After
    public void tearDown() throws BundleException {
        if (installWarBundle != null) {
            installWarBundle.stop();
            installWarBundle.uninstall();
        }
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
    public void testHelloWorldServlet() throws Exception {

        Thread.sleep(6000); // let the web.xml parser finish his job

        testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/helloworld/wc",
                "Hello World");
    }

}
