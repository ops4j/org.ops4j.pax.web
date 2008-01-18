package org.ops4j.pax.web.samples.authentication.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * Extension of the default OSGi bundle activator
 */
public final class Activator
    implements BundleActivator
{

    private ServiceReference m_httpServiceRef;
    private HttpService m_httpService;

    /**
     * Called whenever the OSGi framework starts our bundle
     */
    public void start( BundleContext bc )
        throws Exception
    {
        m_httpServiceRef = bc.getServiceReference( HttpService.class.getName() );
        if( m_httpServiceRef != null )
        {
            m_httpService = (HttpService) bc.getService( m_httpServiceRef );
            m_httpService.registerServlet( "/status", new StatusServlet(), null, null );
            m_httpService.registerServlet( "/status-with-auth", new StatusServlet(), null, new AuthHttpContext() );
        }
    }

    /**
     * Called whenever the OSGi framework stops our bundle
     */
    public void stop( BundleContext bc )
        throws Exception
    {
        if( m_httpService != null )
        {
            bc.ungetService( m_httpServiceRef );
            m_httpServiceRef = null;
            m_httpService = null;
        }
    }
}

