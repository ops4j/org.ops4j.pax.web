package org.ops4j.pax.web.itest.support;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedContext implements HttpContext {

	private static final Logger LOG = LoggerFactory
			.getLogger(SharedContext.class);

	public boolean handleSecurity(final HttpServletRequest request,
			final HttpServletResponse response) throws IOException {
		LOG.info("Forbiden access!");
		return false;
	}

	public URL getResource(final String name) {
		throw new IllegalStateException(
				"This method should not be possible to be called as the access is denied");
	}

	public String getMimeType(String s) {
		throw new IllegalStateException(
				"This method should not be possible to be called as the access is denied");
	}
}
