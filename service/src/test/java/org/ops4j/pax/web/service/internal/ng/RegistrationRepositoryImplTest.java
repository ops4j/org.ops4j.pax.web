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

public class RegistrationRepositoryImplTest
{

    private RegistrationRepositoryImpl m_underTest;
    private Registration m_registration;
    private Servlet m_servlet;
    private HttpContext m_context;
    private Dictionary m_initParams;

    @Before
    public void setUp()
    {
        m_registration = createMock( Registration.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_initParams = new Hashtable();
        m_underTest = new RegistrationRepositoryImpl();
    }

    @Test
    public void getAfterRegisteration()
    {
        // execute
        m_underTest.registerServlet( "/alias", m_servlet, m_initParams, m_context  );
        Collection<Registration> registrations = m_underTest.get();
        // verify
        assertNotNull( "registrations cannot be null", registrations);
        assertEquals( "expected just one registration", 1, registrations.size() );
        for( Registration registration : registrations)
        {
            assertEquals( "/alias", registration.getAlias() );
        }
    }

}
