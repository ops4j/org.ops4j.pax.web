/* Copyright 2013 Guillaume Yziquel.
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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Hello World Servlet.
 *
 * @author Guillaume Yziquel
 * @since 4.0.0, September 25, 2013
 */
public class HelloWorldStartupTalkativeServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		if (HelloWorldStartupSilentServlet.isActive) {
			final PrintWriter writer = response.getWriter();
			writer.println("<body align='center'>");
			writer.println("<h1>Silent Servlet activated</h1>");
			writer.println("</body>");
		} else {
			throw new ServletException("Silent Servlet is not active.");
		}
	}

}
