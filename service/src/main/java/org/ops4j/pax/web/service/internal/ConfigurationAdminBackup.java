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

import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;

class ConfigurationAdminBackup
    implements FrameworkListener
{
    private BundleContext m_bundleContext;
    private ServerManager m_manager;

    public ConfigurationAdminBackup( final BundleContext bundleContext, final ServerManager manager )
    {
        m_bundleContext = bundleContext;
        m_manager = manager;
    }

    public void frameworkEvent( FrameworkEvent frameworkEvent )
    {
        switch( frameworkEvent.getType() )
        {
            case FrameworkEvent.STARTED:
                maybeActAsConfigurationAdmin();
                break;
        }
    }

    private void maybeActAsConfigurationAdmin()
    {
        ServiceReference reference = m_bundleContext.getServiceReference( ConfigurationAdmin.class.getName() );
        if ( reference == null )
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
