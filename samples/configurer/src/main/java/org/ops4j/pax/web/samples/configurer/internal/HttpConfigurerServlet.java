package org.ops4j.pax.web.samples.configurer.internal;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import org.ops4j.pax.web.service.HttpServiceConfigurer;
import org.ops4j.pax.web.service.SimpleHttpServiceConfiguration;

public class HttpConfigurerServlet extends HttpServlet
{
    private HttpServiceConfigurer m_httpServiceConfigurer;
    private int m_port;

    public HttpConfigurerServlet( final HttpServiceConfigurer httpServiceConfigurer )
    {
        m_httpServiceConfigurer = httpServiceConfigurer;
    }

    protected void doGet( HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        m_port = Integer.parseInt( request.getParameter( "port" ) );
        String newUrl = request.getScheme() + "://" + request.getServerName() + ":" + m_port + "/config/stats"; 
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.addHeader("LOCATION", newUrl);
        new Timer().schedule( new DoConfiguration(), 500 );
    }

    private class DoConfiguration extends TimerTask
    {
        public void run()
        {
            SimpleHttpServiceConfiguration config = new SimpleHttpServiceConfiguration();
            config.copyFrom( m_httpServiceConfigurer.get() );
            config.setHttpPort( m_port );
            m_httpServiceConfigurer.configure( config );
        }
    }
}
