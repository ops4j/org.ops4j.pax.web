package org.ops4j.pax.web.itest.webapp.bridge;

import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.web.itest.base.HttpTestClient;
import org.ops4j.pax.web.itest.base.ServletListenerImpl;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.servlet.ServletContext;

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

    protected WebListener webListener;

    protected ServletListener servletListener;

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

    protected void initWebListener() {
        webListener = new WebListenerImpl();
        if (getBundleContext() == null) {
            return;
        }
        getBundleContext().registerService(WebListener.class, webListener, null);
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
        if (getBundleContext() == null) {
            return;
        }
        getBundleContext().registerService(ServletListener.class, servletListener,
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
