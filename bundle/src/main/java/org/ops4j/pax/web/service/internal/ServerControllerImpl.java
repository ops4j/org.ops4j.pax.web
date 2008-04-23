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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.model.ErrorPageModel;
import org.ops4j.pax.web.service.internal.model.EventListenerModel;
import org.ops4j.pax.web.service.internal.model.FilterModel;
import org.ops4j.pax.web.service.internal.model.ServletModel;

class ServerControllerImpl
    implements ServerController
{

    private static final Log LOG = LogFactory.getLog( ServerControllerImpl.class );

    private Configuration m_configuration;
    private State m_state;
    private final JettyFactory m_jettyFactory;
    private JettyServer m_jettyServer;
    private final Set<ServerListener> m_listeners;

    ServerControllerImpl( final JettyFactory jettyFactory )
    {
        m_jettyFactory = jettyFactory;
        m_configuration = null;
        m_state = new Unconfigured();
        m_listeners = new HashSet<ServerListener>();
    }

    public synchronized void start()
    {
        LOG.info( "starting server: " + this );
        m_state.start();
    }

    public synchronized void stop()
    {
        LOG.info( "stopping server: " + this );
        m_state.stop();
    }

    public synchronized void configure( final Configuration configuration )
    {
        LOG.info( "configuring server: " + this + " -> " + configuration );
        if( configuration == null )
        {
            throw new IllegalArgumentException( "configuration == null" );
        }
        m_configuration = configuration;
        m_state.configure();
    }

    public Configuration getConfiguration()
    {
        return m_configuration;
    }

    public void addListener( ServerListener listener )
    {
        if( listener == null )
        {
            throw new IllegalArgumentException( "listener == null" );
        }
        m_listeners.add( listener );
    }

    public void addServlet( final ServletModel model )
    {
        m_state.addServlet( model );
    }

    public void removeServlet( final ServletModel model )
    {
        m_state.removeServlet( model );
    }

    public boolean isStarted()
    {
        return m_state instanceof Started;
    }

    public boolean isConfigured()
    {
        return !( m_state instanceof Unconfigured );
    }

    public void addEventListener( final EventListenerModel eventListenerModel )
    {
        m_state.addEventListener( eventListenerModel );
    }

    public void removeEventListener( final EventListenerModel eventListenerModel )
    {
        m_state.removeEventListener( eventListenerModel );
    }

    public void removeContext( HttpContext httpContext )
    {
        m_state.removeContext( httpContext );
    }

    public void addFilter( final FilterModel filterModel )
    {
        m_state.addFilter( filterModel );
    }

    public void removeFilter( final FilterModel filterModel )
    {
        m_state.removeFilter( filterModel );
    }

    public void addErrorPage( final ErrorPageModel model )
    {
        m_state.addErrorPage( model );
    }

    public void removeErrorPage( final ErrorPageModel model )
    {
        m_state.removeErrorPage( model );
    }

    void notifyListeners( ServerEvent event )
    {
        for( ServerListener listener : m_listeners )
        {
            listener.stateChanged( event );
        }
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( ServerControllerImpl.class.getSimpleName() )
            .append( "{" )
            .append( "state=" )
            .append( m_state )
            .append( "}" )
            .toString();
    }

    private interface State
    {

        void start();

        void stop();

        void configure();

        void addServlet( ServletModel model );

        void removeServlet( ServletModel model );

        void addEventListener( EventListenerModel eventListenerModel );

        void removeEventListener( EventListenerModel eventListenerModel );

        void removeContext( HttpContext httpContext );

        void addFilter( FilterModel filterModel );

        void removeFilter( FilterModel filterModel );

        void addErrorPage( ErrorPageModel model );

        void removeErrorPage( ErrorPageModel model );
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

        public void addServlet( final ServletModel model )
        {
            m_jettyServer.addServlet( model );
        }

        public void removeServlet( final ServletModel model )
        {
            m_jettyServer.removeServlet( model );
        }

        public void addEventListener( EventListenerModel eventListenerModel )
        {
            m_jettyServer.addEventListener( eventListenerModel );
        }

        public void removeEventListener( EventListenerModel eventListenerModel )
        {
            m_jettyServer.removeEventListener( eventListenerModel );
        }

        public void removeContext( HttpContext httpContext )
        {
            m_jettyServer.removeContext( httpContext );
        }

        public void addFilter( FilterModel filterModel )
        {
            m_jettyServer.addFilter( filterModel );
        }

        public void removeFilter( FilterModel filterModel )
        {
            m_jettyServer.removeFilter( filterModel );
        }

        public void addErrorPage( ErrorPageModel model )
        {
            m_jettyServer.addErrorPage( model );
        }

        public void removeErrorPage( ErrorPageModel model )
        {
            m_jettyServer.removeErrorPage( model );
        }

        @Override
        public String toString()
        {
            return "STARTED";
        }
    }

    private class Stopped implements State
    {

        public void start()
        {
            m_jettyServer = m_jettyFactory.createServer();
            for (String address : m_configuration.getListeningAddresses())
            {
                if( m_configuration.isHttpEnabled() )
                {
                    m_jettyServer.addConnector( m_jettyFactory.createConnector( m_configuration.getHttpPort(), address ) );
                }
                if( m_configuration.isHttpSecureEnabled() )
                {
                    final String sslPassword = m_configuration.getSslPassword();
                    final String sslKeyPassword = m_configuration.getSslKeyPassword();
                    if( sslPassword != null && sslKeyPassword != null )
                    {
                        m_jettyServer.addConnector(
                            m_jettyFactory.createSecureConnector(
                                m_configuration.getHttpSecurePort(),
                                m_configuration.getSslKeystore(),
                                sslPassword,
                                sslKeyPassword,
                                address,
                                m_configuration.getSslKeystoreType()
                            )
                        );
                    }
                    else
                    {
                        LOG.warn( "SSL pasword and SSL keystore pasword must be set in order to enable SSL." );
                        LOG.warn( "SSL connector will not be started" );
                    }
                }
            }
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put( "javax.servlet.context.tempdir", m_configuration.getTemporaryDirectory() );
            m_jettyServer.configureContext( attributes, m_configuration.getSessionTimeout() );
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

        public void addServlet( final ServletModel model )
        {
            // do nothing if server is not started
        }

        public void removeServlet( final ServletModel model )
        {
            // do nothing if server is not started
        }

        public void addEventListener( EventListenerModel eventListenerModel )
        {
            // do nothing if server is not started
        }

        public void removeEventListener( EventListenerModel eventListenerModel )
        {
            // do nothing if server is not started
        }

        public void removeContext( HttpContext httpContext )
        {
            // do nothing if server is not started
        }

        public void addFilter( FilterModel filterModel )
        {
            // do nothing if server is not started
        }

        public void removeFilter( FilterModel filterModel )
        {
            // do nothing if server is not started
        }

        public void addErrorPage( ErrorPageModel model )
        {
            // do nothing if server is not started
        }

        public void removeErrorPage( ErrorPageModel model )
        {
            // do nothing if server is not started
        }

        @Override
        public String toString()
        {
            return "STOPPED";
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
            ServerControllerImpl.this.start();
        }

        @Override
        public String toString()
        {
            return "UNCONFIGURED";
        }
    }

}
