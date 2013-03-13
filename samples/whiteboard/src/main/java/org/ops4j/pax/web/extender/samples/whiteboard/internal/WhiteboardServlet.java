package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WhiteboardServlet extends HttpServlet {

	/**
     * 
     */
	private static final long serialVersionUID = 2468029128065282904L;
	private String alias;

	public WhiteboardServlet(final String alias) {
		this.alias = alias;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("<h1>Hello Whiteboard Extender</h1>");
		response.getWriter().println("request alias: " + alias);
	}

}
