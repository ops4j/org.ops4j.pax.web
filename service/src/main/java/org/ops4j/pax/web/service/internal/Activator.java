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
import org.ops4j.pax.web.service.HttpServiceConfigurer;
import org.ops4j.pax.web.service.SysPropsHttpServiceConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

public class Activator
    implements BundleActivator
{

    private static final Log m_logger = LogFactory.getLog( Activator.class );

    private ServerController m_serverController;
    private HttpServiceFactoryImpl m_httpServiceFactory;
    private BundleContext m_bundleContext;
    private ServiceRegistration m_httpServiceFactoryReg;
    private ServiceRegistration m_httpServiceServerReg;
    private RegistrationsCluster m_registrationsCluster;

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
            m_logger.info( "Stoped pax http service" );
        }
    }

    private void createHttpServiceFactory()
    {
        m_httpServiceFactory = new HttpServiceFactoryImpl() {
            HttpService createService( final Bundle bundle)
            {
                return new HttpServiceImpl( bundle, m_serverController, m_registrationsCluster.create() );
            }
        };
        m_httpServiceFactoryReg = m_bundleContext.registerService(
            HttpService.class.getName(), m_httpServiceFactory, new Hashtable() );
    }

    private void createHttpServiceConfigurer()
    {
        HttpServiceConfigurer configurer = new HttpServiceConfigurerImpl( m_serverController );
        m_httpServiceServerReg = m_bundleContext.registerService(
            HttpServiceConfigurer.class.getName(), configurer, new Hashtable() );
        configurer.configure( new SysPropsHttpServiceConfiguration() );
    }

    private void createServerController()
    {
        m_registrationsCluster = new RegistrationsClusterImpl();
        m_serverController = new ServerControllerImpl(
            new JettyFactoryImpl(),
            new HttpServiceHandler( m_registrationsCluster ) );
    }

}
