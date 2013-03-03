package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator
    implements BundleActivator
{

    private ServiceRegistration<HttpContextMapping> m_httpContextMappingReg;
    private ServiceRegistration<HttpContextMapping> m_httpContextMappingReg2;
    private ServiceRegistration<HttpContextMapping> m_httpContextMappingReg3;
    private ServiceRegistration<Servlet> m_servletReg;
    private ServiceRegistration<Servlet> m_servletReg2;
    private ServiceRegistration<Servlet> m_servletReg3;

    public void start( final BundleContext bundleContext )
        throws Exception
    {
        Dictionary<String, String> props;

        // register the first context
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended" );
        HashMap<String,String> contextMappingParams = new HashMap<String,String>();
        contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS, "localhost");
        contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_CONNECTORS, "jettyConn1");
        m_httpContextMappingReg =
            bundleContext.registerService( HttpContextMapping.class, new WhiteboardHttpContextMapping("extended", "foo", contextMappingParams), props );
        
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended2" );
        contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS, "127.0.0.1");
        contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_CONNECTORS, "default");
        m_httpContextMappingReg =
            bundleContext.registerService( HttpContextMapping.class, new WhiteboardHttpContextMapping("extended2", "bar", contextMappingParams), props );
        
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended3" );
        contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS, "127.0.0.1");
        contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_CONNECTORS, "jettyConn1");
        m_httpContextMappingReg =
            bundleContext.registerService( HttpContextMapping.class, new WhiteboardHttpContextMapping("extended3", null , contextMappingParams), props );

              
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_ALIAS, "/whiteboard" );
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended" );
        m_servletReg =
            bundleContext.registerService( Servlet.class, new WhiteboardServlet( "/whiteboard" ), props );
        
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_ALIAS, "/whiteboard2" );
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended2" );
        m_servletReg =
            bundleContext.registerService( Servlet.class, new WhiteboardServlet( "/whiteboard2" ), props );
        
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_ALIAS, "/whiteboard3" );
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended3" );
        m_servletReg =
            bundleContext.registerService( Servlet.class, new WhiteboardServlet( "/whiteboard3" ), props );
    }

    public void stop( BundleContext bundleContext )
        throws Exception
    {
        if( m_servletReg != null )
        {
            m_servletReg.unregister();
            m_servletReg = null;
        }
        if( m_servletReg2 != null )
        {
            m_servletReg2.unregister();
            m_servletReg2 = null;
        }
        if( m_servletReg3 != null )
        {
            m_servletReg3.unregister();
            m_servletReg3 = null;
        }
        if( m_httpContextMappingReg != null )
        {
            m_httpContextMappingReg.unregister();
            m_httpContextMappingReg = null;
        }
        if( m_httpContextMappingReg2 != null )
        {
            m_httpContextMappingReg2.unregister();
            m_httpContextMappingReg2 = null;
        }
        if( m_httpContextMappingReg3 != null )
        {
            m_httpContextMappingReg3.unregister();
            m_httpContextMappingReg3 = null;
        }
    }
}
