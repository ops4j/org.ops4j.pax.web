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
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ErrorServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String servletName = (String) req.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
		Throwable exception = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		Class<?> exceptionType = (Class<?>) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
		String errorMessage = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
		String requestURI = (String) req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		Integer statusCode = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

		resp.getWriter().print(String.format("%s: [%s][%s][%s][%s][%s][%d]",
				req.getPathInfo(), // allows us to recognize error page model
				servletName,
				exception == null ? "null" : exception.getClass().getName(),
				exceptionType == null ? "null" : exceptionType.getName(),
				errorMessage, requestURI, statusCode));
	}

}
