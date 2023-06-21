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
package org.ops4j.pax.web.extender.samples.war.formauth;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ExampleServlet extends HttpServlet {

	private static final long serialVersionUID = -3820576584247236099L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final PrintWriter writer = response.getWriter();
		writer.println("<body>");
		writer.println("<h1>Hello World</h1>");
		writer.println("<img src='" + request.getContextPath() + "/images/logo.png' border='0'/>");
		writer.println("<h1>from WEB-INF/classes</h1><br />");
		writer.println("<h2>Welcome " + request.getUserPrincipal().getName() + " ("
				+ request.getUserPrincipal().getClass().getName() + ")</h2><br />");
		writer.println("<a href=\"" + request.getContextPath() + "/logout\">logout</a><br />");
		writer.println("</body>");
	}

}
