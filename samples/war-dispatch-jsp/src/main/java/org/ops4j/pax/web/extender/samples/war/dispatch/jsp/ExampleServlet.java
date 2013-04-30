package org.ops4j.pax.web.extender.samples.war.dispatch.jsp;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

public class ExampleServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		RequestDispatcher rd = super.getServletContext().getNamedDispatcher(
				"jsp");
		rd.forward(new HttpServletRequestFilter(request, "/subjsp/test.jsp"),
				response);
	}

	private static class HttpServletRequestFilter extends
			HttpServletRequestWrapper {

		private String pathInfo;

		public HttpServletRequestFilter(HttpServletRequest request,
				String pathInfo) {
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
