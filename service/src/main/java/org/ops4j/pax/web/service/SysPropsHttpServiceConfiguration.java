/* Copyright 2007 Alin Dreghiciu.
 * Copyright 2007 Toni Menzel
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.ops4j.pax.web.service.internal.Assert;
import org.ops4j.pax.web.service.internal.DelegatingHttpServiceConfiguration;

public class SysPropsHttpServiceConfiguration extends DelegatingHttpServiceConfiguration
{

    private static final Log m_logger = LogFactory.getLog( SysPropsHttpServiceConfiguration.class );

    private final static String PROPERTY_HTTP_PORT = "org.osgi.service.http.port";
    private final static String PROPERTY_HTTP_SECURE_PORT = "org.osgi.service.http.port.secure";
    private static final String PROPERTY_HTTP_ENABLED = "org.osgi.service.http.enabled";
    private static final String PROPERTY_HTTP_SECURE_ENABLED = "org.osgi.service.http.secure.enabled";
    private final static String PROPERTY_SSL_KEYSTORE = "org.ops4j.pax.web.ssl.keystore";
    private final static String PROPERTY_SSL_PASSWORD = "org.ops4j.pax.web.ssl.password";
    private final static String PROPERTY_SSL_KEYPASSWORD = "org.ops4j.pax.web.ssl.keypassword";
    private final static String PROPERTY_TEMP_DIR = "javax.servlet.context.tempdir";
    private final static String PROPERTY_SESSION_TIMEOUT = "org.ops4j.pax.web.session.timeout";

    public SysPropsHttpServiceConfiguration( final BundleContext bundleContext )
    {
        this( bundleContext, null );
    }

    public SysPropsHttpServiceConfiguration(
        final BundleContext bundleContext,
        final HttpServiceConfiguration httpServiceConfiguration )
    {
        super( httpServiceConfiguration );
        Assert.notNull( "bundleContext == null", bundleContext );
        try
        {
            if( bundleContext.getProperty( PROPERTY_HTTP_PORT ) != null )
            {
                m_httpPort = Integer.parseInt( bundleContext.getProperty( PROPERTY_HTTP_PORT ) );
            }
        }
        catch( Exception e )
        {
            m_logger.warn( "Reading property " + PROPERTY_HTTP_PORT + " has failed" );
        }

        try
        {
            if( bundleContext.getProperty( PROPERTY_HTTP_SECURE_PORT ) != null )
            {
                m_httpSecurePort = Integer.parseInt( bundleContext.getProperty( PROPERTY_HTTP_SECURE_PORT ) );
            }
        }
        catch( Exception e )
        {
            m_logger.warn( "Reading property " + PROPERTY_HTTP_SECURE_PORT + " has failed" );
        }
        if( bundleContext.getProperty( PROPERTY_HTTP_ENABLED ) != null )
        {
            m_httpEnabled = Boolean.valueOf( bundleContext.getProperty( PROPERTY_HTTP_ENABLED ) );
        }
        if( bundleContext.getProperty( PROPERTY_HTTP_SECURE_ENABLED ) != null )
        {
            m_httpSecureEnabled = Boolean.valueOf( bundleContext.getProperty( PROPERTY_HTTP_SECURE_ENABLED ) );
        }
        if( bundleContext.getProperty( PROPERTY_SSL_KEYSTORE ) != null )
        {
            m_sslKeystore = bundleContext.getProperty( PROPERTY_SSL_KEYSTORE );
        }
        if( bundleContext.getProperty( PROPERTY_SSL_PASSWORD ) != null )
        {
            m_sslPassword = bundleContext.getProperty( PROPERTY_SSL_PASSWORD );
        }
        if( bundleContext.getProperty( PROPERTY_SSL_KEYPASSWORD ) != null )
        {
            m_sslKeyPassword = bundleContext.getProperty( PROPERTY_SSL_KEYPASSWORD );
        }
        // resolve temporary directory
        try
        {
            String tempDirName = bundleContext.getProperty( PROPERTY_TEMP_DIR );
            if( tempDirName != null )
            {
                final File tempDir = new File( tempDirName );
                if( !tempDir.exists() )
                {
                    tempDir.mkdirs();
                }
                m_temporaryDirectory = tempDir;
            }
        }
        catch( Exception ignore )
        {
            m_logger.warn( "Reading property " + PROPERTY_TEMP_DIR + " has failed" );
        }
        // resolve session timeout
        try
        {
            if( bundleContext.getProperty( PROPERTY_SESSION_TIMEOUT ) != null )
            {
                m_sessionTimeout = Integer.parseInt( bundleContext.getProperty( PROPERTY_SESSION_TIMEOUT ) );
            }
        }
        catch( Exception e )
        {
            m_logger.warn( "Reading property " + PROPERTY_SESSION_TIMEOUT + " has failed" );
        }

    }

}
