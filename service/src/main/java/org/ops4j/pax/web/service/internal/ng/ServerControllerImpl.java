package org.ops4j.pax.web.service.internal.ng;

import java.util.Set;
import java.util.HashSet;
import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.web.service.HttpServiceConfiguration;
import org.mortbay.jetty.Handler;

public class ServerControllerImpl implements ServerController
{

    private static final Log m_logger = LogFactory.getLog( ServerControllerImpl.class );
    
    private HttpServiceConfiguration m_configuration;
    private State m_state;
    private JettyFactory m_jettyFactory;
    private JettyServer m_jettyServer;
    private Set<ServerListener> m_listeners;
    private Handler m_handler;

    public ServerControllerImpl( final JettyFactory jettyFactory, final Handler handler )
    {
        m_jettyFactory = jettyFactory;
        m_configuration = null;
        m_state = new Unconfigured();
        m_listeners = new HashSet<ServerListener>();
        m_handler = handler;
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
        Assert.notNull( "alias == null", alias);
        Assert.notEmpty( "alias is empty", alias );
        Assert.notNull( "servlet == null", servlet);
        m_state.addServlet( alias, servlet);
    }

    public void removeServlet( String alias )
    {
        Assert.notNull( "alias == null", alias);
        Assert.notEmpty( "alias is empty", alias );
        m_state.removeServlet( alias );
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

    private interface State
    {
        void start();
        void stop();
        void configure();
        void addServlet( String alias, Servlet servlet );
        void removeServlet ( String alias );
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
            m_jettyServer.addServlet( alias, servlet);
        }

        public void removeServlet( final String alias )
        {
            m_jettyServer.removeServlet( alias );
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
            m_jettyServer.addContext( m_handler );
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
            // do nothing if server is not started
        }

        public void removeServlet( String alias )
        {
            // do nothing if server is not started
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
