package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Collection;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

public class RegistrationsImplTest
{

    private RegistrationsImpl m_underTest;
    private HttpTarget m_httpTarget;
    private Servlet m_servlet;
    private HttpContext m_context;
    private Dictionary m_initParams;
    private RegistrationsCluster m_registrationsCluster;

    @Before
    public void setUp()
    {
        m_httpTarget = createMock( HttpTarget.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_initParams = new Hashtable();
        m_registrationsCluster = createMock( RegistrationsCluster.class );
        m_underTest = new RegistrationsImpl( m_registrationsCluster );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullRegistrattionCluster()
    {
        new RegistrationsImpl( null );
    }

    @Test
    public void getAfterServletRegistration()
        throws NamespaceException
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
        throws NamespaceException
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
        throws NamespaceException
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
    
    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithNullAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                null,
                m_servlet,
                new Hashtable(),
                m_context
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithNullServlet()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "/test",
                null,
                new Hashtable(),
                m_context
        );
    }

    @Test
    public void registerServletWithNullInitParams()
        throws NamespaceException, ServletException
    {
        // must be allowed
        m_underTest.registerServlet(
                "/test",
                m_servlet,
                null,
                m_context
        );
    }

    @Test
    public void registerServletWithNullContext()
        throws NamespaceException, ServletException
    {
        // must be allowed
        m_underTest.registerServlet(
                "/test",
                m_servlet,
                new Hashtable(),
                null
            );
    }

    @Test
    public void registerServletWithOnlySlashInAlias()
        throws NamespaceException, ServletException
    {
        // must be allowed
        m_underTest.registerServlet(
                "/",
                m_servlet,
                new Hashtable(),
                null
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithEndSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "/test/",
                m_servlet,
                new Hashtable(),
                null
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithoutStartingSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "test",
                m_servlet,
                new Hashtable(),
                null
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithoutStartingSlashAndWithEndingSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
            "test/",
                m_servlet,
            new Hashtable(),
            null
        );
    }
    
    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithNullAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
                null,
                "resources",
                m_context
        );
    }

    @Test
    public void registerResourcesWithOnlySlashInAlias()
        throws NamespaceException
    {
        // must be allowed
        m_underTest.registerResources(
                "/",
                "resources",
                m_context
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithEndSlashInAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "/malformed/",
                "resources",
                m_context
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithoutStartingSlashInAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "malformed",
                "resources",
                m_context
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithhoutStartingSlashAndWthEndingSlashInAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "malformed/",
                "resources",
                m_context
        );
    }
    
    @Test( expected = NamespaceException.class )
    public void registerServletWithDuplicateAliasWithinTheSameRegistrations()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "/test",
                m_servlet,
                new Hashtable(),
                null
            );
        m_underTest.registerServlet(
                "/test",
                m_servlet,
                new Hashtable(),
                null
            );
    }

    @Test( expected = NamespaceException.class )
    public void registerServletWithDuplicateAliasWithinDifferentRegistrations()
        throws NamespaceException, ServletException
    {
        // prepare
        expect( m_registrationsCluster.getByAlias( "/test" ) ).andReturn( null );
        expect( m_registrationsCluster.getByAlias( "/test" ) ).andReturn( m_httpTarget );
        replay( m_registrationsCluster );
        // execute
        new RegistrationsImpl( m_registrationsCluster ).registerServlet(
                "/test",
                m_servlet,
                new Hashtable(),
                null
            );
        new RegistrationsImpl( m_registrationsCluster ).registerServlet(
                "/test",
                m_servlet,
                new Hashtable(),
                null
            );
        // verify
        verify( m_registrationsCluster );
    }

    @Test( expected = NamespaceException.class )
    public void registerResourcesWithDuplicateAliasWithinTheSameRegistrations()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "/test",
                "resources",
                m_context
        );
        m_underTest.registerResources(
                "/test",
                "resources",
                m_context
        );
    }

    @Test( expected = NamespaceException.class )
    public void registerResourceWithDuplicateAliasWithinDifferentRegistrations()
        throws NamespaceException, ServletException
    {
        // prepare
        expect( m_registrationsCluster.getByAlias( "/test" ) ).andReturn( null );
        expect( m_registrationsCluster.getByAlias( "/test" ) ).andReturn( m_httpTarget );
        replay( m_registrationsCluster );
        // execute
        new RegistrationsImpl( m_registrationsCluster ).registerResources(
                "/test",
                "/name",
                null
            );
        new RegistrationsImpl( m_registrationsCluster ).registerResources(
                "/test",
                "/name",
                null
            );
        // verify
        verify( m_registrationsCluster );
    }

}
