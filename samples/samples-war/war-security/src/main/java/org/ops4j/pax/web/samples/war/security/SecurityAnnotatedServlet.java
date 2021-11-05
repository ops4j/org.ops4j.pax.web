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
package org.ops4j.pax.web.samples.war.security;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ServletSecurity(
		// for given HTTP methods
		httpMethodConstraints = {
				@HttpMethodConstraint(value = "HEAD", rolesAllowed = { "role-admin" })
		},
		// for remaining HTTP methods
		value = @HttpConstraint(rolesAllowed = { "role-admin", "role-manager" })
)
public class SecurityAnnotatedServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		resp.getWriter().println("servlet: " + getServletConfig().getServletName());
		resp.getWriter().println("user principal: " + (req.getUserPrincipal() == null ? "null" : req.getUserPrincipal().getName()));
		resp.getWriter().println("remote user: " + req.getRemoteUser());
		resp.getWriter().println("auth type: " + req.getAuthType());

		// 13.3 Programmatic Security
		//     For each distinct role reference used in a call to isUserInRole(), a security-role-ref
		//     element with role-name corresponding to the role reference should be declared in
		//     the deployment descriptor.
		resp.getWriter().println("is admin: " + req.isUserInRole("admin"));
		resp.getWriter().println("is manager: " + req.isUserInRole("manager"));
		resp.getWriter().println("is role-admin: " + req.isUserInRole("role-admin"));
		resp.getWriter().println("is role-manager: " + req.isUserInRole("role-manager"));

		if ("true".equals(req.getParameter("login"))) {
			req.login(req.getParameter("user"), req.getParameter("password"));
		} else if ("true".equals(req.getParameter("logout"))) {
			req.logout();
		} else if ("true".equals(req.getParameter("auth"))) {
			req.authenticate(resp);
		}
	}

}
