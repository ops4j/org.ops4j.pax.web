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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ErrorServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// attributes should be like this:
		// value: java.lang.Object  = {java.util.concurrent.ConcurrentHashMap@8832}  size = 5
		//  {@8840} "javax.servlet.error.status_code" -> {java.lang.Integer@8841} 401
		//  {@8842} "org.eclipse.jetty.server.error_context" -> {org.eclipse.jetty.servlet.ServletContextHandler$Context@8811} "ServletContext@o.o.p.w.s.j.i.PaxWebServletContextHandler@29fc4879{/,null,AVAILABLE}"
		//  {@8843} "javax.servlet.error.request_uri" -> {@8805} "/very-secure/x"
		//  {@8844} "javax.servlet.error.servlet_name" -> {@8845} "secure-servlet"
		//  {@8846} "javax.servlet.error.message" -> {@8847} "Unauthorized"
		response.setContentType("text/html");
		Integer rc = (Integer) request.getAttribute("javax.servlet.error.status_code");
		response.setStatus(rc);
		if (rc == 401) {
			response.getWriter().println("Not authenticated");
		} else if (rc == 403) {
			response.getWriter().println("Not authorized");
		} else if (rc == 404) {
			response.getWriter().println("Not found");
		}
	}

}
