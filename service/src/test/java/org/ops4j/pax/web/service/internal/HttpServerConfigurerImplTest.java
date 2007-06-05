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
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

public class HttpServerConfigurerImplTest
{
    private ServerController m_serverController;
    private HttpServiceConfiguration m_configuration;
    private HttpServiceConfigurerImpl m_underTest;

    @Before
    public void setUp()
    {
        m_serverController = createMock( ServerController.class );
        m_configuration = createMock( HttpServiceConfiguration.class );
        m_underTest = new HttpServiceConfigurerImpl( m_serverController );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullServer()
    {
        new HttpServiceConfigurerImpl( null );
    }

    @Test
    public void configureBeforeServerStarted()
    {
       // prepare
        m_serverController.configure( m_configuration );
        expect( m_serverController.isStarted() ).andReturn( false );
        m_serverController.start();
        replay( m_serverController );
        // run
        m_underTest.configure( m_configuration );
        // verify
        verify ( m_serverController );
    }

    @Test
    public void configureAfterServerStarted()
    {
       // prepare
        m_serverController.configure( m_configuration );
        expect( m_serverController.isStarted() ).andReturn( true );
        replay( m_serverController );
        // run
        m_underTest.configure( m_configuration );
        // verify
        verify ( m_serverController );
    }

}
