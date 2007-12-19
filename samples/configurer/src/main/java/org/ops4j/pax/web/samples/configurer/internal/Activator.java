package org.ops4j.pax.web.samples.configurer.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.ops4j.pax.web.service.HttpServiceConfigurer;
import java.util.Hashtable;

public class Activator
    implements BundleActivator
{
    private ServiceReference m_httpServiceRef;
    private HttpService m_httpService;
    private ServiceReference m_httpServiceConfigurerRef;
    private HttpServiceConfigurer m_httpServiceConfigurer;

    public void start( final BundleContext bundleContext )
        throws Exception
    {
        // TODO use service tracker for getting the service
        m_httpServiceRef = bundleContext.getServiceReference( HttpService.class.getName() );
        if ( m_httpServiceRef != null )
        {
            m_httpService = (HttpService) bundleContext.getService( m_httpServiceRef );
            m_httpServiceConfigurerRef = bundleContext.getServiceReference( HttpServiceConfigurer.class.getName() );
            if ( m_httpServiceConfigurerRef != null )
            {
                m_httpServiceConfigurer = (HttpServiceConfigurer) bundleContext.getService( m_httpServiceConfigurerRef );
                m_httpService.registerServlet( "/config/set", new HttpConfigurerServlet( m_httpServiceConfigurer ), new Hashtable( ), null );
                m_httpService.registerServlet( "/config/stats", new DisplayConfigurationServlet( m_httpServiceConfigurer ), new Hashtable( ), null );
                m_httpService.registerResources( "/images", "/images", null);
            }            
        }
    }

    public void stop( BundleContext bundleContext )
        throws Exception
    {
        if ( m_httpService != null )
        {
            bundleContext.ungetService( m_httpServiceRef );
        }
        if  (m_httpServiceConfigurerRef != null )
        {
            bundleContext.ungetService( m_httpServiceConfigurerRef );
        }
    }

}
