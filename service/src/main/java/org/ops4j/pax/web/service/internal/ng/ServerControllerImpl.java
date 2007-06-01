package org.ops4j.pax.web.service.internal.ng;

import java.util.Set;
import java.util.HashSet;
import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

public class ServerControllerImpl implements ServerController
{

    private static final Log m_logger = LogFactory.getLog( ServerControllerImpl.class );
    
    private HttpServiceConfiguration m_configuration;
    private State m_state;
    private JettyFactory m_jettyFactory;
    private RegistrationsCluster m_registrationsCluster;
    private JettyServer m_jettyServer;
    private Set<ServerListener> m_listeners;

    public ServerControllerImpl( final JettyFactory jettyFactory, final RegistrationsCluster registrationsCluster )
    {
        m_jettyFactory = jettyFactory;
        m_registrationsCluster = registrationsCluster;
        m_configuration = null;
        m_state = new Unconfigured();
        m_listeners = new HashSet<ServerListener>();
    }

    public synchronized void start() {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "starting server: " + this + ". current state: " + m_state );
        }
        m_state.start();
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "started server: " + this + ". current state: " + m_state );
        }
    }

    public synchronized void stop() {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "stopping server: " + this + ". current state: " + m_state );
        }
        m_state.stop();
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "stopped server: " + this + ". current state: " + m_state );
        }
    }

    public synchronized void configure( final HttpServiceConfiguration configuration )
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "configuring server: " + this + " -> " + configuration );
        }
        if ( configuration == null )
        {
            throw new IllegalArgumentException( "configuration == null" );
        }
        m_configuration = configuration;
        m_state.configure();
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "configured server: " + this + " -> " + configuration );
        }
    }

    public HttpServiceConfiguration getConfiguration()
    {
        return m_configuration;
    }

    public void addListener( ServerListener listener )
    {
        if ( listener == null)
        {
            throw new IllegalArgumentException( "listener == null" );
        }
        m_listeners.add( listener );
    }

    public void addServlet( final String alias, final Servlet servlet )
    {
        m_state.addServlet( alias, servlet);
    }

    public boolean isStarted()
    {
        return m_state instanceof Started;
    }

    void notifyListeners( ServerEvent event )
    {
        for ( ServerListener listener : m_listeners)
        {
            listener.stateChanged( event );
        }
    }

    public RegistrationsCluster getRegistrationsCluster()
    {
        return m_registrationsCluster;
    }

    private interface State
    {
        void start();
        void stop();
        void configure();
        void addServlet( String alias, Servlet servlet );
    }

    private class Started implements State
    {
        public void start()
        {
            throw new IllegalStateException( "server is already started. must be stopped first." );
        }

        public void stop()
        {
            m_jettyServer.stop();
            m_state = new Stopped();
            notifyListeners( ServerEvent.STOPPED );
        }

        public void configure()
        {
            ServerControllerImpl.this.stop();
            ServerControllerImpl.this.start();
        }

        public void addServlet( final String alias, final Servlet servlet )
        {
            System.out.println("add servlet");
            m_jettyServer.addServlet( alias, servlet);
        }
    }

    private class Stopped implements State
    {
        public void start()
        {
            m_jettyServer = m_jettyFactory.createServer();
            if ( m_configuration.isHttpEnabled() )
            {
                m_jettyServer.addConnector( m_jettyFactory.createConnector( m_configuration.getHttpPort() ) );
            }
            // TODO handle ssl port
            m_jettyServer.addContext( new HttpServiceHandler( m_registrationsCluster ) );
            m_jettyServer.start();
            m_state = new Started();
            notifyListeners( ServerEvent.STARTED );
        }

        public void stop()
        {
            // do nothing. already stopped
        }

        public void configure()
        {
            notifyListeners( ServerEvent.CONFIGURED );
        }

        public void addServlet( String alias, Servlet servlet )
        {
            //do nothing if server is not started
        }
    }

    private class Unconfigured extends Stopped
    {
        public void start()
        {
            throw new IllegalStateException( "server is not yet configured." );
        }

        public void configure()
        {
            m_state = new Stopped();
            notifyListeners( ServerEvent.CONFIGURED );
        }
    }

    // TODO verify synchronization 

}
