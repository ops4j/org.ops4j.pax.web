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
import org.ops4j.pax.web.service.ExtendedHttpService;
import org.ops4j.pax.web.service.HttpServiceConfigurer;
import org.ops4j.pax.web.service.SysPropsHttpServiceConfiguration;
import org.ops4j.pax.web.service.internal.model.ServiceModel;

public class Activator
    implements BundleActivator
{

    private static final Log m_logger = LogFactory.getLog( Activator.class );

    private ServerController m_serverController;
    private BundleContext m_bundleContext;
    private ServiceRegistration m_httpServiceFactoryReg;
    private ServiceRegistration m_httpServiceServerReg;
    private ServiceModel m_serviceModel;

    public Activator()
    {
        Thread.currentThread().setContextClassLoader( Activator.class.getClassLoader() );
        JCLLogger.init();
    }

    public void start( final BundleContext bundleContext )
        throws Exception
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Starting pax http service" );
        }
        m_bundleContext = bundleContext;
        createServerController();
        createHttpServiceConfigurer();
        createHttpServiceFactory();
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Started pax http service" );
        }
    }

    public void stop( BundleContext bundleContext )
        throws Exception
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Stoping pax http service" );
        }
        m_httpServiceServerReg.unregister();
        m_httpServiceFactoryReg.unregister();
        m_serverController.stop();
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Stopped pax http service" );
        }
    }

    private void createHttpServiceFactory()
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
        m_httpServiceFactoryReg = m_bundleContext.registerService(
            new String[]{ HttpService.class.getName(), ExtendedHttpService.class.getName() },
            httpServiceFactory,
            new Hashtable()
        );
    }

    private void createHttpServiceConfigurer()
    {
        HttpServiceConfigurer configurer = new HttpServiceConfigurerImpl( m_serverController );
        m_httpServiceServerReg = m_bundleContext.registerService(
            HttpServiceConfigurer.class.getName(), configurer, new Hashtable()
        );
        new ConfigAdminConfigurationSynchronizer(
            m_bundleContext,
            configurer,
            new SysPropsHttpServiceConfiguration(
                m_bundleContext,
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
