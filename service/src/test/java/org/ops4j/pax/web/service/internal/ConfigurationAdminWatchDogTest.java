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

import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.FrameworkEvent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;

public class ConfigurationAdminWatchDogTest
{

    private Bundle m_bundle;
    private BundleContext m_bundleContext;
    private ServiceReference m_configAdminRef;
    private ServerManager m_serverManager;
    private ConfigurationAdminWatchDog m_underTest;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_bundleContext = createMock( BundleContext.class );
        m_configAdminRef = createMock( ServiceReference.class );
        m_serverManager = createMock( ServerManager.class );
        m_underTest = new ConfigurationAdminWatchDog( m_bundleContext, m_serverManager );
    }

    @Test
    public void updateGetsCalledWhenNoConfigAdminAvailable()
        throws ConfigurationException
    {
        expect( m_bundleContext.getServiceReference( ConfigurationAdmin.class.getName() ) )
            .andReturn( null );
        m_bundleContext.addServiceListener( m_underTest );
        m_serverManager.updated( null );
        replay( m_bundleContext, m_serverManager );
        m_underTest.start();
        verify( m_bundleContext, m_serverManager );
    }

    @Test
    public void updateDoesNotGetCalledWhenConfigAdminAvailable()
    {
        expect( m_bundleContext.getServiceReference( ConfigurationAdmin.class.getName() ) )
            .andReturn( m_configAdminRef );
        m_bundleContext.addServiceListener( m_underTest );
        replay( m_bundleContext, m_serverManager );
        m_underTest.start();
        verify( m_bundleContext, m_serverManager );        
    }

}
