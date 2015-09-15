package org.ops4j.pax.web.samples.jersey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import java.util.logging.Logger;

@Path("/")
public class RootResource {

	private Logger logger = Logger.getLogger(getClass().getName());

	@Context
	private HttpServletRequest request;

	@GET
	public String createSession() {
		String message;
		if (request == null) {
			logger.severe("request is null!!!!!");
			throw new RuntimeException();
		}
		HttpSession session = request.getSession(false);
		if (session == null) {
			session = request.getSession();
			message = "New session created: " + session.getId();
		} else {
			message = "Session already exists: " + session.getId();
		}
		logger.info(message);
		return htmlPage(message);
	}

	private String htmlPage(String message) {
		return "<html><body>" + message + "<br><br>"
				+ "If this <a href=\"/images/success.png\">link</a> leads to an image the test is successful"
				+ "</body></html>";
	}
}
