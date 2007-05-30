package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Connector;
import org.ops4j.pax.web.service.HttpServiceConfiguration;
import javax.servlet.Servlet;

public class HttpServiceServerImplTest
{
    private HttpServiceServerImpl m_underTest;
    private HttpServiceConfiguration m_configuration;
    private JettyFactory m_jettyFactory;
    private JettyServer m_jettyServer;
    private Connector m_jettyConnector;
    private HttpServiceServerListener m_listener;
    private Servlet m_servlet;

    @Before
    public void setUp()
    {
        m_configuration = createMock( HttpServiceConfiguration.class );
        m_jettyFactory = createMock( JettyFactory.class );
        m_jettyServer = createMock( JettyServer.class );
        m_jettyConnector = createMock( Connector.class );
        m_listener = createMock( HttpServiceServerListener.class );
        m_servlet = createMock( Servlet.class );
        m_underTest = new HttpServiceServerImpl( m_jettyFactory );
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
        m_jettyServer.addContext();
        m_jettyServer.start();
        m_jettyServer.stop();
        m_listener.stateChanged( HttpServiceServerEvent.CONFIGURED);
        m_listener.stateChanged( HttpServiceServerEvent.STARTED);
        m_listener.stateChanged( HttpServiceServerEvent.STOPPED);
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
        HttpServiceServerListener listener = createMock( HttpServiceServerListener.class );
        m_listener.stateChanged( HttpServiceServerEvent.STARTED);
        listener.stateChanged( HttpServiceServerEvent.STARTED);
        replay ( m_listener, listener );
        // run
        m_underTest.addListener( m_listener );
        m_underTest.addListener( listener );
        m_underTest.notifyListeners( HttpServiceServerEvent.STARTED );
        // verify
        verify( m_listener, listener );
    }

    @Test
    public void addServletFlow()
    {
        // prepare
        expect( m_jettyFactory.createServer() ).andReturn( m_jettyServer );
        m_jettyServer.addContext();
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
