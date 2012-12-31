package org.ops4j.pax.web.itest.support;

import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebListenerImpl implements WebListener {

	protected Logger LOG = LoggerFactory.getLogger(getClass());
	
	private boolean event = false;

	public void webEvent(WebEvent event) {
		LOG.info("Got event: " + event);
		if (event.getType() == 2)
			this.event = true;
	}

	public boolean gotEvent() {
		return event;
	}

}