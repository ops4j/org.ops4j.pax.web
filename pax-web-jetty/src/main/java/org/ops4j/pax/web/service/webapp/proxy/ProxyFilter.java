package org.ops4j.pax.web.service.webapp.proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import javax.servlet.*;
import java.io.IOException;

/**
 * A proxy filter to relay to the OSGi bridge filter
 */
public class ProxyFilter implements Filter {

    ServletContext servletContext;
    FilterDispatcherTracker filterDispatcherTracker = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
        try {
            filterDispatcherTracker = new FilterDispatcherTracker(getBundleContext());
            filterDispatcherTracker.open();
        } catch (InvalidSyntaxException e) {
            throw new ServletException("Error during proxy filter init", e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Filter dispatcherFilter = filterDispatcherTracker.getServletFilter();
        if (dispatcherFilter != null) {
            dispatcherFilter.doFilter(request, response, chain);
        }
    }

    @Override
    public void destroy() {
        filterDispatcherTracker.close();
        servletContext = null;
    }

    private BundleContext getBundleContext()
            throws ServletException {
        Object context = servletContext.getAttribute(BundleContext.class.getName());
        if (context == null) {
            throw new ServletException("Bundle context attribute [" + BundleContext.class.getName() +
                    "] not set in servlet context");
        }
        if (context instanceof BundleContext) {
            return (BundleContext) context;
        }
        throw new ServletException("Bundle context attribute [" + BundleContext.class.getName() +
                "] is not of type BundleContext but of type " + context.getClass());
    }

}
