package org.ops4j.pax.web.service.internal;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.ops4j.pax.web.service.DefaultHttpServiceConfiguration;
import org.ops4j.pax.web.service.ExtendedHttpService;
import org.ops4j.pax.web.service.HttpServiceConfigurer;
import org.ops4j.pax.web.service.SimpleHttpServiceConfiguration;

public class EventListenerTest
    extends IntegrationTests
{

    @Test
    public void listenerIsCalled()
        throws IOException, NamespaceException, ServletException, InterruptedException
    {
        HttpSessionListener listener = createMock( HttpSessionListener.class );
        listener.sessionCreated( (HttpSessionEvent) notNull() );
        listener.sessionDestroyed( (HttpSessionEvent) notNull() );

        replay( listener );

        HttpContext context = m_httpService.createDefaultHttpContext();
        m_httpService.registerServlet( "/test", new TestServlet(), null, context );
        m_httpService.registerEventListener( listener, context );

        HttpMethod method = new GetMethod( "http://localhost:8080/test" );
        m_client.executeMethod( method );
        System.out.println( "Waiting the session to expire for two minutes..." );
        method.releaseConnection();
        Thread.sleep( 2 * 60 * 1000 );

        verify( listener );

        ( (StoppableHttpService) m_httpService ).stop();
    }

    private static class TestServlet
        extends HttpServlet

    {

        protected void doGet( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
        {
            // create the session
            req.getSession();
        }

    }

}
