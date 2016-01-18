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
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

/**
 * Created by loom on 18.01.16.
 */
@RunWith(PaxExam.class)
public class JspSimpleWBIntegrationTest extends ITestBase {

    private static final Logger LOG = LoggerFactory.getLogger(JspSimpleWBIntegrationTest.class);

    private Bundle installWarBundle;

    @Configuration
    public Option[] configure() {
        return options(
                workingDirectory("target/paxexam/"),
                cleanCaches(true),
                junitBundles(),
                mavenWar()
                        .groupId("org.ops4j.pax.web.samples")
                        .artifactId("webapp-bridge-war")
                        .version(asInProject())
        );
    }

    @Before
    public void setUp() throws BundleException, InterruptedException {
        initWebListener();
        String bundlePath = WEB_BUNDLE
                + "mvn:org.ops4j.pax.web.samples/war-simple/"
                + VersionUtil.getProjectVersion() + "/war?"
                + WEB_CONTEXT_PATH + "=/jsp-simple";
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

    /**
     * You will get a list of bundles installed by default plus your testcase,
     * wrapped into a bundle called pax-exam-probe
     */
    @Test
    public void listBundles() {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getState() != Bundle.ACTIVE) {
                fail("Bundle should be active: " + b);
            }

            Dictionary<String,String> headers = b.getHeaders();
            String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
            if (ctxtPath != null) {
                System.out.println("Bundle " + b.getBundleId() + " : "
                        + b.getSymbolicName() + " : " + ctxtPath);
            } else {
                System.out.println("Bundle " + b.getBundleId() + " : "
                        + b.getSymbolicName());
            }
        }

    }

    @Test
    public void testSimpleJsp() throws Exception {

        Thread.sleep(3000); // let the web.xml parser finish his job

        testClient.testWebPath("http://localhost:8282/jsp-simple/index.jsp",
                "Hello, World, from JSP");

    }

}
