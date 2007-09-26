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

import static org.easymock.EasyMock.*;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class SysPropsHttpServiceConfigurationTest
{

    @Test
    public void constructorFlow()
    {
        BundleContext bundleContext = createMock( BundleContext.class );
        expect( bundleContext.getProperty( "org.osgi.service.http.port" ) ).andReturn( null );
        expect( bundleContext.getProperty( "org.osgi.service.http.port.secure" ) ).andReturn( null );
        expect( bundleContext.getProperty( "org.osgi.service.http.enabled" ) ).andReturn( "false" );
        expect( bundleContext.getProperty( "org.osgi.service.http.secure.enabled" ) ).andReturn( "false" );
        expect( bundleContext.getProperty( "org.ops4j.pax.web.ssl.keystore" ) ).andReturn( "keystore" );
        expect( bundleContext.getProperty( "org.ops4j.pax.web.ssl.password" ) ).andReturn( "password" );
        expect( bundleContext.getProperty( "org.ops4j.pax.web.ssl.keypassword" ) ).andReturn( "keyPassword" );

        replay( bundleContext );
        new SysPropsHttpServiceConfiguration( bundleContext );
        verify( bundleContext );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullParameter()
    {
        new SysPropsHttpServiceConfiguration( null );
    }

}
