package org.ops4j.pax.web.itest.webapp.bridge;

import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.base.HttpTestClient;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.servlet.ServletContext;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.CoreOptions.maven;

/**
 * Created by loom on 18.01.16.
 */
public class ITestBase {

    protected static final String WEB_CONTEXT_PATH = "Web-ContextPath";
    protected static final String WEB_CONNECTORS = "Web-Connectors";
    protected static final String WEB_VIRTUAL_HOSTS = "Web-VirtualHosts";
    protected static final String WEB_BUNDLE = "webbundle:";

    protected static final String COVERAGE_COMMAND = "coverage.command";

    protected static final String REALM_NAME = "realm.properties";

    private static final Logger LOG = LoggerFactory.getLogger(ITestBase.class);

    @Inject
    protected ServletContext servletContext;

    private BundleContext bundleContext = null;

    protected HttpTestClient testClient;

    @Before
    public void setUpITestBase() throws Exception {
        testClient = new HttpTestClient();
    }

    @After
    public void tearDownITestBase() throws Exception {
        testClient.close();
        testClient = null;
    }

    protected Option[] configureBridge() {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
        System.setProperty("org.ops4j.pax.logging.skipJUL", "true"); // needed to solve issue similar to this : https://issues.jboss.org/browse/AS7-1445
        return options(
                workingDirectory("target/paxexam/"),
                keepCaches(),
                warProbe()
                        .library("target/test-classes")
                        .overlay(
                                maven("org.ops4j.pax.web.samples", "webapp-bridge-war")
                                        .type("war").versionAsInProject())
                        .library(maven("org.ops4j.pax.exam", "pax-exam-servlet-bridge", "4.8.0"))
        );
    }

    protected BundleContext getBundleContext() {
        if (bundleContext == null) {
            if (servletContext != null) {
                bundleContext = (BundleContext) servletContext.getAttribute(BundleContext.class.getName());
                if (bundleContext != null) {
                    System.err.println("Karaf BundleContext successfully retrieved.");
                } else {
                    System.err.println("ERROR : Couldn't retrieve Karaf BundleContext");
                }
            } else {
                System.err.println("ERROR : No access to servlet context !");
            }
        }
        return bundleContext;
    }

    protected void waitForServer(final String path) throws InterruptedException {
        new WaitCondition("server") {
            @Override
            protected boolean isFulfilled() throws Exception {
                return testClient.checkServer(path);
            }
        }.waitForCondition();
    }

    protected Bundle installAndStartBundle(String bundlePath)
            throws BundleException, InterruptedException {
        if (getBundleContext() == null) {
            return null;
        }
        final Bundle bundle = getBundleContext().installBundle(bundlePath);
        bundle.start();
        new WaitCondition("bundle startup") {
            @Override
            protected boolean isFulfilled() {
                return bundle.getState() == Bundle.ACTIVE;
            }
        }.waitForCondition();
        return bundle;
    }

}
