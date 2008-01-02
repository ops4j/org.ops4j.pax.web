package org.ops4j.pax.web.samples.helloworld.hs.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Extension of the default OSGi bundle activator
 */
public final class Activator
    implements BundleActivator
{

    private ServiceReference m_httpServiceRef;

    /**
     * Called when the OSGi framework starts our bundle
     */
    public void start( BundleContext bc )
        throws Exception
    {
        m_httpServiceRef = bc.getServiceReference( HttpService.class.getName() );
        if( m_httpServiceRef != null )
        {
            final HttpService httpService = (HttpService) bc.getService( m_httpServiceRef );
            if( httpService != null )
            {
                // create a default context to share between registrations
                final HttpContext httpContext = httpService.createDefaultHttpContext();
                // register the hello world servlet
                final Dictionary initParams = new Hashtable();
                initParams.put( "from", "HttpService" );
                httpService.registerServlet(
                    "/helloworld/hs",              // alias
                    new HelloWorldServlet(),    // registered servlet
                    initParams,                 // init params
                    httpContext                 // http context
                );
                // register images as resources
                httpService.registerResources(
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
        if( m_httpServiceRef != null )
        {
            bc.ungetService( m_httpServiceRef );
            m_httpServiceRef = null;
        }
    }
}

