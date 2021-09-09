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
package org.ops4j.pax.web.service.spi.servlet;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

/**
 * <p>{@link Filter} wrapper that can skip delegate's invocation if
 * {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel} doesn't match.</p>
 *
 * <p>This is important because of:
 * <blockquote>
 *     140.5 Registering Servlet Filters
 *      [...] Servlet filters are only applied to servlet requests if they are bound to the same Servlet
 *      Context Helper and the same Http Whiteboard implementation.
 * </blockquote></p>
 *
 * <p>In Jetty we can configure the filters associated with invocation, but not in Undertow or Tomcat, which use
 * fixed, static, final helper classes that can't be customized. Jetty wins here.</p>
 */
public class ScopedFilter implements Filter {

	private final Filter filter;
	private final FilterModel model;

	public ScopedFilter(Filter filter, FilterModel model) {
		this.filter = filter;
		this.model = model;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		filter.init(filterConfig);
	}

	@Override
	public void destroy() {
		filter.destroy();
	}

	public Filter getDelegate() {
		return filter;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		ServletContext context = request.getServletContext();
		boolean skip = false;
		if (context instanceof OsgiScopedServletContext) {
			if (!model.getContextModels().contains(((OsgiScopedServletContext) context).getOsgiContextModel())) {
				skip = true;
			}
		} else if (context instanceof OsgiServletContext) {
			if (!model.getContextModels().contains(((OsgiServletContext) context).getOsgiContextModel())) {
				skip = true;
			}
		}
		if (!skip) {
			// proceed with the filter
			filter.doFilter(request, response, chain);
		} else {
			// skip the filter, proceed with the chain
			chain.doFilter(request, response);
		}
	}

}
