package org.ops4j.pax.web.service.tomcat.internal;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.BundleContext;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/9/12
 * Time: 7:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class TomcatServerControllerFactory implements ServerControllerFactory {

    private final ServerStateFactory serverStateFactory;

    private TomcatServerControllerFactory(ServerStateFactory serverStateFactory) {

        this.serverStateFactory = serverStateFactory;
    }

    @Override
    public ServerController createServerController(ServerModel serverModel) {
        return TomcatServerController.newInstance(serverStateFactory.newInstalledState());
    }

    static ServerControllerFactory newInstance(ServerStateFactory serverStateFactory) {
        return new TomcatServerControllerFactory(serverStateFactory);
    }
}
