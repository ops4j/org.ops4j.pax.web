/*  Copyright 2007 Niclas Hedhman.
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

public class Activator
    implements BundleActivator
{

    private static final Log m_logger = LogFactory.getLog( HttpServiceFactoryImpl.class );

    private ServiceRegistration m_serviceRegistration;
    private ServerManager m_serverManager;
    private BundleContext m_bundleContext;
    private ConfigurationAdminWatchDog m_watchDog;

    public void start( BundleContext bundleContext )
        throws Exception
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Starting pax http service" );
        }
        m_bundleContext = bundleContext;
        // start the server manager
        m_serverManager = new ServerManagerImpl( bundleContext );
        // start the watchdog for configuration admin service
        m_watchDog = new ConfigurationAdminWatchDog( m_bundleContext, m_serverManager );
        m_watchDog.start();
        // register the http service
        Dictionary properties = new Hashtable();
        m_serviceRegistration = bundleContext.registerService(
                HttpService.class.getName(),
                new HttpServiceFactoryImpl( m_serverManager ),
                properties );
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
        m_watchDog.stop();
        m_serviceRegistration.unregister();
        m_serverManager.stop();
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Stoped pax http service" );
        }
    }

}
