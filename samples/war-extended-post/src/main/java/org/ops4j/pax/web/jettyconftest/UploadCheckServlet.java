package org.ops4j.pax.web.jettyconftest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class UploadCheckServlet
 */

@WebServlet(name = "helloWorld", urlPatterns = {"/upload-check"})
public class UploadCheckServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().println("POST data size is: " + request.getParameter("data").length());
	}
}
