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

public class HttpServiceImplTest
{
    private HttpServiceImpl m_underTest;
    private Bundle m_bundle;
    private Servlet m_servlet;
    private HttpContext m_context;
    private Dictionary m_initParams;
    private Registrations m_registrations;
    private HttpTarget m_httpServlet;
    private HttpTarget m_httpResource;
    private ServerController m_serverController;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_registrations = createMock( Registrations.class );
        m_httpServlet = createMock( HttpTarget.class );
        m_httpResource = createMock( HttpTarget.class );
        m_serverController = createMock( ServerController.class );
        m_initParams = new Hashtable();
        m_underTest = new HttpServiceImpl( m_bundle, m_serverController, m_registrations );
        reset( m_bundle, m_servlet, m_context, m_registrations, m_httpServlet, m_httpResource, m_serverController );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundle()
        throws ServletException
    {
        new HttpServiceImpl( null, m_serverController, m_registrations );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullRegistrationRepository()
        throws ServletException
    {
        new HttpServiceImpl( m_bundle, m_serverController, null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullHttpServiceServer()
        throws ServletException
    {
        new HttpServiceImpl( m_bundle, null, m_registrations );
    }

    @Test
    public void stateChangedOnServerStarted() {
        // prepare
        HttpTarget[] httpTargets = new HttpTarget[] { m_httpServlet };
        expect( m_registrations.get() ).andReturn( httpTargets );
        m_httpServlet.register( m_serverController );
        replay( m_registrations, m_httpServlet );
        // execute
        m_underTest.stateChanged( ServerEvent.STARTED );
        // verify
        verify( m_registrations, m_httpServlet );
    }

    // expect to not do anything
    @Test
    public void stateChangedOnServerConfigured() {
        // prepare
        replay( m_registrations, m_httpServlet );
        // execute
        m_underTest.stateChanged( ServerEvent.CONFIGURED );
        // verify
        verify( m_registrations, m_httpServlet );
    }

    // expect to not do anything
    @Test
    public void stateChangedOnServerStoped() {
        // prepare
        replay( m_registrations, m_httpServlet );
        // execute
        m_underTest.stateChanged( ServerEvent.STOPPED );
        // verify
        verify( m_registrations, m_httpServlet );
    }

    @Test
    public void checkRegisterServletFlow()
        throws NamespaceException, ServletException
    {
        // prepare
        expect( m_registrations.registerServlet( "/alias", m_servlet, m_initParams, m_context ) ).andReturn( m_httpServlet );
        m_httpServlet.register( m_serverController );
        replay( m_registrations, m_httpServlet );
        // execute
        m_underTest.registerServlet( "/alias", m_servlet, m_initParams, m_context );
        // verify
        verify( m_registrations, m_httpServlet );
    }

    @Test
    public void registerServletWithNullHttpContext()
        throws NamespaceException, ServletException
    {
        // prepare
        expect( m_registrations.registerServlet( eq( "/alias" ), eq( m_servlet) , eq( m_initParams) , (HttpContext) notNull() )).andReturn( m_httpServlet );
        replay( m_registrations );
        // execute
        m_underTest.registerServlet( "/alias", m_servlet, m_initParams, null );
        // verify
        verify( m_registrations );
    }

    @Test
    public void createDefaultContext()
        throws NamespaceException, ServletException
    {
        assertNotNull( "not null", m_underTest.createDefaultHttpContext() );   
    }

    @Test
    public void checkRegistrationAsHttpServiceServerListener() {
        // prepare
        m_serverController.addListener( (ServerListener) notNull() );
        replay( m_serverController );
        // execute
        new HttpServiceImpl( m_bundle, m_serverController, m_registrations );
        // verify
        verify( m_serverController );
    }

    @Test
    public void checkRegisterResourceFlow()
        throws NamespaceException
    {
        // prepare
        expect( m_registrations.registerResources( "/alias", "/name", m_context ) ).andReturn( m_httpResource );
        m_httpResource.register( m_serverController );
        replay( m_registrations, m_httpResource );
        // execute
        m_underTest.registerResources( "/alias", "/name", m_context );
        // verify
        verify( m_registrations, m_httpResource );
    }

    @Test
    public void stop()
    {
        //prepare
        HttpTarget[] targets = new HttpTarget[] { m_httpServlet };
        expect( m_registrations.get() ).andReturn( targets );
        m_registrations.unregister( m_httpServlet );
        m_httpServlet.unregister( m_serverController );
        replay( m_registrations, m_httpServlet );
        // execute
        m_underTest.stop();
        // verify
        verify( m_registrations, m_httpServlet );
    }

    @Test
    public void unregisterFlow()
    {
        // prepare
        expect( m_registrations.getByAlias( "/alias" ) ).andReturn( m_httpServlet);
        m_httpServlet.unregister( m_serverController );
        replay( m_registrations, m_httpServlet );
        // execute
        m_underTest.unregister( "/alias" );
        // verify
        verify( m_registrations, m_httpServlet );
    }

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
