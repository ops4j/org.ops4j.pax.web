package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class TomcatInterceptFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain originalChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            String pathInfo = request.getPathInfo();
            //Phase one: Check for any filter to apply...
            final List<Filter> filters = getFiltersRegisteredForPath(pathInfo);
            DelegatingFilterChain filterChain = new DelegatingFilterChain(filters);
            filterChain.doFilter(request, response);
            if (filterChain.proceedRequest) {
                //Phase two: check for Servlet to handle request...
                Servlet servlet = getMostSpecificServletForPath(pathInfo);
                if (servlet != null) {
                    //The servlet will serve this request
                    servlet.service(request, response);
                    return;
                }
                //Phase three: check for resourcepath registered...
                URL resourceUrl = getMostSpecificResourceURLForPath(pathInfo);
                if (resourceUrl != null) {
                    //The resource will serve this request
                    URLConnection connection = resourceUrl.openConnection();
                    String contentType = connection.getContentType();
                    if (contentType != null) {
                        response.setContentType(contentType);
                    }
                    InputStream inputStream = connection.getInputStream();
                    try {
                        ServletOutputStream outputstream = response.getOutputStream();
                        copy(inputStream, outputstream);
                        outputstream.flush();
                        outputstream.close();
                    } finally {
                        inputStream.close();
                    }
                    return;
                }
            } else {
                //A filter has already handled the request.
                return;
            }
        }
        //All not yet handled requests are delegated to the original chain...
        originalChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * @param pathInfo
     * @return the most specific resource {@link URL} for this path, the URL
     *         must point to the resource specified by pathInfo (it might not
     *         exits!) or <code>null</code> if no registered resources exits...
     */
    protected abstract URL getMostSpecificResourceURLForPath(String pathInfo);

    /**
     * @param pathInfo
     * @return the most specific {@link Servlet} for this path or
     *         <code>null</code> if no such servlet exits...
     */
    protected abstract Servlet getMostSpecificServletForPath(String pathInfo);

    /**
     * @param pathInfo
     * @return all {@link Filter}s currently known to the system under the given
     *         pathInfo or sub pathes in priority order
     */
    protected abstract List<Filter> getFiltersRegisteredForPath(String pathInfo);

    private static final class DelegatingFilterChain implements FilterChain {
        private final List<Filter> filters;
        private int                index          = 0;
        private boolean            proceedRequest = false;

        private DelegatingFilterChain(List<Filter> filters) {
            this.filters = filters;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            if (index < filters.size()) {
                Filter filter = filters.get(index);
                index++;
                filter.doFilter(request, response, this);
            } else {
                //all filters done, but all called the chain, so proceed...
                proceedRequest = true;
            }
        }
    }

    protected abstract void copy(InputStream inputStream, ServletOutputStream outputstream);

}
