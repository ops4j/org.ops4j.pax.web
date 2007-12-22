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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;

public class HttpServiceHandlerTest
{

    private HttpServiceServletHandler m_underTest;
    private Registrations m_registrations;
    private Registration m_registration;
    private HttpContext m_httpContext;
    private HttpServletRequest m_httpRequest;
    private HttpServletResponse m_httpResponse;

    @Before
    public void setUp()
        throws IOException
    {
        m_registrations = createMock( Registrations.class );
        m_registration = createMock( Registration.class );
        m_httpContext = createMock( HttpContext.class );
        m_httpRequest = createMock( HttpServletRequest.class );
        m_httpResponse = createMock( HttpServletResponse.class );
        m_underTest = new HttpServiceServletHandler( m_registrations );
    }

    @Test
    public void unauthorizedRequestFlow()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrations.getByAlias( "/alias" ) ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( (HttpServletRequest) notNull(), (HttpServletResponse) notNull() )
        ).andReturn( false );
        expect( m_httpResponse.isCommitted() ).andReturn( false );
        m_httpResponse.sendError( HttpServletResponse.SC_UNAUTHORIZED );
        replay( m_registrations, m_registration, m_httpContext, m_httpResponse );
        // execute
        m_underTest.handle( "/alias", m_httpRequest, m_httpResponse, 0 );
        // verify
        verify( m_registrations, m_registration, m_httpContext, m_httpResponse );
    }

    @Test
    public void exactMatchFlow()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrations.getByAlias( "/fudd/bugs" ) ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( (HttpServletRequest) notNull(), (HttpServletResponse) notNull() )
        ).andReturn( true );
        m_httpResponse.setStatus( HttpServletResponse.SC_OK );
        expect( m_httpResponse.isCommitted() ).andReturn( true );
        replay( m_registrations, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
        // execute
        new HttpServiceServletHandler( m_registrations )
        {

            @Override
            protected void internalHandle( String target, HttpServletRequest request, int dispatchMode,
                                           HttpServletResponse response )
                throws IOException, ServletException
            {
                response.setStatus( HttpServletResponse.SC_OK );
            }
        }.handle( "/fudd/bugs", m_httpRequest, m_httpResponse, 0 );
        // verify
        verify( m_registrations, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
    }

    /**
     * Test that if request is not handled a new match on the substruct till the last / is done even if the last char
     * is /.
     */
    @Test
    public void trailingSlashesFallback()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrations.getByAlias( "/fudd/bugs/" ) ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( (HttpServletRequest) notNull(), (HttpServletResponse) notNull() )
        ).andReturn( true );
        expect( m_registrations.getByAlias( "/fudd/bugs" ) ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( (HttpServletRequest) notNull(), (HttpServletResponse) notNull() )
        ).andReturn( true );

        expect( m_registrations.getByAlias( "/fudd" ) ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( (HttpServletRequest) notNull(), (HttpServletResponse) notNull() )
        ).andReturn( true );
        m_httpResponse.setStatus( HttpServletResponse.SC_OK );
        expect( m_httpResponse.isCommitted() ).andReturn( true );
        replay( m_registrations, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
        // execute
        new HttpServiceServletHandler( m_registrations )
        {
            private int m_counter;

            @Override
            protected void internalHandle( String target, HttpServletRequest request, int dispatchMode,
                                           HttpServletResponse response )
                throws IOException, ServletException
            {
                if( ++m_counter == 3 )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                }
            }

        }.handle( "/fudd/bugs/", m_httpRequest, m_httpResponse, 0 );
        // verify
        verify( m_registrations, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
    }

    /**
     * Test that if request is not handled a new match on the substruct till the last / is done.
     */
    @Test
    public void fallbackIfRequestNotHandled()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrations.getByAlias( "/fudd/bugs" ) ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( (HttpServletRequest) notNull(), (HttpServletResponse) notNull() )
        ).andReturn( true );
        expect( m_registrations.getByAlias( "/fudd" ) ).andReturn( m_registration );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.handleSecurity( (HttpServletRequest) notNull(), (HttpServletResponse) notNull() )
        ).andReturn( true );
        m_httpResponse.setStatus( HttpServletResponse.SC_OK );
        expect( m_httpResponse.isCommitted() ).andReturn( true );
        replay( m_registrations, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
        // execute
        new HttpServiceServletHandler( m_registrations )
        {
            private int m_counter;

            @Override
            protected void internalHandle( String target, HttpServletRequest request, int dispatchMode,
                                           HttpServletResponse response )
                throws IOException, ServletException
            {
                if( ++m_counter == 2 )
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                }
            }

        }.handle( "/fudd/bugs", m_httpRequest, m_httpResponse, 0 );
        // verify
        verify( m_registrations, m_registration, m_httpContext, m_httpRequest, m_httpResponse );
    }

    @Test
    public void substringMatching()
        throws IOException, ServletException
    {
        // prepare
        expect( m_registrations.getByAlias( "/fudd/bugs/x.gif" ) ).andReturn( null );
        expect( m_registrations.getByAlias( "/fudd/bugs" ) ).andReturn( null );
        expect( m_registrations.getByAlias( "/fudd" ) ).andReturn( null );
        replay( m_registrations, m_httpResponse );
        // execute
        m_underTest.handle( "/fudd/bugs/x.gif", null, m_httpResponse, 0 );
        // verify
        verify( m_registrations, m_httpResponse );
    }

//    @Test
//    public void Return404IfNoMatch()
//        throws IOException, ServletException
//    {
//        // prepare
//        expect( m_registrations.getByAlias( "/nomatch" ) ).andReturn( null );
//        expect( m_registrations.getByAlias( "/" ) ).andReturn( null );
//        m_httpResponse.sendError( HttpServletResponse.SC_NOT_FOUND );
//        replay( m_registrations, m_httpResponse );
//        // execute
//        m_underTest.handle( "/nomatch", null, m_httpResponse, 0 );
//        // verify
//        verify( m_registrations, m_httpResponse );
//    }

}
