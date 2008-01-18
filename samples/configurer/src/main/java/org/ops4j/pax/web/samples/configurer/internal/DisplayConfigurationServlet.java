package org.ops4j.pax.web.samples.configurer.internal;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ops4j.pax.web.service.HttpServiceConfiguration;
import org.ops4j.pax.web.service.HttpServiceConfigurer;

public class DisplayConfigurationServlet extends HttpServlet
{

    private HttpServiceConfigurer m_httpServiceConfigurer;

    public DisplayConfigurationServlet( final HttpServiceConfigurer httpServiceConfigurer )
    {
        m_httpServiceConfigurer = httpServiceConfigurer;
    }

    protected void doGet( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        response.setContentType( "text/html" );
        response.setStatus( HttpServletResponse.SC_OK );
        response.getWriter().println( "<img src=\"/images/ops4j-logo.png\" alt=\"ops4j logo\"/><br/>" );
        response.getWriter().println( "<h1>Http Server Configuration</h1>" );
        HttpServiceConfiguration config = m_httpServiceConfigurer.get();
        if( config == null )
        {
            response.getWriter().println( "unknown" );
        }
        else
        {
            response.getWriter().println( "port: [" + config.getHttpPort() + "]<br/>" );
            response.getWriter().println( "secure port: [" + config.getHttpSecurePort() + "]<br/>" );
        }
    }
}
