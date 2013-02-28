package org.ops4j.pax.web.itest.support;


import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

public class SimpleFilter implements Filter{
    FilterConfig filterConfig;
    
    URL resource;

    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println(filterConfig.getServletContext().getContextPath());
        this.filterConfig = filterConfig;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        resource = filterConfig.getServletContext().getResource("/");
//        System.out.println("Filtering with resource: " + resource);

        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
    }
    
    public URL getResource() {
    	return resource;
    }
}