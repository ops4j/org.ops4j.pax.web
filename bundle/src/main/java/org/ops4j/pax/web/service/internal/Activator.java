/* Copyright 2007 Niclas Hedhman.
 * Copyright 2007 Alin Dreghiciu.
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

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.pax.web.service.WebContainer;
import static org.ops4j.pax.web.service.WebContainerConstants.*;
import org.ops4j.pax.web.service.internal.model.ServerModel;
import org.ops4j.pax.web.service.internal.util.JCLLogger;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;

public class Activator
    implements BundleActivator
{

    private static final Log LOG = LogFactory.getLog( Activator.class );

    private final Lock m_lock;
    private ServerController m_serverController;
    private ServerModel m_serverModel;
    private ServiceRegistration m_httpServiceFactoryReg;
    private Dictionary m_httpServiceFactoryProps;

    public Activator()
    {
        m_lock = new ReentrantLock();
        final ClassLoader backup = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( Activator.class.getClassLoader() );
        JCLLogger.init();
        Thread.currentThread().setContextClassLoader( backup );
    }

    public void start( final BundleContext bundleContext )
        throws Exception
    {
        LOG.info( "Starting pax http service" );
        createServerController();
        createManagedService( bundleContext );
        createHttpServiceFactory( bundleContext );
        LOG.info( "Started pax http service" );
    }

    public void stop( final BundleContext bundleContext )
        throws Exception
    {
        LOG.info( "Stopping pax http service" );
        if( m_serverController != null )
        {
            m_serverController.stop();
            m_serverController = null;
        }
        m_serverModel = null;
        LOG.info( "Stopped pax http service" );
    }

    private void createHttpServiceFactory( final BundleContext bundleContext )
    {
        m_httpServiceFactoryReg = bundleContext.registerService(
            new String[]{ HttpService.class.getName(), WebContainer.class.getName() },
            new HttpServiceFactoryImpl()
            {
                HttpService createService( final Bundle bundle )
                {
                    return new HttpServiceProxy(
                        new HttpServiceStarted( bundle, m_serverController, m_serverModel )
                    );
                }
            },
            m_httpServiceFactoryProps
        );
    }

    private void createServerController()
    {
        m_serverModel = new ServerModel();
        m_serverController = new ServerControllerImpl(
            new JettyFactoryImpl( m_serverModel )
        );
    }

    /**
     * Registers a managed service to listen on configuration updates.
     *
     * @param bundleContext bundle context to use for registration
     */
    private void createManagedService( final BundleContext bundleContext )
    {
        final ManagedService managedService = new ManagedService()
        {
            /**
             * Sets the resolver on sever controller.
             *
             * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
             */
            public void updated( final Dictionary config )
                throws ConfigurationException
            {
                try
                {
                    m_lock.lock();
                    final PropertyResolver resolver;
                    if( config == null )
                    {
                        resolver =
                            new BundleContextPropertyResolver(
                                bundleContext,
                                new DefaultPropertyResolver()
                            );
                    }
                    else
                    {
                        resolver =
                            new DictionaryPropertyResolver(
                                config,
                                new BundleContextPropertyResolver(
                                    bundleContext,
                                    new DefaultPropertyResolver()
                                )
                            );
                    }
                    final ConfigurationImpl configuration = new ConfigurationImpl( resolver );
                    m_serverController.configure( configuration );
                    // maybe we should do this only once configured
                    determineServiceProperties(
                        config, configuration, m_serverController.getHttpPort(), m_serverController.getHttpSecurePort() 
                    );
                    if( m_httpServiceFactoryReg != null )
                    {
                        m_httpServiceFactoryReg.setProperties( m_httpServiceFactoryProps );
                    }
                }
                finally
                {
                    m_lock.unlock();
                }
            }

        };
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put( Constants.SERVICE_PID, PID );
        bundleContext.registerService(
            ManagedService.class.getName(),
            managedService,
            props
        );
        try
        {
            m_lock.lock();
            if( !m_serverController.isConfigured() )
            {
                try
                {
                    managedService.updated( null );
                }
                catch( ConfigurationException ignore )
                {
                    // this should never happen
                    LOG.error( "Internal error. Cannot set initial configuration resolver.", ignore );
                }
            }
        }
        finally
        {
            m_lock.unlock();
        }
    }

    private void determineServiceProperties( final Dictionary managedConfig,
                                             final Configuration config,
                                             final Integer httpPort,
                                             final Integer httpSecurePort )
    {
        final Hashtable<String, Object> toPropagate = new Hashtable<String, Object>();
        // first store all configuration properties as received via managed service
        if( managedConfig != null )
        {

        }

        // then add/replace configuration properties
        setProperty( toPropagate, PROPERTY_HTTP_ENABLED, config.isHttpEnabled() );
        setProperty( toPropagate, PROPERTY_HTTP_PORT, config.getHttpPort() );
        setProperty( toPropagate, PROPERTY_HTTP_SECURE_ENABLED, config.isHttpEnabled() );
        setProperty( toPropagate, PROPERTY_HTTP_SECURE_PORT, config.getHttpSecurePort() );
        setProperty( toPropagate, PROPERTY_HTTP_USE_NIO, config.useNIO() );
        setProperty( toPropagate, PROPERTY_SSL_CLIENT_AUTH_NEEDED, config.isClientAuthNeeded() );
        setProperty( toPropagate, PROPERTY_SSL_CLIENT_AUTH_WANTED, config.isClientAuthWanted() );
        setProperty( toPropagate, PROPERTY_SSL_KEYSTORE, config.getSslKeystore() );
        setProperty( toPropagate, PROPERTY_SSL_KEYSTORE_TYPE, config.getSslKeystoreType() );
        //store( toPropagate, PROPERTY_SSL_PASSWORD, config.getSslPassword());
        setProperty( toPropagate, PROPERTY_SSL_PASSWORD, null );
        //store( toPropagate, PROPERTY_SSL_KEYPASSWORD, config.getSslKeyPassword());
        setProperty( toPropagate, PROPERTY_SSL_KEYPASSWORD, null );
        setProperty( toPropagate, PROPERTY_TEMP_DIR, config.getTemporaryDirectory() );
        setProperty( toPropagate, PROPERTY_SESSION_TIMEOUT, config.getSessionTimeout() );
        setProperty( toPropagate, PROPERTY_LISTENING_ADDRESSES, config.getListeningAddresses() );

        // then replace ports
        setProperty( toPropagate, PROPERTY_HTTP_PORT, httpPort );
        setProperty( toPropagate, PROPERTY_HTTP_SECURE_PORT, httpSecurePort );

        m_httpServiceFactoryProps = toPropagate;
    }

    private void setProperty( final Hashtable<String, Object> properties,
                              final String name,
                              final Object value )
    {
        if( value != null )
        {
            if( value instanceof File )
            {
                properties.put( name, ( (File) value ).getAbsolutePath() );
            }
            else if( value instanceof Object[] )
            {
                properties.put( name, join( ",", (Object[]) value ) );
            }
            else
            {
                properties.put( name, value.toString() );
            }
        }
        else
        {
            properties.remove( name );
        }
    }

    private static String join( String token, Object[] array )
    {
        if( array == null )
        {
            return null;
        }
        if( array.length == 0 )
        {
            return "";
        }
        StringBuffer sb = new StringBuffer();

        for( int x = 0; x < ( array.length - 1 ); x++ )
        {
            if( array[ x ] != null )
            {
                sb.append( array[ x ].toString() );
            }
            else
            {
                sb.append( "null" );
            }
            sb.append( token );
        }
        sb.append( array[ array.length - 1 ] );

        return ( sb.toString() );
    }

}
