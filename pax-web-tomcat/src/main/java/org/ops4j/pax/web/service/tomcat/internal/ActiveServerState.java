package org.ops4j.pax.web.service.tomcat.internal;

import org.ops4j.pax.web.service.spi.Configuration;

import static org.ops4j.pax.web.service.tomcat.internal.ServerState.States.ACTIVE;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/10/12
 * Time: 5:05 PM
 * To change this template use File | Settings | File Templates.
 */
class ActiveServerState extends AbstractServerState implements ServerState {


    private final ServerState initializedState;
    private final ServerWrapper serverWrapper;

    ActiveServerState(ServerStateFactory serverStateFactory, ServerState initializedState, ServerWrapper serverWrapper) {
        super(serverStateFactory);
        this.initializedState = initializedState;
        this.serverWrapper = serverWrapper;
    }
    static ServerState getInstance(ServerStateFactory serverStateFactory, ServerState initializedState, ServerWrapper server) {
        return new ActiveServerState(serverStateFactory, initializedState, server);
    }

    @Override
    public ServerState start() {
        return throwIllegalState();
    }

    @Override
    public ServerState stop() {
        serverWrapper.stop();
        return initializedState;
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public ServerState configure(Configuration configuration) {
        return stop().configure(configuration).start();
    }

    @Override
    public States getState() {
        return ACTIVE;
    }

    @Override
    public Configuration getConfiguration() {
        return initializedState.getConfiguration();
    }
}
