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
import javax.servlet.ServletException;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class StartedHttpServiceTest
{

    private StartedHttpService m_underTest;
    private Bundle m_bundle;
    private Servlet m_servlet;
    private HttpContext m_context;
    private Dictionary m_initParams;
    private RegistrationsSet m_registrationsSet;
    private Registrations m_registrations;
    private Registration m_httpServlet;
    private Registration m_httpResource;
    private ServerController m_serverController;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_registrationsSet = createMock( RegistrationsSet.class );
        m_registrations = createMock( Registrations.class );
        m_httpServlet = createMock( Registration.class );
        m_httpResource = createMock( Registration.class );
        m_serverController = createMock( ServerController.class );
        m_initParams = new Hashtable();
        m_underTest = new StartedHttpService( m_bundle, m_serverController, m_registrationsSet );
        reset( m_bundle, m_servlet, m_context, m_registrationsSet, m_httpServlet, m_httpResource,
               m_serverController
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundle()
        throws ServletException
    {
        new StartedHttpService( null, m_serverController, m_registrationsSet );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullRegistrationRepository()
        throws ServletException
    {
        new StartedHttpService( m_bundle, m_serverController, null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullHttpServiceServer()
        throws ServletException
    {
        new StartedHttpService( m_bundle, null, m_registrationsSet );
    }

    @Test
    public void checkRegisterServletFlow()
        throws NamespaceException, ServletException
    {
        // prepare
        expect( m_registrationsSet.createRegistrations( m_context )).andReturn( m_registrations );
        expect( m_registrations.registerServlet( "/alias", m_servlet, m_initParams ) ).andReturn( m_httpServlet );
        m_httpServlet.register( m_serverController );
        replay( m_registrationsSet, m_registrations, m_httpServlet );
        // execute
        m_underTest.registerServlet( "/alias", m_servlet, m_initParams, m_context );
        // verify
        verify( m_registrationsSet, m_registrations, m_httpServlet );
    }

    @Test
    public void registerServletWithNullHttpContext()
        throws NamespaceException, ServletException
    {
        // prepare
        expect( m_registrationsSet.createRegistrations( ( HttpContext) notNull() )).andReturn( m_registrations );
        expect( m_registrations.registerServlet( eq( "/alias" ), eq( m_servlet ), eq( m_initParams ) ) ).andReturn(
            m_httpServlet
        );
        replay( m_registrationsSet, m_registrations );
        // execute
        m_underTest.registerServlet( "/alias", m_servlet, m_initParams, null );
        // verify
        verify( m_registrationsSet, m_registrations );
    }

    @Test
    public void createDefaultContext()
        throws NamespaceException, ServletException
    {
        assertNotNull( "not null", m_underTest.createDefaultHttpContext() );
    }

    @Test
    public void checkRegistrationAsHttpServiceServerListener()
    {
        // prepare
        m_serverController.addListener( (ServerListener) notNull() );
        replay( m_serverController );
        // execute
        new StartedHttpService( m_bundle, m_serverController, m_registrationsSet );
        // verify
        verify( m_serverController );
    }

    @Test
    public void checkRegisterResourceFlow()
        throws NamespaceException
    {
        // prepare
        expect( m_registrationsSet.createRegistrations( m_context )).andReturn( m_registrations );
        expect( m_registrations.registerResources( "/alias", "/name" ) ).andReturn( m_httpResource );
        m_httpResource.register( m_serverController );
        replay( m_registrationsSet, m_httpResource, m_registrations );
        // execute
        m_underTest.registerResources( "/alias", "/name", m_context );
        // verify
        verify( m_registrationsSet, m_httpResource, m_registrations );
    }

//    @Test
//    public void stop()
//    {
//        //prepare
//        Registration[] targets = new Registration[]{ m_httpServlet };
//        expect( m_registrationsSet.get() ).andReturn( targets );
//        m_registrationsSet.unregister( m_httpServlet );
//        m_httpServlet.unregister( m_serverController );
//        replay( m_registrationsSet, m_httpServlet );
//        // execute
//        m_underTest.stop();
//        // verify
//        verify( m_registrationsSet, m_httpServlet );
//    }
//
//    @Test
//    public void unregisterFlow()
//    {
//        // prepare
//        expect( m_registrationsSet.getByAlias( "/alias" ) ).andReturn( m_httpServlet );
//        m_registrationsSet.unregister( m_httpServlet );
//        m_httpServlet.unregister( m_serverController );
//        replay( m_registrationsSet, m_httpServlet );
//        // execute
//        m_underTest.unregister( "/alias" );
//        // verify
//        verify( m_registrationsSet, m_httpServlet );
//    }

    @Test( expected = IllegalArgumentException.class )
    public void unregisterWithNullAlias()
    {
        m_underTest.unregister( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void unregisterWithEmptyAlias()
    {
        m_underTest.unregister( "" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void unregisterWithUnregisteredAlias()
    {
        m_underTest.unregister( "/unregistered" );
    }

}