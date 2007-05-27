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

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

public class HttpServiceFactoryImplTest
{

    private Bundle m_bundle;
    private ServiceRegistration m_serviceReg;
    private HttpServiceFactoryImpl m_factoryUnderTest;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_serviceReg = createMock( ServiceRegistration.class );
        m_factoryUnderTest = new HttpServiceFactoryImpl();
    }

    @Test
    public void getService()
    {
        Object returnedHttpService = m_factoryUnderTest.getService( m_bundle, m_serviceReg );
        assertNotNull( "returned HttpService is not null", returnedHttpService );
        assertEquals( HttpServiceImpl.class, returnedHttpService.getClass() );
    }

    public void ungetService()
    {
        // TODO complete the test
         m_factoryUnderTest.ungetService( m_bundle, m_serviceReg, null );
    }

}
