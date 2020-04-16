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
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * {@link Servlet} wrapper that uses correct {@link ServletConfig} wrapper that returns correct wrapper
 * for {@link javax.servlet.ServletContext} related to given servlet. This servlet wrapper class should be used
 * for all the runtimes of Pax Web.
 */
public class OsgiInitializedServlet implements Servlet {

	private final Servlet servlet;
	private final ServletContext servletContext;

	public OsgiInitializedServlet(Servlet servlet, ServletContext servletSpecificContext) {
		this.servlet = servlet;
		this.servletContext = servletSpecificContext;
	}

	@Override
	public void init(final ServletConfig config) throws ServletException {
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
	}

	@Override
	public ServletConfig getServletConfig() {
		return servlet.getServletConfig();
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		servlet.service(req, res);
	}

	@Override
	public String getServletInfo() {
		return servlet.getServletInfo();
	}

	@Override
	public void destroy() {
		servlet.destroy();
	}

}
