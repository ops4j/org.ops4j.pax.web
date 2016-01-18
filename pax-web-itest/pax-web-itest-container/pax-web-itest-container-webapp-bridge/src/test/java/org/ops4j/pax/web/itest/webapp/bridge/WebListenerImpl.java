package org.ops4j.pax.web.itest.webapp.bridge;

import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by loom on 18.01.16.
 */
public class WebListenerImpl implements WebListener {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private boolean event;

    public void webEvent(WebEvent webEvent) {
        log.info("Got event: " + webEvent);
        if (webEvent.getType() == WebEvent.DEPLOYED) {
            this.event = true;
        } else if (webEvent.getType() == WebEvent.UNDEPLOYED) {
            this.event = false;
        }
    }

    public boolean gotEvent() {
        return event;
    }

}