package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Server;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/11/12
 * Time: 2:03 PM
 * To change this template use File | Settings | File Templates.
 */
class TomcatServerWrapper implements ServerWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(TomcatServerWrapper.class);
    private final Server server;

    private TomcatServerWrapper(Server server) {
        NullArgumentException.validateNotNull(server, "server");
        this.server = server;
    }

    static final ServerWrapper getInstance(Server server) {
        return new TomcatServerWrapper(server);
    }

    @Override
    public void start() {
        try {
            long t1 = System.nanoTime();
            server.start();
            long t2 = System.nanoTime();
            if (LOG.isInfoEnabled()) {
                LOG.info("TomCat server startup in " + ((t2 - t1) / 1000000) + " ms");
            }
        } catch (LifecycleException e) {
            throw new ServerStartException(server.getInfo(), e);
        }
    }

    @Override
    public void stop() {
        LifecycleState state = server.getState();
        if (LifecycleState.STOPPING_PREP.compareTo(state) <= 0
                && LifecycleState.DESTROYED.compareTo(state) >= 0) {
            // Nothing to do. stop() was already called
        } else {
            try {
                server.stop();
                server.destroy();
            } catch (LifecycleException e) {
                throw new ServerStopException(server.getInfo(), e);
            }
        }
    }

    public void addServlet(final ServletModel model) {
        LOG.debug("adding servlet [{}]",model);
        server.
    }
}
