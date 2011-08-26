package org.ops4j.web.samples.helloworld.servlet3.internal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet (urlPatterns = {"/hello"}, name="helloWorld")
public class HelloWorld extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final PrintWriter writer = resp.getWriter();
        writer.println( "<body align='center'>" );
        writer.println( "<h1>Hello World</h1>" );
        writer.println( "<img src='"+req.getContextPath()+"/images/logo.png' border='0'/>" );
        writer.println( "<h1>from WEB-INF/classes</h1>" );
        writer.println( "</body>" );
	}
	
}
