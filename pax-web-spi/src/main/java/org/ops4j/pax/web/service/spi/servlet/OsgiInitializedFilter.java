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
import java.util.Enumeration;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * {@link Filter} wrapper that uses correct {@link FilterConfig} wrapper that returns correct wrapper
 * for {@link javax.servlet.ServletContext}
 */
public class OsgiInitializedFilter implements Filter {

	private final Filter filter;
	private final ServletContext servletContext;

	public OsgiInitializedFilter(Filter filter, ServletContext servletSpecificContext) {
		this.filter = filter;
		this.servletContext = servletSpecificContext;
	}

	@Override
	public void init(final FilterConfig config) throws ServletException {
		filter.init(new FilterConfig() {
			@Override
			public String getFilterName() {
				return config.getFilterName();
			}

			@Override
			public ServletContext getServletContext() {
				// TODO: this should come either from the servlet that's at the end of current chain or
				//       be the "best" ServletContext for given ServletContextModel
				return OsgiInitializedFilter.this.servletContext;
			}

			@Override
			public String getInitParameter(String name) {
				return config.getInitParameter(name);
			}

			@Override
			public Enumeration<String> getInitParameterNames() {
				return config.getInitParameterNames();
			}
		});
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		filter.doFilter(request, response, chain);
	}

	@Override
	public void destroy() {
		filter.destroy();
	}

}
