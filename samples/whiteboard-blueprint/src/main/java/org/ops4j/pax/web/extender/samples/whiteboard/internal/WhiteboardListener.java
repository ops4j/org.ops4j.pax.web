package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiteboardListener implements ServletRequestListener {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(WhiteboardListener.class);

	public void requestInitialized(final ServletRequestEvent sre) {
		LOG.info("Request initialized from ip: "
				+ sre.getServletRequest().getRemoteAddr());
	}

	public void requestDestroyed(final ServletRequestEvent sre) {
		LOG.info("Request destroyed from ip:"
				+ sre.getServletRequest().getRemoteAddr());
	}

}
