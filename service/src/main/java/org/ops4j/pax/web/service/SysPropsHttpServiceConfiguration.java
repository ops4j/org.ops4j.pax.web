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

    public SysPropsHttpServiceConfiguration( final BundleContext bundleContext )
    {
        this( bundleContext, null );    
    }

    public SysPropsHttpServiceConfiguration(
        final BundleContext bundleContext,
        final HttpServiceConfiguration httpServiceConfiguration)
    {
        super( httpServiceConfiguration );
        Assert.notNull( "bundleContext == null", bundleContext );
        try
        {
            if ( bundleContext.getProperty(PROPERTY_HTTP_PORT) != null )
            {
                m_httpPort = Integer.parseInt( bundleContext.getProperty( PROPERTY_HTTP_PORT ) );
            }
        }
        catch ( Exception e ) {
            m_logger.warn( "Reading property " + PROPERTY_HTTP_PORT + " has failed" );
        }

        try
        {
            if ( bundleContext.getProperty(PROPERTY_HTTP_SECURE_PORT) != null )
            {
                m_httpSecurePort = Integer.parseInt( bundleContext.getProperty( PROPERTY_HTTP_SECURE_PORT ) );
            }
        }
        catch ( Exception e ) {
            m_logger.warn( "Reading property " + PROPERTY_HTTP_SECURE_PORT + " has failed" );
        }

    }

}
