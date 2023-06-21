/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.samples.config.commands.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;

public class TestServlet extends HttpServlet {

	private final String name;
	private final String mapping;
	private final String type;

	public TestServlet(String name, String mapping, String type) {
		this.name = name;
		this.mapping = mapping;
		this.type = type;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.getWriter().println("Servlet Name: " + getServletConfig().getServletName());
		resp.getWriter().println("Servlet Config: " + getServletConfig());
		resp.getWriter().println("Servlet ClassLoader: " + req.getServletContext().getClassLoader());
		ServletContext servletContext = getServletConfig().getServletContext();
		resp.getWriter().println("Servlet Context: " + servletContext);
		if (servletContext instanceof OsgiScopedServletContext) {
			OsgiContextModel ocm = ((OsgiScopedServletContext) servletContext).getOsgiContextModel();
			resp.getWriter().println("OSGi Context Model:");
			resp.getWriter().println(" - Context Path: " + ocm.getContextPath());
			resp.getWriter().println(" - Direct org.osgi.service.http.HttpContext (HttpService only): " + ocm.getDirectHttpContextInstance());
			resp.getWriter().println(" - Owner Bundle: " + ocm.getOwnerBundle());
			resp.getWriter().println(" - Temp Location: " + ocm.getTemporaryLocation());
			resp.getWriter().println(" - ClassLoader: " + ocm.getClassLoader());
		}
		resp.getWriter().println("Configured Name: " + name);
		resp.getWriter().println("Configured Mapping: " + mapping);
		resp.getWriter().println("Servlet Type: " + type);
		resp.getWriter().flush();
	}

}
