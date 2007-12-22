/* Copyright 2007 Alin Dreghiciu.
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
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Connector;

public class JettyFactoryImplTest
{

    private JettyFactoryImpl m_underTest;

    @Before
    public void setUp()
    {
        m_underTest = new JettyFactoryImpl( createMock( RegistrationsCluster.class ) );
    }

    @Test
    public void createServer()
    {
        assertNotNull( "server is not null", m_underTest.createServer() );
    }

    @Test
    public void createConnector()
    {
        Connector connector = m_underTest.createConnector( 80 );
        assertNotNull( "connector is not null", connector );
        assertEquals( 80, connector.getPort() );
    }

}
