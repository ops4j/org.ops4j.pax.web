package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;

/**
 * Created by loom on 13.01.16.
 */
public class BridgeServerControllerFactory implements ServerControllerFactory {

    private BridgeServer bridgeServer;

    public BridgeServerControllerFactory(BridgeServer bridgeServer) {
        this.bridgeServer = bridgeServer;
    }

    @Override
    public ServerController createServerController(ServerModel serverModel) {
        bridgeServer.setServerModel(serverModel);
        return new BridgeServerController(bridgeServer);
    }
}
