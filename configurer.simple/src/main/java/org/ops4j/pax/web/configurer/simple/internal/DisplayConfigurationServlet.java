package org.ops4j.pax.web.configurer.simple.internal;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import org.ops4j.pax.web.service.HttpServiceConfigurer;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

public class DisplayConfigurationServlet extends HttpServlet
{
    private HttpServiceConfigurer m_httpServiceConfigurer;

    public DisplayConfigurationServlet( final HttpServiceConfigurer httpServiceConfigurer )
    {
        m_httpServiceConfigurer = httpServiceConfigurer;
    }

    protected void doGet( HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                                         IOException
    {
        response.setContentType( "text/html" );
        response.setStatus( HttpServletResponse.SC_OK);
        response.getWriter().println( "<h1>Http Server Configuration</h1>" );
        HttpServiceConfiguration config =  m_httpServiceConfigurer.get();
        if ( config == null ){
            response.getWriter().println( "unknown" );
        }
        else
        {
            response.getWriter().println( "port: [" + config.getHttpPort() + "]<br/>" );
            response.getWriter().println( "secure port: [" + config.getHttpSecurePort() + "]<br/>" );
        }
    }
}
