package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.ops4j.pax.web.service.HttpServiceConfiguration;
import javax.servlet.Servlet;

public class ServerControllerImplTest
{
    private ServerControllerImpl m_underTest;
    private HttpServiceConfiguration m_configuration;
    private JettyFactory m_jettyFactory;
    private JettyServer m_jettyServer;
    private Connector m_jettyConnector;
    private ServerListener m_listener;
    private Servlet m_servlet;
    private RegistrationsCluster m_cluster;

    @Before
    public void setUp()
    {
        m_configuration = createMock( HttpServiceConfiguration.class );
        m_jettyFactory = createMock( JettyFactory.class );
        m_jettyServer = createMock( JettyServer.class );
        m_jettyConnector = createMock( Connector.class );
        m_cluster = createMock( RegistrationsCluster.class );
        m_listener = createMock( ServerListener.class );
        m_servlet = createMock( Servlet.class );
        m_underTest = new ServerControllerImpl( m_jettyFactory, m_cluster );
        reset( m_configuration, m_jettyFactory, m_jettyServer, m_jettyConnector, m_cluster, m_listener, m_servlet );
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
        m_jettyServer.addConnector( m_jettyConnector );
        m_jettyServer.addContext( (ServletHandler) notNull() );
        m_jettyServer.start();
        m_jettyServer.stop();
        m_listener.stateChanged( ServerEvent.CONFIGURED);
        m_listener.stateChanged( ServerEvent.STARTED);
        m_listener.stateChanged( ServerEvent.STOPPED);
        replay( m_jettyFactory, m_jettyServer, m_configuration, m_listener );
        // run
        m_underTest.addListener ( m_listener );
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
        m_listener.stateChanged( ServerEvent.STARTED);
        listener.stateChanged( ServerEvent.STARTED);
        replay ( m_listener, listener );
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
        m_jettyServer.addContext( (ServletHandler) notNull() );
        m_jettyServer.start();
        m_jettyServer.addServlet( "/alias", m_servlet );
        replay( m_jettyFactory, m_jettyServer );
        // execute
        m_underTest.configure( m_configuration );
        m_underTest.start();
        m_underTest.addServlet( "/alias", m_servlet );
        // verify
        verify( m_jettyFactory, m_jettyServer );
    }
    
}
