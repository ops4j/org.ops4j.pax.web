/*
 * Copyright 2022 OPS4J.
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
package org.ops4j.pax.web.service.jetty.internal.web;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.ops4j.pax.web.service.spi.servlet.OsgiHttpServletRequestWrapper;

public class PaxWebWebSocketUpgradeFilter implements Filter {

	private final Filter delegate;

	public PaxWebWebSocketUpgradeFilter(Filter delegate) {
		this.delegate = delegate;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		delegate.init(filterConfig);
	}

	@Override
	public void doFilter(final ServletRequest externalRequest, final ServletResponse externalResponse, FilterChain chain) throws IOException, ServletException {
		delegate.doFilter(new HttpServletRequestWrapper((HttpServletRequest) externalRequest) {
			@Override
			public ServletContext getServletContext() {
				if (externalRequest instanceof OsgiHttpServletRequestWrapper) {
					return ((OsgiHttpServletRequestWrapper) externalRequest).getRequest().getServletContext();
				}
				return externalRequest.getServletContext();
			}
		}, externalResponse, (request, response) -> chain.doFilter(externalRequest, externalResponse));
	}

	@Override
	public void destroy() {
		delegate.destroy();
	}

}
