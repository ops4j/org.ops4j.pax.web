package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;

/**
 * Created by loom on 13.01.16.
 */
public class BridgeServerControllerFactory implements ServerControllerFactory {
    @Override
    public ServerController createServerController(ServerModel serverModel) {
        return new BridgeServerController(serverModel);
    }
}
