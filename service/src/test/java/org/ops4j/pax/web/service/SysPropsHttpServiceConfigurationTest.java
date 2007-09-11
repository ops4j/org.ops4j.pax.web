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
package org.ops4j.pax.web.service;

import org.osgi.framework.BundleContext;
import org.junit.Before;
import org.junit.Test;
import org.easymock.EasyMock;

public class SysPropsHttpServiceConfigurationTest
{

    private SysPropsHttpServiceConfiguration m_underTest;
    private BundleContext m_bundleContext;

    @Before
    public void setUp()
    {
        m_bundleContext = EasyMock.createMock( BundleContext.class );
        m_underTest = new SysPropsHttpServiceConfiguration( m_bundleContext );
    }

    @Test
    public void constructorFlow()
    {
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullParameter()
    {
        new SysPropsHttpServiceConfiguration( null );
    }

}
