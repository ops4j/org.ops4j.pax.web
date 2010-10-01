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
package org.ops4j.pax.web.service.jetty.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Dispatcher;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;

public class ResourceServletTest
{

    private HttpContext m_httpContext;
    private HttpServletRequest m_httpRequest;
    private HttpServletResponse m_httpResponse;

    @Before
    public void setUp()
    {
        m_httpContext = createMock( HttpContext.class );
        m_httpRequest = createMock( HttpServletRequest.class );
        m_httpResponse = createMock( HttpServletResponse.class );
    }

    private void checkResourceNameSpaceMapping( String alias, String name, String uri, String expected )
        throws IOException, ServletException
    {
        // prepare
        expect( m_httpRequest.getRequestURI() ).andReturn( uri );
        expect( m_httpRequest.getAttribute( Dispatcher.INCLUDE_REQUEST_URI ) ).andReturn( null );
	m_httpResponse.sendError(404);
        expect( m_httpContext.getResource( expected ) ).andReturn( null );

        replay( m_httpContext, m_httpRequest, m_httpResponse );
        // execute
        new ResourceServlet( m_httpContext, "", alias, name ).doGet( m_httpRequest, m_httpResponse );
        // verify
        verify( m_httpContext, m_httpRequest, m_httpResponse );
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
