package org.ops4j.pax.web.samples.helloworld.wc.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.WebContainer;

/**
 * Extension of the default OSGi bundle activator
 */
public final class Activator
    implements BundleActivator
{

    private ServiceReference m_webContainerRef;

    /**
     * Called when the OSGi framework starts our bundle
     */
    public void start( BundleContext bc )
        throws Exception
    {
        m_webContainerRef = bc.getServiceReference( WebContainer.class.getName() );
        if( m_webContainerRef != null )
        {
            final WebContainer webContainer = (WebContainer) bc.getService( m_webContainerRef );
            if( webContainer != null )
            {
                // create a default context to share between registrations
                final HttpContext httpContext = webContainer.createDefaultHttpContext();
                // register the hello world servlet
                final Dictionary initParams = new Hashtable();
                initParams.put( "from", "WebContainer" );
                webContainer.registerServlet(
                    new HelloWorldServlet(),            // registered servlet
                    new String[]{ "/helloworld/wc/*" }, // url patterns
                    initParams,                         // init params
                    httpContext                         // http context
                );
                // register images as resources
                webContainer.registerResources(
                    "/images",
                    "/images",
                    httpContext
                );
            }
        }
    }

    /**
     * Called when the OSGi framework stops our bundle
     */
    public void stop( BundleContext bc )
        throws Exception
    {
        if( m_webContainerRef != null )
        {
            bc.ungetService( m_webContainerRef );
            m_webContainerRef = null;
        }
    }
}

