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
import java.util.LinkedList;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.framework.Bundle;
import org.osgi.service.http.whiteboard.Preprocessor;

/**
 * A {@link FilterChain} that can be configured in any runtime to correctly handle two concepts defined in
 * Http Service / Whiteboard Service specifications:<ul>
 *     <li>{@link Preprocessor} filters</li>
 *     <li>{@link org.osgi.service.http.HttpContext#handleSecurity} and/or
 *     {@link org.osgi.service.http.context.ServletContextHelper#handleSecurity} +
 *     {@link org.osgi.service.http.context.ServletContextHelper#finishSecurity}</li>
 * </ul>
 *
 * TODO: ensure proper behavior in REQUEST, INCLUDE, FORWARD dispatches
 */
public class OsgiFilterChain implements FilterChain {

	private final List<Preprocessor> preprocessors = new LinkedList<>();

	private final ServletContext servletContext;
	private final WebContainerContext webContext;

	private final OsgiSessionAttributeListener osgiSessionsBridge;

	private FilterChain chain;

	private int index = 0;

	/**
	 * Creates {@link FilterChain} that will invoke all the processors, security handlers, filters and target
	 * servlet in correct order.
	 *
	 * @param preprocessors
	 * @param servletContext wrapped {@link ServletContext} with proper delegation
	 * @param context already resolved (with proper {@link Bundle}) {@link WebContainerContext}.
	 * @param originalChain
	 */
	public OsgiFilterChain(List<Preprocessor> preprocessors, ServletContext servletContext,
			WebContainerContext context, FilterChain originalChain,
			OsgiSessionAttributeListener osgiSessionsBridge) {
		this.preprocessors.addAll(preprocessors);
		this.webContext = context;
		this.servletContext = servletContext;
		this.chain = originalChain;
		this.osgiSessionsBridge = osgiSessionsBridge;
	}

	public void setChain(FilterChain chain) {
		this.chain = chain;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		// Here's the best place to wrap a request - but only when called for the first time!
		if (index == 0 && servletContext != null) {
			req = new OsgiHttpServletRequestWrapper(req, servletContext, osgiSessionsBridge);
		}

		if (index < preprocessors.size()) {
			// still something left
			Preprocessor filter = preprocessors.get(index++);
			filter.doFilter(req, res, this);
			return;
		}

		// nothing left - time to call security and if it passes - call the rest of the chain (normal filters
		// and target servlet)
		try {
			if (webContext == null || webContext.handleSecurity(req, res)) {
				// continue normally with normal filters and target servlet
				chain.doFilter(req, res);
			} else {
				// authentication failed
				if (!res.isCommitted()) {
					// Pax Web before 8.0.0 was sending HTTP 401 here, but the thing is that it should be
					// the role of actual implementation of handleSecurity() to respond with 401 if there's
					// a need (for example when returning "WWW-Authenticate: Basic Realm") which will make
					// the response committed
					// When it's not committed, we can assume that user has no chance to authenticate
					res.sendError(HttpServletResponse.SC_FORBIDDEN);
				}
			}
		} finally {
			if (webContext != null) {
				webContext.finishSecurity(req, res);
			}
		}
	}

}
