package org.ops4j.pax.web.itest.base.support;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class SimpleFilter implements Filter {
	private FilterConfig filterConfig;

	private URL resource;
	private boolean haveBundleContext = false;

	@Override
	public void init(FilterConfig config) throws ServletException {
		System.out.println(config.getServletContext().getContextPath());
		this.filterConfig = config;
		haveBundleContext = filterConfig.getServletContext().getAttribute(
				"osgi-bundlecontext") != null;
	}

	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		resource = filterConfig.getServletContext().getResource("/");

		filterChain.doFilter(servletRequest, servletResponse);
		servletResponse.getWriter().println("FILTER-INIT: "+ haveBundleContext);
	}

	public void destroy() {
	}

	public URL getResource() {
		return resource;
	}
}