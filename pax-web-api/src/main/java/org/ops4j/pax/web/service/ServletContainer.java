package org.ops4j.pax.web.service;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;


public interface ServletContainer {
    
    void deploy(WebApp webApp);
    void undeploy(WebApp webApp);

}
