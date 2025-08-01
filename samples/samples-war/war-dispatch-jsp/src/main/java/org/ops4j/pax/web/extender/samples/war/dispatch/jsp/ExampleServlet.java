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
package org.ops4j.pax.web.extender.samples.war.dispatch.jsp;

import java.io.IOException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

public class ExampleServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		RequestDispatcher rd = super.getServletContext().getNamedDispatcher("jsp");
		rd.forward(new HttpServletRequestFilter(request, "/subjsp/test.jsp"), response);
	}

	private static class HttpServletRequestFilter extends HttpServletRequestWrapper {

		private final String pathInfo;

		HttpServletRequestFilter(HttpServletRequest request, String pathInfo) {
			super(request);
			this.pathInfo = pathInfo;
		}

		public String getServletPath() {
			return "/";
		}

		public String getPathInfo() {
			return pathInfo;
		}
	}

}
