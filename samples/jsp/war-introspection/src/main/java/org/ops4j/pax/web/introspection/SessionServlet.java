/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.introspection;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionServlet extends IntrospectionServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");

		if ("/login".equals(req.getPathInfo())) {
			resp.setStatus(HttpServletResponse.SC_OK);
			HttpSession session = req.getSession(true);
			String user = req.getParameter("user");
			session.setAttribute("user", user);
			resp.getWriter().println("Welcome " + user);
			session.setAttribute("counter", 0);
		} else if ("/visit".equals(req.getPathInfo())) {
			HttpSession session = req.getSession(false);
			if (session == null) {
				resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				resp.getWriter().println("Please log in first");
				return;
			}
			String user = (String) req.getSession().getAttribute("user");
			Integer counter = (Integer) session.getAttribute("counter");
			session.setAttribute("counter", ++counter);
			resp.getWriter().println("That's your " + counter + " visit after logging in, " + user);
		}
	}

}
