/* Copyright 2007 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;

public class HttpServiceHandlerTest
{
    private HttpServiceHandler m_underTest;
    private RegistrationsCluster m_registrationsCluster;
    private HttpTarget m_httpTarget;
    private HttpContext m_httpContext;
    private URL m_url;
    private HttpServletResponse m_httpResponse;

    @Before
    public void setUp()
        throws IOException
    {
        m_registrationsCluster = createMock( RegistrationsCluster.class );
        m_httpTarget = createMock( HttpTarget.class );
        m_httpContext = createMock( HttpContext.class );
        m_httpResponse = createMock( HttpServletResponse.class );
        File file = File.createTempFile( "test", ".txt" );
        file.deleteOnExit();
        m_url = file.toURL();
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
        expect( m_httpTarget.getHttpContext() ).andReturn( m_httpContext );
        replay( m_registrationsCluster, m_httpTarget, m_httpContext );
        // execute
        m_underTest.handle( "/alias", null, null, 0 );
        // verify
        verify( m_registrationsCluster, m_httpTarget, m_httpContext );
    }

    @Test
    public void checkHandlingOfResourceExactMatch()
        throws IOException, ServletException
    {
        // prepare
        HttpResource httpResource = new HttpResource( "/fudd", "/tmp", m_httpContext );
        expect( m_registrationsCluster.getByAlias( "/fudd/bugs") ).andReturn( httpResource );
        expect( m_httpContext.handleSecurity( null, m_httpResponse ) ).andReturn( true );
        expect( m_httpContext.getResource( "/tmp/bugs" ) ).andReturn( m_url );
        expect( m_httpContext.getMimeType( "/tmp/bugs" ) ).andReturn( "text/plain" );
        expect( m_httpResponse.getOutputStream() ).andReturn( null );
        replay( m_registrationsCluster, m_httpContext, m_httpResponse );
        // execute
        m_underTest.handle( "/fudd/bugs", null, m_httpResponse, 0 );
        // verify
        verify( m_registrationsCluster, m_httpContext, m_httpResponse );
    }

    @Test
    public void checkResourceFallbackIfNullURLReturned()
        throws IOException, ServletException
    {
        // prepare
        HttpResource httpResource = new HttpResource( "/fudd", "", m_httpContext );
        expect( m_registrationsCluster.getByAlias( "/fudd/bugs") ).andReturn( httpResource );
        expect( m_httpContext.handleSecurity( null, m_httpResponse ) ).andReturn( true );
        expect( m_httpContext.getResource( "/bugs" ) ).andReturn( null );
        expect( m_registrationsCluster.getByAlias( "/fudd") ).andReturn( httpResource );
        expect( m_httpContext.handleSecurity( null, m_httpResponse ) ).andReturn( true );
        expect( m_httpContext.getResource( "/bugs" ) ).andReturn( m_url );
        expect( m_httpContext.getMimeType( "/bugs" ) ).andReturn( "text/plain" );
        expect( m_httpResponse.getOutputStream() ).andReturn( null );
        replay( m_registrationsCluster, m_httpContext, m_httpResponse );
        // execute
        m_underTest.handle( "/fudd/bugs", null, m_httpResponse, 0 );
        // verify
        verify( m_registrationsCluster, m_httpContext, m_httpResponse );
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
        m_underTest.handle( "/fudd/bugs/x.gif", null, m_httpResponse, 0 );
        // verify
        verify( m_registrationsCluster );
    }

    public void checkResourceNameSpaceMapping( String alias, String name, String uri, String expected)
        throws IOException, ServletException
    {
        // prepare
        HttpResource httpResource = new HttpResource( alias, name, m_httpContext );
        expect( m_registrationsCluster.getByAlias( uri) ).andReturn( httpResource );
        expect( m_httpContext.handleSecurity( null, m_httpResponse ) ).andReturn( true );
        expect( m_httpContext.getResource( expected ) ).andReturn( m_url );
        expect( m_httpContext.getMimeType( expected ) ).andReturn( "text/plain" );
        expect( m_httpResponse.getOutputStream() ).andReturn( null );
        replay( m_registrationsCluster, m_httpContext, m_httpResponse );
        // execute
        m_underTest.handle( uri, null, m_httpResponse, 0 );
        // verify
        verify( m_registrationsCluster, m_httpContext, m_httpResponse );
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

    @Test
    public void check404IfNoMatch()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrationsCluster.getByAlias( "/nomatch" ) ).andReturn( null );
        expect( m_registrationsCluster.getByAlias( "/" ) ).andReturn( null );
        m_httpResponse.sendError( HttpServletResponse.SC_NOT_FOUND );
        replay( m_registrationsCluster, m_httpResponse );
        // execute
        m_underTest.handle( "/nomatch", null, m_httpResponse, 0);
        // verify
        verify( m_registrationsCluster, m_httpResponse );
    }

}
