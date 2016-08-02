/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.samples.helloworld.wc.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Hello World Error Maker Servlet. Creates exceptions based on a request
 * parameter that specifies the full qualified name of the exception. Parameter
 * name is "type".
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 12, 2008
 */
public class HelloWorldErrorMakerServlet extends HttpServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = -8105406020181795765L;

	protected void doGet(final HttpServletRequest request,
						 final HttpServletResponse response) throws ServletException,
			IOException {
		final String exceptionType = request.getParameter("type");
		if (exceptionType == null || exceptionType.trim().length() == 0) {
			throw new IllegalArgumentException(
					"Request parameter [type] is not set or is empty");
		}
		try {
			final Exception exception = (Exception) Class
					.forName(exceptionType).newInstance();
			if (exception instanceof RuntimeException) {
				throw (RuntimeException) exception;
			}
			throw new ServletException(exception);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new ServletException("Cannot create exception", e);
		}
	}

}