package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;
import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

public class ServletRegistrationTest
{

    private ServletRegistration m_underTest;
    private Bundle m_bundle;
    private HttpServiceServer m_httpServiceServer;
    private Servlet m_servlet;
    private HttpContext m_context;
    private Dictionary m_initParams;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_initParams = new Hashtable();
        m_httpServiceServer = createMock( HttpServiceServer.class );
        m_underTest = new ServletRegistration( "/alias", m_servlet, m_initParams, m_context );
    }

    @Test
    public void registerFlow()
    {
        // prepare
        m_httpServiceServer.addServlet( "/alias", m_servlet );
        replay( m_httpServiceServer );
        // execute
        m_underTest.register( m_httpServiceServer );
        // verify
        verify( m_httpServiceServer );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerWithNullServer()
    {
        m_underTest.register( null );
    }

}
