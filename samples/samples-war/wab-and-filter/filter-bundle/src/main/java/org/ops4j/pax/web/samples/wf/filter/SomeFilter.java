/*
 * Copyright 2023 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.samples.wf.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SomeFilter implements Filter {

	public static final Logger LOG = LoggerFactory.getLogger(SomeFilter.class);

	private FilterConfig filterConfig;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		LOG.info("SomeFilter.init()");
		this.filterConfig = filterConfig;
		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) filterConfig.getServletContext().getAttribute("SomeFilter.list");
		if (list != null) {
			throw new ServletException("Already initialized");
		}
		filterConfig.getServletContext().setAttribute("SomeFilter.list",
				Collections.singletonList("SomeFilter initialized"));
	}

	@Override
	public void destroy() {
		LOG.info("SomeFilter.destroy()");
		if (filterConfig == null) {
			throw new RuntimeException("Not initialized");
		}
		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) filterConfig.getServletContext().getAttribute("SomeFilter.list");
		if (list == null) {
			throw new RuntimeException("Not initialized - no SomeFilter.list attribute");
		}
		filterConfig.getServletContext().removeAttribute("SomeFilter.list");
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException {
		((HttpServletResponse)servletResponse).setStatus(HttpServletResponse.SC_OK);
		servletResponse.setContentType("text/plain");
		servletResponse.getWriter().println("Hello from " + this.getClass().getName());
		servletResponse.getWriter().println("My ServletContext: "
				+ (filterConfig == null ? "<no filter config>" : filterConfig.getServletContext()));
	}

}
