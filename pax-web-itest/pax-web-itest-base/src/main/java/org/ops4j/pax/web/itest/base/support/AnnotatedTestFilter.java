/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package org.ops4j.pax.web.itest.base.support;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
import java.net.URL;

@WebFilter(value = "/*")
public class AnnotatedTestFilter implements Filter {
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
		// resource = filterConfig.getServletContext().getResource("/");

		filterChain.doFilter(servletRequest, servletResponse);
		servletResponse.getWriter().println("FILTER-INIT: "+ haveBundleContext);
	}

	public void destroy() {
	}

	public URL getResource() {
		return resource;
	}
}