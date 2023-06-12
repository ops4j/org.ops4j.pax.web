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
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.osgi.framework.wiring.BundleWiring;

/**
 * {@link Servlet} wrapper that uses correct {@link ServletConfig} wrapper that returns correct wrapper
 * for {@link jakarta.servlet.ServletContext} related to given servlet. This servlet wrapper class should be used
 * for all the runtimes of Pax Web.
 */
public class OsgiInitializedServlet implements Servlet {

	private final Servlet servlet;
	private final OsgiScopedServletContext servletContext;

	/**
	 * Whether TCCL should be set to servlet's bundle classloader. If {@code false}, TCCL from
	 * containing {@link ServletContext} will be used.
	 */
	private final boolean whiteboardTCCL;

	public OsgiInitializedServlet(Servlet servlet, OsgiScopedServletContext servletSpecificContext, boolean whiteboardTCCL) {
		this.servlet = servlet;
		this.servletContext = servletSpecificContext;
		this.whiteboardTCCL = whiteboardTCCL;
	}

	@Override
	public void init(final ServletConfig config) throws ServletException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader newCl = null;
			if (servletContext != null) {
				newCl = servletContext.getClassLoader();
				if (newCl == null && servletContext.getBundle() != null) {
					BundleWiring wiring = servletContext.getBundle().adapt(BundleWiring.class);
					if (wiring != null) {
						newCl = wiring.getClassLoader();
					}
				}
			}
			if (newCl != null) {
				Thread.currentThread().setContextClassLoader(newCl);
			}
			servlet.init(new ServletConfig() {
				@Override
				public String getServletName() {
					return config.getServletName();
				}

				@Override
				public ServletContext getServletContext() {
					return OsgiInitializedServlet.this.servletContext;
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
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public ServletConfig getServletConfig() {
		return servlet.getServletConfig();
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		if (!whiteboardTCCL) {
			servlet.service(req, res);
		} else {
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(req.getServletContext().getClassLoader());
				servlet.service(req, res);
			} finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}
		}
	}

	@Override
	public String getServletInfo() {
		return servlet.getServletInfo();
	}

	@Override
	public void destroy() {
		servlet.destroy();
	}

	public Servlet getDelegate() {
		return servlet;
	}

}
