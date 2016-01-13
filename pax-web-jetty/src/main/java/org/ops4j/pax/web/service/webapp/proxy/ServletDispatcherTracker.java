package org.ops4j.pax.web.service.webapp.proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import javax.servlet.Servlet;

/**
 * A service tracker to retrieve the dispatcher servlet
 */
public class ServletDispatcherTracker extends BridgeServiceTracker<Servlet> {

    private Servlet servlet;

    public ServletDispatcherTracker(BundleContext context) throws InvalidSyntaxException {
        super(context, Servlet.class);
    }

    @Override
    protected void setService(Servlet service) {
        this.servlet = (Servlet) service;
    }

    public Servlet getServlet() {
        return servlet;
    }

    @Override
    protected void unsetService() {
        this.servlet = null;
    }
}
