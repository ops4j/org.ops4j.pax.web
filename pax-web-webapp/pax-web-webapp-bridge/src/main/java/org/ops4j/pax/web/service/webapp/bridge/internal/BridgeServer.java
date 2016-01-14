package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.ServerModel;

/**
 * Created by loom on 14.01.16.
 */
public class BridgeServer {

    private ServerModel serverModel = null;

    public BridgeServer() {
    }

    public ServerModel getServerModel() {
        return serverModel;
    }

    public void setServerModel(ServerModel serverModel) {
        this.serverModel = serverModel;
    }
}
