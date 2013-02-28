package org.ops4j.pax.web.itest.support;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet (value="/test", name="test")
public class AnnotatedTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private boolean initCalled;

	@Override
	public void init(ServletConfig config) throws ServletException {
		this.initCalled = true;
		super.init(config);
	}
	
	public boolean isInitCalled() {
		return initCalled;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().write("TEST OK");
	}
}