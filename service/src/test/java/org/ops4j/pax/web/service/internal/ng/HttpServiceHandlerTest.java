package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import org.easymock.internal.IMocksControlState;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.HttpConnection;
import org.osgi.service.http.HttpContext;
import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletException;

public class HttpServiceHandlerTest
{
    private HttpServiceHandler m_underTest;
    private RegistrationsCluster m_registrationsCluster;
    private HttpTarget m_httpTarget;
    private HttpContext m_httpContext;

    @Before
    public void setUp()
    {
        m_registrationsCluster = createMock( RegistrationsCluster.class );
        m_httpTarget = createMock( HttpTarget.class );
        m_httpContext = createMock( HttpContext.class );
        m_underTest = new HttpServiceHandler( m_registrationsCluster );
    }

    @Test
    public void checkHandlingOfUnauthorizedRequest()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrationsCluster.getByAlias( "/alias") ).andReturn( m_httpTarget );
        expect( m_httpTarget.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( null, null )).andReturn( false );
        replay( m_registrationsCluster, m_httpTarget, m_httpContext );
        // execute
        m_underTest.handle( "/alias", null, null, 0 );
        // verify
        verify( m_registrationsCluster, m_httpTarget, m_httpContext );
    }

    @Test
    public void checkHandlingOfServletExactMatch()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrationsCluster.getByAlias( "/alias") ).andReturn( m_httpTarget );
        expect( m_httpTarget.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpTarget.getType() ).andReturn( HttpTarget.Type.SERVLET );
        expect( m_httpContext.handleSecurity( null, null )).andReturn( true );
        replay( m_registrationsCluster, m_httpTarget, m_httpContext );
        // execute
        m_underTest.handle( "/alias", null, null, 0 );
        // verify
        verify( m_registrationsCluster, m_httpTarget, m_httpContext );
    }

    @Test
    public void checkSubstringMatching()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrationsCluster.getByAlias( "/fudd/bugs/x.gif") ).andReturn( null );
        expect( m_registrationsCluster.getByAlias( "/fudd/bugs") ).andReturn( null );
        expect( m_registrationsCluster.getByAlias( "/fudd") ).andReturn( null );
        expect( m_registrationsCluster.getByAlias( "/") ).andReturn( null );
        replay( m_registrationsCluster );
        // execute
        m_underTest.handle( "/fudd/bugs/x.gif", null, null, 0 );
        // verify
        verify( m_registrationsCluster );
    }

    public void checkResourceNameSpaceMapping( String alias, String name, String uri, String expected)
        throws IOException, ServletException
    {
        // prepare
        HttpResource httpResource = new HttpResource( alias, name, m_httpContext ); 
        expect( m_registrationsCluster.getByAlias( uri) ).andReturn( httpResource );
        expect( m_httpContext.handleSecurity( null, null ) ).andReturn( true );
        expect( m_httpContext.getResource( expected )).andReturn( new URL( "file://") );
        replay( m_registrationsCluster, m_httpContext );
        // execute
        m_underTest.handle( uri, null, null, 0 );
        // verify
        verify( m_registrationsCluster, m_httpContext );
    }

    @Test
    public void checkResourceNameSpaceMapping01()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/", "", "/fudd/bugs", "/fudd/bugs");
    }

    @Test
    public void checkResourceNameSpaceMapping02()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/", "/", "/fudd/bugs", "/fudd/bugs");
    }

    @Test
    public void checkResourceNameSpaceMapping03()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/", "/tmp", "/fudd/bugs", "/tmp/fudd/bugs");
    }

    @Test
    public void checkResourceNameSpaceMapping04()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd", "", "/fudd/bugs", "/bugs");
    }

    @Test
    public void checkResourceNameSpaceMapping05()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd", "/", "/fudd/bugs", "/bugs");
    }

    @Test
    public void checkResourceNameSpaceMapping06()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd", "/tmp", "/fudd/bugs", "/tmp/bugs");
    }

    @Test
    public void checkResourceNameSpaceMapping07()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd", "tmp", "/fudd/bugs/x.gif", "tmp/bugs/x.gif");
    }

    @Test
    public void checkResourceNameSpaceMapping08()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd/bugs/x.gif", "tmp/y.gif", "/fudd/bugs/x.gif", "tmp/y.gif");
    }

}
