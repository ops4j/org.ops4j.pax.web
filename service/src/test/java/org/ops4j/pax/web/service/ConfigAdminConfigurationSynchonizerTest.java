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

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import static org.ops4j.pax.web.service.Capture.*;

public class ConfigAdminConfigurationSynchonizerTest
{

    private BundleContext m_bundleContext;
    private ServiceRegistration m_serviceRegistration;
    private HttpServiceConfigurer m_httpServiceConfigurer;
    private HttpServiceConfiguration m_httpServiceConfiguration;
    private ServiceReference m_serviceReference;
    private Filter m_filter;

    @Before
    public void setUp()
    {
        m_bundleContext = createMock( BundleContext.class );
        m_serviceRegistration = createMock( ServiceRegistration.class );
        m_httpServiceConfigurer = createMock( HttpServiceConfigurer.class );
        m_httpServiceConfiguration = createMock( HttpServiceConfiguration.class );
        m_serviceReference = createMock( ServiceReference.class );
        m_filter = createMock( Filter.class );
    }

    @Test
    public void constructorFlowWithBundleContext()
        throws InvalidSyntaxException
    {
        // prepare
        expect(
            m_bundleContext.registerService( eq( ManagedService.class.getName() ), notNull(), (Dictionary) notNull() )
        )
            .andReturn( m_serviceRegistration );
        String filter = "(objectClass=org.ops4j.pax.web.service.HttpServiceConfigurer)";
        expect( m_bundleContext.createFilter( filter ) ).andReturn( m_filter );
        m_bundleContext.addServiceListener( (ServiceListener) notNull(), eq( filter ) );
        expect( m_bundleContext.getServiceReferences( HttpServiceConfigurer.class.getName(), null ) ).andReturn( null );
        replay( m_bundleContext, m_serviceReference, m_httpServiceConfigurer, m_serviceRegistration, m_filter );
        // execute
        new ConfigAdminConfigurationSynchronizer( m_bundleContext );
        // verify
        verify( m_bundleContext, m_serviceReference, m_httpServiceConfigurer, m_serviceRegistration, m_filter );
    }

    @Test
    public void constructorFlowWithHttpServiceConfigurer()
    {
        // prepare
        expect(
            m_bundleContext.registerService( eq( ManagedService.class.getName() ), notNull(), (Dictionary) notNull() )
        ).andReturn( m_serviceRegistration );
        m_httpServiceConfigurer.configure( (HttpServiceConfiguration) notNull() );
        replay( m_bundleContext, m_httpServiceConfigurer, m_serviceRegistration );
        // execute
        new ConfigAdminConfigurationSynchronizer( m_bundleContext, m_httpServiceConfigurer );
        // verify
        verify( m_bundleContext, m_httpServiceConfigurer, m_serviceRegistration );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundleContext1()
    {
        new ConfigAdminConfigurationSynchronizer( null, m_httpServiceConfigurer );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundleContext2()
    {
        new ConfigAdminConfigurationSynchronizer( null, m_httpServiceConfiguration );
    }

    @Test
    public void constructorWithNullHttpServiceConfiguration()
    {
        // allowed
        new ConfigAdminConfigurationSynchronizer( m_bundleContext, m_httpServiceConfigurer, null );
    }

    @Test
    public void tempDirAsStringURL()
        throws IOException, ConfigurationException
    {
        File tempDir = File.createTempFile( "tempDir", "" );
        tempDir.delete();
        tempDir = new File( tempDir.getAbsolutePath() );
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        assertTrue( "Temporary directory", tempDir.toURL().sameFile(
            tempDir( tempDir.toURL().toExternalForm() ).toURL()
        )
        );
    }

    @Test
    public void tempDirAsString()
        throws IOException, ConfigurationException
    {
        File tempDir = File.createTempFile( "tempDir", "" );
        tempDir.delete();
        tempDir = new File( tempDir.getAbsolutePath() );
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        assertEquals( "Temporary directory", tempDir, tempDir( tempDir.getAbsolutePath() ) );
    }

    @SuppressWarnings( "unchecked" )
    public File tempDir( Object tempDir )
        throws IOException, ConfigurationException
    {
        Capture<ManagedService> managedService = new Capture<ManagedService>();
        Capture<HttpServiceConfiguration> configuration = new Capture<HttpServiceConfiguration>();
        expect(
            m_bundleContext.registerService( eq( ManagedService.class.getName() ), capture( managedService ),
                                             (Dictionary) notNull()
            )
        ).andReturn( m_serviceRegistration );
        m_httpServiceConfigurer.configure( (HttpServiceConfiguration) notNull() );
        m_httpServiceConfigurer.configure( capture( configuration ) );

        replay( m_bundleContext, m_httpServiceConfigurer, m_serviceRegistration );
        new ConfigAdminConfigurationSynchronizer( m_bundleContext, m_httpServiceConfigurer, null );
        Dictionary props = new Hashtable();
        props.put( "javax.servlet.context.tempdir", tempDir );
        managedService.getCaptured().updated( props );
        verify( m_bundleContext, m_httpServiceConfigurer, m_serviceRegistration );
        return configuration.getCaptured().getTemporaryDirectory();
    }

}
