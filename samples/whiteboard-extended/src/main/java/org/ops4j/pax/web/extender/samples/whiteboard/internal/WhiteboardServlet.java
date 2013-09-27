package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiteboardServlet extends HttpServlet {
	private static final Logger LOG = LoggerFactory
			.getLogger(WhiteboardServlet.class);

	/**
     * 
     */
	private static final long serialVersionUID = 2468029128065282904L;
	private String servletAlias;

	public WhiteboardServlet(final String alias) {
		servletAlias = alias;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext context = config.getServletContext();
		LOG.debug(
				"Servlet Context info - ContextName = [{}], ContextPath = [{}]",
				context.getServletContextName(), context.getContextPath());
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("<h1>Hello Whiteboard Extender</h1>");
		response.getWriter().println("request alias: " + servletAlias);
	}

}
