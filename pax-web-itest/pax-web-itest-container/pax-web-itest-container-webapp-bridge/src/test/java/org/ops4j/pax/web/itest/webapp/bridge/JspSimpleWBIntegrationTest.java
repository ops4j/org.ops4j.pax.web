package org.ops4j.pax.web.itest.webapp.bridge;

import java.util.Dictionary;

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

import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * Integration test for simple JSP WAR web application
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class JspSimpleWBIntegrationTest extends ITestBase {

    private static final Logger LOG = LoggerFactory.getLogger(JspSimpleWBIntegrationTest.class);

    private Bundle installWarBundle;

    @Configuration
    public Option[] configure() {
        System.out.println("Configuring Test Bridge");
        return configureBridge();
    }

    @Before
    public void setUp() throws BundleException, InterruptedException {
        if (installWarBundle == null) {
            String bundlePath = WEB_BUNDLE
                    + "mvn:org.ops4j.pax.web.samples/war-simple/"
                    + VersionUtil.getProjectVersion() + "/war?"
                    + WEB_CONTEXT_PATH + "=/jsp-simple";
            installWarBundle = installAndStartBundle(bundlePath);
            System.out.println("Waiting for deployment to finish...");
            Thread.sleep(10000); // let the web.xml parser finish his job
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
    public void testSimpleJsp() throws Exception {

        testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/jsp-simple/index.jsp",
                "Hello, World, from JSP");
    }

}
