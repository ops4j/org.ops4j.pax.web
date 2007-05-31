package org.ops4j.pax.web.service.internal.ng;

import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletHandler;

class JettyServerImpl implements JettyServer
{

    private static final Log m_logger = LogFactory.getLog( JettyServerImpl.class );

    private Server m_server;
    private Context m_context;

    JettyServerImpl()
    {
        m_server = new Server();
    }

    public void start()
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "starting " + this );
        }
        try
        {
            m_server.start();
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "started " + this );
            }
        }
        catch( Exception e )
        {
            if( m_logger.isErrorEnabled() )
            {
                m_logger.error( e );
            }
        }
    }

    public void stop()
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "stopping " + this );
        }
        try
        {
            m_server.stop();
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "stopped " + this );
            }
        }
        catch( Exception e )
        {
            if( m_logger.isErrorEnabled() )
            {
                m_logger.error( e );
            }
        }
    }

    public void addConnector( Connector connector )
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "adding connector" + connector );
        }
        m_server.addConnector( connector );
        // TODO handle the case that port is in use. maybe not start the service at all.
    }

    public void addContext( ServletHandler servletHandler )
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "adding context");
        }
        m_context = new Context( m_server, "/", Context.SESSIONS );
        m_context.setServletHandler( servletHandler );
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "added context: " + m_context );
        }
    }

    public void addServlet( final String alias, final Servlet servlet )
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "adding servlet: [" + alias + "] -> " + servlet );
        }
        m_context.addServlet(new ServletHolder( servlet ), alias + "/*" );
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "added servlet: [" + alias + "] -> " + servlet );
        }
    }

    // TODO loading of resources
    // TODO handle security on context before handling request
}
