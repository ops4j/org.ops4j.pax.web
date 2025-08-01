/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.itest.utils.web;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class SimpleOnlyFilter implements Filter {

	@Override
	public void init(FilterConfig config) throws ServletException {
		System.out.println(config.getServletContext().getContextPath());
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		servletResponse.setContentType("text/html");
		((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_OK);
		servletResponse.getWriter().println("<h1>Hello Whiteboard Filter</h1>");

		if ("true".equals(servletRequest.getParameter("chain"))) {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	public void destroy() {
	}

}
