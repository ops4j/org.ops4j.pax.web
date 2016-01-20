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

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.Info.getOps4jBaseVersion;
import static org.ops4j.pax.exam.Info.getPaxExamVersion;

/**
 * Created by loom on 18.01.16.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class JspSimpleWBIntegrationTest extends ITestBase {

    private static final Logger LOG = LoggerFactory.getLogger(JspSimpleWBIntegrationTest.class);

    private Bundle installWarBundle;

    @Configuration
    public Option[] configure() {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
        return options(
                workingDirectory("target/paxexam/"),
                keepCaches(),
                warProbe()
                        .library("target/test-classes")
                        .overlay(
                                maven("org.ops4j.pax.web.samples", "webapp-bridge-war")
                                        .type("war").versionAsInProject())
                        .library(maven("org.ops4j.pax.exam", "pax-exam-servlet-bridge", "4.8.0"))
                        .library(maven("org.ops4j.pax.exam", "pax-exam", "4.8.0"))
                        .library(maven("org.osgi", "org.osgi.core", "6.0.0"))
                        .library(maven("org.ops4j.pax.web", "pax-web-spi", "4.2.5-SNAPSHOT"))
                        .library(maven("org.ops4j.pax.web.itest", "pax-web-itest-base", "4.2.5-SNAPSHOT"))
                        .library(maven("org.apache.httpcomponents", "httpcore", "4.3.3"))
                        .library(maven("org.apache.httpcomponents", "httpclient", "4.3.3"))
                        .library(maven("org.apache.httpcomponents", "httpmime", "4.3.3"))
                        .library(maven("org.ops4j.pax.exam", "pax-exam-cdi", "4.8.0"))
                        .library(maven("org.slf4j", "jul-to-slf4j", "1.6.6"))
                        .library(maven("org.slf4j", "jcl-over-slf4j", "1.6.6"))
                        .library(maven("org.slf4j", "slf4j-api", "1.6.6"))
                        .library(maven("org.slf4j", "slf4j-ext", "1.6.6"))
                        .library(maven("org.ops4j.base", "ops4j-base-spi", "1.5.0"))
                        .library(maven("junit", "junit", "4.9"))
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
        for (Bundle b : getBundleContext().getBundles()) {
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

        testClient.testWebPath("http://localhost:9080/helloworld-jsp/simple.jsp",
                "Hello, World, from JSP");
    }

}
