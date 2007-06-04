package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Collection;
import javax.servlet.Servlet;

public class RegistrationsImplTest
{

    private RegistrationsImpl m_underTest;
    private HttpTarget m_httpTarget;
    private Servlet m_servlet;
    private HttpContext m_context;
    private Dictionary m_initParams;

    @Before
    public void setUp()
    {
        m_httpTarget = createMock( HttpTarget.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_initParams = new Hashtable();
        m_underTest = new RegistrationsImpl();
    }

    @Test
    public void getAfterServletRegistration()
    {
        // execute
        HttpTarget registered = m_underTest.registerServlet( "/alias", m_servlet, m_initParams, m_context  );
        assertNotNull( "must return a valid http servlet", registered );
        HttpTarget[] httpTargets = m_underTest.get();
        // verify
        assertNotNull( "registrations cannot be null", httpTargets );
        assertEquals( "expected just one registration", 1, httpTargets.length );
        for( HttpTarget httpTarget : httpTargets )
        {
            assertEquals( "/alias", httpTarget.getAlias() );
        }
    }

    @Test
    public void getAfterResourceRegistration()
    {
        // execute
        HttpTarget registered = m_underTest.registerResources( "/alias", "/name", m_context  );
        assertNotNull( "must return a valid http resource", registered );
        HttpTarget[] httpTargets = m_underTest.get();
        // verify
        assertNotNull( "registrations cannot be null", httpTargets );
        assertEquals( "expected just one registration", 1, httpTargets.length );
        for( HttpTarget httpTarget : httpTargets )
        {
            assertEquals( "/alias", httpTarget.getAlias() );
        }
    }

    @Test
    public void getWithNoRegsitration()
    {
        HttpTarget[] targets = m_underTest.get();
        assertNotNull( "targets cannot be null", targets );
        assertEquals( "targets size", 0, targets.length );
    }

    @Test
    public void unregisterFlow()
    {
        m_underTest.unregister( m_underTest.registerServlet( "/alias", m_servlet, m_initParams, m_context ) );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void unregisterOfUnregisteredTarget()
    {
        // prepare
        expect( m_httpTarget.getAlias() ).andReturn( "/alias" );
        replay( m_httpTarget );
        // execute
        m_underTest.unregister( m_httpTarget );
        // verify
        verify( m_httpTarget );
    }

    @Test( expected = IllegalArgumentException.class )
    public void unregisterOfNullTarget()
    {
        // execute
        m_underTest.unregister( null );
    }

}
