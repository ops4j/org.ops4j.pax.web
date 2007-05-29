/*  Copyright 2007 Alin Dreghiciu.
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
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.bio.SocketConnector;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Bundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ServerManagerImpl
    implements ServerManager
{

    private static final Log m_logger = LogFactory.getLog( ServerManager.class );

    private BundleContext m_bundleContext;
    private ServiceRegistration m_registration;
    private Dictionary<String,Object> m_properties;

    private Server m_server;
    private DefaultHandler m_defHandler;
    private OsgiHandlerImpl m_mainHandler;
    private Context m_rootContext;

    public ServerManagerImpl( BundleContext bundleContext)
    {
        m_bundleContext = bundleContext;
    }

    public void start()
        throws Exception
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "starting " + this );
        }
        m_server = new Server();
        m_mainHandler = new OsgiHandlerImpl();
        m_rootContext = new Context( m_server, "/" );
        m_rootContext.addHandler( m_mainHandler );

        m_defHandler = new DefaultHandler();
        m_server.addHandler( m_defHandler );

        m_properties = new Hashtable<String,Object>();
        m_properties.put( Constants.SERVICE_PID, PID );

        m_server.start();

        m_registration = m_bundleContext.registerService(
            ManagedService.class.getName(),
            this,
            m_properties
        );
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "started " + this );
        }
    }

    public void updated( final Dictionary dictionary )
        throws ConfigurationException
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Updating configuration to: " + dictionary );
        }
        ServerConfiguration config = new ServerConfigurationImpl( dictionary );

        // TODO couldn't be done more efficient, as for example to check if is not the same settings
        Connector[] connectors = m_server.getConnectors();
        if ( connectors != null ) {
            for ( Connector connector : connectors )
            {
                m_server.removeConnector( connector );
            }            
        }

        if ( config.isHttpEnabled() )
        {
            Connector httpPort = new SocketConnector();
            httpPort.setPort( config.getHttpPort() );
            m_server.addConnector( httpPort );
            try
            {
                httpPort.start();
            }
            catch( Exception e )
            {
                throw new ConfigurationException( ServerConfiguration.HTTP_PORT, "invalid port", e );
            }
        }

        if ( config.isHttpSecureEnabled() )
        {
            Connector httpPort = new SocketConnector();
            httpPort.setPort( config.getHttpSecurePort() );
            m_server.addConnector( httpPort );
            try
            {
                httpPort.start();
            }
            catch( Exception e )
            {
                throw new ConfigurationException( ServerConfiguration.HTTP_SECURE_PORT, "invalid port", e );
            }
        }
        m_registration.setProperties( m_properties );
    }

    public void stop()
        throws Exception
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "stoping " + this );
        }
        m_properties = null;
        if ( m_registration != null)
        {
            m_registration.unregister();
        }
        m_server.stop();
        m_server.destroy();
                if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "stoped " + this );
        }
    }

    public HttpServiceImpl createHttpService( final Bundle bundle )
    {
        return new HttpServiceImpl( bundle, m_mainHandler );
    }
}
