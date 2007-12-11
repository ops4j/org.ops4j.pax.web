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

import java.util.Map;
import javax.servlet.Servlet;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Connector;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

public class ServerControllerImplTest
{

    private ServerControllerImpl m_underTest;
    private HttpServiceConfiguration m_configuration;
    private JettyFactory m_jettyFactory;
    private JettyServer m_jettyServer;
    private Connector m_jettyConnector;
    private ServerListener m_listener;
    private Servlet m_servlet;
    private HttpContext m_httpContext;
    private Registrations m_registrations;

    @Before
    public void setUp()
    {
        m_configuration = createMock( HttpServiceConfiguration.class );
        m_jettyFactory = createMock( JettyFactory.class );
        m_jettyServer = createMock( JettyServer.class );
        m_jettyConnector = createMock( Connector.class );
        m_listener = createMock( ServerListener.class );
        m_servlet = createMock( Servlet.class );
        m_httpContext = createMock( HttpContext.class );
        m_registrations = createMock( Registrations.class );
        m_underTest = new ServerControllerImpl( m_jettyFactory );
    }

    @Test( expected = IllegalStateException.class )
    public void startWithoutBeingConfigured()
    {
        m_underTest.start();
    }

    @Test
    public void fullLifeCycle()
    {
        // prepare
        expect( m_jettyFactory.createServer() ).andReturn( m_jettyServer );
        expect( m_jettyFactory.createConnector( 80 ) ).andReturn( m_jettyConnector );
        expect( m_configuration.isHttpEnabled() ).andReturn( true );
        expect( m_configuration.getHttpPort() ).andReturn( 80 );
        expect( m_configuration.isHttpSecureEnabled() ).andReturn( false );
        expect( m_configuration.getTemporaryDirectory() ).andReturn( null );
        expect( m_configuration.getSessionTimeout() ).andReturn( 30 );
        m_jettyServer.addConnector( m_jettyConnector );
        m_jettyServer.configureContext( (Map<String, Object>) notNull(), eq( 30 ) );
        m_jettyServer.start();
        m_jettyServer.stop();
        m_listener.stateChanged( ServerEvent.CONFIGURED );
        m_listener.stateChanged( ServerEvent.STARTED );
        m_listener.stateChanged( ServerEvent.STOPPED );
        replay( m_jettyFactory, m_jettyServer, m_configuration, m_listener );
        // run
        m_underTest.addListener( m_listener );
        m_underTest.configure( m_configuration );
        m_underTest.start();
        m_underTest.stop();
        // verify
        verify( m_jettyFactory, m_jettyServer, m_configuration, m_listener );
    }

    @Test
    public void fullLifeCycleWithSsl()
    {
        // prepare
        expect( m_jettyFactory.createServer() ).andReturn( m_jettyServer );
        expect( m_configuration.isHttpEnabled() ).andReturn( false );
        expect( m_configuration.isHttpSecureEnabled() ).andReturn( true );
        expect( m_configuration.getHttpSecurePort() ).andReturn( 443 );
        expect( m_configuration.getSslKeystore() ).andReturn( "keystore" );
        expect( m_configuration.getSslPassword() ).andReturn( "password" );
        expect( m_configuration.getSslKeyPassword() ).andReturn( "keyPassword" );
        expect( m_jettyFactory.createSecureConnector( 443, "keystore", "password", "keyPassword" ) ).andReturn(
            m_jettyConnector
        );
        expect( m_configuration.getTemporaryDirectory() ).andReturn( null );
        expect( m_configuration.getSessionTimeout() ).andReturn( null );
        m_jettyServer.addConnector( m_jettyConnector );
        m_jettyServer.configureContext( (Map<String, Object>) notNull(), (Integer) eq( null ) );
        m_jettyServer.start();
        m_jettyServer.stop();
        m_listener.stateChanged( ServerEvent.CONFIGURED );
        m_listener.stateChanged( ServerEvent.STARTED );
        m_listener.stateChanged( ServerEvent.STOPPED );
        replay( m_jettyFactory, m_jettyServer, m_configuration, m_listener );
        // run
        m_underTest.addListener( m_listener );
        m_underTest.configure( m_configuration );
        m_underTest.start();
        m_underTest.stop();
        // verify
        verify( m_jettyFactory, m_jettyServer, m_configuration, m_listener );
    }

    @Test
    public void stopWithoutBeingConfigured()
    {
        m_underTest.stop();
    }

    @Test
    public void stopWithoutBeingStarted()
    {
        m_underTest.configure( m_configuration );
        m_underTest.stop();
    }

    @Test( expected = IllegalArgumentException.class )
    public void configureWithNullConfiguration()
    {
        m_underTest.configure( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void addNullListener()
    {
        m_underTest.addListener( null );
    }

    @Test
    public void notifyListeners()
    {
        // prepare
        ServerListener listener = createMock( ServerListener.class );
        m_listener.stateChanged( ServerEvent.STARTED );
        listener.stateChanged( ServerEvent.STARTED );
        replay( m_listener, listener );
        // run
        m_underTest.addListener( m_listener );
        m_underTest.addListener( listener );
        m_underTest.notifyListeners( ServerEvent.STARTED );
        // verify
        verify( m_listener, listener );
    }

    @Test
    public void addServletFlow()
    {
        // prepare
        expect( m_jettyFactory.createServer() ).andReturn( m_jettyServer );
        expect( m_configuration.isHttpEnabled() ).andReturn( false );
        expect( m_configuration.isHttpSecureEnabled() ).andReturn( false );
        expect( m_configuration.getTemporaryDirectory() ).andReturn( null );
        expect( m_configuration.getSessionTimeout() ).andReturn( null );
        m_jettyServer.configureContext( (Map<String, Object>) notNull(), (Integer) eq( null ) );
        m_jettyServer.start();
        expect( m_jettyServer.addServlet( "/alias", m_servlet, null, m_httpContext, m_registrations ) ).andReturn(
            "name"
        );

        replay( m_jettyFactory, m_jettyServer, m_configuration, m_httpContext, m_registrations );
        // execute
        m_underTest.configure( m_configuration );
        m_underTest.start();
        assertEquals( "Returned name", "name",
                      m_underTest.addServlet( "/alias", m_servlet, null, m_httpContext, m_registrations )
        );
        // verify
        verify( m_jettyFactory, m_jettyServer, m_configuration, m_httpContext, m_registrations );
    }

    @Test( expected = IllegalArgumentException.class )
    public void addServletWithNullAlias()
    {
        // prepare
        replay( m_servlet, m_httpContext );
        // execute
        m_underTest.addServlet( null, m_servlet, null, m_httpContext, m_registrations );
        // verify
        verify( m_servlet, m_httpContext );
    }

    @Test( expected = IllegalArgumentException.class )
    public void addServletWithEmptyAlias()
    {
        // prepare
        replay( m_servlet, m_httpContext, m_registrations );
        // execute
        m_underTest.addServlet( "", m_servlet, null, m_httpContext, m_registrations );
        // verify
        verify( m_servlet, m_httpContext, m_registrations );
    }

    @Test( expected = IllegalArgumentException.class )
    public void addServletWithNullServlet()
    {
        // execute
        m_underTest.addServlet( null, m_servlet, null, m_httpContext, m_registrations );
    }

    @Test
    public void removeServletFlowOnServerNotStarted()
    {
        // prepare
        replay( m_jettyServer, m_httpContext );
        // execute
        m_underTest.removeServlet( "/alias", m_httpContext );
        // verify
        verify( m_jettyServer, m_httpContext );
    }

    @Test
    public void removeServletFlowOnServerStarted()
    {
        // prepare
        expect( m_jettyFactory.createServer() ).andReturn( m_jettyServer );
        expect( m_configuration.isHttpEnabled() ).andReturn( false );
        expect( m_configuration.isHttpSecureEnabled() ).andReturn( false );
        expect( m_configuration.getTemporaryDirectory() ).andReturn( null );
        expect( m_configuration.getSessionTimeout() ).andReturn( null );
        m_jettyServer.configureContext( (Map<String, Object>) notNull(), (Integer) eq( null ) );
        m_jettyServer.start();
        m_jettyServer.removeServlet( "/alias", m_httpContext );

        replay( m_jettyServer, m_jettyFactory, m_configuration, m_httpContext );
        // execute
        m_underTest.configure( m_configuration );
        m_underTest.start();
        m_underTest.removeServlet( "/alias", m_httpContext );
        // verify
        verify( m_jettyServer, m_jettyFactory, m_configuration, m_httpContext );
    }

    @Test( expected = IllegalArgumentException.class )
    public void removeServletWithNullAlias()
    {
        // execute
        m_underTest.removeServlet( null, m_httpContext );
    }

    @Test( expected = IllegalArgumentException.class )
    public void removeServletWithEmptyAlias()
    {
        // execute
        m_underTest.removeServlet( "", m_httpContext );
    }

    // TODO add unit tests for initParams

}
