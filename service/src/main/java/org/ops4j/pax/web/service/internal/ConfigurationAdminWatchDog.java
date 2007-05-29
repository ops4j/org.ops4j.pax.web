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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;

class ConfigurationAdminWatchDog
    implements ServiceListener
{

    private static final Log m_logger = LogFactory.getLog( ConfigurationAdminWatchDog.class );

    private BundleContext m_bundleContext;
    private ServerManager m_manager;

    ConfigurationAdminWatchDog( final BundleContext bundleContext, final ServerManager manager )
    {
        m_bundleContext = bundleContext;
        m_manager = manager;
    }

    void start()
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "starting " + this );
        }
        // checkout if there is a config admin service
        maybeActAsConfigurationAdmin();
        // watch if the config admin get's unregistered in order to reconfigure the server
        m_bundleContext.addServiceListener( this );
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "started " + this );
        }
    }

    void stop()
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "stoping " + this );
        }
        m_bundleContext.removeServiceListener( this );
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "stoped " + this );
        }
    }

    public void serviceChanged( ServiceEvent serviceEvent )
    {
        if ( serviceEvent.getType() == ServiceEvent.UNREGISTERING )
        {
            String[] interfaces = (String[]) serviceEvent.getServiceReference().getProperty( Constants.OBJECTCLASS );
            for ( String interfaceName : interfaces )
            {
                if ( ConfigurationAdmin.class.getName().equals( interfaceName ) )
                {
                    try
                    {
                        m_manager.updated( null );
                    }
                    catch( ConfigurationException e )
                    {
                        // TODO shall this be ignored?
                    }
                }
            }
        }
    }

    private void maybeActAsConfigurationAdmin()
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "Searching for configuration admin service..." );
        }
        ServiceReference reference = m_bundleContext.getServiceReference( ConfigurationAdmin.class.getName() );
        if ( reference == null )
        {
            try
            {
                if( m_logger.isDebugEnabled() )
                {
                    m_logger.debug( "configuration admin service not found. trigger manual update." );
                }
                m_manager.updated( null );
            }
            catch( ConfigurationException e )
            {
                // TODO shall this be ignored?
            }
        }
        else {
            if( m_logger.isDebugEnabled() )
            {
                m_logger.debug( "configuration admin service found. nothing to do." );
            }
        }
    }

}
