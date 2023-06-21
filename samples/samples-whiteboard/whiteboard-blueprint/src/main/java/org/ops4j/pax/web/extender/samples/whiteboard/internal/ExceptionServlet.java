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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet that either throws and exception if errorException request parameter
 * is set and is a correct FQN of Throwable subtype. or sends an error if
 * errorCode request parameter is provided.
 * <p>
 * Used to test error page registration.
 * <p>
 * errorCode takes precedence or errorException.
 * <p>
 * If neither is provided or errorCode is not valid http error code or
 * errorException is not correct will throw an IllegalArgumentException.
 * errorCode must in in 400 or 500 range to be considered valid.
 */
public class ExceptionServlet extends HttpServlet {

	private static final long serialVersionUID = -58844579506172515L;

	private static final Set<Integer> VALID_ERROR_CODES = new HashSet<>() {
		private static final long serialVersionUID = -5608318022683417716L;

		{
			add(HttpServletResponse.SC_BAD_REQUEST);
			add(HttpServletResponse.SC_UNAUTHORIZED);
			add(HttpServletResponse.SC_FORBIDDEN);
			add(HttpServletResponse.SC_NOT_FOUND);
			add(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			add(HttpServletResponse.SC_NOT_ACCEPTABLE);
			add(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
			add(HttpServletResponse.SC_REQUEST_TIMEOUT);
			add(HttpServletResponse.SC_CONFLICT);
			add(HttpServletResponse.SC_GONE);
			add(HttpServletResponse.SC_LENGTH_REQUIRED);
			add(HttpServletResponse.SC_PRECONDITION_FAILED);
			add(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
			add(HttpServletResponse.SC_REQUEST_URI_TOO_LONG);
			add(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			add(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			add(HttpServletResponse.SC_EXPECTATION_FAILED);
			add(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			add(HttpServletResponse.SC_NOT_IMPLEMENTED);
			add(HttpServletResponse.SC_BAD_GATEWAY);
			add(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			add(HttpServletResponse.SC_GATEWAY_TIMEOUT);
			add(HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED);
		}
	};

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String error = req.getParameter("errorCode");
		if (error != null && error.trim().length() > 0) {
			int errorCode = -1;
			try {
				errorCode = Integer.parseInt(error.trim());
				if (VALID_ERROR_CODES.contains(errorCode)) {
					resp.sendError(errorCode, " Echo errorCode " + error);
					return;
				}
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		error = req.getParameter("errorException");
		if (error != null && error.trim().length() > 0) {
			try {
				Class<?> exp = this.getClass().getClassLoader().loadClass(error.trim());
				if (Throwable.class.isAssignableFrom(exp)) {
					throw new ServletException("Rethrowing " + error, (Throwable) exp.getConstructor().newInstance());
				}
			} catch (Exception ignored) {
			}
		}

		throw new IllegalArgumentException("Just throwing IllegalArgumentException");
	}

}
