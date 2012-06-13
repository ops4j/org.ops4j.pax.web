package org.ops4j.pax.web.service.tomcat.internal;

import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.model.ServerModel;

import static org.ops4j.pax.web.service.tomcat.internal.ServerState.States.INITIALIZED;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/9/12
 * Time: 8:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class TomcatServerStateFactory implements ServerStateFactory {
    private final ServerFactory serverFactory;
    TomcatServerStateFactory(ServerFactory serverFactory) {
        this.serverFactory = serverFactory;
    }

    @Override
    public ServerState newInstalledState() {
        return InstalledServerState.getInstance(this);
    }

    @Override
    public ServerState newActiveState(ServerWrapper server, ServerState serverState) {
        if (serverState == null) {
            throw new IllegalArgumentException("server state parameter must be not null");
        }
        if (serverState.getState() != INITIALIZED) {
            throw new IllegalArgumentException(String.format("server state parameter must be in state: '%s'; and not: '%s'", INITIALIZED, serverState.getState()));
        }
        return ActiveServerState.getInstance(this, serverState, server);
    }

    @Override
    public ServerState newInitializedState(Configuration configuration) {
        return InitializedServerState.getInstance(this, configuration, serverFactory);
    }

    static ServerStateFactory newInstance(ServerFactory serverFactory) {
        return new TomcatServerStateFactory(serverFactory);
    }

}
