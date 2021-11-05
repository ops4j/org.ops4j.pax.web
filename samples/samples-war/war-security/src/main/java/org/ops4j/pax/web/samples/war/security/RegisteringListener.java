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
import java.util.Collections;
import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RegisteringListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletRegistration.Dynamic reg1 = sce.getServletContext().addServlet("s1", new HttpServlet() {
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
		});
		reg1.addMapping("/s1/*");
		// 13.4.2
		//     The javax.servlet.ServletSecurityElement argument to
		//     setServletSecurity is analogous in structure and model to the
		//     ServletSecurity interface of the @ServletSecurity annotation.
		reg1.setServletSecurity(new ServletSecurityElement(
				// for remaining HTTP methods
				new HttpConstraintElement(
						ServletSecurity.EmptyRoleSemantic.PERMIT,
						ServletSecurity.TransportGuarantee.NONE,
						"role-admin", "role-manager"
				),
				// for given HTTP methods
				Collections.singletonList(
						new HttpMethodConstraintElement("HEAD",
								new HttpConstraintElement(
										ServletSecurity.EmptyRoleSemantic.PERMIT,
										ServletSecurity.TransportGuarantee.NONE,
										"role-admin"
								)
						)
				)
		));

		// Strange (13.4.1 @ServletSecurity Annotation)...
		//     The @ServletSecurity annotation is not applied to the url-patterns of a
		//     ServletRegistration created using the addServlet(String, Servlet)
		//     method of the ServletContext interface, unless the Servlet was constructed by
		//     the createServlet method of the ServletContext interface.
		// But indeed - Tomcat scans for @ServletSecurity annotation after ensuring that
		// org.apache.catalina.core.StandardContext.wasCreatedDynamicServlet() == true.
		// Jetty and Undertow doesn't seem to bother at all...
		ServletRegistration.Dynamic reg2;
		try {
			reg2 = sce.getServletContext().addServlet("s2", sce.getServletContext().createServlet(SecurityAnnotatedServlet.class));
			reg2.addMapping("/s2/*");
		} catch (ServletException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
