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
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.internal.model.ServiceBundleModel;
import org.ops4j.pax.web.service.internal.util.JCLLogger;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;

public class Activator
    implements BundleActivator
{

    private static final Log LOG = LogFactory.getLog( Activator.class );

    private final Lock m_lock;
    private ServerController m_serverController;
    private ServiceBundleModel m_serviceBundleModel;

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
        m_serviceBundleModel = null;
        LOG.info( "Stopped pax http service" );
    }

    private void createHttpServiceFactory( final BundleContext bundleContext )
    {
        bundleContext.registerService(
            new String[]{ HttpService.class.getName(), WebContainer.class.getName() },
            new HttpServiceFactoryImpl()
            {
                HttpService createService( final Bundle bundle )
                {
                    return new HttpServiceProxy(
                        new HttpServiceStarted( bundle, m_serverController, m_serviceBundleModel )
                    );
                }
            },
            new Hashtable()
        );
    }

    private void createServerController()
    {
        m_serviceBundleModel = new ServiceBundleModel();
        m_serverController = new ServerControllerImpl(
            new JettyFactoryImpl( m_serviceBundleModel )
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
                    m_serverController.configure( new ConfigurationImpl( resolver ) );
                }
                finally
                {
                    m_lock.unlock();
                }
            }

        };
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put( Constants.SERVICE_PID, WebContainerConstants.PID );
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

}
