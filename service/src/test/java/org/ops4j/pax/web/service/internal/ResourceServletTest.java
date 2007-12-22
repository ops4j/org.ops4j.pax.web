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

public class ResourceServletTest
{

    private ResourceServlet m_underTest;
    private RegistrationsSet m_registrationsSet;
    private Registration m_registration;
    private HttpContext m_httpContext;
    private HttpServletRequest m_httpRequest;
    private HttpServletResponse m_httpResponse;

    @Before
    public void setUp()
    {
        m_registrationsSet = createMock( RegistrationsSet.class );
        m_registration = createMock( Registration.class );
        m_httpContext = createMock( HttpContext.class );
        m_httpRequest = createMock( HttpServletRequest.class );
        m_httpResponse = createMock( HttpServletResponse.class );
        m_underTest = new ResourceServlet();
        m_underTest.setRegistration( m_registration );
    }

    @Test( expected = IllegalArgumentException.class )
    public void setNullReqistration()
    {
        m_underTest.setRegistration( null );
    }

    @Test( expected = IllegalStateException.class )
    public void doGetWithRegistrationNotSet()
        throws IOException, ServletException
    {
        new ResourceServlet().doGet( m_httpRequest, m_httpResponse );
    }

    public void checkResourceNameSpaceMapping( String alias, String name, String uri, String expected )
        throws IOException, ServletException
    {
        // prepare
        expect( m_registration.getAlias() ).andReturn( alias );
        expect( m_registration.getName() ).andReturn( name );
        expect( m_httpRequest.getRequestURI() ).andReturn( uri );
        expect( m_registration.getHttpContext() ).andReturn( m_httpContext );
        expect( m_httpContext.getResource( expected ) ).andReturn( null );
        replay( m_registrationsSet, m_httpContext, m_httpRequest, m_httpResponse, m_registration );
        // execute
        m_underTest.doGet( m_httpRequest, m_httpResponse );
        // verify
        verify( m_registrationsSet, m_httpContext, m_httpRequest, m_httpResponse, m_registration );
    }

    @Test
    public void checkResourceNameSpaceMapping01()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/", "", "/fudd/bugs", "/fudd/bugs" );
    }

    @Test
    public void checkResourceNameSpaceMapping02()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/", "/", "/fudd/bugs", "/fudd/bugs" );
    }

    @Test
    public void checkResourceNameSpaceMapping03()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/", "/tmp", "/fudd/bugs", "/tmp/fudd/bugs" );
    }

    @Test
    public void checkResourceNameSpaceMapping04()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd", "", "/fudd/bugs", "/bugs" );
    }

    @Test
    public void checkResourceNameSpaceMapping05()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd", "/", "/fudd/bugs", "/bugs" );
    }

    @Test
    public void checkResourceNameSpaceMapping06()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd", "/tmp", "/fudd/bugs", "/tmp/bugs" );
    }

    @Test
    public void checkResourceNameSpaceMapping07()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd", "tmp", "/fudd/bugs/x.gif", "tmp/bugs/x.gif" );
    }

    @Test
    public void checkResourceNameSpaceMapping08()
        throws IOException, ServletException
    {
        checkResourceNameSpaceMapping( "/fudd/bugs/x.gif", "tmp/y.gif", "/fudd/bugs/x.gif", "tmp/y.gif" );
    }

}
