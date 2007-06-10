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

import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Servlet;
import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class HttpServletTest
{

    private HttpServlet m_underTest;
    private Bundle m_bundle;
    private ServerController m_serverController;
    private Servlet m_servlet;
    private HttpContext m_context;
    private Dictionary m_initParams;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_serverController = createMock( ServerController.class );
        m_underTest = new HttpServlet( "/alias", m_servlet, null, m_context );
    }

    @Test
    public void registerFlow()
    {
        // prepare
        expect( m_serverController.addServlet( "/alias", m_servlet, null ) ).andReturn( "name" );
        replay( m_serverController );
        // execute
        m_underTest.register( m_serverController );
        // verify
        verify( m_serverController );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerWithNullServer()
    {
        m_underTest.register( null );
    }

    @Test
    public void unregisterFlow()
    {
        // prepare
        m_serverController.removeServlet( null );
        replay( m_serverController );
        // execute
        m_underTest.unregister( m_serverController );
        // verify
        verify( m_serverController );
    }

    @Test( expected = IllegalArgumentException.class )
    public void unregisterWithNullServer()
    {
        m_underTest.unregister( null );
    }

    // TODO add unit tests for initParams

}
