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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.io.IOException;
import java.util.Date;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiteboardFilter implements Filter {

	private static final Logger LOG = LoggerFactory.getLogger(WhiteboardFilter.class);

	private boolean haveBundleContext;

	public void init(FilterConfig filterConfig) throws ServletException {
		LOG.info("Initialized");
		haveBundleContext = filterConfig.getServletContext().getAttribute("osgi-bundlecontext") != null;
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		response.getWriter().println("Filter was there before. Time: " + new Date().toString());
		chain.doFilter(request, response);
		response.getWriter().println("Filter was there after. Time: " + new Date().toString());
		response.getWriter().println("Have bundle context in filter: " + haveBundleContext);
		response.getWriter().close();
	}

	public void destroy() {
		LOG.info("Destroyed");
	}

}
