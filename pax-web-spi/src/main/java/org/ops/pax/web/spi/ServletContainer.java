package org.ops.pax.web.spi;



public interface ServletContainer {
    
    void deploy(WabModel webApp);
    void undeploy(WabModel webApp);

}
