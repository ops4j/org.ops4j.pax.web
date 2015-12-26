package org.ops4j.pax.web.service.undertow.internal;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.xnio.XnioProvider;

/**
 * @author Achim Nierbeck
 */
@Component
public class UndertowServerControllerFactory implements ServerControllerFactory {

    // dummy reference to make sure the provider is available by the time the server starts
    private XnioProvider provider; 

    @Reference
    public void setNioXnioProvider(XnioProvider provider) {
        this.provider = provider;
    }
    
    public void unsetNioXnioProvider(XnioProvider provider) {
        this.provider = null;
    }

    @Override
    public ServerController createServerController(ServerModel serverModel) {
        return new ServerControllerImpl();
    }

}
