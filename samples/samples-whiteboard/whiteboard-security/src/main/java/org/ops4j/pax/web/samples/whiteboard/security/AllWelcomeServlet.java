/*
 * Copyright 2023 OPS4J.
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
package org.ops4j.pax.web.samples.whiteboard.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

public class AllWelcomeServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Principal p = request.getUserPrincipal();
		String name = p == null ? "<no principal>" : p.getName();
		String className = p == null ? "<no principal>" : p.getClass().getName();
		request.setAttribute("auth", p != null);
		request.setAttribute("user", name);
		request.setAttribute("principalClass", className);

		request.getRequestDispatcher("/welcome.jsp").forward(request, response);
	}

}
