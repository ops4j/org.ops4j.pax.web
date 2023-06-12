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
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Default404Servlet extends HttpServlet {

	public static final Logger LOG = LoggerFactory.getLogger(Default404Servlet.class);

	/**
	 * This flag implements something that's handled in:<ul>
	 *     <li>Jetty: {@code org.eclipse.jetty.server.handler.ContextHandler#setAllowNullPathInfo(boolean)}</li>
	 *     <li>Tomcat: {@code org.apache.catalina.core.StandardContext#setMapperContextRootRedirectEnabled(boolean)}</li>
	 * </ul>
	 */
	final boolean undertowRedirectContextRoot;

	public Default404Servlet() {
		this(false);
	}

	public Default404Servlet(boolean undertowRedirectContextRoot) {
		this.undertowRedirectContextRoot = undertowRedirectContextRoot;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (undertowRedirectContextRoot) {
			if (req.getRequestURI().equals(req.getContextPath())) {
				if (!resp.isCommitted()) {
					if (req.getDispatcherType() == DispatcherType.INCLUDE) {
						LOG.warn("Can't redirect to welcome page for INCLUDE dispatch");
						return;
					}

					StringBuilder location = new StringBuilder(req.getRequestURI());
					location.append('/');
					if (req.getQueryString() != null) {
						location.append('?');
						location.append(req.getQueryString());
					}
					// Avoid protocol relative redirects
					while (location.length() > 1 && location.charAt(1) == '/') {
						location.deleteCharAt(0);
					}
					resp.sendRedirect(resp.encodeRedirectURL(location.toString()));
					return;
				}
			}
		}
		if (!resp.isCommitted()) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

}
