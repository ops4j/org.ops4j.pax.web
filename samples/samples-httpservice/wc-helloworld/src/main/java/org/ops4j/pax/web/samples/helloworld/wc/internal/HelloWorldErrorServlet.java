/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.samples.helloworld.wc.internal;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Hello World Error Page Servlet.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 12, 2008
 */
public class HelloWorldErrorServlet extends HttpServlet {

	private static final long serialVersionUID = -2179524669842453079L;

	/**
	 * Prints an error request attribute.
	 *
	 * @param writer    print writer to write to
	 * @param request   servlet request
	 * @param attribute attribute name
	 */
	private static void printAttribute(final PrintWriter writer, final HttpServletRequest request, final String attribute) {
		writer.println("<tr>"
				+ "<td>" + attribute + "</td>"
				+ "<td>" + (request.getAttribute(attribute) != null ? request.getAttribute(attribute) : "") + "</td>"
				+ "</tr>");
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html");

		final PrintWriter writer = response.getWriter();
		writer.println("<html><body>");
		writer.println("<h1>Hello World Error Page</h1>");
		writer.println("<h2>Error attributes:</h2>");
		writer.println("<table border='1'>");
		writer.println("<tr><th>Request attribute name</th><th>Value</th></tr>");
		printAttribute(writer, request, "javax.servlet.error.exception");
		printAttribute(writer, request, "javax.servlet.error.exception_type");
		printAttribute(writer, request, "javax.servlet.error.message");
		printAttribute(writer, request, "javax.servlet.error.request_uri");
		printAttribute(writer, request, "javax.servlet.error.servlet_name");
		printAttribute(writer, request, "javax.servlet.error.status_code");
		writer.println("</table>");
		writer.println("</body></html>");
	}

}
