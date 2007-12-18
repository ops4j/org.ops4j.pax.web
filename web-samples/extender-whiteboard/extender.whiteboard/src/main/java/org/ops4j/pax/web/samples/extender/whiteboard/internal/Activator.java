package org.ops4j.pax.web.samples.extender.whiteboard.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Servlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.ops4j.pax.web.extender.Resources;

public class Activator
    implements BundleActivator
{

    private ServiceRegistration m_registrationRootServlet;
    private ServiceRegistration m_registrationServlet;
    private ServiceRegistration m_registrationResources;

    public void start( final BundleContext bundleContext )
        throws Exception
    {
        Dictionary props = new Hashtable();
        props.put( "alias", "/whiteboard" );
        m_registrationServlet =
            bundleContext.registerService( Servlet.class.getName(), new WhiteboardServlet( "/whiteboard" ), props );

        props = new Hashtable();
        props.put( "alias", "/" );
        m_registrationRootServlet =
            bundleContext.registerService( Servlet.class.getName(), new WhiteboardServlet( "/" ), props );

        props.put( "alias", "/whiteboardresources" );
        m_registrationResources =
            bundleContext.registerService( Resources.class.getName(), new Resources( "/myResources" ), props );
    }

    public void stop( BundleContext bundleContext )
        throws Exception
    {
        if( m_registrationRootServlet != null )
        {
            m_registrationRootServlet.unregister();
        }
        if( m_registrationServlet != null )
        {
            m_registrationServlet.unregister();
        }
        if( m_registrationResources != null )
        {
            m_registrationResources.unregister();
        }
    }

}
