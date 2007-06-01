package org.ops4j.pax.web.service.internal.ng;

import java.util.Dictionary;
import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.HttpContext;

public class HttpServlet implements HttpTarget
{

    private static final Log m_logger = LogFactory.getLog( HttpServlet.class );

    private String m_alias;
    private Servlet m_servlet;
    private Dictionary m_initParams;
    private HttpContext m_httpContext;

    public HttpServlet(
        final String alias,
        final Servlet servlet,
        final Dictionary initParams,
        final HttpContext httpContext )
    {
        m_alias = alias;
        m_servlet = servlet;
        m_initParams = initParams;
        m_httpContext = httpContext;
    }

    public void register(
        final ServerController serverController )
    {
        if ( serverController == null )
        {
            throw new IllegalArgumentException( "httpServiceServer == null" );
        }
        serverController.addServlet( m_alias, m_servlet );
        if ( serverController == null )
        {
            throw new IllegalArgumentException( "httpServiceServer == null" );
        }
    }

    public String getAlias()
    {
        return m_alias;
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public Type getType()
    {
        return Type.SERVLET;
    }
}
