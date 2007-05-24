package org.ops4j.pax.web.service.internal;

import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.ops4j.pax.web.service.ExampleService;

/**
 * Extension of the default OSGi bundle activator
 */
public final class Activator
    implements BundleActivator
{
    /**
     * Called whenever the OSGi framework starts our bundle
     */
    public void start( BundleContext bc )
        throws Exception
    {
        System.out.println( "STARTING org.ops4j.pax.web.service" );

        Dictionary props = new Properties();
        // add specific service properties here...

        System.out.println( "REGISTER org.ops4j.pax.web.service.ExampleService" );

        // Register our example service implementation in the OSGi service registry
        bc.registerService( ExampleService.class.getName(), new ExampleServiceImpl(), props );
    }

    /**
     * Called whenever the OSGi framework stops our bundle
     */
    public void stop( BundleContext bc )
        throws Exception
    {
        System.out.println( "STOPPING org.ops4j.pax.web.service" );

        // no need to unregister our service - the OSGi framework handles it for us
    }
}

