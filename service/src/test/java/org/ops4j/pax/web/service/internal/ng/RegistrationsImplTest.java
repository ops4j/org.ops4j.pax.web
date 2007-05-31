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
    public void getAfterRegisteration()
    {
        // execute
        m_underTest.registerServlet( "/alias", m_servlet, m_initParams, m_context  );
        Collection<HttpTarget> httpTargets = m_underTest.get();
        // verify
        assertNotNull( "registrations cannot be null", httpTargets );
        assertEquals( "expected just one registration", 1, httpTargets.size() );
        for( HttpTarget httpTarget : httpTargets )
        {
            assertEquals( "/alias", httpTarget.getAlias() );
        }
    }

}
