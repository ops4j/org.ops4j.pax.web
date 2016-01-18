package org.ops4j.pax.web.itest.webapp.bridge;

import org.ops4j.pax.web.itest.base.*;
import org.ops4j.pax.web.itest.base.WebListenerImpl;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

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
    protected BundleContext bundleContext;

    protected WebListener webListener;

    protected ServletListener servletListener;

    protected HttpTestClient testClient;

    protected void initWebListener() {
        webListener = new WebListenerImpl();
        if (bundleContext == null) {
            return;
        }
        bundleContext.registerService(WebListener.class, webListener, null);
    }

    protected void initServletListener() {
        initServletListener(null);
    }

    protected void initServletListener(String servletName) {
        if (servletName == null) {
            servletListener = new ServletListenerImpl();
        } else {
            servletListener = new ServletListenerImpl(servletName);
        }
        if (bundleContext == null) {
            return;
        }
        bundleContext.registerService(ServletListener.class, servletListener,
                null);
    }

    protected void waitForWebListener() throws InterruptedException {
        new WaitCondition("webapp startup") {
            @Override
            protected boolean isFulfilled() {
                return ((WebListenerImpl) webListener).gotEvent();
            }
        }.waitForCondition();
    }

    protected void waitForServletListener() throws InterruptedException {
        new WaitCondition("servlet startup") {
            @Override
            protected boolean isFulfilled() {
                return ((ServletListenerImpl) servletListener).gotEvent();
            }
        }.waitForCondition();
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
        if (bundleContext == null) {
            return null;
        }
        final Bundle bundle = bundleContext.installBundle(bundlePath);
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
