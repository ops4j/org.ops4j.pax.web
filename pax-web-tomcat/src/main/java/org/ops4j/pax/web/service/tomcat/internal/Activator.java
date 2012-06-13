package org.ops4j.pax.web.service.tomcat.internal;

import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/9/12
 * Time: 7:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        context.registerService(ServerControllerFactory.class.getName(), TomcatServerControllerFactory.newInstance(TomcatServerStateFactory.newInstance(new TomcatServerFactory(null))), null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
