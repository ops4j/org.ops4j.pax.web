package org.ops4j.pax.web.service.tomcat.internal;

import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.model.*;
import org.osgi.service.http.HttpContext;

import javax.servlet.Servlet;

import static org.ops4j.pax.web.service.tomcat.internal.ServerState.States.INSTALLED;

/**
 * @author Romain Gilles
 */
class InstalledServerState extends AbstractServerState {

    static ServerState getInstance(ServerStateFactory serverStateFactory) {
        return new InstalledServerState(serverStateFactory);
    }

    InstalledServerState(ServerStateFactory serverStateFactory) {
        super(serverStateFactory);
    }

    @Override
    public ServerState start() {
        return throwIllegalState();
    }

    @Override
    public ServerState stop() {
        return throwIllegalState();
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public ServerState configure(Configuration configuration) {
        return getServerStateFactory().newInitializedState(configuration);
    }

    @Override
    public States getState() {
        return INSTALLED;
    }
}
