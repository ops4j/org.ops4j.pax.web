/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.samples.helloworld.hs.internal;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Hello World Servlet.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2008
 */
public class HelloWorldServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private final String registrationPath;

	public HelloWorldServlet(final String registrationPath) {
		this.registrationPath = registrationPath;
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

		response.setContentType("text/html");

		request.getSession(true);

		final PrintWriter writer = response.getWriter();
		writer.println("<html><body align='center'>");
		writer.println("<h1>Hello World</h1>");
		writer.println("<img src='/images/logo.png' border='0'/>");
		writer.println("<img src='/alt-images/logo.png' border='0'/>");
		writer.println("<h1>" + getServletConfig().getInitParameter("from") + "</h1>");
		writer.println("<p>");
		writer.println("Served by servlet registered at: \"" + registrationPath + "\"");
		writer.println("<br/>");
		writer.println("Servlet Path: \"" + request.getServletPath() + "\"");
		writer.println("<br/>");
		writer.println("Path Info: \"" + request.getPathInfo() + "\"");
		writer.println("</p>");
		writer.println("</body></html>");
	}

}
