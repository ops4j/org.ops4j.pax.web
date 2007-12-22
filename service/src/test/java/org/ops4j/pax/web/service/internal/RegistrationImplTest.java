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
import java.util.Map;
import javax.servlet.Servlet;
import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;

public class RegistrationImplTest
{

    private RegistrationImpl m_underTest;
    private ServerController m_serverController;
    private Servlet m_servlet;
    private HttpContext m_context;

    @Before
    public void setUp()
    {
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_serverController = createMock( ServerController.class );
        m_underTest = new RegistrationImpl( "/alias", m_servlet, null, m_context );
    }

    @Test
    public void registerFlowWithNullInitParams()
    {
        // prepare
        expect( m_serverController.addServlet( "/alias", m_servlet, null, m_context ) ).andReturn(
            "name"
        );
        replay( m_serverController );
        // execute
        m_underTest.register( m_serverController );
        // verify
        verify( m_serverController );
    }

    @Test
    @SuppressWarnings( { "unchecked" } )
    public void registerFlowWithNotNullInitParams()
    {
        // prepare
        expect( m_serverController.addServlet( eq( "/alias" ), eq( m_servlet ), (Map<String, String>) notNull(),
                                               eq( m_context )
        )
        ).andReturn( "name" );
        Dictionary<String, String> initParams = new Hashtable<String, String>();
        initParams.put( "key", "value" );
        m_underTest = new RegistrationImpl( "/alias", m_servlet, initParams, m_context );
        replay( m_serverController );
        // execute
        m_underTest.register( m_serverController );
        // verify
        verify( m_serverController );
    }

    @Test( expected = IllegalArgumentException.class )
    @SuppressWarnings( { "unchecked" } )
    public void registerWithInvalidDictionaryValue()
    {
        // prepare
        Dictionary initParams = new Hashtable();
        initParams.put( "key", Boolean.TRUE );
        m_underTest = new RegistrationImpl( "/alias", m_servlet, initParams, m_context );
        replay( m_serverController );
        // execute
        m_underTest.register( m_serverController );
        // verify
        verify( m_serverController );
    }

    @Test( expected = IllegalArgumentException.class )
    @SuppressWarnings( { "unchecked" } )
    public void registerWithInvalidDictionaryKey()
    {
        // prepare
        Dictionary initParams = new Hashtable();
        initParams.put( Boolean.TRUE, "value" );
        m_underTest = new RegistrationImpl( "/alias", m_servlet, initParams, m_context );
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
        m_serverController.removeServlet( null, m_context );
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

}
