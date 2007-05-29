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
import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

public class ServerManagerImplTest
{

    private BundleContext m_bundleContext;
    private ServiceRegistration m_registration;
    private ServerManagerImpl m_underTest;

    @Before
    public void setUp()
    {
        m_bundleContext = createMock( BundleContext.class );
        m_registration = createMock( ServiceRegistration.class );
        m_underTest = new ServerManagerImpl( m_bundleContext );
    }

    @Test
    public void start()
        throws Exception
    {
        expect( m_bundleContext.registerService(
            eq( ManagedService.class.getName() ) ,
            eq( m_underTest ) ,
            (Dictionary) notNull() 
        ) ).andReturn( m_registration );
        replay( m_bundleContext );
        m_underTest.start();
        verify( m_bundleContext );
    }

}
