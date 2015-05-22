package org.ops4j.pax.web.spi;



public interface ServletContainer {
    
    void deploy(WabModel webApp);
    void undeploy(WabModel webApp);

}
