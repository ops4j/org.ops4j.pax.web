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
import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.HttpMethodConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Equivalent of <security-constraint>
// the roles used should be the ones from <security-role>, not the ones that can be used in isUserInRole(), otherwise
// I'm getting (Tomcat logs):
// Nov 04, 2021 9:55:55 AM org.apache.catalina.startup.ContextConfig validateSecurityRoles
// WARNING: Security role name [admin] used in an <auth-constraint> without being defined in a <security-role>
// Nov 04, 2021 9:55:55 AM org.apache.catalina.startup.ContextConfig validateSecurityRoles
// WARNING: Security role name [manager] used in an <auth-constraint> without being defined in a <security-role>
@ServletSecurity(
		// for give HTTP methods
		httpMethodConstraints = {
				@HttpMethodConstraint(value = "HEAD", rolesAllowed = { "role-admin" })
		},
		// for remaining HTTP methods
		value = @HttpConstraint(rolesAllowed = { "role-admin", "role-manager" })
)
@WebServlet(name = "s3", urlPatterns = { "/s3/*" })
@DeclareRoles("role-manager")
public class FullyAnnotatedServlet extends HttpServlet {

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
