package org.ops4j.pax.web.itest.support;

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

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println(filterConfig.getServletContext().getContextPath());
		this.filterConfig = filterConfig;
	}

	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		resource = filterConfig.getServletContext().getResource("/");
		// System.out.println("Filtering with resource: " + resource);

		filterChain.doFilter(servletRequest, servletResponse);
	}

	public void destroy() {
	}

	public URL getResource() {
		return resource;
	}
}