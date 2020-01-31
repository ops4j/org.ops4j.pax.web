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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BrokenServlet extends IntrospectionServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		String what = req.getParameter("what");
		if ("throw".equals(what)) {
			String exceptionClass = req.getParameter("ex");
			String exceptionMessage = req.getParameter("message");
			try {
				Class<?> tc = Class.forName(exceptionClass);
				Constructor<?> ct = tc.getConstructor(String.class);
				if (RuntimeException.class.isAssignableFrom(tc)) {
					throw (RuntimeException) ct.newInstance(exceptionMessage);
				} else if (IOException.class.isAssignableFrom(tc)) {
					throw (IOException) ct.newInstance(exceptionMessage);
				} else {
					throw new ServletException((Throwable)ct.newInstance(exceptionMessage));
				}
			} catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				throw new RuntimeException("unexpected");
			}
		} else if ("return".equals(what)) {
			Integer code = Integer.parseInt(req.getParameter("code"));
			resp.sendError(code);
		} else {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().println("OK");
		}
	}

}
