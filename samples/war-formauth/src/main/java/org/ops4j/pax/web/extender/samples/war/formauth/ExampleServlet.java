package org.ops4j.pax.web.extender.samples.war.formauth;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ExampleServlet extends HttpServlet {

	/**
     * 
     */
	private static final long serialVersionUID = -3820576584247236099L;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		final PrintWriter writer = response.getWriter();
		writer.println("<body align='center'>");
		writer.println("<h1>Hello World</h1>");
		writer.println("<img src='" + request.getContextPath()
				+ "/images/logo.png' border='0'/>");
		writer.println("<h1>from WEB-INF/classes</h1>");
		writer.println("</body>");
	}

}