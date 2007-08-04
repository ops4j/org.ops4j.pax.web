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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;

public class HttpServiceHandlerTest
{

    private HttpServiceHandler m_underTest;
    private RegistrationsCluster m_registrationsCluster;
    private Registration m_registration;
    private HttpContext m_httpContext;
    private HttpServletRequest m_httpRequest;
    private HttpServletResponse m_httpResponse;

    @Before
    public void setUp()
        throws IOException
    {
        m_registrationsCluster = createMock( RegistrationsCluster.class );
        m_registration = createMock( Registration.class );
        m_httpContext = createMock( HttpContext.class );
        m_httpRequest = createMock( HttpServletRequest.class );
        m_httpResponse = createMock( HttpServletResponse.class );
        m_underTest = new HttpServiceHandler( m_registrationsCluster );
    }

    @Test
    public void unauthorizedRequestFlow()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrationsCluster.getByAlias( "/alias") ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( m_httpRequest, m_httpResponse ) ).andReturn( false );
        replay( m_registrationsCluster, m_registration, m_httpContext );
        // execute
        m_underTest.handle( "/alias", m_httpRequest, m_httpResponse, 0 );
        // verify
        verify( m_registrationsCluster, m_registration, m_httpContext );
    }

    @Test
    public void exactMatchFlow()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrationsCluster.getByAlias( "/fudd/bugs") ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( m_httpRequest, m_httpResponse ) ).andReturn( true );
        m_httpRequest.setAttribute( ResourceServlet.REQUEST_HANDLED, true );
        expect( m_httpRequest.getAttribute( ResourceServlet.REQUEST_HANDLED ) ).andReturn( true );
        replay( m_registrationsCluster, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
        // execute
        m_underTest.handle( "/fudd/bugs", m_httpRequest, m_httpResponse, 0 );
        // verify
        verify( m_registrationsCluster, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
    }
    
    @Test
    public void trailingSlashesFallback()
        throws IOException, ServletException
    {
    	 // prepare
        expect( m_registrationsCluster.getByAlias( "/fudd/bugs/") ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( m_httpRequest, m_httpResponse ) ).andReturn( true );
        m_httpRequest.setAttribute( ResourceServlet.REQUEST_HANDLED, true );
        expect( m_httpRequest.getAttribute( ResourceServlet.REQUEST_HANDLED ) ).andReturn( false );

        expect( m_registrationsCluster.getByAlias( "/fudd/bugs") ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( m_httpRequest, m_httpResponse ) ).andReturn( true );
        m_httpRequest.setAttribute( ResourceServlet.REQUEST_HANDLED, true );
        expect( m_httpRequest.getAttribute( ResourceServlet.REQUEST_HANDLED ) ).andReturn( false );

        expect( m_registrationsCluster.getByAlias( "/fudd") ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( m_httpRequest, m_httpResponse ) ).andReturn( true );
        m_httpRequest.setAttribute( ResourceServlet.REQUEST_HANDLED, true );
        expect( m_httpRequest.getAttribute( ResourceServlet.REQUEST_HANDLED ) ).andReturn( true );

        replay( m_registrationsCluster, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
        // execute
        m_underTest.handle( "/fudd/bugs/", m_httpRequest, m_httpResponse, 0 );
        // verify
        verify( m_registrationsCluster, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
    }

    @Test
    public void fallbackIfRequestNotHandled()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrationsCluster.getByAlias( "/fudd/bugs") ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( m_httpRequest, m_httpResponse ) ).andReturn( true );
        m_httpRequest.setAttribute( ResourceServlet.REQUEST_HANDLED, true );
        expect( m_httpRequest.getAttribute( ResourceServlet.REQUEST_HANDLED ) ).andReturn( false );

        expect( m_registrationsCluster.getByAlias( "/fudd") ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( m_httpRequest, m_httpResponse ) ).andReturn( true );
        m_httpRequest.setAttribute( ResourceServlet.REQUEST_HANDLED, true );
        expect( m_httpRequest.getAttribute( ResourceServlet.REQUEST_HANDLED ) ).andReturn( true );

        replay( m_registrationsCluster, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
        // execute
        m_underTest.handle( "/fudd/bugs", m_httpRequest, m_httpResponse, 0 );
        // verify
        verify( m_registrationsCluster, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
    }

    @Test
    public void substringMatching()
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

    @Test
    public void Return404IfNoMatch()
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
    
    @Test
    public void trimTrailingSlashesTest() {
    	char trail = '/';
    	assertEquals("/brick/foo/",m_underTest.trimTrailingChars("/brick/foo//",trail));
    	assertEquals("/brick/foo",m_underTest.trimTrailingChars("/brick/foo",trail));
    	assertEquals("/brick/foo/",m_underTest.trimTrailingChars("/brick/foo///",trail));
    	assertEquals("/brick",m_underTest.trimTrailingChars("/brick",trail));
    	assertEquals("/",m_underTest.trimTrailingChars("/",trail));
    	assertEquals("/",m_underTest.trimTrailingChars("//",trail));
    }

}
