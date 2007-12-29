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

import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.ops4j.pax.web.service.ConfigAdminConfigurationSynchronizer;
import org.ops4j.pax.web.service.DefaultHttpServiceConfiguration;
import org.ops4j.pax.web.service.HttpServiceConfigurer;
import org.ops4j.pax.web.service.SysPropsHttpServiceConfiguration;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.model.ServiceModel;
import org.ops4j.pax.web.service.internal.util.JCLLogger;

public class Activator
    implements BundleActivator
{

    private static final Log LOG = LogFactory.getLog( Activator.class );

    private ServerController m_serverController;
    private ServiceRegistration m_httpServiceFactoryReg;
    private ServiceRegistration m_httpServiceServerReg;
    private ServiceModel m_serviceModel;

    public Activator()
    {
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
        createHttpServiceConfigurer( bundleContext );
        createHttpServiceFactory( bundleContext );
        LOG.info( "Started pax http service" );
    }

    public void stop( final BundleContext bundleContext )
        throws Exception
    {
        LOG.info( "Stopping pax http service" );
        if( m_httpServiceServerReg != null )
        {
            m_httpServiceServerReg.unregister();
            m_httpServiceServerReg = null;
        }
        if( m_httpServiceFactoryReg != null )
        {
            m_httpServiceFactoryReg.unregister();
            m_httpServiceFactoryReg = null;
        }
        if( m_serverController != null )
        {
            m_serverController.stop();
            m_serverController = null;
        }
        m_serviceModel = null;
        LOG.info( "Stopped pax http service" );
    }

    private void createHttpServiceFactory( final BundleContext bundleContext )
    {
        HttpServiceFactoryImpl httpServiceFactory = new HttpServiceFactoryImpl()
        {
            HttpService createService( final Bundle bundle )
            {
                return new HttpServiceProxy(
                    new HttpServiceStarted( bundle, m_serverController, m_serviceModel )
                );
            }
        };
        m_httpServiceFactoryReg = bundleContext.registerService(
            new String[]{ HttpService.class.getName(), WebContainer.class.getName() },
            httpServiceFactory,
            new Hashtable()
        );
    }

    private void createHttpServiceConfigurer( final BundleContext bundleContext )
    {
        HttpServiceConfigurer configurer = new HttpServiceConfigurerImpl( m_serverController );
        m_httpServiceServerReg = bundleContext.registerService(
            HttpServiceConfigurer.class.getName(), configurer, new Hashtable()
        );
        new ConfigAdminConfigurationSynchronizer(
            bundleContext,
            configurer,
            new SysPropsHttpServiceConfiguration(
                bundleContext,
                new DefaultHttpServiceConfiguration()
            )
        );
    }

    private void createServerController()
    {
        m_serviceModel = new ServiceModel();
        m_serverController = new ServerControllerImpl(
            new JettyFactoryImpl( m_serviceModel )
        );
    }

}
