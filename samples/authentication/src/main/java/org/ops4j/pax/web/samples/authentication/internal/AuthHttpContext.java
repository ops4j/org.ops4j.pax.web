package org.ops4j.pax.web.samples.authentication.internal;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * Created by IntelliJ IDEA. User: alin.dreghiciu Date: Dec 10, 2007 Time:
 * 3:12:24 PM To change this template use File | Settings | File Templates.
 */
public class AuthHttpContext implements HttpContext {

	public boolean handleSecurity(HttpServletRequest req,
			HttpServletResponse res) throws IOException {
		req.setAttribute(AUTHENTICATION_TYPE, HttpServletRequest.BASIC_AUTH);
		req.setAttribute(REMOTE_USER, "Some Authenticated User");
		return true;
	}

	public URL getResource(String s) {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}

	public String getMimeType(String s) {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}
}
