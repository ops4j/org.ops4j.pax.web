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

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class JspFilterIntegrationTest extends ITestBase {

    private static final Logger LOG = LoggerFactory.getLogger(JspFilterIntegrationTest.class);

    private Bundle installWarBundle;

    @Configuration
    public Option[] configure() {
        System.out.println("Configuring Test Bridge");
        return configureBridge();
    }

    @Before
    public void setUp() throws BundleException, InterruptedException {
        if (installWarBundle == null) {
            final String bundlePath = "mvn:org.ops4j.pax.web.samples/jsp-filter/" + VersionUtil.getProjectVersion()
                    + "/war";
            installWarBundle = installAndStartBundle(bundlePath);
            Thread.sleep(10000); // let the web.xml parser finish his job
        }
    }

    @After
    public void tearDown() throws BundleException {
    }

    @Test
    public void testSimpleJsp() throws Exception {

        testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/jsp-filter/", "Filtered");

    }
}
