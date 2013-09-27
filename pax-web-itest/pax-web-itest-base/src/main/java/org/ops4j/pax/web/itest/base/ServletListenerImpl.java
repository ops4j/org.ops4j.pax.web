package org.ops4j.pax.web.itest.base;

import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletListenerImpl implements ServletListener {
	private static final Logger LOG = LoggerFactory.getLogger(ServletListenerImpl.class);
	
	private boolean event;
	
	private String servletName;

	public ServletListenerImpl(String servletName) {
		this.servletName = servletName;
	}

	public ServletListenerImpl() {
	}

	@Override
	public void servletEvent(ServletEvent servletEvent) {
		LOG.info("Got event: " + servletEvent);
		boolean checkServletName = servletName != null ? true : false;
		
		boolean servletMatch = true;
		if (checkServletName) {
			servletMatch = servletName.equalsIgnoreCase(servletEvent.getServletName());
		}
		if (servletEvent.getType() == ServletEvent.DEPLOYED && servletMatch) {
			LOG.info("servletEventMatched with checkServletName?{}", checkServletName);
			this.event = true;
		} else if (servletEvent.getType() == ServletEvent.UNDEPLOYED) {
			this.event = false;
		}
	}

	public boolean gotEvent() {
		return event;
	}
}