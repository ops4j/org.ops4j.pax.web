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
package org.ops4j.pax.web.samples.authentication;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ops4j.pax.web.service.http.HttpContext;

public class StatusServlet extends HttpServlet {

	private static final long serialVersionUID = 1861037384364913913L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final PrintWriter writer = response.getWriter();
		writer.println(HttpContext.AUTHENTICATION_TYPE + " : " + request.getAttribute(HttpContext.AUTHENTICATION_TYPE));
		writer.println("Request.getAuthType() : " + request.getAuthType());
		writer.println(HttpContext.REMOTE_USER + " : " + request.getAttribute(HttpContext.REMOTE_USER));
		writer.println("Request.getRemoteUser() : " + request.getRemoteUser());
		writer.println(HttpContext.AUTHORIZATION + " : " + request.getAttribute(HttpContext.AUTHORIZATION));
	}

}
