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
        expect( bundleContext.getProperty( "org.osgi.service.http.port" ) ).andReturn( "80" ).times( 2 );
        expect( bundleContext.getProperty( "org.osgi.service.http.port.secure" ) ).andReturn( "443" ).times( 2 );
        expect( bundleContext.getProperty( "org.osgi.service.http.enabled" ) ).andReturn( "true" ).times( 2 );
        expect( bundleContext.getProperty( "org.osgi.service.http.secure.enabled" ) ).andReturn( "true" ).times( 2 );
        expect( bundleContext.getProperty( "org.ops4j.pax.web.ssl.keystore" ) ).andReturn( "keystore" ).times( 2 );
        expect( bundleContext.getProperty( "org.ops4j.pax.web.ssl.password" ) ).andReturn( "password" ).times( 2 );
        expect( bundleContext.getProperty( "org.ops4j.pax.web.ssl.keypassword" ) ).andReturn( "keyPassword" )
            .times( 2 );

        replay( bundleContext );
        new SysPropsHttpServiceConfiguration( bundleContext );
        verify( bundleContext );
    }

    @Test
    public void constructorFlowWithNulls()
    {
        BundleContext bundleContext = createMock( BundleContext.class );
        expect( bundleContext.getProperty( "org.osgi.service.http.port" ) ).andReturn( null );
        expect( bundleContext.getProperty( "org.osgi.service.http.port.secure" ) ).andReturn( null );
        expect( bundleContext.getProperty( "org.osgi.service.http.enabled" ) ).andReturn( null );
        expect( bundleContext.getProperty( "org.osgi.service.http.secure.enabled" ) ).andReturn( null );
        expect( bundleContext.getProperty( "org.ops4j.pax.web.ssl.keystore" ) ).andReturn( null );
        expect( bundleContext.getProperty( "org.ops4j.pax.web.ssl.password" ) ).andReturn( null );
        expect( bundleContext.getProperty( "org.ops4j.pax.web.ssl.keypassword" ) ).andReturn( null );

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
