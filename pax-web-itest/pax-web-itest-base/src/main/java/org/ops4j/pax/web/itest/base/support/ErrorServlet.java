/*
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
package org.ops4j.pax.web.itest.base.support;

import java.io.IOException;
import java.util.Dictionary;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ErrorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");

		// Servlets 3.1 spec, 10.9.1 "Request Attributes"
		Integer status_code = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		Class<?> exception_type = (Class<?>) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
		String message = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
		Throwable exception = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		String request_uri = (String) req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		String servlet_name = (String) req.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
		resp.getWriter().println(String.format("%d|%s|%s|%s|%s|%s",
				status_code == null ? 0 : status_code,
				exception_type == null ? "null" : exception_type.getName(),
				message,
				exception == null ? "null" : exception.getClass().getName(),
				request_uri,
				servlet_name));
	}

	public static ServiceRegistration<Servlet> register(BundleContext context, Dictionary<String, ?> properties) {
		return context.registerService(Servlet.class, new ErrorServlet(), properties);
	}

}
