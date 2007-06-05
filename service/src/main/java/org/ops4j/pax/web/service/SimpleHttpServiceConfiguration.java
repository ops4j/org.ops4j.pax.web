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

public class SimpleHttpServiceConfiguration
    implements HttpServiceConfiguration
{
    private final static int DEFAULT_HTTP_PORT = 8080;
    private final static int DEFAULT_HTTP_SECURE_PORT = 8443;

    private int m_httpPort = DEFAULT_HTTP_PORT;
    private int m_httpSecurePort = DEFAULT_HTTP_SECURE_PORT;

    private boolean m_httpEnabled = true;
    private boolean m_httpSecureEnabled = false;

    public SimpleHttpServiceConfiguration()
    {
        
    }

    public SimpleHttpServiceConfiguration(final HttpServiceConfiguration configuration)
    {
        if ( configuration != null )
        {
            setHttpPort( configuration.getHttpPort() );
            setHttpSecurePort( configuration.getHttpSecurePort() );
            setHttpEnabled( configuration.isHttpEnabled() );
            setHttpSecureEnabled( configuration.isHttpSecureEnabled() );
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

    public void setHttpPort( final int httpPort )
    {
        m_httpPort = httpPort;
    }

    public void setHttpSecurePort( final int httpSecurePort )
    {
        m_httpSecurePort = httpSecurePort;
    }

    public void setHttpEnabled( final boolean httpEnabled )
    {
        m_httpEnabled = httpEnabled;
    }

    public void setHttpSecureEnabled( final boolean httpSecureEnabled )
    {
        m_httpSecureEnabled = httpSecureEnabled;
    }    
}
