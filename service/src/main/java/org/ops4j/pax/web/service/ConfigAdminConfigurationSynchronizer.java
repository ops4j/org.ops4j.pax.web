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

import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.web.service.internal.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConfigAdminConfigurationSynchronizer
{

    private static final Log m_logger = LogFactory.getLog( ConfigAdminConfigurationSynchronizer.class );

    public static final String PID = HttpService.class.getName();

    public static final String PROPERTY_HTTP_PORT = "org.osgi.service.http.port";
    public static final String PROPERTY_HTTP_SECURE_PORT = "org.osgi.service.http.port.secure";
    public static final String PROPERTY_HTTP_ENABLED = "org.osgi.service.http.enabled";
    public static final String PROPERTY_HTTP_SECURE_ENABLED = "org.osgi.service.http.secure.enabled";

    private SimpleHttpServiceConfiguration m_httpServiceConfiguration;
    private BundleContext m_bundleContext;
    private HttpServiceConfigurer m_httpServiceConfigurer;

    public ConfigAdminConfigurationSynchronizer( final BundleContext bundleContext )
    {
        this( bundleContext, null , null );
    }

    public ConfigAdminConfigurationSynchronizer(
        final BundleContext bundleContext,
        final HttpServiceConfigurer httpServiceConfigurer )
    {
        this( bundleContext, httpServiceConfigurer, null );
    }

    public ConfigAdminConfigurationSynchronizer(
        final BundleContext bundleContext,
        final HttpServiceConfiguration httpServiceConfiguration )
    {
        this( bundleContext, null , httpServiceConfiguration );
    }

    public ConfigAdminConfigurationSynchronizer(
        final BundleContext bundleContext,
        final HttpServiceConfigurer httpServiceConfigurer,
        final HttpServiceConfiguration httpServiceConfiguration )
    {
        Assert.notNull( "bundleContext == null", bundleContext );
        m_bundleContext = bundleContext;
        m_httpServiceConfiguration = new SimpleHttpServiceConfiguration( httpServiceConfiguration );

        // make us known as a configuration admin managed service
        Dictionary< String, Object > properties = new Hashtable< String, Object >();
        properties.put( Constants.SERVICE_PID , PID );
        bundleContext.registerService( ManagedService.class.getName(), new ConfigurationTarget(), properties );

        m_httpServiceConfigurer = httpServiceConfigurer;
        // look for the http service configurer if we do not have one
       if ( m_httpServiceConfigurer == null )
        {
            new ServiceTracker( bundleContext,
                                HttpServiceConfigurer.class.getName(),
                                new HttpServiceConfigurerTracker() )
                .open();
        }

        // if we have a configurer as passed in or already found
        if ( m_httpServiceConfigurer != null )
        {
            m_httpServiceConfigurer.configure( m_httpServiceConfiguration );
        }
    }

    private class ConfigurationTarget implements ManagedService
    {

        public void updated( final Dictionary dictionary )
            throws ConfigurationException
        {
            if ( m_logger.isInfoEnabled() )
            {
                m_logger.info( "configuration has been updated to: " + dictionary );
            }
            m_httpServiceConfiguration.setHttpPort( null );
            m_httpServiceConfiguration.setHttpSecurePort( null );
            m_httpServiceConfiguration.setHttpEnabled( null );
            m_httpServiceConfiguration.setHttpSecureEnabled( null );
            
            if ( dictionary != null )
            {
                try
                {
                    m_httpServiceConfiguration.setHttpPort( (Integer) dictionary.get( PROPERTY_HTTP_PORT ) );
                }
                catch( ClassCastException ignore )
                {
                    // use default value
                    m_logger.warn( "Reading configuration property " + PROPERTY_HTTP_PORT + " has failed" );
                }
                try
                {
                    m_httpServiceConfiguration.setHttpSecurePort( (Integer) dictionary.get( PROPERTY_HTTP_SECURE_PORT ) );
                }
                catch( ClassCastException ignore )
                {
                    // use default value
                    m_logger.warn( "Reading configuration property " + PROPERTY_HTTP_SECURE_PORT + " has failed" );
                }
                try
                {
                    m_httpServiceConfiguration.setHttpEnabled( (Boolean) dictionary.get( PROPERTY_HTTP_ENABLED ) );
                }
                catch( ClassCastException ignore )
                {
                    // use default value
                    m_logger.warn( "Reading configuration property " + PROPERTY_HTTP_ENABLED + " has failed" );
                }
                try
                {
                   m_httpServiceConfiguration.setHttpSecureEnabled( (Boolean) dictionary.get( PROPERTY_HTTP_SECURE_ENABLED ) );
                }
                catch( ClassCastException ignore )
                {
                    // use default value
                    m_logger.warn( "Reading configuration property " + PROPERTY_HTTP_SECURE_ENABLED + " has failed" );
                }
            }
            if ( m_httpServiceConfigurer != null )
            {
                m_httpServiceConfigurer.configure( m_httpServiceConfiguration );
            }
        }

    }

    private class HttpServiceConfigurerTracker implements ServiceTrackerCustomizer
    {

        public Object addingService( final ServiceReference serviceReference )
        {
            m_httpServiceConfigurer = (HttpServiceConfigurer) m_bundleContext.getService( serviceReference );
            if ( m_logger.isInfoEnabled() )
            {
                m_logger.info( "using http service configurator " + m_httpServiceConfiguration );
            }            
            m_httpServiceConfigurer.configure( m_httpServiceConfiguration );
            return m_httpServiceConfigurer;
        }

        public void modifiedService( final ServiceReference serviceReference, final Object object )
        {
            // do nothing
        }

        public void removedService( final ServiceReference serviceReference, final Object object )
        {
            m_bundleContext.ungetService( serviceReference );
        }
    }

    // TODO checkout synchronization between availability of configurer, managed service configuration updated and .configure

}
