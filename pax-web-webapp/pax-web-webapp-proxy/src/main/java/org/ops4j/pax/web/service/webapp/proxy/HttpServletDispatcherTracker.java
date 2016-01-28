package org.ops4j.pax.web.service.webapp.proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import javax.servlet.http.HttpServlet;

/**
 * A service tracker to retrieve the dispatcher servlet
 */
public class HttpServletDispatcherTracker extends BridgeServiceTracker<HttpServlet> {

    private HttpServlet httpServlet;

    public HttpServletDispatcherTracker(BundleContext context) throws InvalidSyntaxException {
        super(context, HttpServlet.class);
    }

    @Override
    protected void setService(HttpServlet service) {
        this.httpServlet = (HttpServlet) service;
    }

    public HttpServlet getHttpServlet() {
        return httpServlet;
    }

    @Override
    protected void unsetService() {
        this.httpServlet = null;
    }
}
