package org.ops4j.pax.web.samples.authentication.internal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

public class StatusServlet extends HttpServlet {

	/**
     *
     **/
	private static final long serialVersionUID = 1861037384364913913L;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		final PrintWriter writer = response.getWriter();
		writer.println(HttpContext.AUTHENTICATION_TYPE + " : "
				+ request.getAttribute(HttpContext.AUTHENTICATION_TYPE));
		writer.println("Request.getAuthType() : " + request.getAuthType());
		writer.println(HttpContext.REMOTE_USER + " : "
				+ request.getAttribute(HttpContext.REMOTE_USER));
		writer.println("Request.getRemoteUser() : " + request.getRemoteUser());
		writer.println(HttpContext.AUTHORIZATION + " : "
				+ request.getAttribute(HttpContext.AUTHORIZATION));
	}

}
