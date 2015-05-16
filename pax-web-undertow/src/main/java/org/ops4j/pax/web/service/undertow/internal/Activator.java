package org.ops4j.pax.web.service.undertow.internal;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Guillaume Nodet
 */
public class Activator implements BundleActivator, ServerControllerFactory {

    private ServiceRegistration<ServerControllerFactory> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        registration = context.registerService(ServerControllerFactory.class,
                this, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        registration.unregister();
    }

    @Override
    public ServerController createServerController(ServerModel serverModel) {
        return new ServerControllerImpl();
    }

}
