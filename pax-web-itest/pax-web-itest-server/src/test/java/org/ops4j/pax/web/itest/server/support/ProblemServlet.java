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
package org.ops4j.pax.web.itest.server.support;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ProblemServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String exception = req.getParameter("ex");
		String message = req.getParameter("msg");
		String code = req.getParameter("result");
		if (exception != null && message != null) {
			try {
				Class<?> tc = Class.forName(exception);
				Constructor<?> ct = tc.getConstructor(String.class);
				if (RuntimeException.class.isAssignableFrom(tc)) {
					throw (RuntimeException) ct.newInstance(message);
				} else if (IOException.class.isAssignableFrom(tc)) {
					throw (IOException) ct.newInstance(message);
				} else {
					throw new ServletException((Throwable) ct.newInstance(message));
				}
			} catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				throw new RuntimeException("unexpected");
			}
		}
		if (code != null) {
			if (message != null) {
				resp.sendError(Integer.parseInt(code), message);
			} else {
				resp.sendError(Integer.parseInt(code));
			}
		}
	}

}
