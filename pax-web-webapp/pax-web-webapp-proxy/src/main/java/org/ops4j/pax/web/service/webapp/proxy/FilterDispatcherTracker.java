package org.ops4j.pax.web.service.webapp.proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import javax.servlet.Filter;

/**
 * A service tracker to retrieve the dispatcher filter
 */
public class FilterDispatcherTracker extends BridgeServiceTracker<Filter> {

    private Filter servletFilter;

    public FilterDispatcherTracker(BundleContext context) throws InvalidSyntaxException {
        super(context, Filter.class);
    }

    @Override
    protected void setService(Filter service) {
        this.servletFilter = (Filter) service;
    }

    public Filter getServletFilter() {
        return servletFilter;
    }

    @Override
    protected void unsetService() {
        this.servletFilter = null;
    }
}
