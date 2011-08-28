package org.ops4j.web.samples.helloworld.servlet3.internal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

@WebServlet (value="/hello", name="helloWorld")
public class HelloWorld extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	@Override
	public void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {
		final PrintWriter writer = res.getWriter();
		writer.println( "<body align='center'>" );
		writer.println( "<h1>Hello World</h1>" );
		writer.println( "<img src='"+req.getServletContext().getContextPath()+"/images/logo.png' border='0'/>" );
		writer.println( "<h1>from WEB-INF/classes</h1>" );
		writer.println( "</body>" );
	}
	
}
