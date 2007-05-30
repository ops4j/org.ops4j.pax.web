package org.ops4j.pax.web.service.internal.ng;

import javax.servlet.Servlet;
import java.util.Dictionary;
import org.osgi.service.http.HttpContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ServletRegistration implements Registration
{

    private static final Log m_logger = LogFactory.getLog( ServletRegistration.class );

    private String m_alias;
    private Servlet m_servlet;
    private Dictionary m_initParams;
    private HttpContext m_httpContext;

    ServletRegistration( final String alias, final Servlet servlet, final Dictionary initParams, final HttpContext httpContext)
    {
        if ( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "Creating servlet registration: [" + alias + "] -> " + servlet );
        }
        m_alias = alias;
        m_servlet = servlet;
        m_initParams = initParams;
        m_httpContext = httpContext;
    }

    public void register( final HttpServiceServer httpServiceServer )
    {
        if ( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "registering servlet: [" + m_alias + "] -> " + httpServiceServer );
        }
        if ( httpServiceServer == null )
        {
            throw new IllegalArgumentException( "httpServiceServer == null" );
        }
        httpServiceServer.addServlet( m_alias, m_servlet );
        if ( httpServiceServer == null)
        {
            throw new IllegalArgumentException( "httpServiceServer == null" );
        }
    }

    public String getAlias()
    {
        return m_alias;
    }
}
