package org.ops4j.web.samples.helloworld.servlet3.internal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/duplicate"})
public class DuplicateDefinitionServlet extends HttpServlet {
	
	@Override
	public void init() throws ServletException {
		System.out.println("****** INIT DuplicateDefinitionServlet");
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final PrintWriter writer = response.getWriter();
		writer.println("<head>");
		writer.println("<link rel=\"stylesheet\" href=\""
				+ request.getServletContext().getContextPath()
				+ "/css/content.css\">");
		writer.println("</head>");
		writer.println("<body align='center'>");
		writer.println("<h1>Duplicate Servlet</h1>");
		writer.println("<img src='"
				+ request.getServletContext().getContextPath()
				+ "/images/logo.png' border='0'/>");
		writer.println("<h1>from WEB-INF/classes</h1>");
		writer.println("</body>");
	}

}
