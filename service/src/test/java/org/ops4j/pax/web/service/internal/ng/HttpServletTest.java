package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

public class HttpServletTest
{

    private HttpServlet m_underTest;
    private Bundle m_bundle;
    private ServerController m_serverController;
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
        m_serverController = createMock( ServerController.class );
        m_underTest = new HttpServlet( "/alias", m_servlet, m_initParams, m_context );
    }

    @Test
    public void registerFlow()
    {
        // prepare
        m_serverController.addServlet( "/alias", m_servlet );
        replay( m_serverController );
        // execute
        m_underTest.register( m_serverController );
        // verify
        verify( m_serverController );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerWithNullServer()
    {
        m_underTest.register( null );
    }

    @Test
    public void unregisterFlow()
    {
        // prepare
        m_serverController.removeServlet( m_underTest.getAlias() );
        replay( m_serverController );
        // execute
        m_underTest.unregister( m_serverController );
        // verify
        verify( m_serverController );
    }

    @Test( expected = IllegalArgumentException.class )
    public void unregisterWithNullServer()
    {
        m_underTest.unregister( null );
    }

}
