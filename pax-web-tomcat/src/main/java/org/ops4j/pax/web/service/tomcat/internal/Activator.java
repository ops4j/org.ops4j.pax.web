package org.ops4j.pax.web.service.tomcat.internal;

import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Romaim Gilles
 */
public class Activator implements BundleActivator
{

    @Override
    public void start(BundleContext context) throws Exception
    {
        context.registerService( ServerControllerFactory.class.getName(), TomcatServerControllerFactory.newInstance( TomcatServerStateFactory.newInstance( new TomcatServerFactory() ) ), null );
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
