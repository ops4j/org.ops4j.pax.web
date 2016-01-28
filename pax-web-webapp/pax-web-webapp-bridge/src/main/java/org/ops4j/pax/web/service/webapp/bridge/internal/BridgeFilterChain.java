package org.ops4j.pax.web.service.webapp.bridge.internal;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by loom on 18.01.16.
 */
public class BridgeFilterChain implements FilterChain {

    List<Filter> filters = new ArrayList<Filter>();

    int filterIndex = 0;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (filterIndex > filters.size() -1) {
            return;
        }
        Filter currentFilter = filters.get(filterIndex);
        filterIndex++;
        currentFilter.doFilter(request, response, this);

    }

    public void addFilter(Filter filter) {
        filters.add(filter);
        filterIndex = 0;
    }

    /**
     * This class should always be added as the last filter in the chain, it will invoke the filter.
     */
    public static class ServletDispatchingFilter implements Filter {

        private Servlet servlet;

        public ServletDispatchingFilter(Servlet servlet) {
            this.servlet = servlet;
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            this.servlet.service(request, response);
        }

        @Override
        public void destroy() {

        }

    }
}
