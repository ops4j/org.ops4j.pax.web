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

import java.util.Dictionary;
import org.osgi.service.cm.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ServerConfigurationImpl implements ServerConfiguration
{

    private static final Log m_logger = LogFactory.getLog( ServerConfigurationImpl.class );

    private final static int DEFAULT_HTTP_PORT = 8080;
    private final static int DEFAULT_HTTP_SECURE_PORT = 8443;

    private int m_httpPort;
    private int m_httpSecurePort;

    private boolean m_httpEnabled;
    private boolean m_httpSecureEnabled;

    public ServerConfigurationImpl( final Dictionary properties )
        throws ConfigurationException
    {
        m_httpPort = Integer.getInteger( "org.osgi.service.http.port", DEFAULT_HTTP_PORT );
        m_httpSecurePort = Integer.getInteger( "org.osgi.service.http.port.secure", DEFAULT_HTTP_SECURE_PORT );
        m_httpEnabled = true;
        m_httpSecureEnabled = false;

        // if not properties are available use the default values
        if ( properties == null)
        {
            return;
        }
        // else try to extract them from properties
        Object property;

        property = properties.get( HTTP_PORT ); 
        if ( property != null )
        {
            if ( property instanceof Integer) {
                m_httpPort = ((Integer) property).intValue(); 
            }
            else
            {
                throw new ConfigurationException( HTTP_PORT, "invalid value");
            }

        }
        property = properties.get( HTTP_SECURE_PORT ); 
        if ( property != null )
        {
            if ( property instanceof Integer) {
                m_httpSecurePort = ((Integer) property).intValue();
            }
            else
            {
                throw new ConfigurationException( HTTP_SECURE_PORT, "invalid value");
            }

        }

    }

    public int getHttpPort()
    {
        return m_httpPort;
    }

    public boolean isHttpEnabled()
    {
        return m_httpEnabled;
    }

    public int getHttpSecurePort()
    {
        return m_httpSecurePort;
    }

    public boolean isHttpSecureEnabled()
    {
        return m_httpSecureEnabled;
    }
    
}
