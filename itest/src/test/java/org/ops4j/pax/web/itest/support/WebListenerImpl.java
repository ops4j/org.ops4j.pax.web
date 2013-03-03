package org.ops4j.pax.web.itest.support;

import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebListenerImpl implements WebListener {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private boolean event;
	
	public void webEvent(WebEvent webEvent) {
		log.info("Got event: " + webEvent);
		if (webEvent.getType() == 2) {
			this.event = true;
		}
	}

	public boolean gotEvent() {
		return event;
	}

}